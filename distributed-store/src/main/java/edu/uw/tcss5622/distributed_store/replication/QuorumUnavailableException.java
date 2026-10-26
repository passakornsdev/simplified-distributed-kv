package edu.uw.tcss5622.distributed_store.replication;

public final class QuorumUnavailableException extends RuntimeException {

	public QuorumUnavailableException(final String operation, final int required, final int received) {
		super("Unable to meet %s quorum: required %d responses but received %d."
				.formatted(operation, required, received));
	}
}
