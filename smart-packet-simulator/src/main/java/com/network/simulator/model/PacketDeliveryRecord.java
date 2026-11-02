package com.network.simulator.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PacketDeliveryRecord {
    private String packetId;
    private String sourceNodeId;
    private String destinationNodeId;
    private List<String> route = new ArrayList<>();
    private Instant createdAt;
    private Instant deliveredAt;
    private Long deliveryDurationMillis;
    private PacketStatus status;
    private String failureReason;

    public PacketDeliveryRecord() {
    }

    public PacketDeliveryRecord(Packet packet) {
        this.packetId = packet.getPacketId();
        this.sourceNodeId = packet.getSourceNodeId();
        this.destinationNodeId = packet.getDestinationNodeId();
        this.route = packet.getRoute() == null ? new ArrayList<>() : new ArrayList<>(packet.getRoute());
        this.status = packet.getStatus();
        this.failureReason = packet.getErrorMessage();
        this.createdAt = Instant.now();
        if (this.status == PacketStatus.DELIVERED) {
            this.deliveredAt = Instant.now();
            if (createdAt != null && deliveredAt != null) {
                this.deliveryDurationMillis = deliveredAt.toEpochMilli() - createdAt.toEpochMilli();
            }
        }
    }

    public String getPacketId() { return packetId; }
    public void setPacketId(String packetId) { this.packetId = packetId; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public void setDestinationNodeId(String destinationNodeId) { this.destinationNodeId = destinationNodeId; }
    public List<String> getRoute() { return route == null ? Collections.emptyList() : route; }
    public void setRoute(List<String> route) { this.route = route == null ? new ArrayList<>() : new ArrayList<>(route); }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
        if (createdAt != null && deliveredAt != null) {
            this.deliveryDurationMillis = deliveredAt.toEpochMilli() - createdAt.toEpochMilli();
        }
    }
    public Long getDeliveryDurationMillis() { return deliveryDurationMillis; }
    public void setDeliveryDurationMillis(Long deliveryDurationMillis) { this.deliveryDurationMillis = deliveryDurationMillis; }
    public PacketStatus getStatus() { return status; }
    public void setStatus(PacketStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
