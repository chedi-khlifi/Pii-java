package com.example.virtualrooms.model;

import java.time.LocalDateTime;

public class VirtualRoom {
    private int id;
    private String name;
    private String description;
    private String subject;
    private int capacity;
    private String status;
    private Integer creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private int participantCount;
    private boolean currentUserJoined;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public boolean isCurrentUserJoined() {
        return currentUserJoined;
    }

    public void setCurrentUserJoined(boolean currentUserJoined) {
        this.currentUserJoined = currentUserJoined;
    }

    public String getCapacityLabel() {
        int cap = capacity <= 0 ? 10 : capacity;
        return participantCount + "/" + cap;
    }

    public String getStatusLabel() {
        return status == null || status.isBlank() ? "ACTIVE" : status.toUpperCase();
    }
}
