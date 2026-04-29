package com.mindforge.controller;

import com.mindforge.service.EmailService;
import com.mindforge.util.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.sql.*;
import java.util.Optional;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final EmailService emailService = new EmailService();

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("⚠️ Please fill in all fields!", "orange");
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, email, password, roles FROM user WHERE email = ?")) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");

                if (BCrypt.checkpw(password, hashedPassword)) {
                    int userId = rs.getInt("id");
                    String roles = rs.getString("roles");

                    UserSession.getInstance().login(userId, email, roles);
                    showMessage("✅ Login successful!", "green");

                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(this::goToDashboard);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();

                } else {
                    showMessage("❌ Invalid password!", "red");
                }
            } else {
                showMessage("❌ Email not found!", "red");
            }

        } catch (Exception e) {
            showMessage("❌ Login error: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    /**
     * Show forgot password dialog - sends code via email
     */
    @FXML
    private void showForgotPasswordDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Enter your email to receive reset code");

        ButtonType sendButtonType = new ButtonType("Send Code", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField emailInput = new TextField();
        emailInput.setPromptText("your@email.com");
        emailInput.setStyle("-fx-background-color: #2e2e3e; -fx-text-fill: white; -fx-prompt-text-fill: #777;");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailInput, 1, 0);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(emailInput::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                return emailInput.getText().trim();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(email -> {
            if (email.isEmpty() || !email.contains("@")) {
                showAlert(Alert.AlertType.WARNING, "Invalid Email", "Please enter a valid email address.");
                return;
            }

            if (!emailExists(email)) {
                // Don't reveal if email exists for security
                showAlert(Alert.AlertType.INFORMATION, "Email Sent",
                        "If an account exists with this email, you will receive reset instructions shortly.");
                return;
            }

            // Generate and save token
            String resetCode = generateResetToken();
            String userName = getUserNameByEmail(email);

            // Save to database with expiration
            if (saveResetToken(email, resetCode)) {
                // Send email
                boolean sent = emailService.sendPasswordResetEmail(email, resetCode, userName);

                if (sent) {
                    showAlert(Alert.AlertType.INFORMATION, "Email Sent",
                            "A password reset code has been sent to " + email +
                                    "\n\nPlease check your inbox (and spam folder).");

                    // Show reset dialog
                    showPasswordResetDialog(email, resetCode);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to send email. Please try again later or contact support.");
                }
            }
        });
    }

    private String generateResetToken() {
        return java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Get user name by email
     */
    private String getUserNameByEmail(String email) {
        // Try to get from profile first, then email prefix
        String sql = "SELECT p.display_name FROM profile p JOIN user u ON p.user_id = u.id WHERE u.email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getString("display_name") != null) {
                return rs.getString("display_name");
            }
        } catch (Exception e) {
            // Ignore, fallback to email
        }
        return email.split("@")[0];
    }

    /**
     * Save reset token to database with expiration
     */
    private boolean saveResetToken(String email, String token) {
        // Create password_resets table if not exists
        String createTable = """
            CREATE TABLE IF NOT EXISTS password_resets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                email VARCHAR(255) NOT NULL,
                token VARCHAR(255) NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                used TINYINT(1) DEFAULT 0,
                INDEX (email),
                INDEX (token)
            )
            """;

        String insertToken = """
            INSERT INTO password_resets (email, token, expires_at) 
            VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE))
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTable);

            // Invalidate old tokens
            stmt.execute("UPDATE password_resets SET used = 1 WHERE email = '" + email + "'");

            PreparedStatement pstmt = conn.prepareStatement(insertToken);
            pstmt.setString(1, email);
            pstmt.setString(2, token);
            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verify reset token from database
     */
    private boolean verifyResetToken(String email, String token) {
        String sql = """
            SELECT id FROM password_resets 
            WHERE email = ? AND token = ? AND expires_at > NOW() AND used = 0
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, token);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Mark as used
                int id = rs.getInt("id");
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE password_resets SET used = 1 WHERE id = ?"
                );
                update.setInt(1, id);
                update.executeUpdate();
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Show password reset dialog
     */
    private void showPasswordResetDialog(String email, String expectedToken) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Enter the code from your email");

        ButtonType resetButtonType = new ButtonType("Reset Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField codeField = new TextField();
        codeField.setPromptText("Enter 8-digit code");
        codeField.setStyle("-fx-background-color: #2e2e3e; -fx-text-fill: white;");

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("New password (min 6 chars)");
        newPassField.setStyle("-fx-background-color: #2e2e3e; -fx-text-fill: white;");

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm password");
        confirmPassField.setStyle("-fx-background-color: #2e2e3e; -fx-text-fill: white;");

        grid.add(new Label("Reset Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPassField, 1, 1);
        grid.add(new Label("Confirm:"), 0, 2);
        grid.add(confirmPassField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(codeField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == resetButtonType) {
                return new String[]{codeField.getText().trim(), newPassField.getText(), confirmPassField.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();

        result.ifPresent(data -> {
            String code = data[0];
            String newPass = data[1];
            String confirmPass = data[2];

            // Validate
            if (code.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please enter the reset code!");
                return;
            }

            // Verify against database
            if (!verifyResetToken(email, code)) {
                showAlert(Alert.AlertType.ERROR, "Invalid Code",
                        "The code is invalid or has expired. Please request a new one.");
                return;
            }

            if (newPass.length() < 6) {
                showAlert(Alert.AlertType.ERROR, "Error", "Password must be at least 6 characters!");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Passwords do not match!");
                return;
            }

            // Update password
            if (updatePassword(email, newPass)) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Password reset successfully! You can now login with your new password.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to reset password. Please try again.");
            }
        });
    }

    private boolean updatePassword(String email, String newPassword) {
        String sql = "UPDATE user SET password = ? WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, email);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean emailExists(String email) {
        String sql = "SELECT id FROM user WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void goToRegister() {
        try {
            URL fxmlUrl = findRegisterFxml();
            if (fxmlUrl == null) {
                showMessage("❌ Register FXML not found!", "red");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root, 500, 700);

            URL cssUrl = getClass().getResource("/com/mindforge/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("MindForge - Register");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            showMessage("❌ Failed to load register page: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    private URL findRegisterFxml() {
        String[] possiblePaths = {
                "/com/mindforge/fxml/register.fxml",
                "/fxml/register.fxml",
                "/register.fxml"
        };

        for (String path : possiblePaths) {
            URL url = getClass().getResource(path);
            if (url != null) return url;
        }

        for (String path : possiblePaths) {
            URL url = getClass().getClassLoader().getResource(path);
            if (url != null) return url;
        }

        return null;
    }

    private void goToDashboard() {
        try {
            if (UserSession.getInstance().isAdmin()) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/mindforge/fxml/admin_dashboard.fxml")
                );
                Parent root = loader.load();
                Scene scene = new Scene(root, 1400, 900);

                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setTitle("MindForge - Admin Dashboard");
                stage.setScene(scene);
                stage.show();
            } else {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/mindforge/fxml/dashboard.fxml")
                );
                Parent root = loader.load();
                Scene scene = new Scene(root, 1200, 800);

                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setTitle("MindForge - Dashboard");
                stage.setScene(scene);
                stage.show();
            }
        } catch (Exception e) {
            showMessage("❌ Failed to load dashboard: " + e.getMessage(), "red");
            e.printStackTrace();
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