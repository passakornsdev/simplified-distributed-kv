package edu.uw.tcss5622.monolith_store.store;

public final class KeyNotFoundException extends RuntimeException {

	public KeyNotFoundException(final String key) {
		super("No value is stored for key '%s'.".formatted(key));
	}
}
