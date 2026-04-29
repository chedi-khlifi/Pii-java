package com.mindforge.model;

import java.sql.Timestamp;

public class RoleRequest {
    private int       id;
    private int       userId;
    private String    userEmail;
    private String    motivation;
    private String    status;
    private Timestamp requestedAt;

    public RoleRequest(int id, int userId, String userEmail,
                       String motivation, String status, Timestamp requestedAt) {
        this.id          = id;
        this.userId      = userId;
        this.userEmail   = userEmail;
        this.motivation  = motivation;
        this.status      = status;
        this.requestedAt = requestedAt;
    }

    public int       getId()          { return id; }
    public int       getUserId()      { return userId; }
    public String    getUserEmail()   { return userEmail; }
    public String    getMotivation()  { return motivation; }
    public String    getStatus()      { return status; }
    public Timestamp getRequestedAt() { return requestedAt; }
}