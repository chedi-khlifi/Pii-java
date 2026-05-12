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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private ImageView avatarImage;
    @FXML private Label     avatarInitials;
    @FXML private Label     labelFullName;
    @FXML private Label     labelEmail;
    @FXML private Label     labelRole;
    @FXML private Button    btnSocialHub;
    @FXML private Button    btnMentalHealth;   // ← NEW

    // ── Stats ─────────────────────────────────────────────────────────────────
    @FXML private Label       labelXP;
    @FXML private Label       labelLevel;
    @FXML private Label       labelTasks;
    @FXML private Label       labelFocus;
    @FXML private ProgressBar xpBar;
    @FXML private Label       xpBarLabel;

    // ── Other sections ────────────────────────────────────────────────────────
    @FXML private FlowPane badgesPane;
    @FXML private Label    labelTimezone;
    @FXML private Label    labelLocale;
    @FXML private Label    labelSince;
    @FXML private Label    labelStreak;
    @FXML private Label    labelBio;
    @FXML private VBox     roleRequestSection;
    @FXML private Label    labelRoleRequestStatus;

    /**
     * Same upload directory used by EditProfileController.
     * Adjust if your project uses a different path.
     */
    private static final String UPLOAD_DIR =
            System.getProperty("user.dir") + "/uploads/avatars/";

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Apply circular clip to the ImageView so photos are round
        Circle clip = new Circle(38, 38, 38);
        avatarImage.setClip(clip);

        loadProfile();
    }

    // ── Navigation handlers ───────────────────────────────────────────────────
    @FXML
    private void goToDashboard() {
        navigateTo("/com/mindforge/fxml/dashboard.fxml", "MindForge - Dashboard");
    }

    @FXML
    private void goToSocialHub() {
        navigateTo("/com/mindforge/fxml/social_hub.fxml", "MindForge - Social Hub");
    }



    @FXML
    private void goToEmotions() {
        navigateTo("/com/mindforge/fxml/emotions.fxml", "MindForge - Emotions");
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadProfile() {
        int    userId = UserSession.getInstance().getUserId();
        String email  = UserSession.getInstance().getEmail();
        String roles  = UserSession.getInstance().getRoles();

        labelEmail.setText(email != null ? email : "");

        // Role badge text
        if (roles != null && roles.contains("ROLE_ADMIN")) {
            labelRole.setText("Admin");
            labelRole.setStyle(labelRole.getStyle() +
                    "-fx-background-color: #FDE8E8; -fx-text-fill: #8B0000;");
        } else {
            labelRole.setText("Student");
        }

        String sql = """
            SELECT
                p.first_name, p.last_name, p.bio, p.timezone, p.locale, p.avatar,
                u.created_at,
                COALESCE(g.total_xp, 0)        AS total_xp,
                COALESCE(g.current_level, 1)   AS current_level,
                COALESCE(g.streak_days, 0)     AS streak_days,
                COALESCE(g.total_focus_time, 0) AS total_focus_time,
                COALESCE(g.tasks_completed, 0) AS tasks_completed
            FROM user u
            LEFT JOIN profile p          ON p.user_id = u.id
            LEFT JOIN gamification_stats g ON g.user_id = u.id
            WHERE u.id = ?
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String firstName = rs.getString("first_name");
                String lastName  = rs.getString("last_name");
                String bio       = rs.getString("bio");
                String timezone  = rs.getString("timezone");
                String locale    = rs.getString("locale");
                String avatar    = rs.getString("avatar");
                Timestamp since  = rs.getTimestamp("created_at");

                int totalXp       = rs.getInt("total_xp");
                int level         = rs.getInt("current_level");
                int streak        = rs.getInt("streak_days");
                int focusMinutes  = rs.getInt("total_focus_time");
                int tasksDone     = rs.getInt("tasks_completed");

                // Full name / initials
                String fullName = buildFullName(firstName, lastName, email);
                labelFullName.setText(fullName);
                setAvatarDisplay(avatar, firstName, lastName, email);

                // Bio
                labelBio.setText(bio != null && !bio.isBlank() ? bio : "No bio yet.");

                // Account info
                labelTimezone.setText(timezone != null ? timezone : "UTC");
                labelLocale.setText(locale != null ? locale : "en");
                if (since != null) {
                    labelSince.setText(since.toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("MMM yyyy")));
                }
                labelStreak.setText(streak + " day" + (streak == 1 ? "" : "s"));

                // Stats
                labelXP.setText(String.valueOf(totalXp));
                labelLevel.setText(String.valueOf(level));
                labelTasks.setText(String.valueOf(tasksDone));
                labelFocus.setText(focusMinutes >= 60
                        ? (focusMinutes / 60) + "h " + (focusMinutes % 60) + "m"
                        : focusMinutes + " min");

                // XP progress bar (500 XP per level)
                int xpIntoLevel  = totalXp % 500;
                int xpNeeded     = 500;
                xpBar.setProgress((double) xpIntoLevel / xpNeeded);
                xpBarLabel.setText(xpIntoLevel + " / " + xpNeeded + " XP to next level");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        loadBadges(userId);
    }

    /**
     * Decides whether to show the uploaded photo or the initials fallback.
     */
    private void setAvatarDisplay(String avatarFilename,
                                  String firstName, String lastName, String email) {
        if (avatarFilename != null && !avatarFilename.isBlank()) {
            // 1 — try the uploads/avatars/ directory (photos uploaded via EditProfile)
            File uploadedFile = new File(UPLOAD_DIR + avatarFilename);
            if (uploadedFile.exists()) {
                loadAvatarFromUrl(uploadedFile.toURI().toString());
                return;
            }

            // 2 — try classpath resources (SVG avatars from avatar builder)
            URL resource = getClass().getResource("/com/mindforge/avatars/" + avatarFilename);
            if (resource != null) {
                loadAvatarFromUrl(resource.toExternalForm());
                return;
            }
        }

        // 3 — fall back to initials
        showInitials(firstName, lastName, email);
    }

    private void loadAvatarFromUrl(String imageUrl) {
        try {
            Image img = new Image(imageUrl, 76, 76, false, true);
            avatarImage.setImage(img);
            avatarImage.setVisible(true);
            avatarInitials.setVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
            showInitials(null, null, null);
        }
    }

    private void showInitials(String firstName, String lastName, String email) {
        avatarImage.setVisible(false);
        avatarInitials.setVisible(true);

        String initials = "?";
        if (firstName != null && !firstName.isBlank()) {
            initials = String.valueOf(firstName.charAt(0)).toUpperCase();
            if (lastName != null && !lastName.isBlank())
                initials += String.valueOf(lastName.charAt(0)).toUpperCase();
        } else if (email != null && !email.isBlank()) {
            initials = String.valueOf(email.charAt(0)).toUpperCase();
        }
        avatarInitials.setText(initials);
    }

    private String buildFullName(String first, String last, String email) {
        if (first != null && last != null && !first.isBlank() && !last.isBlank())
            return first + " " + last;
        if (first != null && !first.isBlank()) return first;
        if (last  != null && !last.isBlank())  return last;
        return email != null ? email.split("@")[0] : "User";
    }

    private void loadBadges(int userId) {
        String sql = """
            SELECT b.name, b.icon, b.rarity
            FROM user_badge ub
            JOIN badge b ON b.id = ub.badge_id
            WHERE ub.user_id = ?
            ORDER BY ub.earned_at DESC
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            boolean hasBadge = false;
            while (rs.next()) {
                hasBadge = true;
                String name   = rs.getString("name");
                String rarity = rs.getString("rarity");

                Label badge = new Label(name);
                String bg = switch (rarity != null ? rarity : "common") {
                    case "epic"      -> "#F3E8FF";
                    case "legendary" -> "#FFF3CD";
                    case "rare"      -> "#E8F4FD";
                    default          -> "#F0F0F0";
                };
                String fg = switch (rarity != null ? rarity : "common") {
                    case "epic"      -> "#6B21A8";
                    case "legendary" -> "#92400E";
                    case "rare"      -> "#1D4ED8";
                    default          -> "#555555";
                };
                badge.setStyle(
                        "-fx-background-color: " + bg + ";" +
                                "-fx-text-fill: " + fg + ";" +
                                "-fx-background-radius: 20;" +
                                "-fx-padding: 4 12;" +
                                "-fx-font-size: 12px;"
                );
                badgesPane.getChildren().add(badge);
            }

            if (!hasBadge) {
                Label none = new Label("No badges earned yet.");
                none.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaaaa;");
                badgesPane.getChildren().add(none);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML
    private void goToEditProfile() {
        navigateTo("/com/mindforge/fxml/edit_profile.fxml", "MindForge - Edit Profile");
    }

    @FXML
    private void goToAvatarBuilder() {
        navigateTo("/com/mindforge/fxml/avatar_builder.fxml", "MindForge - Avatar Builder");
    }

    @FXML
    private void goToRoleRequest() {
        navigateTo("/com/mindforge/fxml/role_request.fxml", "MindForge - Role Request");
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        navigateTo("/com/mindforge/fxml/login.fxml", "MindForge - Login");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root  = loader.load();
            URL css = getClass().getResource("/com/mindforge/css/style.css");
            Stage stage = (Stage) labelFullName.getScene().getWindow();
            stage.setTitle(title);
            if (css != null && stage.getScene() != null) stage.getScene().getStylesheets().add(css.toExternalForm());
            stage.setTitle(title);
            stage.getScene().setRoot(root);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── DB ─────────────────────────────────────────────────────────────────────
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mindforge_db", "root", "");
    }
}