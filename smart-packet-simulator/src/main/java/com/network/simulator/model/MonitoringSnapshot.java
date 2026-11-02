package com.network.simulator.model;

import java.time.Instant;

public class MonitoringSnapshot {
    private Instant timestamp;
    private long totalPackets;
    private long deliveredPackets;
    private long failedPackets;
    private double packetDeliveryRatio;
    private double averageDeliveryTime;
    private int activeNodes;
    private int failedNodes;
    private int activeLinks;
    private int failedLinks;

    public MonitoringSnapshot() {
        this.timestamp = Instant.now();
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public long getTotalPackets() { return totalPackets; }
    public void setTotalPackets(long totalPackets) { this.totalPackets = totalPackets; }
    public long getDeliveredPackets() { return deliveredPackets; }
    public void setDeliveredPackets(long deliveredPackets) { this.deliveredPackets = deliveredPackets; }
    public long getFailedPackets() { return failedPackets; }
    public void setFailedPackets(long failedPackets) { this.failedPackets = failedPackets; }
    public double getPacketDeliveryRatio() { return packetDeliveryRatio; }
    public void setPacketDeliveryRatio(double packetDeliveryRatio) { this.packetDeliveryRatio = packetDeliveryRatio; }
    public double getAverageDeliveryTime() { return averageDeliveryTime; }
    public void setAverageDeliveryTime(double averageDeliveryTime) { this.averageDeliveryTime = averageDeliveryTime; }
    public int getActiveNodes() { return activeNodes; }
    public void setActiveNodes(int activeNodes) { this.activeNodes = activeNodes; }
    public int getFailedNodes() { return failedNodes; }
    public void setFailedNodes(int failedNodes) { this.failedNodes = failedNodes; }
    public int getActiveLinks() { return activeLinks; }
    public void setActiveLinks(int activeLinks) { this.activeLinks = activeLinks; }
    public int getFailedLinks() { return failedLinks; }
    public void setFailedLinks(int failedLinks) { this.failedLinks = failedLinks; }
}
