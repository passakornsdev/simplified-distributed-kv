package edu.uw.tcss5622.distributed_store.ring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Md5HashFunctionTests {

	private final Md5HashFunction hashFunction = new Md5HashFunction();

	@Test
	void hashesValuesDeterministicallyUsingTheFirstEightMd5Bytes() {
		assertEquals(6719722671305337462L, hashFunction.hash("hello"));
		assertEquals(hashFunction.hash("hello"), hashFunction.hash("hello"));
	}

	@Test
	void rejectsNullValues() {
		assertThrows(NullPointerException.class, () -> hashFunction.hash(null));
	}
}
