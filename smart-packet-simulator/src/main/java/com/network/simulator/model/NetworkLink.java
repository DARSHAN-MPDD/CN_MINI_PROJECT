package com.network.simulator.model;

import java.util.Objects;

/**
 * Represents an edge/link between two network nodes.
 */
public class NetworkLink {
    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private double cost;
    private LinkStatus status;

    public NetworkLink() {
    }

    public NetworkLink(String id, String sourceNodeId, String targetNodeId, double cost, LinkStatus status) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.cost = cost;
        this.status = status == null ? LinkStatus.ACTIVE : status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public LinkStatus getStatus() {
        return status;
    }

    public void setStatus(LinkStatus status) {
        this.status = status == null ? LinkStatus.ACTIVE : status;
    }

    public boolean isActive() {
        return status == LinkStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkLink that = (NetworkLink) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
