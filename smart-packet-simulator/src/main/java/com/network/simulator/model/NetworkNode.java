package com.network.simulator.model;

import java.util.Objects;

/**
 * Represents a network device/node in the simulation.
 */
public class NetworkNode {
    private String id;
    private String name;
    private NodeType type;
    private NodeStatus status;

    public NetworkNode() {
    }

    public NetworkNode(String id, String name, NodeType type, NodeStatus status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status == null ? NodeStatus.ACTIVE : status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status == null ? NodeStatus.ACTIVE : status;
    }

    public boolean isActive() {
        return status == NodeStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkNode that = (NetworkNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
