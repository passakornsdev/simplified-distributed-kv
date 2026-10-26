package edu.uw.tcss5622.distributed_store.replication;

import edu.uw.tcss5622.distributed_store.cluster.ClusterProperties;
import edu.uw.tcss5622.distributed_store.cluster.PeerClient;
import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.store.KeyValueStore;

abstract class PrimaryNodeCoordinator {

	protected final KeyValueStore store;
	protected final PeerClient peerClient;

	private final ClusterProperties properties;
	private final ConsistentHashRing ring;

	PrimaryNodeCoordinator(
			final KeyValueStore store,
			final ClusterProperties properties,
			final ConsistentHashRing ring,
			final PeerClient peerClient) {
		this.store = store;
		this.properties = properties;
		this.ring = ring;
		this.peerClient = peerClient;
	}

	protected String primaryNodeId(final String key) {
		return ring.nodeFor(key);
	}

	protected boolean isLocalNode(final String nodeId) {
		return nodeId.equals(properties.getNodeId());
	}
}
