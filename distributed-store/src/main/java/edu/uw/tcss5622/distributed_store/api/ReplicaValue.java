package edu.uw.tcss5622.distributed_store.api;

import edu.uw.tcss5622.distributed_store.store.VersionedValue;

public record ReplicaValue(String value, long timestamp, String origin) {

	public static ReplicaValue from(final VersionedValue value) {
		return new ReplicaValue(value.value(), value.timestamp(), value.origin());
	}
}
