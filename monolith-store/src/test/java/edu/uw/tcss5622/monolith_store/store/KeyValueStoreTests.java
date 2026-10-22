package edu.uw.tcss5622.monolith_store.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KeyValueStoreTests {

	private final KeyValueStore store = new KeyValueStore();

	@Test
	void returnsStoredValue() {
		store.put("greeting", "hello");

		assertEquals("hello", store.get("greeting"));
	}

	@Test
	void replacesValueForExistingKey() {
		store.put("greeting", "hello");
		store.put("greeting", "hi");

		assertEquals("hi", store.get("greeting"));
	}

	@Test
	void rejectsBlankKeysAndValues() {
		assertThrows(StoreValidationException.class, () -> store.put(" ", "value"));
		assertThrows(StoreValidationException.class, () -> store.put("key", " "));
	}

	@Test
	void reportsMissingKeys() {
		assertThrows(KeyNotFoundException.class, () -> store.get("missing"));
	}
}
