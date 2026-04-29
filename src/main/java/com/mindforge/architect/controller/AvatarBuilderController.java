package com.mindforge.controller;

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
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.sql.*;
import java.util.Objects;
import java.util.ResourceBundle;

public class AvatarBuilderController implements Initializable {

    @FXML private ComboBox<String> comboStyle;
    @FXML private TextField        fieldSeed;
    @FXML private ComboBox<String> comboBgColor;
    @FXML private CheckBox         checkFlip;
    @FXML private CheckBox         checkRound;
    @FXML private ImageView        previewImage;
    @FXML private Label            labelMessage;

    private static final String[] STYLES = {
            "avataaars", "bottts", "identicon", "initials",
            "micah", "miniavs", "pixel-art", "shapes"
    };

    private static final String[] BG_COLORS = {
            "b6e3f4", "c0aede", "d1d4f9", "ffd5dc",
            "ffdfbf", "transparent"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboStyle.getItems().addAll(STYLES);
        comboStyle.setValue("avataaars");

        comboBgColor.getItems().addAll(BG_COLORS);
        comboBgColor.setValue("b6e3f4");

        // Default seed based on user id
        fieldSeed.setText("mindforge" + UserSession.getInstance().getUserId());

        // Auto-preview on any control change
        comboStyle.setOnAction(e -> handlePreview());
        comboBgColor.setOnAction(e -> handlePreview());
        checkFlip.setOnAction(e -> handlePreview());
        checkRound.setOnAction(e -> handlePreview());

        // Auto-preview while user types in the seed field
        fieldSeed.textProperty().addListener((obs, oldVal, newVal) -> handlePreview());

        handlePreview();
    }

    @FXML
    private void handlePreview() {
        String url = buildDiceBearUrl();

        // Use background-loading Image so the UI thread is never blocked
        Image img = new Image(url, 150, 150, true, true, true);

        // Show loading indicator until the image is ready
        img.progressProperty().addListener((obs, oldP, newP) -> {
            if (newP.doubleValue() >= 1.0 && !img.isError()) {
                previewImage.setImage(img);
                applyClip();
            }
        });

        img.errorProperty().addListener((obs, oldE, isError) -> {
            if (isError) {
                showMessage("Preview failed: " + img.getException().getMessage(), "#A32D2D");
            }
        });

        // Also set immediately in case the image is already cached
        if (!img.isError()) {
            previewImage.setImage(img);
            applyClip();
        }
    }

    private void applyClip() {
        if (checkRound.isSelected()) {
            Circle clip = new Circle(75, 75, 75);
            previewImage.setClip(clip);
        } else {
            previewImage.setClip(null);
        }
    }

    @FXML
    private void handleRandomSeed() {
        fieldSeed.setText("seed" + System.currentTimeMillis());
        handlePreview();
    }

    @FXML
    private void handleSave() {
        showMessage("Saving...", "#854F0B");

        String diceBearUrl = buildDiceBearUrl();
        int    userId      = UserSession.getInstance().getUserId();

        // Run download + DB write on a background thread so the UI stays responsive
        new Thread(() -> {
            try {
                // ── 1. Download PNG bytes ────────────────────────────────
                URL url = new URL(diceBearUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "MindForge-JavaFX");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                if (conn.getResponseCode() != 200) {
                    javafx.application.Platform.runLater(() ->
                            showMessage("Could not reach avatar API.", "#A32D2D"));
                    return;
                }

                byte[] pngBytes;
                try (InputStream is = conn.getInputStream();
                     ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                    byte[] data = new byte[4096];
                    int n;
                    while ((n = is.read(data, 0, data.length)) != -1) {
                        buf.write(data, 0, n);
                    }
                    pngBytes = buf.toByteArray();
                }

                // ── 2. Write to uploads/avatars/ ─────────────────────────
                String uploadDir = System.getProperty("user.dir") + "/public/uploads/avatars/";
                Files.createDirectories(Paths.get(uploadDir));

                // Delete old avatar file if present
                String oldAvatar = getOldAvatar(userId);
                if (oldAvatar != null && !oldAvatar.isEmpty()) {
                    Files.deleteIfExists(Paths.get(uploadDir + oldAvatar));
                }

                String filename = "avatar_" + System.currentTimeMillis() + ".png";
                Files.write(Paths.get(uploadDir + filename), pngBytes);

                // ── 3. Persist filename to DB ────────────────────────────
                saveAvatarToDb(userId, filename);

                javafx.application.Platform.runLater(() ->
                        showMessage("Avatar saved!", "#3B6D11"));

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showMessage("Save failed: " + e.getMessage(), "#A32D2D"));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Builds a DiceBear v7 URL that returns a PNG (JavaFX-compatible).
     */
    private String buildDiceBearUrl() {
        String style  = comboStyle.getValue()  != null ? comboStyle.getValue()  : "avataaars";
        String seed   = fieldSeed.getText().trim().isEmpty() ? "mindforge" : fieldSeed.getText().trim();
        String bg     = comboBgColor.getValue() != null ? comboBgColor.getValue() : "b6e3f4";
        String flip   = checkFlip.isSelected()  ? "true" : "false";
        String radius = checkRound.isSelected() ? "50"   : "0";

        // PNG endpoint — JavaFX cannot render SVG natively
        return "https://api.dicebear.com/7.x/" + style + "/png"
                + "?seed="            + encode(seed)
                + "&backgroundColor=" + bg
                + "&flip="            + flip
                + "&radius="          + radius
                + "&size=150";
    }

    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private String getOldAvatar(int userId) {
        String sql = "SELECT avatar FROM profile WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("avatar");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveAvatarToDb(int userId, String filename) throws SQLException {
        String checkSql = "SELECT id FROM profile WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {

            checkPs.setInt(1, userId);
            ResultSet rs = checkPs.executeQuery();

            if (rs.next()) {
                String updateSql = "UPDATE profile SET avatar = ? WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, filename);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }
            } else {
                String insertSql =
                        "INSERT INTO profile (user_id, avatar, timezone, locale) VALUES (?, ?, 'UTC', 'en')";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, filename);
                    ps.executeUpdate();
                }
            }
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
            Stage stage = (Stage) fieldSeed.getScene().getWindow();
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
