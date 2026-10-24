package edu.uw.tcss5622.distributed_store.api;

import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalControllerTests {

	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new InternalController(new KeyValueStore()))
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void reportsThatTheNodeIsLive() throws Exception {
		mockMvc.perform(get("/internal/health"))
				.andExpect(status().isOk())
				.andExpect(content().string(""));
	}

	@Test
	void storesAndRetrievesReplicaValues() throws Exception {
		mockMvc.perform(put("/internal/kv/greeting")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"value":"hello","timestamp":100,"origin":"node1"}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

		mockMvc.perform(get("/internal/kv/greeting"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.value").value("hello"))
				.andExpect(jsonPath("$.timestamp").value(100))
				.andExpect(jsonPath("$.origin").value("node1"));
	}

	@Test
	void keepsTheNewestReplicaValue() throws Exception {
		putReplica("newer", 200, "node1");
		putReplica("older", 100, "node2");
		putReplica("same-time-lower-origin", 200, "node0");
		putReplica("same-time-higher-origin", 200, "node2");

		mockMvc.perform(get("/internal/kv/greeting"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.value").value("same-time-higher-origin"))
				.andExpect(jsonPath("$.timestamp").value(200))
				.andExpect(jsonPath("$.origin").value("node2"));
	}

	@Test
	void rejectsReplicaWritesWithMissingMetadata() throws Exception {
		mockMvc.perform(put("/internal/kv/greeting")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"value":"hello","origin":"node1"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Timestamp must be provided."));
	}

	private void putReplica(final String value, final long timestamp, final String origin) throws Exception {
		mockMvc.perform(put("/internal/kv/greeting")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"value":"%s","timestamp":%d,"origin":"%s"}
						""".formatted(value, timestamp, origin)))
				.andExpect(status().isOk());
	}
}
