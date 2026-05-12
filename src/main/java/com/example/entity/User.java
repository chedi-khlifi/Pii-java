package com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "`user`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Current schema uses single role column (ROLE_ADMIN / ROLE_USER).
     */
    @Column(name = "role", nullable = true, insertable = false, updatable = false)
    private String role;

    /**
     * Legacy in-memory/Symfony-style roles JSON support.
     * Marked transient so Hibernate won't query a non-existing 'roles' column.
     */
    @Transient
    private String roles;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<ChatMessage> sentMessages;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private List<Claim> createdClaims;

    @OneToMany(mappedBy = "assignedTo", cascade = CascadeType.ALL)
    private List<Claim> assignedClaims;

    @OneToMany(mappedBy = "sharedBy", cascade = CascadeType.ALL)
    private List<SharedTask> sharedTasks;

    @OneToMany(mappedBy = "sharedWith", cascade = CascadeType.ALL)
    private List<SharedTask> receivedTasks;

    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String email, String password, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Raw JSON roles string from Symfony, e.g. ["ROLE_ADMIN","ROLE_USER"] */
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    /**
     * Returns true if this user has ROLE_ADMIN.
     * Works with both single role column and legacy roles JSON string.
     */
    public boolean isAdmin() {
        if (role != null && role.contains("ROLE_ADMIN")) return true;
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<ChatMessage> getSentMessages() { return sentMessages; }
    public void setSentMessages(List<ChatMessage> sentMessages) { this.sentMessages = sentMessages; }

    public List<Claim> getCreatedClaims() { return createdClaims; }
    public void setCreatedClaims(List<Claim> createdClaims) { this.createdClaims = createdClaims; }

    public List<Claim> getAssignedClaims() { return assignedClaims; }
    public void setAssignedClaims(List<Claim> assignedClaims) { this.assignedClaims = assignedClaims; }

    public List<SharedTask> getSharedTasks() { return sharedTasks; }
    public void setSharedTasks(List<SharedTask> sharedTasks) { this.sharedTasks = sharedTasks; }

    public List<SharedTask> getReceivedTasks() { return receivedTasks; }
    public void setReceivedTasks(List<SharedTask> receivedTasks) { this.receivedTasks = receivedTasks; }

    @Override
    public String toString() {
        return username != null ? username : "User#" + id;
    }
}
