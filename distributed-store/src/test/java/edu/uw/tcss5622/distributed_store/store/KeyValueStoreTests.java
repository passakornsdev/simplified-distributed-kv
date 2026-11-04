package edu.uw.tcss5622.distributed_store.store;

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
	void assignsIndependentLogicalTimestampsPerKey() {
		store.put("first", "one");
		store.put("second", "two");

		assertEquals(1, store.getReplica("first").getTimestamp());
		assertEquals(1, store.getReplica("second").getTimestamp());
		store.put("first", "three");
		assertEquals(2, store.getReplica("first").getTimestamp());
	}

	@Test
	void advancesLogicalClockAfterReceivingReplicaWrite() {
		store.putReplica("remote", "value", 10L, "node2");
		store.put("local", "value");

		assertEquals(1, store.getReplica("local").getTimestamp());
		store.put("remote", "new-value");
		assertEquals(12, store.getReplica("remote").getTimestamp());
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
