package com.example.service;

import com.example.entity.User;

/**
 * Thread-safe singleton holding the currently authenticated user for the session.
 * Other modules can call setCurrentUser() after their own login flow and this
 * module will respect that session automatically.
 */
public class UserSession {

    // Volatile + holder pattern: thread-safe without synchronization overhead
    private static volatile UserSession instance;
    private volatile User currentUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    public User getCurrentUser() { return currentUser; }

    public void setCurrentUser(User user) { this.currentUser = user; }

    public boolean isLoggedIn() { return currentUser != null; }

    /** Delegates to User.isAdmin() which checks the role column */
    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public void logout() { currentUser = null; }
}
