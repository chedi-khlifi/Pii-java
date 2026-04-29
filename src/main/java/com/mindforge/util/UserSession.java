package com.mindforge.util;

/**
 * Singleton that holds the currently logged-in user's data for the
 * lifetime of the JavaFX application session.
 */
public class UserSession {

    private static UserSession instance;

    private int    userId;
    private String email;
    private String roles;
    private boolean loggedIn;

    // ── Private constructor (singleton) ──────────────────────────────
    private UserSession() {
        this.loggedIn = false;
    }

    // ── Singleton accessor ────────────────────────────────────────────
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // ── Login: call this after a successful DB authentication ─────────
    public void login(int userId, String email, String roles) {
        this.userId   = userId;
        this.email    = email;
        this.roles    = roles;
        this.loggedIn = true;
    }

    // ── Logout: wipe session data ─────────────────────────────────────
    public void logout() {
        this.userId   = 0;
        this.email    = null;
        this.roles    = null;
        this.loggedIn = false;
    }

    // ── Getters ───────────────────────────────────────────────────────
    public int     getUserId()   { return userId;   }
    public String  getEmail()    { return email;    }
    public String  getRoles()    { return roles;    }
    public boolean isLoggedIn()  { return loggedIn; }

    // ── Convenience role helpers ──────────────────────────────────────
    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    public boolean isStudentPlus() {
        return roles != null && roles.contains("ROLE_STUDENT_PLUS");
    }

    public boolean isCompany() {
        return roles != null && roles.contains("ROLE_COMPANY");
    }

}
