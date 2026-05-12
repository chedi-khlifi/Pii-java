package com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "shared_task")
public class SharedTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Convert(converter = TaskStatusConverter.class)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Convert(converter = TaskCategoryConverter.class)
    @Column(nullable = false)
    private TaskCategory category;

    @Convert(converter = TaskDifficultyConverter.class)
    private TaskDifficulty difficulty;

    private String attachment;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String attachmentFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private User sharedWith;

    public enum TaskStatus {
        PENDING, ACCEPTED, REJECTED, COMPLETED
    }

    public enum TaskCategory {
        TECH_SKILLS, SOFT_SKILLS, PHYSICAL, CREATIVE
    }

    public enum TaskDifficulty {
        EASY, MEDIUM, HARD
    }

    @Converter(autoApply = false)
    public static class TaskStatusConverter implements AttributeConverter<TaskStatus, String> {
        @Override
        public String convertToDatabaseColumn(TaskStatus attribute) {
            return attribute != null ? attribute.name() : null;
        }

        @Override
        public TaskStatus convertToEntityAttribute(String dbData) {
            return parseEnum(dbData, TaskStatus.class, TaskStatus.PENDING);
        }
    }

    @Converter(autoApply = false)
    public static class TaskCategoryConverter implements AttributeConverter<TaskCategory, String> {
        @Override
        public String convertToDatabaseColumn(TaskCategory attribute) {
            return attribute != null ? attribute.name() : null;
        }

        @Override
        public TaskCategory convertToEntityAttribute(String dbData) {
            return parseEnum(dbData, TaskCategory.class, TaskCategory.TECH_SKILLS);
        }
    }

    @Converter(autoApply = false)
    public static class TaskDifficultyConverter implements AttributeConverter<TaskDifficulty, String> {
        @Override
        public String convertToDatabaseColumn(TaskDifficulty attribute) {
            return attribute != null ? attribute.name() : null;
        }

        @Override
        public TaskDifficulty convertToEntityAttribute(String dbData) {
            return parseEnum(dbData, TaskDifficulty.class, null);
        }
    }

    private static <E extends Enum<E>> E parseEnum(String dbData, Class<E> enumType, E fallback) {
        if (dbData == null || dbData.trim().isEmpty()) return fallback;
        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public SharedTask() {
        this.createdAt = LocalDateTime.now();
    }

    public SharedTask(String title, String description, TaskCategory category, User sharedBy, User sharedWith) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.sharedBy = sharedBy;
        this.sharedWith = sharedWith;
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
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        if (status != TaskStatus.PENDING) {
            this.respondedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public void setCategory(TaskCategory category) {
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public TaskDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(TaskDifficulty difficulty) {
        this.difficulty = difficulty;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAttachmentFile() {
        return attachmentFile;
    }

    public void setAttachmentFile(String attachmentFile) {
        this.attachmentFile = attachmentFile;
    }

    public User getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(User sharedBy) {
        this.sharedBy = sharedBy;
    }

    public User getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(User sharedWith) {
        this.sharedWith = sharedWith;
    }

    @Override
    public String toString() {
        return "SharedTask{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", category=" + category +
                ", difficulty=" + difficulty +
                ", sharedBy=" + (sharedBy != null ? sharedBy.getUsername() : "null") +
                ", sharedWith=" + (sharedWith != null ? sharedWith.getUsername() : "null") +
                '}';
    }
}
