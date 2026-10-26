package edu.uw.tcss5622.distributed_store.cluster;

import java.net.URI;

import edu.uw.tcss5622.distributed_store.api.KeyValueResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class HttpPeerClient implements PeerClient {

	private final ClusterProperties properties;
	private final RestClient client;

	public HttpPeerClient(final ClusterProperties properties) {
		this.properties = properties;
		client = RestClient.create();
	}

	@Override
	public KeyValueResponse get(final String nodeId, final String key) {
		return request(nodeId, () -> client.get()
				.uri(keyUri(nodeId, key))
				.retrieve()
				.body(KeyValueResponse.class));
	}

	@Override
	public KeyValueResponse put(final String nodeId, final String key, final String value) {
		return request(nodeId, () -> client.put()
				.uri(keyUri(nodeId, key))
				.contentType(MediaType.TEXT_PLAIN)
				.body(value)
				.retrieve()
				.body(KeyValueResponse.class));
	}

	private KeyValueResponse request(
			final String nodeId,
			final PeerRequest request) {
		try {
			final KeyValueResponse response = request.execute();
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

	private URI keyUri(final String nodeId, final String key) {
		final String baseUrl = properties.getPeers().get(nodeId);
		if (baseUrl == null) {
			throw new PeerConfigurationException(nodeId);
		}
		return UriComponentsBuilder.fromUriString(baseUrl)
				.pathSegment("kv", key)
				.build()
				.toUri();
	}

	@FunctionalInterface
	private interface PeerRequest {
		KeyValueResponse execute();
	}
}
