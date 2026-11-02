package com.network.simulator.controller;

import com.network.simulator.model.Packet;
import com.network.simulator.service.PacketDeliveryService;
import com.network.simulator.service.TopologyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PacketController {
    private final PacketDeliveryService packetDeliveryService;
    private final TopologyService topologyService;

    public PacketController(PacketDeliveryService packetDeliveryService, TopologyService topologyService) {
        this.packetDeliveryService = packetDeliveryService;
        this.topologyService = topologyService;
    }

    public static class DeliveryRequest {
        private String source;
        private String destination;
        private String payload;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }

    public static class RerouteTestRequest {
        private String linkId;
        private String source;
        private String destination;
        private List<String> oldRoute;

        public String getLinkId() {
            return linkId;
        }

        public void setLinkId(String linkId) {
            this.linkId = linkId;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public List<String> getOldRoute() {
            return oldRoute;
        }

        public void setOldRoute(List<String> oldRoute) {
            this.oldRoute = oldRoute;
        }
    }

    @PostMapping("/packets/deliver")
    public ResponseEntity<?> deliverPacket(@RequestBody DeliveryRequest request) {
        if (request == null || request.getSource() == null || request.getDestination() == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Source and destination are required");
            return ResponseEntity.badRequest().body(err);
        }
        Packet packet = packetDeliveryService.deliverPacket(request.getSource(), request.getDestination(), request.getPayload());
        if (packet.getStatus().name().equals("FAILED")) {
            // Return 200 with FAILED status as per "If no route exists, return a FAILED status with a meaningful explanation"
            // but also say "Invalid requests should return appropriate HTTP errors rather than generic server errors"
            // "source or destination node does not exist" - could be bad request; but spec says check cases - be reasonable
            Map<String, Object> resp = new HashMap<>();
            resp.put("packetId", packet.getPacketId());
            resp.put("status", packet.getStatus().name());
            resp.put("sourceNodeId", packet.getSourceNodeId());
            resp.put("destinationNodeId", packet.getDestinationNodeId());
            resp.put("route", packet.getRoute());
            resp.put("routeCost", packet.getRouteCost());
            resp.put("errorMessage", packet.getErrorMessage());
            return ResponseEntity.ok(resp);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("packetId", packet.getPacketId());
        resp.put("status", packet.getStatus().name());
        resp.put("sourceNodeId", packet.getSourceNodeId());
        resp.put("destinationNodeId", packet.getDestinationNodeId());
        resp.put("payload", packet.getPayload());
        resp.put("route", packet.getRoute());
        resp.put("routeCost", packet.getRouteCost());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/links/{id}/fail-and-reroute")
    public ResponseEntity<?> failAndReroute(@org.springframework.web.bind.annotation.PathVariable("id") String linkId,
                                           @RequestBody RerouteTestRequest request) {
        try {
            if (request == null || request.getSource() == null || request.getDestination() == null) {
                Map<String, String> err = new HashMap<>();
                err.put("error", "Source and destination are required");
                return ResponseEntity.badRequest().body(err);
            }
            topologyService.failLink(linkId);
            PacketDeliveryService.RerouteResult result = packetDeliveryService.testRerouteAfterLinkFailure(
                    linkId, request.getSource(), request.getDestination(), request.getOldRoute());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
