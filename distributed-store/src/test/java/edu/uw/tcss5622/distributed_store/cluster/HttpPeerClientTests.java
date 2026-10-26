package edu.uw.tcss5622.distributed_store.cluster;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpPeerClientTests {

	private HttpServer server;
	private HttpPeerClient client;
	private volatile String receivedMethod;
	private volatile String receivedBody;

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/kv/greeting", this::respond);
		server.start();

		final ClusterProperties properties = new ClusterProperties();
		properties.setPeers(Map.of("node2", "http://localhost:" + server.getAddress().getPort()));
		client = new HttpPeerClient(properties);
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	@Test
	void forwardsGetRequestsToTheConfiguredPeer() {
		final var response = client.get("node2", "greeting");

		assertEquals("GET", receivedMethod);
		assertEquals("greeting", response.key());
		assertEquals("remote-value", response.value());
	}

	@Test
	void forwardsPlainTextPutRequestsToTheConfiguredPeer() {
		final var response = client.put("node2", "greeting", "hello");

		assertEquals("PUT", receivedMethod);
		assertEquals("hello", receivedBody);
		assertEquals("greeting", response.key());
		assertEquals("hello", response.value());
	}

	@Test
	void reportsMissingPeerUrlsAsConfigurationErrors() {
		final HttpPeerClient unconfiguredClient = new HttpPeerClient(new ClusterProperties());

		assertThrows(PeerConfigurationException.class,
				() -> unconfiguredClient.get("node2", "greeting"));
	}

	private void respond(final HttpExchange exchange) throws IOException {
		receivedMethod = exchange.getRequestMethod();
		receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		final String value = "PUT".equals(receivedMethod) ? receivedBody : "remote-value";
		final byte[] response = """
				{"key":"greeting","value":"%s"}
				""".formatted(value).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}
}
