package edu.uw.tcss5622.distributed_store.cluster;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
		final boolean changed = topology.merge(new ClusterConfig(List.of(member)));
		log.debug("Received join from node {}; membership changed: {}.", member.nodeId(), changed);
		return configuration();
	}

	public void merge(final ClusterConfig configuration) {
		final boolean changed = topology.merge(configuration);
		log.debug("Merged membership gossip from {} members; membership changed: {}.",
				configuration.members().size(), changed);
	}

	public ClusterMember localMember() {
		return configuration().members().stream()
				.filter(member -> member.nodeId().equals(properties.getNodeId()))
				.findFirst()
				.orElseThrow(() -> new ClusterConfigException("Local member is missing from membership."));
	}
}
