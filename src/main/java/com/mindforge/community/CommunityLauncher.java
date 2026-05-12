package com.mindforge.community;

import com.example.entity.User;
import com.example.service.UserService;
import com.example.service.UserSession;
import com.example.util.SchemaMigrator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Bridge between the main MindForge application and the Community Module.
 *
 * Responsibilities:
 *  1. Run SchemaMigrator.migrateAll() once to ensure community tables exist.
 *  2. Resolve the currently logged-in MindForge user into a community User entity.
 *  3. Set that entity on the community UserSession singleton.
 *  4. Open the CommunityHubView in a new Stage.
 */
public final class CommunityLauncher {

    private CommunityLauncher() {}

    /**
     * Migrate schema, bridge the session, and open the Community Hub.
     *
     * @param ownerStage  the calling stage (used for positioning the new window)
     */
    public static void launch(Stage ownerStage) {
        // 1. Migrate schema (idempotent — safe to call multiple times)
        try {
            SchemaMigrator.migrateAll();
        } catch (Exception e) {
            System.err.println("[CommunityLauncher] Schema migration warning: " + e.getMessage());
        }

        // 2. Bridge session: look up the community User entity by email
        bridgeSession();

        // 3. Open the Community Hub
        try {
            FXMLLoader loader = new FXMLLoader(
                    CommunityLauncher.class.getResource("/views/CommunityHubView.fxml"));
            Parent root = loader.load();

            com.example.controllers.CommunityHubController ctrl = loader.getController();

            // Create a minimal MainController so back-navigation works
            com.example.controllers.MainController mainCtrl = new com.example.controllers.MainController();

            Stage communityStage = new Stage();
            communityStage.setTitle("Community Hub — MindForge");

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(
                        CommunityLauncher.class.getResource("/styles/main.css").toExternalForm());
            } catch (Exception ignored) {}

            communityStage.setScene(scene);
            communityStage.setMaximized(true);
            communityStage.setResizable(true);

            // Wire the controller after the stage is set
            mainCtrl.setPrimaryStage(communityStage);
            ctrl.setMainController(mainCtrl);

            communityStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Community Hub");
            alert.setHeaderText("Could not open Community Hub");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Resolves the MindForge session user into a community {@link User} entity
     * and stores it in the community {@link UserSession}.
     * Public so callers (e.g. DashboardController) can bridge the session
     * before loading community views inline.
     */
    public static void bridgeSession() {
        // Already bridged?
        if (UserSession.getInstance().isLoggedIn()) return;

        // Read from the main project's UserSession
        com.mindforge.util.UserSession mfSession = com.mindforge.util.UserSession.getInstance();
        if (!mfSession.isLoggedIn()) return;

        String email  = mfSession.getEmail();
        int    userId = mfSession.getUserId();
        String roles  = mfSession.getRoles();

        UserService userService = null;
        try {
            userService = new UserService();

            // Try to find by email first (most reliable)
            Optional<User> found = Optional.empty();
            if (email != null && !email.isBlank()) {
                found = userService.findByEmail(email);
            }

            // Fall back to id lookup
            if (found.isEmpty() && userId > 0) {
                found = userService.findById(userId);
            }

            if (found.isPresent()) {
                User communityUser = found.get();
                // Propagate roles from MindForge session if the community entity lacks them
                if (communityUser.getRole() == null && roles != null) {
                    communityUser.setRoles(roles);
                }
                UserSession.getInstance().setCurrentUser(communityUser);
                System.out.println("[CommunityLauncher] Session bridged for: "
                        + communityUser.getUsername());
            } else {
                // Create a transient User so the community module still works
                User transientUser = buildTransientUser(userId, email, roles);
                UserSession.getInstance().setCurrentUser(transientUser);
                System.out.println("[CommunityLauncher] Transient session created for userId=" + userId);
            }
        } catch (Exception e) {
            System.err.println("[CommunityLauncher] Session bridge failed: " + e.getMessage());
            // Create minimal transient user so the UI doesn't crash
            UserSession.getInstance().setCurrentUser(buildTransientUser(userId, email, roles));
        } finally {
            if (userService != null) {
                try { userService.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static User buildTransientUser(int userId, String email, String roles) {
        User u = new User();
        u.setId(userId);
        u.setEmail(email != null ? email : "unknown@mindforge.local");
        u.setUsername(email != null ? email.split("@")[0] : "user_" + userId);
        u.setPassword("n/a");
        u.setRoles(roles);
        return u;
    }
}
