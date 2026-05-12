package com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "virtual_room_participants")
public class VirtualRoomParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "virtual_room_id", nullable = false)
    private VirtualRoom virtualRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_active")
    private boolean isActive = true;

    public VirtualRoomParticipant() {
        this.joinedAt = LocalDateTime.now();
    }

    public VirtualRoomParticipant(VirtualRoom virtualRoom, User user) {
        this.virtualRoom = virtualRoom;
        this.user = user;
        this.joinedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public VirtualRoom getVirtualRoom() { return virtualRoom; }
    public void setVirtualRoom(VirtualRoom virtualRoom) { this.virtualRoom = virtualRoom; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "VirtualRoomParticipant{" +
                "id=" + id +
                ", virtualRoom=" + (virtualRoom != null ? virtualRoom.getName() : "null") +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", isActive=" + isActive +
                '}';
    }
}