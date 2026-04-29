package com.mindforge.controller;

import com.mindforge.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Objects;

public class SetupPasswordController {

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleSetPassword() {
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        // Validation
        if (password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("⚠️ Please fill in both fields!", "orange");
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

        // Update password in database
        if (updatePassword(password)) {
            showMessage("✅ Password set successfully!", "green");

            // Logout and redirect to login
            UserSession.getInstance().logout();

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(this::goToLogin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showMessage("❌ Failed to set password!", "red");
        }
    }

    private boolean updatePassword(String password) {
        String sql = "UPDATE user SET password = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, UserSession.getInstance().getUserId());

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/mindforge/fxml/login.fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root, 500, 400);

            try {
                scene.getStylesheets().add(
                    Objects.requireNonNull(
                        getClass().getResource("/com/mindforge/css/style.css")
                    ).toExternalForm()
                );
            } catch (Exception e) {
                // CSS not found
            }

            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setTitle("MindForge - Login");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("❌ Failed to load login page", "red");
        }
    }

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/mindforge_db", "root", ""
        );
    }

    private void showMessage(String message, String color) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;");
    }
}