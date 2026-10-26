package edu.uw.tcss5622.distributed_store.replication;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.uw.tcss5622.distributed_store.api.KeyValueResponse;
import edu.uw.tcss5622.distributed_store.cluster.ClusterProperties;
import edu.uw.tcss5622.distributed_store.cluster.PeerClient;
import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
import edu.uw.tcss5622.distributed_store.store.KeyNotFoundException;
import edu.uw.tcss5622.distributed_store.store.VersionedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GetCoordinator extends PrimaryNodeCoordinator {

    public GetCoordinator(
            final KeyValueStore store,
            final ClusterProperties properties,
            final ConsistentHashRing ring,
            final PeerClient peerClient,
            final java.util.concurrent.ExecutorService executor) {
        super(store, properties, ring, peerClient, executor);
    }

    public KeyValueResponse get(final String key) {
        store.validateKey(key);
        final var replicaNodeIds = replicaNodeIds(key);
        final CompletionService<ReplicaRead> completions = new ExecutorCompletionService<>(executor);
        for (final String nodeId : replicaNodeIds) {
            completions.submit(() -> readReplica(nodeId, key));
        }

        final List<ReplicaRead> responses = new java.util.ArrayList<>();
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(properties.getClient().getTimeoutMs());
        while (responses.size() < properties.getReplication().getR()) {
            final Future<ReplicaRead> completion = poll(completions, deadline);
            if (completion == null) break;
            final ReplicaRead response = completedRead(completion);
            if (response.responded()) {
                responses.add(response);
            }
        }
        if (responses.size() < properties.getReplication().getR()) {
            throw new QuorumUnavailableException("read", properties.getReplication().getR(), responses.size());
        }

        final VersionedValue newest = responses.stream()
                .map(ReplicaRead::value)
                .flatMap(Optional::stream)
                .max(Comparator.comparingLong(VersionedValue::getTimestamp)
                        .thenComparing(VersionedValue::getOrigin))
                .orElseThrow(() -> new KeyNotFoundException(key));
        this.repairRead(key, responses, newest);
        return new KeyValueResponse(key, newest.getValue());
    }

    private void repairRead(String key, List<ReplicaRead> responses, VersionedValue newest) {
        if (!properties.getReadRepair().isEnabled()) return;
		responses.stream()
				.filter(response -> response.value().isPresent())
				.filter(response -> newest.isNewerThan(response.value().get()))
				.forEach(response -> executor.submit(() -> repairReplica(response.nodeId(), key, newest)));
    }

    private ReplicaRead readReplica(final String nodeId, final String key) {
        try {
            if (isLocalNode(nodeId)) {
                log.debug("Reading replica for key {} from local node {}.", key, nodeId);
                return new ReplicaRead(nodeId, localValue(key), true);
            }
            log.debug("Reading replica for key {} from node {}.", key, nodeId);
            final Optional<VersionedValue> value = peerClient.getReplica(nodeId, key);
            return new ReplicaRead(nodeId, value, true);
        } catch (final RuntimeException exception) {
            log.debug("Replica read from node {} failed.", nodeId, exception);
            return new ReplicaRead(nodeId, Optional.empty(), false);
        }
    }

    private Optional<VersionedValue> localValue(final String key) {
        try {
            return Optional.of(store.getReplica(key));
        } catch (final KeyNotFoundException exception) {
            return Optional.empty();
        }
    }

    private ReplicaRead completedRead(final Future<ReplicaRead> completion) {
        try {
            return completion.get();
        } catch (final Exception exception) {
            return new ReplicaRead("", Optional.empty(), false);
        }
    }

    private void repairReplica(final String nodeId, final String key, final VersionedValue newest) {
		try {
			if (isLocalNode(nodeId)) {
				store.putReplica(key, newest.getValue(), newest.getTimestamp(), newest.getOrigin());
				return;
			}
			peerClient.putReplica(nodeId, key, newest);
        } catch (final RuntimeException exception) {
            log.debug("Read repair to node {} failed.", nodeId, exception);
        }
    }

    private record ReplicaRead(String nodeId, Optional<VersionedValue> value, boolean responded) { }
}
