package com.mindforge.controller;

import com.mindforge.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.Objects;
import java.util.ResourceBundle;

public class EditProfileController implements Initializable {

    @FXML private TextField  fieldFirstName;
    @FXML private TextField  fieldLastName;
    @FXML private TextArea   fieldBio;
    @FXML private ComboBox<String> comboTimezone;
    @FXML private ComboBox<String> comboLocale;
    @FXML private Label      labelMessage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboTimezone.getItems().addAll(
            "UTC", "Africa/Tunis", "Europe/Paris", "Europe/London",
            "America/New_York", "America/Los_Angeles", "Asia/Dubai"
        );
        comboLocale.getItems().addAll("en", "fr", "ar", "de", "es");

        loadCurrentProfile();
    }

    private void loadCurrentProfile() {
        int userId = UserSession.getInstance().getUserId();
        String sql =
            "SELECT p.first_name, p.last_name, p.bio, p.timezone, p.locale" +
            " FROM profile p WHERE p.user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String fn = rs.getString("first_name");
                String ln = rs.getString("last_name");
                String bio = rs.getString("bio");
                String tz  = rs.getString("timezone");
                String loc = rs.getString("locale");

                fieldFirstName.setText(fn  != null ? fn  : "");
                fieldLastName.setText(ln   != null ? ln  : "");
                fieldBio.setText(bio       != null ? bio : "");
                comboTimezone.setValue(tz  != null ? tz  : "UTC");
                comboLocale.setValue(loc   != null ? loc : "en");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSave() {
        int userId = UserSession.getInstance().getUserId();

        String firstName = fieldFirstName.getText().trim();
        String lastName  = fieldLastName.getText().trim();
        String bio       = fieldBio.getText().trim();
        String timezone  = comboTimezone.getValue() != null ? comboTimezone.getValue() : "UTC";
        String locale    = comboLocale.getValue()   != null ? comboLocale.getValue()   : "en";

        // Check if profile row exists
        String checkSql = "SELECT id FROM profile WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {

            checkPs.setInt(1, userId);
            ResultSet rs = checkPs.executeQuery();

            if (rs.next()) {
                // UPDATE
                String updateSql =
                    "UPDATE profile SET first_name=?, last_name=?, bio=?, timezone=?, locale=?" +
                    " WHERE user_id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, firstName.isEmpty() ? null : firstName);
                    ps.setString(2, lastName.isEmpty()  ? null : lastName);
                    ps.setString(3, bio.isEmpty()       ? null : bio);
                    ps.setString(4, timezone);
                    ps.setString(5, locale);
                    ps.setInt(6, userId);
                    ps.executeUpdate();
                }
            } else {
                // INSERT
                String insertSql =
                    "INSERT INTO profile (user_id, first_name, last_name, bio, timezone, locale)" +
                    " VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, firstName.isEmpty() ? null : firstName);
                    ps.setString(3, lastName.isEmpty()  ? null : lastName);
                    ps.setString(4, bio.isEmpty()       ? null : bio);
                    ps.setString(5, timezone);
                    ps.setString(6, locale);
                    ps.executeUpdate();
                }
            }

            showMessage("Profile saved successfully!", "#3B6D11");

        } catch (Exception e) {
            showMessage("Error saving: " + e.getMessage(), "#A32D2D");
            e.printStackTrace();
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
            Stage stage = (Stage) fieldFirstName.getScene().getWindow();
            stage.setTitle("MindForge - My Profile");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToAvatarBuilder() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/mindforge/fxml/avatar_builder.fxml")
            );
            Parent root  = loader.load();
            Scene  scene = new Scene(root, 800, 640);
            scene.getStylesheets().add(
                Objects.requireNonNull(
                    getClass().getResource("/com/mindforge/css/style.css")
                ).toExternalForm()
            );
            Stage stage = (Stage) fieldFirstName.getScene().getWindow();
            stage.setTitle("MindForge - Avatar Builder");
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
