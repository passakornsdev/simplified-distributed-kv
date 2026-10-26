package edu.uw.tcss5622.distributed_store.cluster;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.ring.HashFunction;
import edu.uw.tcss5622.distributed_store.ring.Md5HashFunction;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClusterProperties.class)
public class RingConfiguration {

	@Bean
	HashFunction hashFunction() {
		return new Md5HashFunction();
	}

	@Bean
	ConsistentHashRing consistentHashRing(
			final ClusterProperties properties,
			final HashFunction hashFunction) {
		final String nodeId = properties.getNodeId();
		if (nodeId == null || nodeId.isBlank()) {
			throw new IllegalArgumentException("kv.node-id must be provided.");
		}
		if (!properties.getRing().getNodeIds().contains(nodeId)) {
			throw new IllegalArgumentException("kv.node-id must appear in kv.ring.node-ids.");
		}
		for (final String peerNodeId : properties.getRing().getNodeIds()) {
			if (!peerNodeId.equals(nodeId)) {
				validatePeerUrl(peerNodeId, properties.getPeers().get(peerNodeId));
			}
		}
		validateReplication(properties);
		return new ConsistentHashRing(
				properties.getRing().getNodeIds(),
				properties.getRing().getVirtualNodes(),
				hashFunction);
	}

	@Bean(destroyMethod = "close")
	ExecutorService replicaExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	private void validateReplication(final ClusterProperties properties) {
		final int memberCount = properties.getRing().getNodeIds().size();
		final var replication = properties.getReplication();
		if (replication.getN() <= 0 || replication.getN() > memberCount) {
			throw new IllegalArgumentException("kv.replication.n must be between 1 and ring member count.");
		}
		if (replication.getW() <= 0 || replication.getW() > replication.getN()) {
			throw new IllegalArgumentException("kv.replication.w must be between 1 and kv.replication.n.");
		}
		if (replication.getR() <= 0 || replication.getR() > replication.getN()) {
			throw new IllegalArgumentException("kv.replication.r must be between 1 and kv.replication.n.");
		}
		if (replication.getW() + replication.getR() <= replication.getN()) {
			throw new IllegalArgumentException("kv.replication.w + kv.replication.r must exceed kv.replication.n.");
		}
		if (properties.getClient().getTimeoutMs() <= 0) {
			throw new IllegalArgumentException("kv.client.timeout-ms must be positive.");
		}
	}

	private void validatePeerUrl(final String nodeId, final String peerUrl) {
		if (peerUrl == null || peerUrl.isBlank()) {
			throw new IllegalArgumentException(
					"kv.peers.%s must be provided for every remote ring node.".formatted(nodeId));
		}
		try {
			final URI uri = new URI(peerUrl);
			if (!uri.isAbsolute() || uri.getHost() == null
					|| (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))) {
				throw new IllegalArgumentException(
						"kv.peers.%s must be an absolute HTTP URL.".formatted(nodeId));
			}
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException(
					"kv.peers.%s must be an absolute HTTP URL.".formatted(nodeId),
					exception);
		}
	}
}
