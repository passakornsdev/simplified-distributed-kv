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
public class GetCoordinator extends PrimaryNodeCoordinator {

	public GetCoordinator(
			final KeyValueStore store,
			final ClusterProperties properties,
			final ConsistentHashRing ring,
			final PeerClient peerClient) {
		super(store, properties, ring, peerClient);
	}

	public KeyValueResponse get(final String key) {
		store.validateKey(key);
		final String primaryNodeId = primaryNodeId(key);
		if (isLocalNode(primaryNodeId)) {
			log.debug("Getting key {} on node {}.", key, primaryNodeId);
			return new KeyValueResponse(key, store.get(key));
		}
		log.debug("Forwarding GET for key {} to node {}.", key, primaryNodeId);
		return peerClient.get(primaryNodeId, key);
	}
}
