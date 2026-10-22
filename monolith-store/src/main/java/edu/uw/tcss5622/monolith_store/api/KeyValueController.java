package edu.uw.tcss5622.monolith_store.api;

import edu.uw.tcss5622.monolith_store.store.KeyValueStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kv")
public class KeyValueController {

	private final KeyValueStore store;

	public KeyValueController(final KeyValueStore store) {
		this.store = store;
	}

	@PutMapping(path = "/{key}")
	public KeyValueResponse put(
			@PathVariable final String key,
			@RequestBody final String value) {
		return new KeyValueResponse(key, store.put(key, value));
	}

	@GetMapping("/{key}")
	public KeyValueResponse get(@PathVariable final String key) {
		return new KeyValueResponse(key, store.get(key));
	}
}
