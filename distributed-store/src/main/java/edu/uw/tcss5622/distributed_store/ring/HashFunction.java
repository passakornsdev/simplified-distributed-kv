package edu.uw.tcss5622.distributed_store.ring;

@FunctionalInterface
public interface HashFunction {
	long hash(String value);
}
