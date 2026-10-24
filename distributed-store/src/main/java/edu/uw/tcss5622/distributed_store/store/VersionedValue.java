package edu.uw.tcss5622.distributed_store.store;

public record VersionedValue(String value, long timestamp, String origin) {

	public boolean isNewerThan(final VersionedValue other) {
		final int timestampComparison = Long.compare(timestamp, other.timestamp);
		return timestampComparison > 0
				|| (timestampComparison == 0 && origin.compareTo(other.origin) > 0);
	}
}
