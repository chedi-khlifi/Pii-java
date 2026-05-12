package com.example.util;

import com.example.entity.*;
import com.example.service.*;

import java.time.LocalDateTime;

/**
 * Utility class to set up test data for the Community module.
 * Run this once to populate the database with sample data.
 */
public class TestDataSetup {
    
    public static void setupTestData() {
        UserService userService = new UserService();
        VirtualRoomService roomService = new VirtualRoomService();
        
        try {
            // Create test users if they don't exist
            createTestUser(userService, "admin", "admin@mindforge.com", "admin123", "Admin", "User", "ROLE_ADMIN");
            createTestUser(userService, "alice", "alice@mindforge.com", "alice123", "Alice", "Johnson", "ROLE_USER");
            createTestUser(userService, "bob", "bob@mindforge.com", "bob123", "Bob", "Smith", "ROLE_USER");
            createTestUser(userService, "charlie", "charlie@mindforge.com", "charlie123", "Charlie", "Brown", "ROLE_USER");
            
            // Create test virtual rooms
            createTestRoom(roomService, "General", "General discussion room", "public");
            createTestRoom(roomService, "Tech Talk", "Technical discussions and programming", "public");
            createTestRoom(roomService, "Random", "Random conversations and off-topic", "public");
            createTestRoom(roomService, "Help Desk", "Get help and support", "support");
            
            System.out.println("Test data setup completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error setting up test data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createTestUser(UserService userService, String username, String email, 
                                     String password, String firstName, String lastName, String role) {
        try {
            if (userService.findByUsername(username).isEmpty()) {
                User user = new User(username, email, password, firstName, lastName);
                // roles stored as Symfony JSON array
                user.setRoles("[\"" + role + "\"]");
                userService.save(user);
                System.out.println("Created user: " + username + " (" + role + ")");
            } else {
                System.out.println("User already exists: " + username);
            }
        } catch (Exception e) {
            System.err.println("Error creating user " + username + ": " + e.getMessage());
        }
    }
    
    private static void createTestRoom(VirtualRoomService roomService, String name, 
                                     String description, String roomType) {
        try {
            if (roomService.findByName(name).isEmpty()) {
                VirtualRoom room = new VirtualRoom(name, description, roomType);
                roomService.save(room);
                System.out.println("Created room: " + name);
            } else {
                System.out.println("Room already exists: " + name);
            }
        } catch (Exception e) {
            System.err.println("Error creating room " + name + ": " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        System.out.println("Setting up test data for MindForge Community module...");
        setupTestData();
    }
}