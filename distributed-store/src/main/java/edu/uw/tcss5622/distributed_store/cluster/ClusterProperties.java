package edu.uw.tcss5622.distributed_store.cluster;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("kv")
public class ClusterProperties {

	private String nodeId;
	private RingProperties ring = new RingProperties();

    public static class RingProperties {

		private List<String> nodeIds = new ArrayList<>();
		@Setter
        @Getter
        private int virtualNodes = 128;

		public List<String> getNodeIds() {
			return List.copyOf(nodeIds);
		}

		public void setNodeIds(final List<String> nodeIds) {
			this.nodeIds = nodeIds == null ? new ArrayList<>() : new ArrayList<>(nodeIds);
		}

    }
}
