package com.network.simulator.service;

import com.network.simulator.model.NetworkTopology;
import com.network.simulator.model.Packet;
import com.network.simulator.model.PacketStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PacketDeliveryService {
    private final TopologyService topologyService;
    private final RoutingService routingService;
    private NetworkMonitoringService networkMonitoringService;

    public PacketDeliveryService(TopologyService topologyService, RoutingService routingService) {
        this.topologyService = topologyService;
        this.routingService = routingService;
        this.networkMonitoringService = null;
    }

    @Autowired
    public PacketDeliveryService(TopologyService topologyService, RoutingService routingService, NetworkMonitoringService networkMonitoringService) {
        this.topologyService = topologyService;
        this.routingService = routingService;
        this.networkMonitoringService = networkMonitoringService;
    }

    public void setNetworkMonitoringService(NetworkMonitoringService networkMonitoringService) {
        this.networkMonitoringService = networkMonitoringService;
    }

    public Packet deliverPacket(String sourceNodeId, String destinationNodeId, String payload) {
        NetworkTopology topology = null;
        if (topologyService != null) {
            topology = topologyService.getTopology();
        } else {
            topology = new NetworkTopology();
        }
        Packet packet = new Packet(sourceNodeId, destinationNodeId, payload == null ? "" : payload);
        packet.setStatus(PacketStatus.CREATED);

        if (topology == null) {
            packet.setStatus(PacketStatus.FAILED);
            packet.setErrorMessage("Topology not available");
            if (networkMonitoringService != null) {
                try {
                    networkMonitoringService.recordPacket(packet);
                } catch (Exception ignored) {
                }
            }
            return packet;
        }

        if (sourceNodeId == null || destinationNodeId == null) {
            packet.setStatus(PacketStatus.FAILED);
            packet.setErrorMessage("Source and destination are required");
            if (networkMonitoringService != null) {
                try {
                    networkMonitoringService.recordPacket(packet);
                } catch (Exception ignored) {
                }
            }
            return packet;
        }
        if (!topology.hasNode(sourceNodeId) || !topology.hasNode(destinationNodeId)) {
            packet.setStatus(PacketStatus.FAILED);
            packet.setErrorMessage("Source or destination node does not exist");
            if (networkMonitoringService != null) {
                networkMonitoringService.recordPacket(packet);
            }
            return packet;
        }
        if (topology.getNode(sourceNodeId).getStatus().name().equals("FAILED") ||
                topology.getNode(destinationNodeId).getStatus().name().equals("FAILED")) {
            packet.setStatus(PacketStatus.FAILED);
            packet.setErrorMessage("Source or destination node is failed");
            if (networkMonitoringService != null) {
                networkMonitoringService.recordPacket(packet);
            }
            return packet;
        }

        if (routingService == null) {
            packet.setStatus(PacketStatus.FAILED);
            packet.setErrorMessage("Routing service not available");
            if (networkMonitoringService != null) {
                try {
                    networkMonitoringService.recordPacket(packet);
                } catch (Exception ignored) {
                }
            }
            return packet;
        }

        RoutingService.RouteResult routeResult = routingService.findShortestPath(topology, sourceNodeId, destinationNodeId);
        if (!routeResult.isFound() || routeResult.getPath().isEmpty()) {
            packet.setStatus(PacketStatus.FAILED);
            packet.setErrorMessage("No route exists between source and destination");
            if (networkMonitoringService != null) {
                networkMonitoringService.recordPacket(packet);
            }
            return packet;
        }

        packet.setStatus(PacketStatus.IN_TRANSIT);
        packet.setRoute(routeResult.getPath());
        packet.setRouteCost(routeResult.getCost());
        packet.setStatus(PacketStatus.DELIVERED);
        if (networkMonitoringService != null) {
            networkMonitoringService.recordPacket(packet);
        }
        return packet;
    }

    public static class RerouteResult {
        private String packetId;
        private boolean rerouted;
        private List<String> oldRoute;
        private List<String> newRoute;
        private double newRouteCost;
        private String message;

        public String getPacketId() {
            return packetId;
        }

        public void setPacketId(String packetId) {
            this.packetId = packetId;
        }

        public boolean isRerouted() {
            return rerouted;
        }

        public void setRerouted(boolean rerouted) {
            this.rerouted = rerouted;
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

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public RerouteResult testRerouteAfterLinkFailure(String failedLinkId, String sourceId, String destId, List<String> oldRoute) {
        RerouteResult result = new RerouteResult();
        result.setPacketId(UUID.randomUUID().toString());
        result.setOldRoute(oldRoute);
        // After failing a link, try to find alternative route
        Packet p = deliverPacket(sourceId, destId, "reroute-test");
        if (p.getStatus() == PacketStatus.DELIVERED) {
            result.setRerouted(true);
            result.setNewRoute(p.getRoute());
            result.setNewRouteCost(p.getRouteCost());
            result.setMessage("Alternative route found after link failure");
            return result;
        } else {
            result.setRerouted(false);
            result.setMessage(p.getErrorMessage() != null ? p.getErrorMessage() : "No alternative route exists");
            return result;
        }
    }
}
