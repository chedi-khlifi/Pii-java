package com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "claim")
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Convert(converter = ClaimStatusConverter.class)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.OPEN;

    @Convert(converter = ClaimPriorityConverter.class)
    @Column(nullable = false)
    private ClaimPriority priority = ClaimPriority.MEDIUM;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    public enum ClaimStatus {
        OPEN, IN_PROGRESS, RESOLVED, CLOSED
    }

    public enum ClaimPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @Converter(autoApply = false)
    public static class ClaimStatusConverter implements AttributeConverter<ClaimStatus, String> {
        @Override
        public String convertToDatabaseColumn(ClaimStatus attribute) {
            return attribute == null ? null : attribute.name();
        }

        @Override
        public ClaimStatus convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return ClaimStatus.OPEN;
            }
            String normalized = dbData.trim()
                    .toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            try {
                return ClaimStatus.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                return ClaimStatus.OPEN;
            }
        }
    }

    @Converter(autoApply = false)
    public static class ClaimPriorityConverter implements AttributeConverter<ClaimPriority, String> {
        @Override
        public String convertToDatabaseColumn(ClaimPriority attribute) {
            return attribute == null ? null : attribute.name();
        }

        @Override
        public ClaimPriority convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return ClaimPriority.MEDIUM;
            }
            String normalized = dbData.trim()
                    .toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            try {
                return ClaimPriority.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                return ClaimPriority.MEDIUM;
            }
        }
    }

    public Claim() {
        this.createdAt = LocalDateTime.now();
    }

    public Claim(String title, String description, ClaimPriority priority, User createdBy) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
        if (status == ClaimStatus.RESOLVED || status == ClaimStatus.CLOSED) {
            this.resolvedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public ClaimPriority getPriority() {
        return priority;
    }

    public void setPriority(ClaimPriority priority) {
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Claim{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", createdBy=" + (createdBy != null ? createdBy.getUsername() : "null") +
                ", assignedTo=" + (assignedTo != null ? assignedTo.getUsername() : "null") +
                '}';
    }
}
