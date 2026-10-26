package edu.uw.tcss5622.distributed_store.replication;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.uw.tcss5622.distributed_store.cluster.ClusterProperties;
import edu.uw.tcss5622.distributed_store.cluster.PeerClient;
import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.store.KeyValueStore;

abstract class PrimaryNodeCoordinator {

	protected final KeyValueStore store;
	protected final PeerClient peerClient;
	protected final ClusterProperties properties;
	protected final ExecutorService executor;

	private final ConsistentHashRing ring;

	PrimaryNodeCoordinator(
			final KeyValueStore store,
			final ClusterProperties properties,
			final ConsistentHashRing ring,
			final PeerClient peerClient,
			final ExecutorService executor) {
		this.store = store;
		this.properties = properties;
		this.ring = ring;
		this.peerClient = peerClient;
		this.executor = executor;
	}

	protected List<String> replicaNodeIds(final String key) {
		return ring.preferenceList(key, properties.getReplication().getN());
	}

	protected boolean isLocalNode(final String nodeId) {
		return nodeId.equals(properties.getNodeId());
	}

	protected <T> Future<T> poll(final CompletionService<T> completions, final long deadline) {
		try {
			final long remaining = deadline - System.nanoTime();
			return remaining <= 0 ? null : completions.poll(remaining, TimeUnit.NANOSECONDS);
		} catch (final InterruptedException exception) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
}
