package edu.uw.tcss5622.distributed_store.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplicaWrite {

	private String value;
	private Long timestamp;
	private String origin;
}
