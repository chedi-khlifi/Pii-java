package com.mindforge.entities.carriere;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OpportuniteCarriere {

    private int id;
    private String title;
    private String description;
    private String type;
    private String location;
    private String duration;
    private LocalDate deadline;
    private String status;
    private LocalDateTime createdAt;
    private int companyId;
    private String companyName; // display only, populated via JOIN

    public OpportuniteCarriere() {}

    public OpportuniteCarriere(String title, String description, String type,
                                String location, String duration, LocalDate deadline,
                                String status, int companyId) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.location = location;
        this.duration = duration;
        this.deadline = deadline;
        this.status = status;
        this.companyId = companyId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    @Override
    public String toString() {
        return "OpportuniteCarriere{id=" + id + ", title='" + title + "', type='" + type +
               "', status='" + status + "', company='" + companyName + "'}";
    }
}
