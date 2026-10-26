package edu.uw.tcss5622.distributed_store.cluster;

public class PeerUnavailableException extends RuntimeException {

	public PeerUnavailableException(final String nodeId, final Throwable cause) {
		super("Node '%s' is unavailable.".formatted(nodeId), cause);
	}

	public PeerUnavailableException(final String nodeId) {
		super("Node '%s' returned an empty response.".formatted(nodeId));
	}
}
