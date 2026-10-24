package edu.uw.tcss5622.distributed_store.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalController {

	@GetMapping("/health")
	public ResponseEntity<Void> health() {
		return ResponseEntity.ok().build();
	}
}
