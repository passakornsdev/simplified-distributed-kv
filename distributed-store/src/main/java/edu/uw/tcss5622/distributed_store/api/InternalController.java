package edu.uw.tcss5622.distributed_store.api;

import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@Slf4j
public class InternalController {

	private final KeyValueStore store;

	public InternalController(final KeyValueStore store) {
		this.store = store;
	}

	@GetMapping("/health")
	public ResponseEntity<Void> health() {
		log.debug("Received internal health check.");
		return ResponseEntity.ok().build();
	}

	@PutMapping("/kv/{key}")
	public ResponseEntity<Void> putReplica(
			@PathVariable final String key,
			@RequestBody final ReplicaWrite replica) {
		log.debug("Received replica write for key {}.", key);
		store.putReplica(key, replica.value(), replica.timestamp(), replica.origin());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/kv/{key}")
	public ReplicaValue getReplica(@PathVariable final String key) {
		log.debug("Received replica read for key {}.", key);
		return ReplicaValue.from(store.getReplica(key));
	}
}
