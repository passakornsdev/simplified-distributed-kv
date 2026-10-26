package edu.uw.tcss5622.distributed_store.replication;

import edu.uw.tcss5622.distributed_store.api.KeyValueResponse;
import edu.uw.tcss5622.distributed_store.cluster.ClusterProperties;
import edu.uw.tcss5622.distributed_store.cluster.PeerClient;
import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PutCoordinator extends PrimaryNodeCoordinator {

	public PutCoordinator(
			final KeyValueStore store,
			final ClusterProperties properties,
			final ConsistentHashRing ring,
			final PeerClient peerClient) {
		super(store, properties, ring, peerClient);
	}

	public KeyValueResponse put(final String key, final String value) {
		store.validateKey(key);
		store.validateValue(value);
		final String primaryNodeId = primaryNodeId(key);
		if (isLocalNode(primaryNodeId)) {
			log.debug("Putting key {} on node {}.", key, primaryNodeId);
			return new KeyValueResponse(key, store.put(key, value));
		}
		log.debug("Forwarding PUT for key {} to node {}.", key, primaryNodeId);
		return peerClient.put(primaryNodeId, key, value);
	}
}
