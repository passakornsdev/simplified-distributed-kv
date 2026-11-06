package edu.uw.tcss5622.distributed_store.cluster;

import java.util.List;

import edu.uw.tcss5622.distributed_store.DistributedStoreApplication;
import edu.uw.tcss5622.distributed_store.ring.Md5HashFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class ClusterTopologyTests {

	@Test
	void createsARingAfterReceivingEnoughMembershipRecords() {
		final ClusterProperties properties = properties("node-a");

		final ClusterTopology topology = new ClusterTopology(properties, new Md5HashFunction());
		topology.merge(new ClusterConfig(List.of(new ClusterMember("node-b", "http://node-b:8080", 1))));

		assertEquals(2, topology.snapshot().ring().preferenceList("key", 2).size());
	}

	@Test
	void requiresTheLocalNodeId() {
		final ClusterProperties missingNodeId = properties(null);

		assertThrows(ClusterConfigException.class,
				() -> new ClusterTopology(missingNodeId, new Md5HashFunction()));
	}

	@Test
	void requiresTheLocalNodeUrl() {
		final ClusterProperties properties = new ClusterProperties();
		properties.setNodeId("node-a");

		assertThrows(ClusterConfigException.class,
				() -> new ClusterTopology(properties, new Md5HashFunction()));
	}

	@Test
	void bindsConfiguredMembershipWhenTheApplicationStarts() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DistributedStoreApplication.class)
				.web(WebApplicationType.NONE)
				.run(
						"--kv.node-id=node-a",
						"--kv.membership.self-url=http://node-a:8080",
						"--kv.ring.virtual-nodes=2",
						"--kv.replication.n=2",
						"--kv.replication.w=1",
						"--kv.replication.r=2")) {
			final ClusterTopology topology = context.getBean(ClusterTopology.class);

			assertEquals(1, topology.snapshot().configuration().members().size());
		}
	}

	private ClusterProperties properties(final String nodeId) {
		final ClusterProperties properties = new ClusterProperties();
		properties.setNodeId(nodeId);
		properties.getMembership().setSelfUrl(nodeId == null ? "http://node-a:8080" : "http://" + nodeId + ":8080");
		properties.getRing().setVirtualNodes(2);
		properties.getReplication().setN(2);
		properties.getReplication().setW(1);
		properties.getReplication().setR(2);
		return properties;
	}
}
