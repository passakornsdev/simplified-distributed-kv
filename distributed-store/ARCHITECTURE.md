# Distributed Key-Value Store: Architecture

## 1. Goals and Scope

A simplified key-value store inspired by Dynamo (DeCandia et al., 2007). It is compared against the
monolithic baseline in `../monolith-store` on throughput, latency, and request success rate under
increasing load and node failure.

**Implementation status:** The module provides in-memory `GET`/`PUT`, primary-owner routing through
a static consistent-hash ring, internal replica APIs, and peer HTTP clients. Replication, quorum
coordination, and Docker Compose deployment are not implemented yet.

**In scope**
- `GET` and `PUT` only, with the same external contract as the monolith (fair comparison).
- Data partitioning with consistent hashing.
- Replication with configurable quorums (N, W, R).
- Request routing: any node can coordinate a request.
- Basic failure handling when a node is unavailable.

**Non-goals** (documented as limitations): `DELETE`, gossip/dynamic membership, vector clocks,
hinted handoff / sloppy quorum, Merkle-tree anti-entropy, persistence, authentication.

## 2. Technology

Java 25, Spring Boot 4, Maven (same stack as the monolith). Package: `edu.uw.tcss5622.distributed_store`.
Every node runs the same application image; behavior is set by configuration.

## 3. System Overview

```
            +--------+
 clients -> | nginx  |  round-robin load balancer (stateless)
            +---+----+
                | any node
   +------+-----+-----+------+------+
   |      |     |     |      |      |
 node1  node2 node3  node4  node5      each node: coordinator + replica
   \______ consistent-hash ring ______/
         node <-> node over HTTP (/internal/*)
```

- The load balancer forwards each client request to any node. That node becomes the **coordinator**
  for the request.
- The coordinator hashes the key, finds the **preference list** (N replicas), and forwards the
  operation to them (handling itself locally if it is a replica).
- Every node holds a share of the keyspace; there is no master.

## 4. External API (identical to the monolith)

| Operation | Endpoint | Request | Success |
| --- | --- | --- | --- |
| PUT | `PUT /kv/{key}` | Plain-text value | `200` `{"key":"...","value":"..."}` |
| GET | `GET /kv/{key}` | none | `200` `{"key":"...","value":"..."}` |

Errors: `400` blank key/value, `404` key not found, plus distributed-specific
`503 Service Unavailable` when the quorum (W or R) cannot be met (error body shape matches the monolith's `ErrorResponse`).

## 5. Internal Node API

Used only between nodes (not exposed through the load balancer).

| Endpoint | Purpose | Body / Response |
| --- | --- | --- |
| `PUT /internal/kv/{key}` | Store a replica | `{"value":"...","timestamp":<epochMillis>,"origin":"nodeId"}` -> `200` |
| `GET /internal/kv/{key}` | Read a replica | `200` `{"value","timestamp"}` or `404` |
| `GET /internal/health` | Liveness probe | `200` |

A replica applies a write only if its timestamp is newer than the stored one (ties broken by `origin` id).

## 6. Partitioning: Consistent Hashing

- Hash ring over a 64-bit space using MD5 (first 8 bytes) or Murmur3 for key and node positions.
- Each physical node owns **V virtual nodes** (default 128) at positions `hash(nodeId + "#" + i)`, for even load.
- A key's coordinator-independent **preference list** is the first N *distinct physical* nodes found
  walking clockwise from `hash(key)`.
- Membership is static (config), so the ring is built once at startup and is identical on every node.

## 7. Replication and Quorums

Defaults: **N=3, W=2, R=2** (R + W > N). All configurable.

**PUT**
1. Coordinator assigns `timestamp = System.currentTimeMillis()` (last-write-wins).
2. Sends the versioned write to all N replicas in parallel (with per-call timeout).
3. Returns `200` once W replicas ack; remaining writes continue best effort.
4. If fewer than W ack within the timeout, return `503`.

**GET**
1. Coordinator queries all N replicas in parallel.
2. After R responses, returns the value with the newest timestamp; `404` if no replica among those responding has the key
   (and R responded).
3. Fewer than R responses within the timeout -> `503`.
4. Read repair (optional, asynchronous): push the newest version to stale replicas that responded.

**Conflict resolution:** last-write-wins by timestamp. Known limitation: clock skew can drop a concurrent write
(vector clocks are a non-goal).

## 8. Failure Detection and Handling

- Static node list; no gossip.
- Failure detection is timeout-based: a failed or slow replica call counts as a missing ack. Optionally, a node
  marks a peer *suspect* after K consecutive failures and skips it for a cool-down period, with `/internal/health`
  probes to restore it.
- One node down (with N=3, W=2, R=2): reads and writes still succeed using the remaining replicas.
- A recovered node is stale until it receives new writes or read repair (no anti-entropy; documented limitation).
- Two or more replicas of a key down: operations on that key return `503` (availability traded for consistency).
- The load balancer uses passive health checks to avoid a dead coordinator node.

## 9. Node Storage

In-memory `ConcurrentHashMap<String, VersionedValue(value, timestamp, origin)>`, discarded on restart,
mirroring the monolith's storage lifecycle so comparisons isolate distribution effects.

## 10. Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `kv.node-id` | required | This node's ID; it must be in `kv.ring.node-ids` |
| `kv.ring.node-ids` | required | Complete static list of physical node IDs |
| `kv.peers.<node-id>` | required for remote nodes | Base HTTP URL for each non-local ring node |
| `kv.replication.n` / `w` / `r` | 3 / 2 / 2 | Replication factor and quorums |
| `kv.ring.virtual-nodes` | 128 | Virtual nodes per physical node |
| `kv.client.timeout-ms` | 500 | Per-replica call timeout |
| `kv.read-repair.enabled` | true | Enable read repair |

Every node must use the same `kv.ring.node-ids` and `kv.ring.virtual-nodes`. It routes a key to
its primary owner, executing locally when it owns the key and forwarding `GET` or `PUT` to the
configured `kv.peers.<node-id>` endpoint otherwise.

## 11. Code Structure

```
edu.uw.tcss5622.distributed_store
  api/          KeyValueController (public), InternalController, error handling
  ring/         ConsistentHashRing, HashFunction
  cluster/      ClusterProperties, RingConfiguration, HttpPeerClient
  replication/  GetCoordinator, PutCoordinator (primary routing; future quorum coordination)
  store/        LocalStore (in-memory)
```

## 12. Deployment

Docker Compose: 5 node containers (`node1`..`node5`, same image) plus an `nginx` load balancer exposing one port.
Failures are simulated with `docker stop/start` on individual nodes. The monolith runs as one container on the same host and resources budget for comparison.

## 13. Evaluation Plan

Metrics: throughput (req/s), latency (p50/p95/p99), success rate.

| Experiment | Description |
| --- | --- |
| Scalability | Ramp concurrent clients (e.g. 10 -> 500) with a fixed read/write mix against both systems |
| Node failure | Stop 1 node mid-run (and 2 nodes for the quorum-loss case); compare success rate; monolith failure = whole service down |
| Recovery | Restart the node; observe latency and staleness |
| Quorum tuning | Vary (N, W, R) to show the latency vs. consistency trade-off |

Use the same load generator (e.g. k6 or wrk), key distribution, and value sizes for both systems.

## 14. Trade-offs and Limitations

- Extra network hops and quorum waits increase per-request latency versus the monolith, but removing the single point of failure improves availability.
- LWW can lose concurrent updates; no anti-entropy; static membership; in-memory data only.
- Single-host Docker testing limits realistic network and scalability conclusions.

## 15. Future Improvements

- **Dynamic cluster scaling:** Replace the static peer list with service discovery or gossip-based
  membership. Version membership views so every coordinator computes the same ring, and rebalance
  affected key ranges when nodes join or leave. During a ring transition, retain old replicas until
  data transfer is verified and use read repair or anti-entropy to converge replicas. Keep `N`, `W`,
  and `R` cluster-wide so physical node count can grow without changing the quorum policy.

## 16. Mapping to Timeline

- Week 4: node storage, internal API, consistent hash ring, inter-node client.
- Week 5: quorum replication, coordinator routing, failure handling, node-down tests.
- Week 6: experiments for both systems.
