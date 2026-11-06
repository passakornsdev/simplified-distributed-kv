package edu.uw.tcss5622.distributed_store.cluster;

import java.util.List;

import edu.uw.tcss5622.distributed_store.ring.Md5HashFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClusterMembershipTests {

	@Test
	void acceptsJoinsWithoutALeader() {
		final ClusterMembership membership = membership("node1");

		final ClusterConfig configuration = membership.join(
				new ClusterMember("node2", "http://node2:8080", 1));

		assertEquals(List.of(
				new ClusterMember("node1", "http://node1:8080", 0),
				new ClusterMember("node2", "http://node2:8080", 1)),
				configuration.members());
	}

	@Test
	void retainsTheNewestMemberIncarnationDuringGossip() {
		final ClusterMembership membership = membership("node1");
		membership.merge(new ClusterConfig(List.of(
				new ClusterMember("node2", "http://old-node2:8080", 1))));

		membership.merge(new ClusterConfig(List.of(
				new ClusterMember("node2", "http://node2:8080", 2))));

		assertEquals("http://node2:8080", membership.configuration().members().get(1).url());
	}

	private ClusterMembership membership(final String nodeId) {
		final ClusterProperties properties = new ClusterProperties();
		properties.setNodeId(nodeId);
		properties.getMembership().setSelfUrl("http://" + nodeId + ":8080");
		properties.getReplication().setN(1);
		properties.getReplication().setW(1);
		properties.getReplication().setR(1);
		return new ClusterMembership(properties, new ClusterTopology(properties, new Md5HashFunction()));
	}
}
