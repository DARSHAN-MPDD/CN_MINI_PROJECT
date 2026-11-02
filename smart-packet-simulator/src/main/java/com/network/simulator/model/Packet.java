package com.network.simulator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Packet {
    private String packetId;
    private String sourceNodeId;
    private String destinationNodeId;
    private String payload;
    private PacketStatus status;
    private List<String> route = new ArrayList<>();
    private double routeCost;
    private String errorMessage;

    public Packet() {
        this.packetId = UUID.randomUUID().toString();
    }

    public Packet(String sourceNodeId, String destinationNodeId, String payload) {
        this.packetId = UUID.randomUUID().toString();
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
        this.payload = payload;
        this.status = PacketStatus.CREATED;
    }

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getDestinationNodeId() {
        return destinationNodeId;
    }

    public void setDestinationNodeId(String destinationNodeId) {
        this.destinationNodeId = destinationNodeId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public PacketStatus getStatus() {
        return status;
    }

    public void setStatus(PacketStatus status) {
        this.status = status;
    }

    public List<String> getRoute() {
        return route == null ? Collections.emptyList() : route;
    }

    public void setRoute(List<String> route) {
        this.route = route == null ? new ArrayList<>() : new ArrayList<>(route);
    }

    public double getRouteCost() {
        return routeCost;
    }

    public void setRouteCost(double routeCost) {
        this.routeCost = routeCost;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Packet packet = (Packet) o;
        return Objects.equals(packetId, packet.packetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packetId);
    }
}
