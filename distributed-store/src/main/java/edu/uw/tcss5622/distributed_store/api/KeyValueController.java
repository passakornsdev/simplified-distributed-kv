package edu.uw.tcss5622.distributed_store.api;

import edu.uw.tcss5622.distributed_store.replication.GetCoordinator;
import edu.uw.tcss5622.distributed_store.replication.PutCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kv")
@RequiredArgsConstructor
@Slf4j
public class KeyValueController {

	private final GetCoordinator getCoordinator;
	private final PutCoordinator putCoordinator;

	@PutMapping(path = "/{key}")
	public KeyValueResponse put(
			@PathVariable final String key,
			@RequestBody final String value) {
		log.debug("Received PUT request for key {}.", key);
		return putCoordinator.put(key, value);
	}

	@GetMapping("/{key}")
	public KeyValueResponse get(@PathVariable final String key) {
		log.debug("Received GET request for key {}.", key);
		return getCoordinator.get(key);
	}
}
