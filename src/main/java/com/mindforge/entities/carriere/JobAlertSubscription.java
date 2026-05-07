package com.mindforge.entities.carriere;

import java.time.LocalDateTime;

public class JobAlertSubscription {

    private int id;
    private int userId;
    private String keywords;
    private String type;
    private String location;
    private boolean isActive;
    private LocalDateTime createdAt;

    public JobAlertSubscription() { this.isActive = true; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "JobAlertSubscription{id=" + id + ", keywords='" + keywords +
               "', type='" + type + "', location='" + location + "', active=" + isActive + "}";
    }
}
