package com.mindforge.entities.carriere;

import java.time.LocalDateTime;

public class Demande {

    private int id;
    private int userId;
    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;
    private int opportunityId;
    private String opportunityTitle; // display only, populated via JOIN

    public Demande() {}

    public Demande(int userId, String coverLetter, String status, int opportunityId) {
        this.userId = userId;
        this.coverLetter = coverLetter;
        this.status = status;
        this.opportunityId = opportunityId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public int getOpportunityId() { return opportunityId; }
    public void setOpportunityId(int opportunityId) { this.opportunityId = opportunityId; }

    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }

    @Override
    public String toString() {
        return "Demande{id=" + id + ", userId=" + userId + ", status='" + status +
               "', opportunity='" + opportunityTitle + "'}";
    }
}
