package edu.uw.tcss5622.distributed_store.cluster;

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
		return new ConsistentHashRing(
				properties.getRing().getNodeIds(),
				properties.getRing().getVirtualNodes(),
				hashFunction);
	}
}
