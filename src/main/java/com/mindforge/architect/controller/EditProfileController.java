package com.mindforge.architect.controller;

import com.mindforge.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

public class EditProfileController implements Initializable {

    // ── Form fields ──────────────────────────────────────────────────────────
    @FXML private TextField  fieldFirstName;
    @FXML private TextField  fieldLastName;
    @FXML private TextArea   fieldBio;
    @FXML private ComboBox<String> comboTimezone;
    @FXML private ComboBox<String> comboLocale;
    @FXML private Label      labelMessage;

    // ── Avatar widgets ────────────────────────────────────────────────────────
    @FXML private ImageView  avatarImageView;
    @FXML private Circle     avatarCircle;
    @FXML private Circle     avatarClip;
    @FXML private Label      avatarPlaceholder;
    @FXML private Button     btnRemovePhoto;

    /** Relative filename stored in DB (e.g. "abc123.jpg"), or null if none. */
    private String currentAvatarFilename = null;

    /**
     * Absolute directory where uploaded profile photos are stored.
     * Adjust this path to match your project's resource/upload folder.
     */
    private static final String UPLOAD_DIR =
            System.getProperty("user.dir") + "/uploads/avatars/";

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboTimezone.getItems().addAll(
                "UTC", "Africa/Tunis", "Europe/Paris", "Europe/London",
                "America/New_York", "America/Los_Angeles", "Asia/Dubai"
        );
        comboLocale.getItems().addAll("en", "fr", "ar", "de", "es");

        // Apply circular clip to the ImageView
        Circle clip = new Circle(52, 52, 52);
        avatarImageView.setClip(clip);
        avatarImageView.setVisible(false);  // hidden until an image is loaded

        loadCurrentProfile();
    }

    // ── DB load ───────────────────────────────────────────────────────────────
    private void loadCurrentProfile() {
        int userId = UserSession.getInstance().getUserId();
        String sql =
                "SELECT p.first_name, p.last_name, p.bio, p.timezone, p.locale, p.avatar" +
                        " FROM profile p WHERE p.user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String fn     = rs.getString("first_name");
                String ln     = rs.getString("last_name");
                String bio    = rs.getString("bio");
                String tz     = rs.getString("timezone");
                String loc    = rs.getString("locale");
                String avatar = rs.getString("avatar");

                fieldFirstName.setText(fn  != null ? fn  : "");
                fieldLastName.setText(ln   != null ? ln  : "");
                fieldBio.setText(bio       != null ? bio : "");
                comboTimezone.setValue(tz  != null ? tz  : "UTC");
                comboLocale.setValue(loc   != null ? loc : "en");

                if (avatar != null && !avatar.isBlank()) {
                    currentAvatarFilename = avatar;
                    loadAvatarFromFilename(avatar);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to display the avatar image from the uploads directory.
     * Falls back to the placeholder emoji if the file is not found.
     */
    private void loadAvatarFromFilename(String filename) {
        File file = new File(UPLOAD_DIR + filename);
        if (file.exists()) {
            showAvatarImage(file.toURI().toString());
        } else {
            // Try classpath / resources fallback (e.g. SVG avatars from avatar builder)
            URL resource = getClass().getResource("/com/mindforge/avatars/" + filename);
            if (resource != null) {
                showAvatarImage(resource.toExternalForm());
            }
            // If still not found just leave the placeholder visible
        }
    }

    private void showAvatarImage(String imageUrl) {
        try {
            Image img = new Image(imageUrl, 104, 104, false, true);
            avatarImageView.setImage(img);
            avatarImageView.setVisible(true);
            avatarPlaceholder.setVisible(false);
            btnRemovePhoto.setVisible(true);
            btnRemovePhoto.setManaged(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAvatarPlaceholder() {
        avatarImageView.setImage(null);
        avatarImageView.setVisible(false);
        avatarPlaceholder.setVisible(true);
        btnRemovePhoto.setVisible(false);
        btnRemovePhoto.setManaged(false);
    }

    // ── Photo upload ──────────────────────────────────────────────────────────
    @FXML
    private void handleUploadPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files",
                        "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp")
        );

        Stage stage = (Stage) fieldFirstName.getScene().getWindow();
        File selected = chooser.showOpenDialog(stage);

        if (selected == null) return;

        // Validate file size (max 5 MB)
        if (selected.length() > 5 * 1024 * 1024) {
            showMessage("Photo must be smaller than 5 MB.", "#A32D2D");
            return;
        }

        // Copy file to uploads directory with a unique name
        try {
            Path uploadDir = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadDir);

            String originalName = selected.getName();
            String ext = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".jpg";
            String uniqueName = UUID.randomUUID().toString().replace("-", "") + ext;

            Path dest = uploadDir.resolve(uniqueName);
            Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            currentAvatarFilename = uniqueName;
            showAvatarImage(dest.toUri().toString());
            showMessage("Photo selected. Click 'Save changes' to apply.", "#3B6D11");

        } catch (IOException e) {
            showMessage("Failed to load photo: " + e.getMessage(), "#A32D2D");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRemovePhoto() {
        currentAvatarFilename = null;
        showAvatarPlaceholder();
        showMessage("Photo removed. Click 'Save changes' to apply.", "#3B6D11");
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        int userId = UserSession.getInstance().getUserId();

        String firstName = fieldFirstName.getText().trim();
        String lastName  = fieldLastName.getText().trim();
        String bio       = fieldBio.getText().trim();
        String timezone  = comboTimezone.getValue() != null ? comboTimezone.getValue() : "UTC";
        String locale    = comboLocale.getValue()   != null ? comboLocale.getValue()   : "en";

        String checkSql = "SELECT id FROM profile WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {

            checkPs.setInt(1, userId);
            ResultSet rs = checkPs.executeQuery();

            if (rs.next()) {
                // UPDATE — include avatar column
                String updateSql =
                        "UPDATE profile SET first_name=?, last_name=?, bio=?, timezone=?, locale=?, avatar=?" +
                                " WHERE user_id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, firstName.isEmpty() ? null : firstName);
                    ps.setString(2, lastName.isEmpty()  ? null : lastName);
                    ps.setString(3, bio.isEmpty()       ? null : bio);
                    ps.setString(4, timezone);
                    ps.setString(5, locale);
                    ps.setString(6, currentAvatarFilename);   // null = remove avatar
                    ps.setInt(7, userId);
                    ps.executeUpdate();
                }
            } else {
                // INSERT — include avatar column
                String insertSql =
                        "INSERT INTO profile (user_id, first_name, last_name, bio, timezone, locale, avatar)" +
                                " VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, firstName.isEmpty() ? null : firstName);
                    ps.setString(3, lastName.isEmpty()  ? null : lastName);
                    ps.setString(4, bio.isEmpty()       ? null : bio);
                    ps.setString(5, timezone);
                    ps.setString(6, locale);
                    ps.setString(7, currentAvatarFilename);
                    ps.executeUpdate();
                }
            }

            showMessage("Profile saved successfully!", "#3B6D11");

        } catch (Exception e) {
            showMessage("Error saving: " + e.getMessage(), "#A32D2D");
            e.printStackTrace();
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
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

    // ── Helpers ───────────────────────────────────────────────────────────────
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