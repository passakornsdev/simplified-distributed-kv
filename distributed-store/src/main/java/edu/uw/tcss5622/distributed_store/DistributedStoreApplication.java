package edu.uw.tcss5622.distributed_store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedStoreApplication {

	static void main() {
		SpringApplication.run(DistributedStoreApplication.class);
	}

}
