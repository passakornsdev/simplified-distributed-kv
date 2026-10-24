package edu.uw.tcss5622.distributed_store.api;

public record ReplicaWrite(String value, Long timestamp, String origin) {
}
