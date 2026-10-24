package edu.uw.tcss5622.distributed_store.api;

import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
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
		mockMvc = MockMvcBuilders
				.standaloneSetup(new KeyValueController(new KeyValueStore()))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
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
}
