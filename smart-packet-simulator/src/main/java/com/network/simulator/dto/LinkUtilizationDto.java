package com.network.simulator.dto;

public class LinkUtilizationDto {
    private String linkId;
    private String sourceNodeId;
    private String targetNodeId;
    private int packetCount;
    private int configuredCapacity;
    private double utilizationPercentage;
    private boolean congested;
    private String statusNote;

    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) { this.linkId = linkId; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    public int getPacketCount() { return packetCount; }
    public void setPacketCount(int packetCount) { this.packetCount = packetCount; }
    public int getConfiguredCapacity() { return configuredCapacity; }
    public void setConfiguredCapacity(int configuredCapacity) { this.configuredCapacity = configuredCapacity; }
    public double getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }
    public boolean isCongested() { return congested; }
    public void setCongested(boolean congested) { this.congested = congested; }
    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String statusNote) { this.statusNote = statusNote; }
}
