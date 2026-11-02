package com.network.simulator.service;

import com.network.simulator.model.LinkStatus;
import com.network.simulator.model.NetworkLink;
import com.network.simulator.model.NetworkNode;
import com.network.simulator.model.NetworkTopology;
import com.network.simulator.model.NodeStatus;
import com.network.simulator.model.NodeType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the in-memory network topology and provides sample topology loading.
 */
@Service
public class TopologyService {
    private final NetworkTopology topology = new NetworkTopology();

    public NetworkTopology getTopology() {
        return topology;
    }

    public Map<String, NetworkNode> getNodes() {
        return topology.getNodes();
    }

    public Map<String, NetworkLink> getLinks() {
        return topology.getLinks();
    }

    public NetworkNode addNode(NetworkNode node) {
        topology.addNode(node);
        return topology.getNode(node.getId());
    }

    public NetworkLink addLink(NetworkLink link) {
        topology.addLink(link);
        return topology.getLink(link.getId());
    }

    public NetworkLink failLink(String linkId) {
        NetworkLink link = topology.getLink(linkId);
        if (link == null) {
            throw new IllegalArgumentException("Link not found: " + linkId);
        }
        link.setStatus(LinkStatus.FAILED);
        return link;
    }

    public NetworkLink recoverLink(String linkId) {
        NetworkLink link = topology.getLink(linkId);
        if (link == null) {
            throw new IllegalArgumentException("Link not found: " + linkId);
        }
        link.setStatus(LinkStatus.ACTIVE);
        return link;
    }

    public void clear() {
        topology.clear();
    }

    /**
     * Load the sample topology as specified in Phase 2.
     * S1 connected to R1(cost 2), R2(cost 5)
     * R1 connected to S1, R3(cost 2), R4(cost 4)
     * R2 connected to S1, R3(cost 1)
     * R3 connected to R1, R2, D1(cost 3)
     * R4 connected to R1, D1(cost 2)
     * D1 connected to R3, R4
     */
    public void loadSampleTopology() {
        topology.clear();

        addNode(new NetworkNode("S1", "Source 1", NodeType.SOURCE, NodeStatus.ACTIVE));
        addNode(new NetworkNode("R1", "Router 1", NodeType.ROUTER, NodeStatus.ACTIVE));
        addNode(new NetworkNode("R2", "Router 2", NodeType.ROUTER, NodeStatus.ACTIVE));
        addNode(new NetworkNode("R3", "Router 3", NodeType.ROUTER, NodeStatus.ACTIVE));
        addNode(new NetworkNode("R4", "Router 4", NodeType.ROUTER, NodeStatus.ACTIVE));
        addNode(new NetworkNode("D1", "Destination 1", NodeType.DESTINATION, NodeStatus.ACTIVE));

        // Bidirectional links as implied
        addLink(new NetworkLink("L1", "S1", "R1", 2, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L2", "R1", "S1", 2, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L3", "S1", "R2", 5, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L4", "R2", "S1", 5, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L5", "R1", "R3", 2, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L6", "R3", "R1", 2, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L7", "R2", "R3", 1, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L8", "R3", "R2", 1, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L9", "R3", "D1", 3, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L10", "D1", "R3", 3, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L11", "R1", "R4", 4, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L12", "R4", "R1", 4, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L13", "R4", "D1", 2, LinkStatus.ACTIVE));
        addLink(new NetworkLink("L14", "D1", "R4", 2, LinkStatus.ACTIVE));
    }
}
