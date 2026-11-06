package edu.uw.tcss5622.distributed_store.api;

import edu.uw.tcss5622.distributed_store.cluster.ClusterConfig;
import edu.uw.tcss5622.distributed_store.cluster.ClusterMember;
import edu.uw.tcss5622.distributed_store.cluster.ClusterMembership;
import edu.uw.tcss5622.distributed_store.store.KeyValueStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@Slf4j
public class InternalController {

	private final KeyValueStore store;
	private final ClusterMembership membership;

	public InternalController(final KeyValueStore store, final ClusterMembership membership) {
		this.store = store;
		this.membership = membership;
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

	@GetMapping("/cluster/config")
	public ClusterConfig getClusterConfig() {
		return membership.configuration();
	}

	@PostMapping("/cluster/join")
	public ClusterConfig joinCluster(@RequestBody final ClusterMember member) {
		return membership.join(member);
	}

	@PutMapping("/cluster/gossip")
	public ResponseEntity<Void> gossipCluster(@RequestBody final ClusterConfig configuration) {
		membership.merge(configuration);
		return ResponseEntity.ok().build();
	}
}
