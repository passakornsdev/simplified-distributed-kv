package edu.uw.tcss5622.distributed_store.cluster;

public class PeerConfigurationException extends RuntimeException {

	public PeerConfigurationException(final String nodeId) {
		super("No peer URL is configured for node '%s'.".formatted(nodeId));
	}
}
