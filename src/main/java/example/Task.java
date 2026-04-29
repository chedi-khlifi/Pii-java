package org.example;

public class Task {
    private int id;
    private String title;
    private String description;
    private String status;
    private int priority;
    private String dueDate;
    private int ownerId;
    private int estimatedMinutes;

    public Task(int id, String title, String description, String status, int priority, String dueDate, int ownerId, int estimatedMinutes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.ownerId = ownerId;
        this.estimatedMinutes = estimatedMinutes;
    }

    public int getId()                 { return id; }
    public String getTitle()           { return title; }
    public String getDescription()     { return description; }
    public String getStatus()          { return status; }
    public int getPriority()           { return priority; }
    public String getDueDate()         { return dueDate; }
    public int getOwnerId()            { return ownerId; }
    public int getEstimatedMinutes()   { return estimatedMinutes; }

    public void setTitle(String title)             { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status)           { this.status = status; }
    public void setPriority(int priority)          { this.priority = priority; }
    public void setDueDate(String dueDate)         { this.dueDate = dueDate; }
    public void setEstimatedMinutes(int m)         { this.estimatedMinutes = m; }
}