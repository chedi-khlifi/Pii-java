package com.mindforge.controller;

import com.mindforge.model.UserModel;
import com.mindforge.service.GoogleOAuthService;
import com.mindforge.util.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Objects;

public class RegisterController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button googleButton;

    private final GoogleOAuthService googleOAuthService;

    public RegisterController() {
        this.googleOAuthService = new GoogleOAuthService();
    }

    @FXML
    private void handleRegister() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("⚠️ Please fill in all fields!", "orange");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showMessage("⚠️ Please enter a valid email!", "orange");
            return;
        }

        if (password.length() < 6) {
            showMessage("⚠️ Password must be at least 6 characters!", "orange");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("⚠️ Passwords do not match!", "orange");
            return;
        }

        if (emailExists(email)) {
            showMessage("❌ This email is already registered!", "red");
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        if (registerUser(email, hashedPassword)) {
            showMessage("✅ Account created successfully!", "green");
            pauseThenNavigate(this::goToLogin);
        } else {
            showMessage("❌ Registration failed! Try again.", "red");
        }
    }

    @FXML
    private void handleGoogleRegister() {
        showMessage("🌐 Opening Google sign-in...", "#7c3aed");
        googleButton.setDisable(true);

        googleOAuthService.authenticate()
                .thenAccept(userInfo -> Platform.runLater(() -> {
                    processGoogleUser(userInfo);
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showMessage("❌ " + throwable.getCause().getMessage(), "red");
                        googleButton.setDisable(false);
                    });
                    return null;
                });
    }

    /**
     * Process authenticated Google user
     */
    private void processGoogleUser(GoogleOAuthService.GoogleUserInfo userInfo) {
        try {
            if (emailExists(userInfo.email)) {
                // Existing user - login and go to password setup
                UserModel existingUser = getUserByEmail(userInfo.email);
                UserSession.getInstance().login(
                        existingUser.getId(),
                        userInfo.email,
                        "[\"ROLE_USER\"]"
                );
                showMessage("✅ Welcome back! Please set your password.", "green");
                pauseThenNavigate(this::goToSetupPassword);
            } else {
                // Create new Google user
                String tempPassword = BCrypt.hashpw(
                        java.util.UUID.randomUUID().toString(),
                        BCrypt.gensalt()
                );

                if (registerUser(userInfo.email, tempPassword)) {
                    UserModel newUser = getUserByEmail(userInfo.email);
                    UserSession.getInstance().login(
                            newUser.getId(),
                            userInfo.email,
                            "[\"ROLE_USER\"]"
                    );
                    showMessage("✅ Account created! Please set your password.", "green");
                    pauseThenNavigate(this::goToSetupPassword);
                } else {
                    showMessage("❌ Failed to create account", "red");
                    googleButton.setDisable(false);
                }
            }
        } catch (Exception e) {
            showMessage("❌ Error: " + e.getMessage(), "red");
            googleButton.setDisable(false);
            e.printStackTrace();
        }
    }

    private boolean registerUser(String email, String hashedPassword) {
        String userSql = "INSERT INTO user (email, password, roles, is_verified, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, email);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, "[\"ROLE_USER\"]");
            pstmt.setInt(4, 1);
            pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));

            int rows = pstmt.executeUpdate();
            if (rows == 0) return false;

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                int newUserId = keys.getInt(1);
                createUserProfile(conn, newUserId);
                createUserStats(conn, newUserId);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createUserProfile(Connection conn, int userId) throws SQLException {
        try {
            String profileSql = "INSERT INTO profile (user_id, timezone, locale) VALUES (?, 'UTC', 'en')";
            try (PreparedStatement ps = conn.prepareStatement(profileSql)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Note: Could not create profile: " + e.getMessage());
        }
    }

    private void createUserStats(Connection conn, int userId) throws SQLException {
        try {
            String statsSql = "INSERT INTO gamification_stats " +
                    "(user_id, total_xp, current_level, streak_days, tasks_completed, total_focus_time) " +
                    "VALUES (?, 0, 1, 0, 0, 0)";
            try (PreparedStatement ps = conn.prepareStatement(statsSql)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Note: Could not create gamification stats: " + e.getMessage());
        }
    }

    private boolean emailExists(String email) {
        String sql = "SELECT id FROM user WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet result = pstmt.executeQuery();
            return result.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private UserModel getUserByEmail(String email) {
        String sql = "SELECT id, email, is_verified, created_at FROM user WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new UserModel(
                        rs.getInt("id"),
                        rs.getString("email"),
                        String.valueOf(rs.getBoolean("is_verified")),
                        rs.getTimestamp("created_at").toString()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void pauseThenNavigate(Runnable action) {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Platform.runLater(action);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        navigateToScene("/com/mindforge/fxml/login.fxml", "MindForge - Login", 500, 400);
    }

    private void goToSetupPassword() {
        navigateToScene("/com/mindforge/fxml/setup_password.fxml", "MindForge - Set Password", 500, 400);
    }

    private void navigateToScene(String fxmlPath, String title, int width, int height) {
        try {
            java.net.URL location = getClass().getResource(fxmlPath);
            if (location == null) {
                System.err.println("FXML not found: " + fxmlPath);
                showMessage("❌ Page not found: " + fxmlPath, "red");
                return;
            }

            FXMLLoader loader = new FXMLLoader(location);
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            try {
                scene.getStylesheets().add(
                        Objects.requireNonNull(
                                getClass().getResource("/com/mindforge/css/style.css")
                        ).toExternalForm()
                );
            } catch (Exception e) {
                // CSS not found - continue without it
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("❌ Failed to load page: " + e.getMessage(), "red");
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mindforge_db", "root", ""
        );
    }

    private void showMessage(String message, String color) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;");
    }
}