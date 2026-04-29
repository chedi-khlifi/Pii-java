package com.mindforge.architect.controller;

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
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("⚠️ Please fill in all fields!", "orange");
            return;
        }

        Stage primaryStage = (Stage) emailField.getScene().getWindow();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, email, password, roles FROM user WHERE email = ?")) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");

                if (BCrypt.checkpw(password, hashedPassword)) {
                    int    userId = rs.getInt("id");
                    String roles  = rs.getString("roles");

                    UserSession.getInstance().login(userId, email, roles);
                    showMessage("✅ Login successful!", "green");

                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(() -> goToDashboard(primaryStage));
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

    // ── Navigation ─────────────────────────────────────────────────────────
    private void goToDashboard(Stage stage) {
        try {
            String fxmlPath = UserSession.getInstance().isAdmin()
                    ? "/com/mindforge/fxml/admin_dashboard.fxml"
                    : "/com/mindforge/fxml/dashboard.fxml";
            String title = UserSession.getInstance().isAdmin()
                    ? "MindForge - Admin Dashboard"
                    : "MindForge - Dashboard";
            int width  = UserSession.getInstance().isAdmin() ? 1400 : 1200;
            int height = UserSession.getInstance().isAdmin() ? 900  : 800;

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Always create a NEW Scene instead of reusing setRoot()
            Scene scene = new Scene(root, width, height);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            showMessage("❌ Failed to load dashboard: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister() {
        try {
            URL fxmlUrl = findRegisterFxml();
            if (fxmlUrl == null) {
                showMessage("❌ Register FXML not found!", "red");
                return;
            }

            Stage stage = (Stage) emailField.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root, 500, 700);

            URL cssUrl = getClass().getResource("/com/mindforge/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            stage.setTitle("MindForge - Register");
            stage.setScene(scene);  // Use setScene, NOT setRoot
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

    // ── Forgot / reset password ────────────────────────────────────────────
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

        dialog.setResultConverter(btn -> btn == sendButtonType ? emailInput.getText().trim() : null);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(email -> {
            if (email.isEmpty() || !email.contains("@")) {
                showAlert(Alert.AlertType.WARNING, "Invalid Email", "Please enter a valid email address.");
                return;
            }
            if (!emailExists(email)) {
                showAlert(Alert.AlertType.INFORMATION, "Email Sent",
                        "If an account exists with this email, you will receive reset instructions shortly.");
                return;
            }
            String resetCode = generateResetToken();
            String userName  = getUserNameByEmail(email);

            if (saveResetToken(email, resetCode)) {
                boolean sent = emailService.sendPasswordResetEmail(email, resetCode, userName);
                if (sent) {
                    showAlert(Alert.AlertType.INFORMATION, "Email Sent",
                            "A password reset code has been sent to " + email +
                                    "\n\nPlease check your inbox (and spam folder).");
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

    private String getUserNameByEmail(String email) {
        String sql = "SELECT p.display_name FROM profile p JOIN user u ON p.user_id = u.id WHERE u.email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getString("display_name") != null)
                return rs.getString("display_name");
        } catch (Exception ignored) {}
        return email.split("@")[0];
    }

    private boolean saveResetToken(String email, String token) {
        String createTable = """
            CREATE TABLE IF NOT EXISTS password_resets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                email VARCHAR(255) NOT NULL,
                token VARCHAR(255) NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                used TINYINT(1) DEFAULT 0,
                INDEX (email), INDEX (token)
            )""";
        String insertToken = """
            INSERT INTO password_resets (email, token, expires_at)
            VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE))""";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
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

    private boolean verifyResetToken(String email, String token) {
        String sql = """
            SELECT id FROM password_resets
            WHERE email = ? AND token = ? AND expires_at > NOW() AND used = 0""";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, token);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                PreparedStatement upd = conn.prepareStatement(
                        "UPDATE password_resets SET used = 1 WHERE id = ?");
                upd.setInt(1, id);
                upd.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

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

        TextField     codeField       = new TextField();      codeField.setPromptText("Enter 8-digit code");
        PasswordField newPassField    = new PasswordField();  newPassField.setPromptText("New password (min 6 chars)");
        PasswordField confirmPassField = new PasswordField(); confirmPassField.setPromptText("Confirm password");

        codeField.setStyle("-fx-background-color: #2e2e3e; -fx-text-fill: white;");
        newPassField.setStyle("-fx-background-color: #2e2e3e; -fx-text-fill: white;");
        confirmPassField.setStyle("-fx-background-color: #2e2e3e; -fx-text-fill: white;");

        grid.add(new Label("Reset Code:"),   0, 0); grid.add(codeField,        1, 0);
        grid.add(new Label("New Password:"), 0, 1); grid.add(newPassField,     1, 1);
        grid.add(new Label("Confirm:"),      0, 2); grid.add(confirmPassField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(codeField::requestFocus);

        dialog.setResultConverter(btn -> btn == resetButtonType
                ? new String[]{codeField.getText().trim(), newPassField.getText(), confirmPassField.getText()}
                : null);

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String code        = data[0];
            String newPass     = data[1];
            String confirmPass = data[2];

            if (code.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please enter the reset code!");
                return;
            }
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
            pstmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
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
            return pstmt.executeQuery().next();
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

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mindforge_db", "root", "");
    }

    private void showMessage(String message, String color) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;");
    }
}