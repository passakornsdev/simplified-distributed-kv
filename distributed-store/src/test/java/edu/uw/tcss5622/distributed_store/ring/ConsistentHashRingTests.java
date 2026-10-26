package edu.uw.tcss5622.distributed_store.ring;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConsistentHashRingTests {

	@Test
	void selectsNodesClockwiseAndWrapsAroundTheRing() {
		final ConsistentHashRing ring = new ConsistentHashRing(
				List.of("node-c", "node-a", "node-b"),
				1,
				hashes(Map.of(
						"node-a#0", 10L,
						"node-b#0", 30L,
						"node-c#0", 50L,
						"middle", 31L,
						"after-last", 60L)));

		assertEquals("node-c", ring.nodeFor("middle"));
		assertEquals("node-a", ring.nodeFor("after-last"));
		assertEquals(List.of("node-c", "node-a", "node-b"), ring.preferenceList("middle", 3));
	}

	@Test
	void returnsDistinctPhysicalNodesWhenVirtualNodesAreEncountered() {
		final ConsistentHashRing ring = new ConsistentHashRing(
				List.of("node-a", "node-b"),
				2,
				hashes(Map.of(
						"node-a#0", 10L,
						"node-a#1", 40L,
						"node-b#0", 20L,
						"node-b#1", 30L,
						"key", 15L)));

		assertEquals(List.of("node-b", "node-a"), ring.preferenceList("key", 2));
	}

	@Test
	void producesTheSamePreferenceListRegardlessOfMembershipInputOrder() {
		final HashFunction hashFunction = new Md5HashFunction();
		final ConsistentHashRing first = new ConsistentHashRing(
				List.of("node-c", "node-a", "node-b"), 8, hashFunction);
		final ConsistentHashRing second = new ConsistentHashRing(
				List.of("node-b", "node-c", "node-a"), 8, hashFunction);

		assertEquals(first.preferenceList("greeting", 3), second.preferenceList("greeting", 3));
	}

	@Test
	void rejectsInvalidConfigurationAndPreferenceRequests() {
		final HashFunction hashFunction = new Md5HashFunction();

		assertThrows(IllegalArgumentException.class,
				() -> new ConsistentHashRing(List.of(), 1, hashFunction));
		assertThrows(IllegalArgumentException.class,
				() -> new ConsistentHashRing(List.of("node-a", "node-a"), 1, hashFunction));
		assertThrows(IllegalArgumentException.class,
				() -> new ConsistentHashRing(List.of("node-a"), 0, hashFunction));
		assertThrows(IllegalArgumentException.class,
				() -> new ConsistentHashRing(List.of("node-a", "node-b"), 1, ignored -> 1L));

		final ConsistentHashRing ring = new ConsistentHashRing(List.of("node-a", "node-b"), 1, hashFunction);
		assertThrows(IllegalArgumentException.class, () -> ring.preferenceList("key", 0));
		assertThrows(IllegalArgumentException.class, () -> ring.preferenceList("key", 3));
		assertThrows(IllegalArgumentException.class, () -> ring.preferenceList(" ", 1));
	}

	private HashFunction hashes(final Map<String, Long> positions) {
		return value -> {
			final Long position = positions.get(value);
			if (position == null) {
				throw new IllegalArgumentException("No hash configured for " + value);
			}
			return position;
		};
	}
}
