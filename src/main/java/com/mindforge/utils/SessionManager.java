package com.mindforge.utils;

public class SessionManager {

    public enum Role {
        STUDENT,
        COMPANY_OWNER
    }

    private static Role currentRole;
    private static int currentUserId;
    private static String currentUserName;

    private SessionManager() {}

    public static void setSession(Role role, int userId, String userName) {
        currentRole = role;
        currentUserId = userId;
        currentUserName = userName;
    }

    public static Role getCurrentRole() { return currentRole; }
    public static int getCurrentUserId() { return currentUserId; }
    public static String getCurrentUserName() { return currentUserName; }

    public static void clear() {
        currentRole = null;
        currentUserId = 0;
        currentUserName = null;
    }
}
