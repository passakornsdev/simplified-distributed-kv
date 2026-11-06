package edu.uw.tcss5622.distributed_store.cluster;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import edu.uw.tcss5622.distributed_store.store.VersionedValue;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
		server.createContext("/internal/kv/greeting", this::respond);
		server.createContext("/internal/kv/missing", exchange -> {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
		});
		server.createContext("/internal/cluster/join", this::respond);
		server.createContext("/internal/cluster/gossip", this::respond);
		server.start();

		final ClusterProperties properties = new ClusterProperties();
		properties.setNodeId("node1");
		properties.getMembership().setSelfUrl("http://node1:8080");
		properties.getReplication().setN(1);
		properties.getReplication().setW(1);
		properties.getReplication().setR(1);
		final ClusterTopology topology = new ClusterTopology(properties,
				new edu.uw.tcss5622.distributed_store.ring.Md5HashFunction());
		topology.merge(new ClusterConfig(java.util.List.of(
				new ClusterMember("node2", "http://localhost:" + server.getAddress().getPort(), 1))));
		client = new HttpPeerClient(properties, topology);
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	@Test
	void forwardsReplicaWritesToInternalEndpoints() {
		client.putReplica("node2", "greeting", new VersionedValue("hello", 100, "node1"));

		assertEquals("PUT", receivedMethod);
		assertEquals("""
				{"value":"hello","timestamp":100,"origin":"node1"}
				""".strip(), receivedBody);
	}

	@Test
	void readsReplicaValuesAndTreatsMissingValuesAsResponses() {
		final Optional<VersionedValue> value = client.getReplica("node2", "greeting");
		final Optional<VersionedValue> missing = client.getReplica("node2", "missing");

		assertEquals(new VersionedValue("remote-value", 100, "node2"), value.orElseThrow());
		assertEquals(Optional.empty(), missing);
	}

	@Test
	void gossipsMembershipToInternalEndpoints() {
		client.gossip("node2", new ClusterConfig(
				java.util.List.of(new ClusterMember("node2", "http://node2:8080", 1))));

		assertEquals("PUT", receivedMethod);
		assertEquals("""
				{"members":[{"nodeId":"node2","url":"http://node2:8080","incarnation":1}]}
				""".strip(), receivedBody);
	}

	@Test
	void joinsThroughSeedEndpoints() {
		final ClusterConfig configuration = client.join(
				"http://localhost:" + server.getAddress().getPort(),
				new ClusterMember("node1", "http://node1:8080", 1));

		assertEquals("POST", receivedMethod);
		assertEquals("node1", configuration.members().getFirst().nodeId());
	}

	private void respond(final HttpExchange exchange) throws IOException {
		receivedMethod = exchange.getRequestMethod();
		receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		final boolean replicaRequest = exchange.getRequestURI().getPath().startsWith("/internal/");
		final String value = "PUT".equals(receivedMethod) ? receivedBody : "remote-value";
		final String responseBody = exchange.getRequestURI().getPath().endsWith("/cluster/join")
				? """
						{"members":[{"nodeId":"node1","url":"http://node1:8080","incarnation":1}]}
						"""
				: replicaRequest && "GET".equals(receivedMethod) ? """
						{"value":"remote-value","timestamp":100,"origin":"node2"}
						"""
				: """
						{"key":"greeting","value":"%s"}
						""".formatted(value);
		final byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}
}
