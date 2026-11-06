package edu.uw.tcss5622.distributed_store.cluster;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MembershipGossip {

	private final ClusterProperties properties;
	private final ClusterMembership membership;
	private final PeerClient peerClient;

	public MembershipGossip(
			final ClusterProperties properties,
			final ClusterMembership membership,
			final PeerClient peerClient) {
		this.properties = properties;
		this.membership = membership;
		this.peerClient = peerClient;
	}

	@EventListener(ApplicationReadyEvent.class)
	void joinSeeds() {
		for (final String seedUrl : properties.getMembership().getSeedUrls()) {
			try {
				membership.merge(peerClient.join(seedUrl, membership.localMember()));
				return;
			} catch (final RuntimeException exception) {
				// Another seed may be available; periodic gossip will retry known peers.
			}
		}
	}

	@Scheduled(fixedDelayString = "${kv.membership.gossip-interval-ms:5000}")
	void gossip() {
		if (membership.configuration().members().size() == 1) {
			joinSeeds();
		}
		final ClusterConfig configuration = membership.configuration();
		for (final ClusterMember member : configuration.members()) {
			if (!member.nodeId().equals(properties.getNodeId())) {
				try {
					peerClient.gossip(member.nodeId(), configuration);
				} catch (final RuntimeException exception) {
					// Failure detection is intentionally outside join-and-gossip scope.
				}
			}
		}
	}
}
