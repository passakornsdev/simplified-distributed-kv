package edu.uw.tcss5622.distributed_store.cluster;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import edu.uw.tcss5622.distributed_store.ring.ConsistentHashRing;
import edu.uw.tcss5622.distributed_store.ring.HashFunction;
import org.springframework.stereotype.Service;

@Service
public class ClusterTopology {

	private final String localNodeId;
	private final HashFunction hashFunction;
	private final int virtualNodes;
	private final int replicationFactor;
	private final AtomicReference<Snapshot> snapshot;

	public ClusterTopology(final ClusterProperties properties, final HashFunction hashFunction) {
		localNodeId = properties.getNodeId();
		validateStaticConfiguration(properties);
		this.hashFunction = hashFunction;
		virtualNodes = properties.getRing().getVirtualNodes();
		replicationFactor = properties.getReplication().getN();
		final String selfUrl = properties.getMembership().getSelfUrl();
		validatePeerUrl(localNodeId, selfUrl);
		snapshot = new AtomicReference<>(createSnapshot(
				new ClusterConfig(List.of(new ClusterMember(localNodeId, selfUrl, 0)))));
	}

	private void validateStaticConfiguration(final ClusterProperties properties) {
		if (localNodeId == null || localNodeId.isBlank()) {
			throw new ClusterConfigException("kv.node-id must be provided.");
		}
		final var replication = properties.getReplication();
		if (replication.getN() <= 0) {
			throw new ClusterConfigException("kv.replication.n must be positive.");
		}
		if (replication.getW() <= 0 || replication.getW() > replication.getN()) {
			throw new ClusterConfigException("kv.replication.w must be between 1 and kv.replication.n.");
		}
		if (replication.getR() <= 0 || replication.getR() > replication.getN()) {
			throw new ClusterConfigException("kv.replication.r must be between 1 and kv.replication.n.");
		}
		if (replication.getW() + replication.getR() <= replication.getN()) {
			throw new ClusterConfigException("kv.replication.w + kv.replication.r must exceed kv.replication.n.");
		}
		if (properties.getClient().getTimeoutMs() <= 0) {
			throw new ClusterConfigException("kv.client.timeout-ms must be positive.");
		}
	}

	public Snapshot snapshot() {
		return snapshot.get();
	}

	public synchronized boolean merge(final ClusterConfig configuration) {
		if (configuration == null) {
			throw new ClusterConfigException("Membership configuration must be provided.");
		}
		final Map<String, ClusterMember> merged = new HashMap<>();
		for (final ClusterMember member : snapshot().configuration().members()) {
			merged.put(member.nodeId(), member);
		}
		for (final ClusterMember member : configuration.members()) {
			validateMember(member);
			merged.merge(member.nodeId(), member, this::newerMember);
		}
		final ClusterConfig next = new ClusterConfig(merged.values().stream()
				.sorted(java.util.Comparator.comparing(ClusterMember::nodeId))
				.toList());
		if (next.equals(snapshot().configuration())) {
			return false;
		}
		snapshot.set(createSnapshot(next));
		return true;
	}

	private Snapshot createSnapshot(final ClusterConfig configuration) {
		if (configuration.members().isEmpty()) {
			throw new ClusterConfigException("Configuration must contain at least one member.");
		}

		final Map<String, String> peerUrls = new HashMap<>();
		final List<String> nodeIds = new java.util.ArrayList<>();
		for (final ClusterMember member : configuration.members()) {
			validateMember(member);
			nodeIds.add(member.nodeId());
			if (!member.nodeId().equals(localNodeId)) {
				peerUrls.put(member.nodeId(), member.url());
			}
		}
		if (!nodeIds.contains(localNodeId)) {
			throw new ClusterConfigException("Configuration must include the local node.");
		}
		return new Snapshot(
				configuration,
				nodeIds.size() >= replicationFactor
						? new ConsistentHashRing(nodeIds, virtualNodes, hashFunction)
						: null,
				Map.copyOf(peerUrls));
	}

	private ClusterMember newerMember(final ClusterMember existing, final ClusterMember incoming) {
		if (incoming.incarnation() > existing.incarnation()
				|| (incoming.incarnation() == existing.incarnation()
				&& incoming.url().compareTo(existing.url()) > 0)) {
			return incoming;
		}
		return existing;
	}

	private void validateMember(final ClusterMember member) {
		if (member == null || member.nodeId() == null || member.nodeId().isBlank()) {
			throw new ClusterConfigException("Member node IDs must not be blank.");
		}
		if (member.incarnation() < 0) {
			throw new ClusterConfigException("Member incarnations must not be negative.");
		}
		validatePeerUrl(member.nodeId(), member.url());
	}

	private void validatePeerUrl(final String nodeId, final String peerUrl) {
		if (peerUrl == null || peerUrl.isBlank()) {
			throw new ClusterConfigException("A peer URL is required for node %s.".formatted(nodeId));
		}
		try {
			final URI uri = new URI(peerUrl);
			if (!uri.isAbsolute() || uri.getHost() == null
					|| (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))) {
				throw new ClusterConfigException("Peer URL for node %s must be an absolute HTTP URL.".formatted(nodeId));
			}
		} catch (final URISyntaxException exception) {
			throw new ClusterConfigException("Peer URL for node %s must be an absolute HTTP URL.".formatted(nodeId));
		}
	}

	public record Snapshot(
			ClusterConfig configuration,
			ConsistentHashRing ring,
			Map<String, String> peerUrls) { }
}
