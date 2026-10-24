package edu.uw.tcss5622.distributed_store.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalControllerTests {

	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new InternalController())
			.build();

	@Test
	void reportsThatTheNodeIsLive() throws Exception {
		mockMvc.perform(get("/internal/health"))
				.andExpect(status().isOk())
				.andExpect(content().string(""));
	}
}
