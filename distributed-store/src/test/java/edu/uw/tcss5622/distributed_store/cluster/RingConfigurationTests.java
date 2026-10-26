package edu.uw.tcss5622.distributed_store.cluster;

import java.util.List;

import edu.uw.tcss5622.distributed_store.DistributedStoreApplication;
import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.ring.Md5HashFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class RingConfigurationTests {

	private final RingConfiguration configuration = new RingConfiguration();

	@Test
	void createsARingForTheConfiguredStaticMembership() {
		final ClusterProperties properties = properties("node-a", List.of("node-a", "node-b"));

		final ConsistentHashRing ring = configuration.consistentHashRing(properties, new Md5HashFunction());

		assertEquals(2, ring.preferenceList("key", 2).size());
	}

	@Test
	void requiresTheLocalNodeToAppearInTheStaticMembership() {
		final ClusterProperties missingNodeId = properties(null, List.of("node-a"));
		final ClusterProperties nonMemberNodeId = properties("node-b", List.of("node-a"));

		assertThrows(IllegalArgumentException.class,
				() -> configuration.consistentHashRing(missingNodeId, new Md5HashFunction()));
		assertThrows(IllegalArgumentException.class,
				() -> configuration.consistentHashRing(nonMemberNodeId, new Md5HashFunction()));
	}

	@Test
	void requiresPeerUrlsForRemoteRingNodes() {
		final ClusterProperties properties = new ClusterProperties();
		properties.setNodeId("node-a");
		properties.getRing().setNodeIds(List.of("node-a", "node-b"));

		assertThrows(IllegalArgumentException.class,
				() -> configuration.consistentHashRing(properties, new Md5HashFunction()));
	}

	@Test
	void bindsConfiguredMembershipWhenTheApplicationStarts() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DistributedStoreApplication.class)
				.web(WebApplicationType.NONE)
				.run(
						"--kv.node-id=node-a",
						"--kv.ring.node-ids=node-a,node-b",
						"--kv.peers.node-b=http://node-b:8080",
						"--kv.ring.virtual-nodes=2")) {
			final ConsistentHashRing ring = context.getBean(ConsistentHashRing.class);

			assertEquals(2, ring.preferenceList("key", 2).size());
		}
	}

	private ClusterProperties properties(final String nodeId, final List<String> nodeIds) {
		final ClusterProperties properties = new ClusterProperties();
		properties.setNodeId(nodeId);
		properties.getRing().setNodeIds(nodeIds);
		properties.setPeers(java.util.Map.of("node-b", "http://node-b:8080"));
		properties.getRing().setVirtualNodes(2);
		return properties;
	}
}
