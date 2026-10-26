package edu.uw.tcss5622.distributed_store.cluster;

import java.net.URI;
import java.net.URISyntaxException;

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
		return new ConsistentHashRing(
				properties.getRing().getNodeIds(),
				properties.getRing().getVirtualNodes(),
				hashFunction);
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
