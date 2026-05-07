package com.mindforge.entities.carriere;

import java.time.LocalDateTime;

public class JobAlertNotification {

    private int id;
    private int subscriptionId;
    private int opportuniteId;
    private LocalDateTime sentAt;
    private boolean isRead;

    // Populated via JOIN for display
    private String opportunityTitle;
    private String opportunityType;
    private String opportunityLocation;
    private String companyName;
    private String subscriptionKeywords;

    public JobAlertNotification() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(int subscriptionId) { this.subscriptionId = subscriptionId; }

    public int getOpportuniteId() { return opportuniteId; }
    public void setOpportuniteId(int opportuniteId) { this.opportuniteId = opportuniteId; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }

    public String getOpportunityType() { return opportunityType; }
    public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }

    public String getOpportunityLocation() { return opportunityLocation; }
    public void setOpportunityLocation(String opportunityLocation) { this.opportunityLocation = opportunityLocation; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSubscriptionKeywords() { return subscriptionKeywords; }
    public void setSubscriptionKeywords(String subscriptionKeywords) { this.subscriptionKeywords = subscriptionKeywords; }
}
