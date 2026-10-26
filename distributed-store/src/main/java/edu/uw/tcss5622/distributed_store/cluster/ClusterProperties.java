package edu.uw.tcss5622.distributed_store.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("kv")
public class ClusterProperties {
	private String nodeId;
	private Map<String, String> peers = new HashMap<>();
	private RingProperties ring = new RingProperties();
	private ReplicationProperties replication = new ReplicationProperties();
	private ClientProperties client = new ClientProperties();
	private ReadRepairProperties readRepair = new ReadRepairProperties();

	public Map<String, String> getPeers() {
		return Map.copyOf(peers);
	}

	public void setPeers(final Map<String, String> peers) {
		this.peers = peers == null ? new HashMap<>() : new HashMap<>(peers);
	}

    public static class RingProperties {
		@Setter
        @Getter
        private int virtualNodes = 128;
		private List<String> nodeIds = new ArrayList<>();

		public List<String> getNodeIds() {
			return List.copyOf(nodeIds);
		}

		public void setNodeIds(final List<String> nodeIds) {
			this.nodeIds = nodeIds == null ? new ArrayList<>() : new ArrayList<>(nodeIds);
		}

    }

	@Setter
	@Getter
	public static class ReplicationProperties {
		private int n = 3;
		private int w = 2;
		private int r = 2;
	}

	@Setter
	@Getter
	public static class ClientProperties {
		private long timeoutMs = 500;
	}

	@Setter
	@Getter
	public static class ReadRepairProperties {
		private boolean enabled = true;
	}
}
