package com.network.simulator.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory representation of the network topology.
 */
public class NetworkTopology {
    private final Map<String, NetworkNode> nodes = new LinkedHashMap<>();
    private final Map<String, NetworkLink> links = new LinkedHashMap<>();

    public NetworkTopology() {
    }

    public Map<String, NetworkNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public Map<String, NetworkLink> getLinks() {
        return Collections.unmodifiableMap(links);
    }

    public void addNode(NetworkNode node) {
        if (node == null || node.getId() == null) {
            throw new IllegalArgumentException("Node and node id are required");
        }
        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Duplicate node id: " + node.getId());
        }
        if (node.getStatus() == null) {
            node.setStatus(NodeStatus.ACTIVE);
        }
        if (node.getType() == null) {
            throw new IllegalArgumentException("Node type is required");
        }
        nodes.put(node.getId(), node);
    }

    public void addLink(NetworkLink link) {
        if (link == null || link.getId() == null) {
            throw new IllegalArgumentException("Link and link id are required");
        }
        if (links.containsKey(link.getId())) {
            throw new IllegalArgumentException("Duplicate link id: " + link.getId());
        }
        if (link.getCost() <= 0) {
            throw new IllegalArgumentException("Link cost must be positive");
        }
        if (!nodes.containsKey(link.getSourceNodeId()) || !nodes.containsKey(link.getTargetNodeId())) {
            throw new IllegalArgumentException("Link references nonexistent node");
        }
        if (link.getStatus() == null) {
            link.setStatus(LinkStatus.ACTIVE);
        }
        links.put(link.getId(), link);
    }

    public NetworkNode getNode(String id) {
        return nodes.get(id);
    }

    public NetworkLink getLink(String id) {
        return links.get(id);
    }

    public void clear() {
        nodes.clear();
        links.clear();
    }

    public boolean hasNode(String id) {
        return nodes.containsKey(id);
    }

    public boolean hasLink(String id) {
        return links.containsKey(id);
    }
}
