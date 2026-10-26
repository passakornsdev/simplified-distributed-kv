package edu.uw.tcss5622.distributed_store.api;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.uw.tcss5622.distributed_store.cluster.ClusterProperties;
import edu.uw.tcss5622.distributed_store.cluster.PeerClient;
import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.ring.Md5HashFunction;
import edu.uw.tcss5622.distributed_store.replication.GetCoordinator;
import edu.uw.tcss5622.distributed_store.replication.PutCoordinator;
import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
import edu.uw.tcss5622.distributed_store.store.VersionedValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KeyValueControllerTests {

	private ExecutorService executor;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		executor = Executors.newVirtualThreadPerTaskExecutor();
		mockMvc = mockMvc(
				new KeyValueStore(),
				new ConsistentHashRing(List.of("node1"), 1, new Md5HashFunction()),
				mock(PeerClient.class),
				List.of("node1"),
				1,
				1,
				1);
	}

	@AfterEach
	void tearDown() {
		executor.close();
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
	void returnsNotFoundWhenAReadQuorumHasNoValue() throws Exception {
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
	void writesToThePreferenceListAndSucceedsAfterWriteQuorum() throws Exception {
		final PeerClient peerClient = mock(PeerClient.class);
		final KeyValueStore store = new KeyValueStore();
		final MockMvc quorumMvc = mockMvc(
				store,
				new ConsistentHashRing(List.of("node1", "node2", "node3"), 1, new Md5HashFunction()),
				peerClient,
				List.of("node1", "node2", "node3"),
				3,
				2,
				2);

		quorumMvc.perform(put("/kv/greeting")
				.contentType(MediaType.TEXT_PLAIN)
				.content("hello"))
				.andExpect(status().isOk());

		verify(peerClient, timeout(100).times(2))
				.putReplica(anyString(), eq("greeting"), any(VersionedValue.class));
	}

	@Test
	void returnsServiceUnavailableWhenWriteQuorumCannotBeMet() throws Exception {
		final PeerClient peerClient = mock(PeerClient.class);
		doThrow(new IllegalStateException("unavailable"))
				.when(peerClient)
				.putReplica(anyString(), anyString(), any(VersionedValue.class));
		final MockMvc quorumMvc = mockMvc(
				new KeyValueStore(),
				new ConsistentHashRing(List.of("node1", "node2", "node3"), 1, new Md5HashFunction()),
				peerClient,
				List.of("node1", "node2", "node3"),
				3,
				2,
				3);

		quorumMvc.perform(put("/kv/greeting")
				.contentType(MediaType.TEXT_PLAIN)
				.content("hello"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error")
						.value("Unable to meet write quorum: required 2 responses but received 1."));
	}

	@Test
	void returnsTheNewestValueFromAReadQuorum() throws Exception {
		final PeerClient peerClient = mock(PeerClient.class);
		when(peerClient.getReplica("node2", "greeting"))
				.thenReturn(Optional.of(new VersionedValue("old", 100, "node2")));
		when(peerClient.getReplica("node3", "greeting"))
				.thenReturn(Optional.of(new VersionedValue("new", 200, "node3")));
		final MockMvc quorumMvc = mockMvc(
				new KeyValueStore(),
				new ConsistentHashRing(List.of("node1", "node2", "node3"), 1, new Md5HashFunction()),
				peerClient,
				List.of("node1", "node2", "node3"),
				3,
				2,
				3);

		quorumMvc.perform(get("/kv/greeting"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.value").value("new"));
	}

	private MockMvc mockMvc(
			final KeyValueStore store,
			final ConsistentHashRing ring,
			final PeerClient peerClient,
			final List<String> nodeIds,
			final int replicationFactor,
			final int writeQuorum,
			final int readQuorum) {
		final ClusterProperties properties = new ClusterProperties();
		properties.setNodeId("node1");
		properties.getRing().setNodeIds(nodeIds);
		properties.getReplication().setN(replicationFactor);
		properties.getReplication().setW(writeQuorum);
		properties.getReplication().setR(readQuorum);
		return MockMvcBuilders
				.standaloneSetup(new KeyValueController(
						new GetCoordinator(store, properties, ring, peerClient, executor),
						new PutCoordinator(store, properties, ring, peerClient, executor)))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

}
