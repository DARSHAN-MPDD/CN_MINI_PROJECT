package com.network.simulator.service;

import com.network.simulator.model.FailureEvent;
import com.network.simulator.model.LinkStatus;
import com.network.simulator.model.NetworkLink;
import com.network.simulator.model.NetworkNode;
import com.network.simulator.model.NodeStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NetworkFailureDetectionService {
    private final TopologyService topologyService;
    private final FailureHistoryService historyService;

    public NetworkFailureDetectionService(TopologyService topologyService, FailureHistoryService historyService) {
        this.topologyService = topologyService;
        this.historyService = historyService;
    }

    public void detectLinkFailure(String linkId) {
        NetworkLink link = topologyService.getTopology().getLink(linkId);
        if (link == null) {
            throw new IllegalArgumentException("Link not found: " + linkId);
        }
        if (link.getStatus() == LinkStatus.FAILED) {
            return;
        }
        String prev = link.getStatus() == null ? "ACTIVE" : link.getStatus().name();
        topologyService.failLink(linkId);
        FailureEvent event = new FailureEvent("LINK_FAILURE", linkId, prev, "FAILED", "Link failure detected");
        historyService.addEvent(event);
    }

    public void detectNodeFailure(String nodeId) {
        NetworkNode node = topologyService.getTopology().getNode(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
        if (node.getStatus() == NodeStatus.FAILED) {
            return;
        }
        String prev = node.getStatus() == null ? "ACTIVE" : node.getStatus().name();
        node.setStatus(NodeStatus.FAILED);
        FailureEvent event = new FailureEvent("NODE_FAILURE", nodeId, prev, "FAILED", "Node failure detected");
        historyService.addEvent(event);
    }

    public void detectMultipleFailures(List<String> linkIds) {
        if (linkIds != null) {
            for (String linkId : linkIds) {
                try {
                    detectLinkFailure(linkId);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public List<FailureEvent> getHistory() {
        return historyService.getHistory();
    }

    public void clearHistory() {
        historyService.clear();
    }
}
