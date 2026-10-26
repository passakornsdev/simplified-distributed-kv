package edu.uw.tcss5622.distributed_store.cluster;

import edu.uw.tcss5622.distributed_store.api.KeyValueResponse;

public interface PeerClient {

	KeyValueResponse get(String nodeId, String key);

	KeyValueResponse put(String nodeId, String key, String value);
}
