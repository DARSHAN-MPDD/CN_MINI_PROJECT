package com.network.simulator.model;

import java.time.Instant;
import java.util.UUID;

public class FailureEvent {
    private String eventId;
    private String eventType;
    private String affectedNodeOrLink;
    private Instant timestamp;
    private String previousStatus;
    private String newStatus;
    private String recoveryAction;
    private String recoveryResult;
    private String explanation;

    public FailureEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public FailureEvent(String eventType, String affectedNodeOrLink, String previousStatus, String newStatus, String explanation) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.affectedNodeOrLink = affectedNodeOrLink;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.explanation = explanation;
        this.timestamp = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAffectedNodeOrLink() {
        return affectedNodeOrLink;
    }

    public void setAffectedNodeOrLink(String affectedNodeOrLink) {
        this.affectedNodeOrLink = affectedNodeOrLink;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getRecoveryAction() {
        return recoveryAction;
    }

    public void setRecoveryAction(String recoveryAction) {
        this.recoveryAction = recoveryAction;
    }

    public String getRecoveryResult() {
        return recoveryResult;
    }

    public void setRecoveryResult(String recoveryResult) {
        this.recoveryResult = recoveryResult;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
