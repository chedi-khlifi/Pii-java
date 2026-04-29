package com.mindforge.controller;

import com.mindforge.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

public class ProfileController implements Initializable {

    /* ── Header ─────────────────────────────────────────────────────── */
    @FXML private Label     avatarInitials;
    @FXML private ImageView avatarImage;
    @FXML private Label     labelFullName;
    @FXML private Label     labelEmail;
    @FXML private Label     labelRole;

    /* ── Stats ──────────────────────────────────────────────────────── */
    @FXML private Label labelXP;
    @FXML private Label labelLevel;
    @FXML private Label labelTasks;
    @FXML private Label labelFocus;

    /* ── XP bar ─────────────────────────────────────────────────────── */
    @FXML private ProgressBar xpBar;
    @FXML private Label       xpBarLabel;

    /* ── Badges ─────────────────────────────────────────────────────── */
    @FXML private FlowPane badgesPane;

    /* ── Account info ───────────────────────────────────────────────── */
    @FXML private Label labelTimezone;
    @FXML private Label labelLocale;
    @FXML private Label labelSince;
    @FXML private Label labelStreak;
    @FXML private Label labelBio;

    /* ── Role request section ────────────────────────────────────────── */
    @FXML private VBox  roleRequestSection;
    @FXML private Label labelRoleRequestStatus;

    /* ════════════════════════════════════════════════════════════════ */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        int userId = UserSession.getInstance().getUserId();
        loadProfile(userId);
        loadGamification(userId);
        loadBadges(userId);
        loadRoleRequestStatus(userId);
    }

    /* ── Profile & account info ──────────────────────────────────────── */
    private void loadProfile(int userId) {
        String sql =
                "SELECT u.email, u.roles, u.created_at," +
                        " p.first_name, p.last_name, p.bio, p.timezone, p.locale, p.avatar" +
                        " FROM user u" +
                        " LEFT JOIN profile p ON p.user_id = u.id" +
                        " WHERE u.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String firstName = rs.getString("first_name");
                String lastName  = rs.getString("last_name");
                String email     = rs.getString("email");
                String roles     = rs.getString("roles");
                String bio       = rs.getString("bio");
                String timezone  = rs.getString("timezone");
                String locale    = rs.getString("locale");
                String avatar    = rs.getString("avatar");
                Timestamp since  = rs.getTimestamp("created_at");

                labelFullName.setText(buildName(firstName, lastName, email));
                labelEmail.setText(email);
                labelRole.setText(formatRole(roles));
                avatarInitials.setText(initials(firstName, lastName, email));

                // Load avatar from DiceBear if it's an SVG filename
                loadAvatar(avatar);

                labelTimezone.setText(timezone != null ? timezone : "UTC");
                labelLocale.setText(locale     != null ? locale   : "en");
                labelSince.setText(since       != null
                        ? since.toLocalDateTime().toLocalDate().toString() : "-");
                labelBio.setText(bio != null && !bio.isEmpty() ? bio : "No bio yet.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Load avatar image ───────────────────────────────────────────── */
    private void loadAvatar(String avatarFilename) {
        if (avatarFilename == null || avatarFilename.isEmpty()) {
            // No avatar — show initials circle
            avatarImage.setVisible(false);
            avatarInitials.setVisible(true);
            return;
        }

        try {
            // Avatar is stored as a filename like "avatar_xyz.svg"
            // We reconstruct the DiceBear URL from the seed embedded in the filename
            // OR load from local uploads directory if it exists
            String localPath = System.getProperty("user.dir") +
                    "/public/uploads/avatars/" + avatarFilename;
            java.io.File localFile = new java.io.File(localPath);

            Image img = null;
            if (localFile.exists()) {
                img = new Image(localFile.toURI().toString(), 76, 76, true, true);
            } else {
                // Fallback: generate from DiceBear using filename as seed
                String seed = avatarFilename.replace("avatar_", "").replace(".svg", "");
                String url  = "https://api.dicebear.com/7.x/avataaars/svg?seed=" + seed
                        + "&backgroundColor=b6e3f4&radius=50";
                img = new Image(url, 76, 76, true, true);
            }

            if (!img.isError()) {
                avatarImage.setImage(img);
                // Clip image to circle
                Circle clip = new Circle(38, 38, 38);
                avatarImage.setClip(clip);
                avatarImage.setVisible(true);
                avatarInitials.setVisible(false);
            } else {
                avatarImage.setVisible(false);
                avatarInitials.setVisible(true);
            }

        } catch (Exception e) {
            avatarImage.setVisible(false);
            avatarInitials.setVisible(true);
        }
    }

    /* ── Gamification stats ──────────────────────────────────────────── */
    private void loadGamification(int userId) {
        String sql =
                "SELECT total_xp, current_level, streak_days," +
                        " total_focus_time, tasks_completed" +
                        " FROM gamification_stats" +
                        " WHERE user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int xp     = rs.getInt("total_xp");
                int level  = rs.getInt("current_level");
                int streak = rs.getInt("streak_days");
                int focus  = rs.getInt("total_focus_time");
                int tasks  = rs.getInt("tasks_completed");

                labelXP.setText(xp + " XP");
                labelLevel.setText("Level " + level);
                labelTasks.setText(tasks + " tasks");
                labelFocus.setText(focus + " min");
                labelStreak.setText(streak + " days");

                double needed   = level * 500.0;
                double progress = Math.min(xp / needed, 1.0);
                xpBar.setProgress(progress);
                xpBarLabel.setText(xp + " / " + (int) needed + " XP  —  to next level");

            } else {
                labelXP.setText("0 XP");
                labelLevel.setText("Level 1");
                labelTasks.setText("0 tasks");
                labelFocus.setText("0 min");
                labelStreak.setText("0 days");
                xpBar.setProgress(0);
                xpBarLabel.setText("0 / 500 XP");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Badges ──────────────────────────────────────────────────────── */
    private void loadBadges(int userId) {
        String earnedSql =
                "SELECT b.name FROM user_badge ub" +
                        " JOIN badge b ON b.id = ub.badge_id" +
                        " WHERE ub.user_id = ?";
        String allSql =
                "SELECT name, description, icon, rarity FROM badge ORDER BY id";

        try (Connection conn = getConnection()) {

            Set<String> earned = new HashSet<String>();
            try (PreparedStatement ps = conn.prepareStatement(earnedSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) earned.add(rs.getString("name"));
            }

            try (PreparedStatement ps = conn.prepareStatement(allSql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    badgesPane.getChildren().add(buildBadgeCard(
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("icon"),
                            rs.getString("rarity"),
                            earned.contains(rs.getString("name"))
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Role request status ─────────────────────────────────────────── */
    private void loadRoleRequestStatus(int userId) {
        String sql =
                "SELECT status, admin_notes, requested_at" +
                        " FROM role_request" +
                        " WHERE user_id = ?" +
                        " ORDER BY requested_at DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String status     = rs.getString("status");
                String adminNotes = rs.getString("admin_notes");

                String displayText;
                String color;

                if ("approved".equals(status)) {
                    displayText = "Role request: Approved";
                    color = "#3B6D11";
                } else if ("rejected".equals(status)) {
                    displayText = "Role request: Rejected"
                            + (adminNotes != null ? " — " + adminNotes : "");
                    color = "#A32D2D";
                } else {
                    displayText = "Role request: Pending review";
                    color = "#854F0B";
                }

                labelRoleRequestStatus.setText(displayText);
                labelRoleRequestStatus.setStyle(
                        "-fx-text-fill: " + color + "; -fx-font-size: 12px;"
                );
                roleRequestSection.setVisible(true);
                roleRequestSection.setManaged(true);
            } else {
                // No request yet — show "Request role upgrade" button
                roleRequestSection.setVisible(true);
                roleRequestSection.setManaged(true);
                labelRoleRequestStatus.setText("No role request submitted yet.");
                labelRoleRequestStatus.setStyle(
                        "-fx-text-fill: #888888; -fx-font-size: 12px;"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Navigate to Edit Profile ────────────────────────────────────── */
    @FXML
    private void goToEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/edit_profile.fxml")
            );
            Parent root  = loader.load();
            Scene  scene = new Scene(root, 800, 640);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/com/mindforge/css/style.css")
                    ).toExternalForm()
            );
            Stage stage = (Stage) labelEmail.getScene().getWindow();
            stage.setTitle("MindForge - Edit Profile");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Navigate to Avatar Builder ──────────────────────────────────── */
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
            Stage stage = (Stage) labelEmail.getScene().getWindow();
            stage.setTitle("MindForge - Avatar Builder");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Navigate to Role Request ────────────────────────────────────── */
    @FXML
    private void goToRoleRequest() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/role_request.fxml")
            );
            Parent root  = loader.load();
            Scene  scene = new Scene(root, 600, 480);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/com/mindforge/css/style.css")
                    ).toExternalForm()
            );
            Stage stage = (Stage) labelEmail.getScene().getWindow();
            stage.setTitle("MindForge - Role Request");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Logout ──────────────────────────────────────────────────────── */
    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/login.fxml")
            );
            Parent root  = loader.load();
            Scene  scene = new Scene(root, 500, 400);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/com/mindforge/css/style.css")
                    ).toExternalForm()
            );
            Stage stage = (Stage) labelEmail.getScene().getWindow();
            stage.setTitle("MindForge - Login");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ── Badge card builder ──────────────────────────────────────────── */
    private VBox buildBadgeCard(String name, String desc,
                                String icon, String rarity, boolean earned) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setPrefWidth(158);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 0.5;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-opacity: " + (earned ? "1.0" : "0.38") + ";"
        );

        Label iconLbl = new Label(iconChar(icon));
        iconLbl.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-background-color: " + rarityBg(rarity) + ";" +
                        "-fx-background-radius: 50;" +
                        "-fx-padding: 7 9 7 9;"
        );

        Label nameLbl = new Label(name);
        nameLbl.setFont(Font.font(null, FontWeight.BOLD, 12));
        nameLbl.setWrapText(true);
        nameLbl.setStyle("-fx-text-fill: #222222;");

        Label descLbl = new Label(desc != null ? desc : "");
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        Label rarityLbl = new Label(rarity);
        rarityLbl.setStyle(
                "-fx-font-size: 10px;" +
                        "-fx-background-color: " + rarityBg(rarity) + ";" +
                        "-fx-text-fill: " + rarityFg(rarity) + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 2 8 2 8;"
        );

        card.getChildren().addAll(iconLbl, nameLbl, descLbl, rarityLbl);

        if (!earned) {
            Label lockedLbl = new Label("Locked");
            lockedLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #bbbbbb;");
            card.getChildren().add(lockedLbl);
        }

        return card;
    }

    /* ── Small helpers ───────────────────────────────────────────────── */
    private String buildName(String fn, String ln, String email) {
        if (fn != null && ln != null) return fn + " " + ln;
        if (fn != null)               return fn;
        return email.split("@")[0];
    }

    private String initials(String fn, String ln, String email) {
        if (fn != null && ln != null)
            return ("" + fn.charAt(0) + ln.charAt(0)).toUpperCase();
        if (fn != null)
            return String.valueOf(fn.charAt(0)).toUpperCase();
        return String.valueOf(email.charAt(0)).toUpperCase();
    }

    private String formatRole(String roles) {
        if (roles == null)                  return "User";
        if (roles.contains("ROLE_ADMIN"))   return "Admin";
        if (roles.contains("ROLE_COMPANY")) return "Company";
        return "Student";
    }

    private String iconChar(String fa) {
        if (fa == null)               return "\u2605";
        if (fa.contains("shoe"))      return "\uD83D\uDC5F";
        if (fa.contains("check"))     return "\u2714";
        if (fa.contains("star"))      return "\u2605";
        if (fa.contains("trophy"))    return "\uD83C\uDFC6";
        if (fa.contains("hourglass")) return "\u23F3";
        if (fa.contains("fire"))      return "\uD83D\uDD25";
        return "\u2605";
    }

    private String rarityBg(String r) {
        if (r == null) return "#EAF3DE";
        if (r.equals("legendary")) return "#FAEEDA";
        if (r.equals("epic"))      return "#EEEDFE";
        if (r.equals("rare"))      return "#E6F1FB";
        return "#EAF3DE";
    }

    private String rarityFg(String r) {
        if (r == null) return "#3B6D11";
        if (r.equals("legendary")) return "#633806";
        if (r.equals("epic"))      return "#3C3489";
        if (r.equals("rare"))      return "#0C447C";
        return "#3B6D11";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mindforge_db", "root", ""
        );
    }
}
