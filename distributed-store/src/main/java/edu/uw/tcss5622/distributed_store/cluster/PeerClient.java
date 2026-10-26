package edu.uw.tcss5622.distributed_store.cluster;

import java.util.Optional;

import edu.uw.tcss5622.distributed_store.store.VersionedValue;

public interface PeerClient {

	Optional<VersionedValue> getReplica(String nodeId, String key);
	void putReplica(String nodeId, String key, VersionedValue value);
}
