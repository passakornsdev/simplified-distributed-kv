package edu.uw.tcss5622.distributed_store.cluster;

public record ClusterMember(String nodeId, String url, long incarnation) { }
