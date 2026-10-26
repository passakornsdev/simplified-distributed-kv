package edu.uw.tcss5622.distributed_store.api;

import java.util.List;
import java.util.Map;

import edu.uw.tcss5622.distributed_store.cluster.ClusterProperties;
import edu.uw.tcss5622.distributed_store.cluster.PeerClient;
import edu.uw.tcss5622.distributed_store.cluster.PeerConfigurationException;
import edu.uw.tcss5622.distributed_store.cluster.PeerUnavailableException;
import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.ring.HashFunction;
import edu.uw.tcss5622.distributed_store.ring.Md5HashFunction;
import edu.uw.tcss5622.distributed_store.replication.GetCoordinator;
import edu.uw.tcss5622.distributed_store.replication.PutCoordinator;
import edu.uw.tcss5622.distributed_store.store.KeyNotFoundException;
import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KeyValueControllerTests {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = mockMvc(
				new KeyValueStore(),
				new ConsistentHashRing(List.of("node1"), 1, new Md5HashFunction()),
				new FailingPeerClient());
	}

	@Test
	void storesAndRetrievesValues() throws Exception {
		mockMvc.perform(put("/kv/greeting")
				.contentType(MediaType.TEXT_PLAIN)
				.content("hello"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.key").value("greeting"))
				.andExpect(jsonPath("$.value").value("hello"));

		mockMvc.perform(get("/kv/greeting"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.key").value("greeting"))
				.andExpect(jsonPath("$.value").value("hello"));
	}

	@Test
	void returnsNotFoundForMissingKey() throws Exception {
		mockMvc.perform(get("/kv/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("No value is stored for key 'missing'."));
	}

	@Test
	void returnsBadRequestForMissingOrBlankValue() throws Exception {
		mockMvc.perform(put("/kv/greeting")
				.contentType(MediaType.TEXT_PLAIN)
				.content(" "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Value must not be blank."));

		mockMvc.perform(put("/kv/greeting")
				.contentType(MediaType.TEXT_PLAIN))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Request body must contain a string value."));
	}

	@Test
	void forwardsWritesToThePrimaryOwner() throws Exception {
		final KeyValueStore store = new KeyValueStore();
		final RecordingPeerClient peerClient = new RecordingPeerClient();
		final MockMvc routingMvc = mockMvc(
				store,
				ring(Map.of("node1#0", 10L, "node2#0", 20L, "greeting", 15L)),
				peerClient);

		routingMvc.perform(put("/kv/greeting")
				.contentType(MediaType.TEXT_PLAIN)
				.content("hello"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.key").value("greeting"))
				.andExpect(jsonPath("$.value").value("hello"));

		assertEquals("node2", peerClient.nodeId);
		assertEquals("greeting", peerClient.key);
		assertEquals("hello", peerClient.value);
		assertThrows(KeyNotFoundException.class, () -> store.get("greeting"));
	}

	@Test
	void forwardsReadsToThePrimaryOwner() throws Exception {
		final RecordingPeerClient peerClient = new RecordingPeerClient();
		final MockMvc routingMvc = mockMvc(
				new KeyValueStore(),
				ring(Map.of("node1#0", 10L, "node2#0", 20L, "greeting", 15L)),
				peerClient);

		routingMvc.perform(get("/kv/greeting"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.key").value("greeting"))
				.andExpect(jsonPath("$.value").value("remote-value"));

		assertEquals("node2", peerClient.nodeId);
		assertEquals("greeting", peerClient.key);
	}

	@Test
	void reportsServiceUnavailableWhenThePrimaryOwnerCannotBeReached() throws Exception {
		final MockMvc routingMvc = mockMvc(
				new KeyValueStore(),
				ring(Map.of("node1#0", 10L, "node2#0", 20L, "greeting", 15L)),
				new UnavailablePeerClient());

		routingMvc.perform(get("/kv/greeting"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error").value("Node 'node2' is unavailable."));
	}

	@Test
	void reportsInternalServerErrorWhenThePrimaryOwnerIsNotConfigured() throws Exception {
		final MockMvc routingMvc = mockMvc(
				new KeyValueStore(),
				ring(Map.of("node1#0", 10L, "node2#0", 20L, "greeting", 15L)),
				new UnconfiguredPeerClient());

		routingMvc.perform(get("/kv/greeting"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").value("No peer URL is configured for node 'node2'."));
	}

	private MockMvc mockMvc(
			final KeyValueStore store,
			final ConsistentHashRing ring,
			final PeerClient peerClient) {
		return MockMvcBuilders
				.standaloneSetup(new KeyValueController(
						new GetCoordinator(store, properties("node1"), ring, peerClient),
						new PutCoordinator(store, properties("node1"), ring, peerClient)))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	private ClusterProperties properties(final String nodeId) {
		final ClusterProperties properties = new ClusterProperties();
		properties.setNodeId(nodeId);
		properties.getRing().setNodeIds(List.of("node1", "node2"));
		return properties;
	}

	private ConsistentHashRing ring(final Map<String, Long> positions) {
		final HashFunction hashFunction = value -> {
			final Long position = positions.get(value);
			if (position == null) {
				throw new IllegalArgumentException("No hash configured for " + value);
			}
			return position;
		};
		return new ConsistentHashRing(List.of("node1", "node2"), 1, hashFunction);
	}

	private static final class FailingPeerClient implements PeerClient {

		@Override
		public KeyValueResponse get(final String nodeId, final String key) {
			throw new AssertionError("Unexpected peer read.");
		}

		@Override
		public KeyValueResponse put(final String nodeId, final String key, final String value) {
			throw new AssertionError("Unexpected peer write.");
		}
	}

	private static final class RecordingPeerClient implements PeerClient {

		private String nodeId;
		private String key;
		private String value;

		@Override
		public KeyValueResponse get(final String nodeId, final String key) {
			this.nodeId = nodeId;
			this.key = key;
			return new KeyValueResponse(key, "remote-value");
		}

		@Override
		public KeyValueResponse put(final String nodeId, final String key, final String value) {
			this.nodeId = nodeId;
			this.key = key;
			this.value = value;
			return new KeyValueResponse(key, value);
		}
	}

	private static final class UnavailablePeerClient implements PeerClient {

		@Override
		public KeyValueResponse get(final String nodeId, final String key) {
			throw new PeerUnavailableException(nodeId, new IllegalStateException());
		}

		@Override
		public KeyValueResponse put(final String nodeId, final String key, final String value) {
			throw new PeerUnavailableException(nodeId, new IllegalStateException());
		}
	}

	private static final class UnconfiguredPeerClient implements PeerClient {

		@Override
		public KeyValueResponse get(final String nodeId, final String key) {
			throw new PeerConfigurationException(nodeId);
		}

		@Override
		public KeyValueResponse put(final String nodeId, final String key, final String value) {
			throw new PeerConfigurationException(nodeId);
		}
	}
}
