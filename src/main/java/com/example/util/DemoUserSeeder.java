package com.example.util;

import com.example.entity.User;
import com.example.service.UserService;

/**
 * Creates demo users once so challenge flows can be tested quickly.
 */
public final class DemoUserSeeder {

    private DemoUserSeeder() {}

    public static void seedIfMissing() {
        UserService userService = new UserService();
        try {
            createIfMissing(userService, "nora", "nora@mindforge.com", "nora123", "Nora", "Lane");
            createIfMissing(userService, "samir", "samir@mindforge.com", "samir123", "Samir", "Khan");
            createIfMissing(userService, "lea", "lea@mindforge.com", "lea123", "Lea", "Martin");
            createIfMissing(userService, "yassine", "yassine@mindforge.com", "yassine123", "Yassine", "Amri");
            createIfMissing(userService, "mila", "mila@mindforge.com", "mila123", "Mila", "Noor");
        } catch (Exception e) {
            System.err.println("Demo user seeding skipped: " + e.getMessage());
        } finally {
            userService.close();
        }
    }

    private static void createIfMissing(UserService userService,
                                        String username,
                                        String email,
                                        String password,
                                        String firstName,
                                        String lastName) {
        if (userService.findByUsername(username).isPresent()) return;
        User u = new User(username, email, password, firstName, lastName);
        userService.save(u);
    }
}
