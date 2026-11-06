package edu.uw.tcss5622.distributed_store.cluster;

import java.util.List;

public record ClusterConfig(List<ClusterMember> members) {

	public ClusterConfig {
		members = members == null ? List.of() : List.copyOf(members);
	}
}
