package edu.uw.tcss5622.distributed_store.cluster;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.uw.tcss5622.distributed_store.ring.HashFunction;
import edu.uw.tcss5622.distributed_store.ring.Md5HashFunction;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClusterProperties.class)
public class ClusterConfiguration {

	@Bean
	HashFunction hashFunction() {
		return new Md5HashFunction();
	}

	@Bean(destroyMethod = "close")
	ExecutorService replicaExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}
}
