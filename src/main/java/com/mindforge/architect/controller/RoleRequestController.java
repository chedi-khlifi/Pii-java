package com.mindforge.controller;

import com.mindforge.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.sql.*;
import java.util.Objects;
import java.util.ResourceBundle;

public class RoleRequestController implements Initializable {

    @FXML private TextArea fieldMotivation;
    @FXML private Label    labelStatus;
    @FXML private Label    labelAdminNotes;
    @FXML private Label    labelMessage;
    @FXML private Button   btnSubmit;
    @FXML private VBox     existingRequestSection;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadExistingRequest();
    }

    private void loadExistingRequest() {
        int userId = UserSession.getInstance().getUserId();
        String sql =
            "SELECT status, admin_notes, motivation, requested_at" +
            " FROM role_request WHERE user_id = ?" +
            " ORDER BY requested_at DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String status     = rs.getString("status");
                String adminNotes = rs.getString("admin_notes");
                String motivation = rs.getString("motivation");
                Timestamp requestedAt = rs.getTimestamp("requested_at");

                // Show existing request info
                existingRequestSection.setVisible(true);
                existingRequestSection.setManaged(true);

                String statusText = "Status: " + status.toUpperCase();
                String statusColor;
                if ("approved".equals(status)) {
                    statusColor = "#3B6D11";
                } else if ("rejected".equals(status)) {
                    statusColor = "#A32D2D";
                } else {
                    statusColor = "#854F0B";
                }

                labelStatus.setText(statusText);
                labelStatus.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold;");

                if (adminNotes != null && !adminNotes.isEmpty()) {
                    labelAdminNotes.setText("Admin note: " + adminNotes);
                    labelAdminNotes.setVisible(true);
                } else {
                    labelAdminNotes.setVisible(false);
                }

                // Pre-fill motivation for editing (only if pending or rejected)
                if ("pending".equals(status)) {
                    fieldMotivation.setText(motivation != null ? motivation : "");
                    fieldMotivation.setDisable(true);
                    btnSubmit.setDisable(true);
                    btnSubmit.setText("Awaiting review");
                } else if ("rejected".equals(status)) {
                    fieldMotivation.setText("");
                    fieldMotivation.setDisable(false);
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Resubmit Request");
                } else {
                    // approved
                    fieldMotivation.setDisable(true);
                    btnSubmit.setDisable(true);
                    btnSubmit.setText("Request Approved");
                }

            } else {
                // No prior request
                existingRequestSection.setVisible(false);
                existingRequestSection.setManaged(false);
                btnSubmit.setDisable(false);
                btnSubmit.setText("Submit Request");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSubmit() {
        String motivation = fieldMotivation.getText().trim();

        if (motivation.isEmpty()) {
            showMessage("Please write your motivation.", "#A32D2D");
            return;
        }
        if (motivation.length() < 20) {
            showMessage("Motivation must be at least 20 characters.", "#A32D2D");
            return;
        }

        int userId = UserSession.getInstance().getUserId();

        try (Connection conn = getConnection()) {

            // Check if a rejected request exists (allow resubmit)
            String checkSql =
                "SELECT id, status FROM role_request WHERE user_id = ?" +
                " ORDER BY requested_at DESC LIMIT 1";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, userId);
                ResultSet rs = checkPs.executeQuery();

                if (rs.next()) {
                    String existingStatus = rs.getString("status");
                    if ("rejected".equals(existingStatus)) {
                        // Insert a new request
                        insertRequest(conn, userId, motivation);
                    } else {
                        showMessage("A request is already submitted.", "#854F0B");
                        return;
                    }
                } else {
                    insertRequest(conn, userId, motivation);
                }
            }

            showMessage("Request submitted! An admin will review it.", "#3B6D11");
            fieldMotivation.setDisable(true);
            btnSubmit.setDisable(true);
            btnSubmit.setText("Awaiting review");
            loadExistingRequest();

        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), "#A32D2D");
            e.printStackTrace();
        }
    }

    private void insertRequest(Connection conn, int userId, String motivation)
            throws SQLException {
        String insertSql =
            "INSERT INTO role_request (user_id, motivation, status, requested_at)" +
            " VALUES (?, ?, 'pending', NOW())";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, userId);
            ps.setString(2, motivation);
            ps.executeUpdate();
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/mindforge/fxml/profile.fxml")
            );
            Parent root  = loader.load();
            Scene  scene = new Scene(root, 800, 640);
            scene.getStylesheets().add(
                Objects.requireNonNull(
                    getClass().getResource("/com/mindforge/css/style.css")
                ).toExternalForm()
            );
            Stage stage = (Stage) fieldMotivation.getScene().getWindow();
            stage.setTitle("MindForge - My Profile");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String msg, String color) {
        labelMessage.setText(msg);
        labelMessage.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;");
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/mindforge_db", "root", ""
        );
    }
}
