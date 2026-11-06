package edu.uw.tcss5622.distributed_store.cluster;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import edu.uw.tcss5622.distributed_store.api.ReplicaValue;
import edu.uw.tcss5622.distributed_store.api.ReplicaWrite;
import edu.uw.tcss5622.distributed_store.store.VersionedValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class HttpPeerClient implements PeerClient {

	private final ClusterTopology topology;
	private final RestClient client;

	public HttpPeerClient(final ClusterProperties properties, final ClusterTopology topology) {
		this.topology = topology;
		final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		final Duration timeout = Duration.ofMillis(properties.getClient().getTimeoutMs());
		requestFactory.setConnectTimeout(timeout);
		requestFactory.setReadTimeout(timeout);
		client = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Override
	public Optional<VersionedValue> getReplica(final String nodeId, final String key) {
		try {
			final ReplicaValue value = client.get()
					.uri(replicaUri(nodeId, key))
					.retrieve()
					.body(ReplicaValue.class);
			if (value == null) {
				throw new PeerUnavailableException(nodeId);
			}
			return Optional.of(new VersionedValue(value.value(), value.timestamp(), value.origin()));
		} catch (final RestClientResponseException exception) {
			if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
				return Optional.empty();
			}
			throw new PeerRequestException(
					exception.getStatusCode(),
					exception.getResponseBodyAsString());
		} catch (final RestClientException exception) {
			throw new PeerUnavailableException(nodeId, exception);
		}
	}

	@Override
	public void putReplica(final String nodeId, final String key, final VersionedValue value) {
		request(nodeId, () -> client.put()
				.uri(replicaUri(nodeId, key))
				.contentType(MediaType.APPLICATION_JSON)
				.body(new ReplicaWrite(value.getValue(), value.getTimestamp(), value.getOrigin()))
				.retrieve()
				.toBodilessEntity());
	}

	@Override
	public ClusterConfig join(final String seedUrl, final ClusterMember member) {
		try {
			final ClusterConfig configuration = client.post()
					.uri(UriComponentsBuilder.fromUriString(seedUrl)
							.pathSegment("internal", "cluster", "join")
							.build()
							.toUri())
					.contentType(MediaType.APPLICATION_JSON)
					.body(member)
					.retrieve()
					.body(ClusterConfig.class);
			if (configuration == null) {
				throw new ClusterConfigException("Seed did not return membership.");
			}
			return configuration;
		} catch (final RestClientException exception) {
			throw new PeerUnavailableException(seedUrl, exception);
		}
	}

	@Override
	public void gossip(final String nodeId, final ClusterConfig configuration) {
		request(nodeId, () -> client.put()
				.uri(uri(nodeId, "internal", "cluster", "gossip"))
				.contentType(MediaType.APPLICATION_JSON)
				.body(configuration)
				.retrieve()
				.toBodilessEntity());
	}

	private <T> T request(
			final String nodeId,
			final PeerRequest<T> request) {
		try {
			final T response = request.execute();
			if (response == null) {
				throw new PeerUnavailableException(nodeId);
			}
			return response;
		} catch (final RestClientResponseException exception) {
			throw new PeerRequestException(
					exception.getStatusCode(),
					exception.getResponseBodyAsString());
		} catch (final RestClientException exception) {
			throw new PeerUnavailableException(nodeId, exception);
		}
	}

	private URI replicaUri(final String nodeId, final String key) {
		return uri(nodeId, "internal", "kv", key);
	}

	private URI uri(final String nodeId, final String... pathSegments) {
		final String baseUrl = topology.snapshot().peerUrls().get(nodeId);
		if (baseUrl == null) {
			throw new PeerConfigurationException(nodeId);
		}
		return UriComponentsBuilder.fromUriString(baseUrl)
				.pathSegment(pathSegments)
				.build()
				.toUri();
	}

	@FunctionalInterface
	private interface PeerRequest<T> {
		T execute();
	}
}
