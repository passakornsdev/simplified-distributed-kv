package edu.uw.tcss5622.monolith_store.store;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

@Service
public class KeyValueStore {

	private final ConcurrentMap<String, String> values = new ConcurrentHashMap<>();

	public String get(final String key) {
		validateKey(key);
		final String value = values.get(key);
		if (value == null) {
			throw new KeyNotFoundException(key);
		}
		return value;
	}

	public String put(final String key, final String value) {
		validateKey(key);
		validateValue(value);
		values.put(key, value);
		return value;
	}

	private void validateKey(final String key) {
		if (key == null || key.isBlank()) {
			throw new StoreValidationException("Key must not be blank.");
		}
	}

	private void validateValue(final String value) {
		if (value == null || value.isBlank()) {
			throw new StoreValidationException("Value must not be blank.");
		}
	}
}
