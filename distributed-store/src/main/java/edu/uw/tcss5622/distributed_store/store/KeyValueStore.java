package edu.uw.tcss5622.distributed_store.store;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class KeyValueStore {

	private final ConcurrentMap<String, VersionedValue> values = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, AtomicLong> logicalClocks = new ConcurrentHashMap<>();

	public String get(final String key) {
		validateKey(key);
		final VersionedValue value = values.get(key);
		if (value == null) {
			throw new KeyNotFoundException(key);
		}
		return value.getValue();
	}

	public String put(final String key, final String value) {
		validateKey(key);
		validateValue(value);
		values.put(key, new VersionedValue(value, nextTimestamp(key), ""));
		return value;
	}

	public void putReplica(final String key, final String value, final Long timestamp, final String origin) {
		validateKey(key);
		validateValue(value);
		validateTimestamp(timestamp);
		validateOrigin(origin);
		observeTimestamp(key, timestamp);

		final VersionedValue replica = new VersionedValue(value, timestamp, origin);
		values.compute(key, (ignored, current) ->
				current == null || replica.isNewerThan(current) ? replica : current);
	}

	public long nextTimestamp(final String key) {
		return clockFor(key).updateAndGet(Math::incrementExact);
	}

	private void observeTimestamp(final String key, final long timestamp) {
		clockFor(key).updateAndGet(current -> Math.incrementExact(Math.max(current, timestamp)));
	}

	private AtomicLong clockFor(final String key) {
		return logicalClocks.computeIfAbsent(key, ignored -> new AtomicLong());
	}

	public VersionedValue getReplica(final String key) {
		validateKey(key);
		final VersionedValue value = values.get(key);
		if (value == null) {
			throw new KeyNotFoundException(key);
		}
		return value;
	}

	public void validateKey(final String key) {
		if (key == null || key.isBlank()) {
			throw new StoreValidationException("Key must not be blank.");
		}
	}

	public void validateValue(final String value) {
		if (value == null || value.isBlank()) {
			throw new StoreValidationException("Value must not be blank.");
		}
	}

	private void validateTimestamp(final Long timestamp) {
		if (timestamp == null) {
			throw new StoreValidationException("Timestamp must be provided.");
		}
	}

	private void validateOrigin(final String origin) {
		if (origin == null || origin.isBlank()) {
			throw new StoreValidationException("Origin must not be blank.");
		}
	}
}
