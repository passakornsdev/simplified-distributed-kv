package edu.uw.tcss5622.distributed_store.replication;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.uw.tcss5622.distributed_store.api.KeyValueResponse;
import edu.uw.tcss5622.distributed_store.cluster.ClusterProperties;
import edu.uw.tcss5622.distributed_store.cluster.PeerClient;
import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
import edu.uw.tcss5622.distributed_store.store.VersionedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PutCoordinator extends PrimaryNodeCoordinator {

	public PutCoordinator(
			final KeyValueStore store,
			final ClusterProperties properties,
			final ConsistentHashRing ring,
			final PeerClient peerClient,
			final java.util.concurrent.ExecutorService executor) {
		super(store, properties, ring, peerClient, executor);
	}

	public KeyValueResponse put(final String key, final String value) {
		store.validateKey(key);
		store.validateValue(value);
		final VersionedValue replica = new VersionedValue(
				value,
				System.currentTimeMillis(),
				properties.getNodeId());
		final var replicaNodeIds = replicaNodeIds(key);
		final CompletionService<Boolean> completions = new ExecutorCompletionService<>(executor);
		for (final String nodeId : replicaNodeIds) {
			completions.submit(() -> writeReplica(nodeId, key, replica));
		}

		int acknowledgements = 0;
		int completed = 0;
		final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(properties.getClient().getTimeoutMs());
		while (completed < replicaNodeIds.size()) {
			final Future<Boolean> completion = poll(completions, deadline);
			if (completion == null) {
				break;
			}
			completed++;
			if (completedSuccessfully(completion)) {
				acknowledgements++;
				if (acknowledgements >= properties.getReplication().getW()) {
					log.debug("Stored key {} on {} replicas.", key, acknowledgements);
					return new KeyValueResponse(key, value);
				}
			}
		}
		throw new QuorumUnavailableException("write", properties.getReplication().getW(), acknowledgements);
	}

	private boolean writeReplica(final String nodeId, final String key, final VersionedValue replica) {
		try {
			if (isLocalNode(nodeId)) {
				log.debug("Writing replica for key {} to local node {}.", key, nodeId);
				store.putReplica(key, replica.getValue(), replica.getTimestamp(), replica.getOrigin());
			} else {
				log.debug("Writing replica for key {} to node {}.", key, nodeId);
				peerClient.putReplica(nodeId, key, replica);
			}
			return true;
		} catch (final RuntimeException exception) {
			log.debug("Replica write to node {} failed.", nodeId, exception);
			return false;
		}
	}

	private boolean completedSuccessfully(final Future<Boolean> completion) {
		try {
			return completion.get();
		} catch (final Exception exception) {
			return false;
		}
	}
}
