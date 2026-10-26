package edu.uw.tcss5622.distributed_store.ring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ConsistentHashRing {

    private final NavigableMap<Long, String> positions;
    private final int memberCount;
    private final HashFunction hashFunction;

    public ConsistentHashRing(
            final Collection<String> nodeIds,
            final int virtualNodes,
            final HashFunction hashFunction) {
        Objects.requireNonNull(nodeIds, "nodeIds must not be null.");
        this.hashFunction = Objects.requireNonNull(hashFunction, "hashFunction must not be null.");
        if (nodeIds.isEmpty()) throw new IllegalArgumentException("At least one node ID must be provided.");
        if (virtualNodes <= 0) throw new IllegalArgumentException("Virtual node count must be positive.");

        final Set<String> uniqueNodeIds = new HashSet<>();
        for (final String nodeId : nodeIds) {
            if (nodeId == null || nodeId.isBlank()) throw new IllegalArgumentException("Node IDs must not be blank.");
            if (!uniqueNodeIds.add(nodeId)) throw new IllegalArgumentException("Node IDs must be unique.");
        }

        final List<String> sortedNodeIds = new ArrayList<>(uniqueNodeIds);
        sortedNodeIds.sort(Comparator.naturalOrder());
        final NavigableMap<Long, String> ring = new TreeMap<>(Long::compareUnsigned);
        for (final String nodeId : sortedNodeIds) {
            for (int index = 0; index < virtualNodes; index++) {
                final long position = hashFunction.hash(nodeId + "#" + index);
                final String existingNodeId = ring.putIfAbsent(position, nodeId);
                if (existingNodeId != null) {
                    throw new IllegalArgumentException("Virtual node hash collision between '%s' and '%s'.".formatted(existingNodeId, nodeId));
                }
                log.debug(
                        "Assigned virtual node {}#{} to ring position {}",
                        nodeId,
                        index,
                        Long.toUnsignedString(position));
            }
        }
        positions = ring;
        memberCount = uniqueNodeIds.size();
    }

    public List<String> preferenceList(final String key, final int count) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Key must not be blank.");
        if (count <= 0 || count > memberCount)
            throw new IllegalArgumentException("Preference count must be between 1 and member count.");

        final long keyPosition = hashFunction.hash(key);
        final Set<String> preference = new LinkedHashSet<>();
        addDistinctNodes(positions.tailMap(keyPosition, true), preference, count);
        if (preference.size() < count) {
            addDistinctNodes(positions.headMap(keyPosition, false), preference, count);
        }
        return List.copyOf(preference);
    }

    private void addDistinctNodes(
            final NavigableMap<Long, String> candidates,
            final Set<String> preference,
            final int count) {
        for (final String nodeId : candidates.values()) {
            preference.add(nodeId);
            if (preference.size() == count) {
                return;
            }
        }
    }
}
