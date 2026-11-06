package edu.uw.tcss5622.distributed_store.cluster;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ClusterMembership {

	private final ClusterProperties properties;
	private final ClusterTopology topology;

	public ClusterMembership(final ClusterProperties properties, final ClusterTopology topology) {
		this.properties = properties;
		this.topology = topology;
	}

	public ClusterConfig configuration() {
		return topology.snapshot().configuration();
	}

	public ClusterConfig join(final ClusterMember member) {
		topology.merge(new ClusterConfig(List.of(member)));
		return configuration();
	}

	public void merge(final ClusterConfig configuration) {
		topology.merge(configuration);
	}

	public ClusterMember localMember() {
		return configuration().members().stream()
				.filter(member -> member.nodeId().equals(properties.getNodeId()))
				.findFirst()
				.orElseThrow(() -> new ClusterConfigException("Local member is missing from membership."));
	}
}
