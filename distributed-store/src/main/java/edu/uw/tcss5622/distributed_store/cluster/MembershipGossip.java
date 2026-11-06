package edu.uw.tcss5622.distributed_store.cluster;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
				log.debug("Joining cluster through seed {}.", seedUrl);
				membership.merge(peerClient.join(seedUrl, membership.localMember()));
				log.debug("Joined cluster through seed {}.", seedUrl);
				return;
			} catch (final RuntimeException exception) {
				log.debug("Could not join cluster through seed {}.", seedUrl, exception);
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
					log.debug("Gossiping membership with {} members to node {}.",
							configuration.members().size(), member.nodeId());
					peerClient.gossip(member.nodeId(), configuration);
				} catch (final RuntimeException exception) {
					log.debug("Could not gossip membership to node {}.", member.nodeId(), exception);
				}
			}
		}
	}
}
