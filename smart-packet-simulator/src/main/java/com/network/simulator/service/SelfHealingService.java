package com.network.simulator.service;

import com.network.simulator.model.FailureEvent;
import com.network.simulator.model.LinkStatus;
import com.network.simulator.model.NetworkNode;
import com.network.simulator.model.NodeStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SelfHealingService {
    private final TopologyService topologyService;
    private final RoutingService routingService;
    private final FailureHistoryService historyService;

    public SelfHealingService(TopologyService topologyService,
                              RoutingService routingService,
                              FailureHistoryService historyService) {
        this.topologyService = topologyService;
        this.routingService = routingService;
        this.historyService = historyService;
    }

    public static class HealingResult {
        private boolean recovered;
        private String message;
        private List<String> oldRoute;
        private List<String> newRoute;
        private double newRouteCost;

        public boolean isRecovered() {
            return recovered;
        }

        public void setRecovered(boolean recovered) {
            this.recovered = recovered;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<String> getOldRoute() {
            return oldRoute;
        }

        public void setOldRoute(List<String> oldRoute) {
            this.oldRoute = oldRoute;
        }

        public List<String> getNewRoute() {
            return newRoute;
        }

        public void setNewRoute(List<String> newRoute) {
            this.newRoute = newRoute;
        }

        public double getNewRouteCost() {
            return newRouteCost;
        }

        public void setNewRouteCost(double newRouteCost) {
            this.newRouteCost = newRouteCost;
        }
    }

    public HealingResult attemptRecovery(String sourceId, String destinationId, List<String> affectedRoute) {
        HealingResult result = new HealingResult();
        result.setOldRoute(affectedRoute);
        RoutingService.RouteResult routeResult = routingService.findShortestPath(topologyService.getTopology(), sourceId, destinationId);
        if (routeResult.isFound() && !routeResult.getPath().isEmpty()) {
            result.setRecovered(true);
            result.setNewRoute(routeResult.getPath());
            result.setNewRouteCost(routeResult.getCost());
            result.setMessage("Alternative route found via self-healing");
            FailureEvent evt = new FailureEvent("SELF_HEALING", (sourceId == null ? "" : sourceId) + "->" + (destinationId == null ? "" : destinationId),
                    "AFFECTED", "HEALED", "Recalculated route after failure");
            evt.setRecoveryAction("REROUTE");
            evt.setRecoveryResult("SUCCESS");
            historyService.addEvent(evt);
            return result;
        } else {
            result.setRecovered(false);
            result.setMessage("No alternative path exists - recovery impossible");
            FailureEvent evt = new FailureEvent("SELF_HEALING", (sourceId == null ? "" : sourceId) + "->" + (destinationId == null ? "" : destinationId),
                    "AFFECTED", "FAILED", "Self-healing could not find alternative route");
            evt.setRecoveryAction("REROUTE");
            evt.setRecoveryResult("FAILED");
            historyService.addEvent(evt);
            return result;
        }
    }

    public void recoverLink(String linkId) {
        var link = topologyService.getTopology().getLink(linkId);
        if (link == null) {
            throw new IllegalArgumentException("Link not found: " + linkId);
        }
        if (link.getStatus() == LinkStatus.ACTIVE) {
            return;
        }
        String prev = link.getStatus().name();
        topologyService.recoverLink(linkId);
        FailureEvent evt = new FailureEvent("LINK_RECOVERY", linkId, prev, "ACTIVE", "Link explicitly recovered");
        evt.setRecoveryAction("RECOVER");
        evt.setRecoveryResult("SUCCESS");
        historyService.addEvent(evt);
    }

    public void recoverNode(String nodeId) {
        NetworkNode node = topologyService.getTopology().getNode(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
        if (node.getStatus() == NodeStatus.ACTIVE) {
            return;
        }
        String prev = node.getStatus().name();
        node.setStatus(NodeStatus.ACTIVE);
        FailureEvent evt = new FailureEvent("NODE_RECOVERY", nodeId, prev, "ACTIVE", "Node explicitly recovered");
        evt.setRecoveryAction("RECOVER");
        evt.setRecoveryResult("SUCCESS");
        historyService.addEvent(evt);
    }
}
