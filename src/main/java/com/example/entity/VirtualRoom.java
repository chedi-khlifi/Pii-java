package com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "virtual_room")
public class VirtualRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "room_type")
    private String roomType;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Transient
    private String subject;

    @Transient
    private int capacity = 10;

    @Transient
    private String status = "ACTIVE";

    @Transient
    private Integer creatorId;

    @Transient
    private String creatorName;

    @Transient
    private int participantCount;

    @Transient
    private boolean currentUserJoined;

    @OneToMany(mappedBy = "virtualRoom", cascade = CascadeType.ALL)
    private List<ChatMessage> messages;

    public VirtualRoom() {
        this.createdAt = LocalDateTime.now();
    }

    public VirtualRoom(String name, String description, String roomType) {
        this.name = name;
        this.description = description;
        this.roomType = roomType;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return name != null ? name : "Room#" + id;
    }
}
