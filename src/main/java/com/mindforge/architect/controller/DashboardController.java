package com.mindforge.architect.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mindforge.util.UserSession;
import com.mindforge.utils.DBConnection;
import com.mindforge.utils.SessionManager;
import com.mindforge.controllers.carriere.StudentController;
import example.PlannerModule;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    // ── Center / Friends ─────────────────────────────────────────────
    @FXML private VBox  centerContent;
    @FXML private VBox  friendsContainer;
    @FXML private Label labelFriendCount;
    @FXML private VBox  communitySection;   // community cards block in center

    // ── Top bar stats ────────────────────────────────────────────────
    @FXML private Label     labelUserName;
    @FXML private Label     labelWelcome;
    @FXML private Label     labelXpTop;
    @FXML private Label     labelLevelTop;
    @FXML private Label     labelStreakTop;
    @FXML private Label     avatarInitialsTop;
    @FXML private ImageView avatarImageTop;
    @FXML private Circle    avatarCircle;

    // ── Stats row ────────────────────────────────────────────────────
    @FXML private Label       statXP;
    @FXML private Label       statLevel;
    @FXML private Label       statTasks;
    @FXML private Label       statFocus;
    @FXML private Label       statStreak;
    @FXML private ProgressBar xpProgressBar;
    @FXML private Label       labelXpProgress;

    // ── Right sidebar profile card ───────────────────────────────────
    @FXML private Label     labelProfileName;
    @FXML private Label     labelProfileEmail;
    @FXML private Label     labelProfileRole;
    @FXML private Label     avatarInitialsRight;
    @FXML private ImageView avatarImageRight;
    @FXML private Circle    avatarCircleRight;

    // ── Social JSON (same path as SocialHubController) ───────────────
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/avatars/";
    private final File friendsFile = new File(
            System.getProperty("user.home") + "/.mindforge/social/friends.json");
    private final Gson gson = new GsonBuilder().create();

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        loadUserData();
        loadFriendsAsync();
    }

    // ═══════════════════════════════════════════════════════════════
    // LOAD CURRENT USER DATA (profile + gamification)
    // ═══════════════════════════════════════════════════════════════
    private void loadUserData() {
        int userId = UserSession.getInstance().getUserId();
        String email = UserSession.getInstance().getEmail();

        Task<UserData> task = new Task<>() {
            @Override
            protected UserData call() throws Exception {
                String sql = """
                    SELECT
                        p.first_name, p.last_name, p.avatar,
                        u.roles,
                        COALESCE(g.total_xp, 0)          AS total_xp,
                        COALESCE(g.current_level, 1)     AS current_level,
                        COALESCE(g.streak_days, 0)       AS streak_days,
                        COALESCE(g.total_focus_time, 0)  AS total_focus_time,
                        COALESCE(g.tasks_completed, 0)   AS tasks_completed
                    FROM user u
                    LEFT JOIN profile p           ON p.user_id = u.id
                    LEFT JOIN gamification_stats g ON g.user_id = u.id
                    WHERE u.id = ?
                    """;
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return new UserData(
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                email,
                                rs.getString("avatar"),
                                rs.getString("roles"),
                                rs.getInt("total_xp"),
                                rs.getInt("current_level"),
                                rs.getInt("streak_days"),
                                rs.getInt("total_focus_time"),
                                rs.getInt("tasks_completed")
                        );
                    }
                }
                return new UserData(null, null, email, null, null, 0, 1, 0, 0, 0);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> applyUserData(getValue()));
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> labelUserName.setText(email != null ? email.split("@")[0] : "User"));
            }
        };
        new Thread(task, "dashboard-user-loader").start();
    }

    private void applyUserData(UserData d) {
        // Build display name
        String displayName = buildDisplayName(d.firstName, d.lastName, d.email);
        String initials    = buildInitials(d.firstName, d.lastName, d.email);

        // Top bar
        labelUserName.setText(displayName);
        avatarInitialsTop.setText(initials);
        labelXpTop.setText(d.totalXp + " XP");
        labelLevelTop.setText("Lv " + d.level);
        labelStreakTop.setText(d.streak + " days");

        // Right sidebar profile card
        labelProfileName.setText(displayName);
        labelProfileEmail.setText(d.email != null ? d.email : "");
        avatarInitialsRight.setText(initials);
        String roleLabel = "Student";
        if (d.roles != null) {
            if (d.roles.contains("ROLE_ADMIN"))        roleLabel = "Admin";
            else if (d.roles.contains("ROLE_COMPANY")) roleLabel = "Company";
            else if (d.roles.contains("ROLE_STUDENT_PLUS")) roleLabel = "Student+";
        }
        labelProfileRole.setText(roleLabel);

        // Stats row
        statXP.setText(String.valueOf(d.totalXp));
        statLevel.setText(String.valueOf(d.level));
        statTasks.setText(String.valueOf(d.tasksDone));
        statStreak.setText(String.valueOf(d.streak));
        statFocus.setText(d.focusMinutes >= 60
                ? (d.focusMinutes / 60) + "h " + (d.focusMinutes % 60) + "m"
                : d.focusMinutes + "m");

        // XP progress bar (500 XP per level)
        int xpIntoLevel = d.totalXp % 500;
        xpProgressBar.setProgress((double) xpIntoLevel / 500.0);
        labelXpProgress.setText(xpIntoLevel + " / 500 XP to next level");

        // Avatar
        if (d.avatar != null && !d.avatar.isBlank()) {
            loadAvatarAsync(d.avatar, avatarImageTop, avatarInitialsTop);
            loadAvatarAsync(d.avatar, avatarImageRight, avatarInitialsRight);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LOAD REAL FRIENDS FROM JSON + DB
    // ═══════════════════════════════════════════════════════════════
    private void loadFriendsAsync() {
        int currentUserId = UserSession.getInstance().getUserId();

        Task<List<FriendRow>> task = new Task<>() {
            @Override
            protected List<FriendRow> call() throws Exception {
                // 1. Read friendships from JSON
                List<FriendshipJson> friendships = readFriendships();
                Set<Integer> friendIds = new HashSet<>();
                for (FriendshipJson f : friendships) {
                    if ("accepted".equals(f.status)) {
                        if (f.requesterId == currentUserId) friendIds.add(f.addresseeId);
                        else if (f.addresseeId == currentUserId) friendIds.add(f.requesterId);
                    }
                }
                if (friendIds.isEmpty()) return Collections.emptyList();

                // 2. Query DB for friend profiles + stats
                String placeholders = friendIds.stream().map(id -> "?").collect(Collectors.joining(","));
                String sql = """
                    SELECT u.id, u.email,
                           p.first_name, p.last_name, p.avatar,
                           COALESCE(g.total_xp, 0)        AS total_xp,
                           COALESCE(g.current_level, 1)   AS current_level
                    FROM user u
                    LEFT JOIN profile p           ON p.user_id = u.id
                    LEFT JOIN gamification_stats g ON g.user_id = u.id
                    WHERE u.id IN (""" + placeholders + ")";

                List<FriendRow> rows = new ArrayList<>();
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    int i = 1;
                    for (int id : friendIds) ps.setInt(i++, id);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        rows.add(new FriendRow(
                                rs.getInt("id"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("email"),
                                rs.getString("avatar"),
                                rs.getInt("total_xp"),
                                rs.getInt("current_level")
                        ));
                    }
                }
                return rows;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> renderFriends(getValue()));
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    friendsContainer.getChildren().clear();
                    Label err = new Label("Could not load friends.");
                    err.setStyle("-fx-text-fill: #6d748a; -fx-font-size: 13px;");
                    friendsContainer.getChildren().add(err);
                });
            }
        };
        new Thread(task, "dashboard-friends-loader").start();
    }

    private void renderFriends(List<FriendRow> friends) {
        friendsContainer.getChildren().clear();
        labelFriendCount.setText(String.valueOf(friends.size()));

        if (friends.isEmpty()) {
            Label empty = new Label("No friends yet. Go to Social Hub to connect with people.");
            empty.setStyle("-fx-text-fill: #6d748a; -fx-font-size: 13px;");
            empty.setWrapText(true);
            friendsContainer.getChildren().add(empty);
            return;
        }

        for (FriendRow f : friends) {
            friendsContainer.getChildren().add(buildFriendCard(f));
        }
    }

    private VBox buildFriendCard(FriendRow f) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #fbfcff; -fx-border-color: #ebedf7;" +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        StackPane av = new StackPane();
        Circle bg = new Circle(20, Color.web("#EEEDFE"));
        bg.setStroke(Color.web("#534AB7")); bg.setStrokeWidth(1.5);
        Label ini = new Label(buildInitials(f.firstName, f.lastName, f.email));
        ini.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3C3489;");
        ImageView iv = new ImageView();
        iv.setFitWidth(40); iv.setFitHeight(40);
        iv.setPreserveRatio(false);
        iv.setClip(new Circle(20, 20, 20));
        iv.setVisible(false);
        if (f.avatar != null && !f.avatar.isBlank()) loadAvatarAsync(f.avatar, iv, ini);
        av.getChildren().addAll(bg, ini, iv);

        // Name + stats
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel = new Label(buildDisplayName(f.firstName, f.lastName, f.email));
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #21273b;");
        Label statsLabel = new Label("Level " + f.level + "  ·  " + f.xp + " XP");
        statsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6d748a;");
        info.getChildren().addAll(nameLabel, statsLabel);

        topRow.getChildren().addAll(av, info);

        // Chat button
        Button chatBtn = new Button("💬 Chat");
        chatBtn.setStyle("-fx-background-color: #534AB7; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
        chatBtn.setOnAction(e -> goToSocialHub());

        card.getChildren().addAll(topRow, chatBtn);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════
    @FXML
    private void goToPlanner() {
        try {
            Scene scene = centerContent.getScene();
            final Parent dashboardRoot = scene.getRoot();
            PlannerModule plannerModule = new PlannerModule();
            Parent plannerRoot = plannerModule.getView();
            scene.setRoot(wrapWithBack(plannerRoot, "← Back to Dashboard", scene, dashboardRoot, "#f0f2f5"));
        } catch (Exception e) {
            showError("Planner", e.getMessage());
        }
    }

    @FXML
    private void goToCareers() {
        try {
            Scene scene = centerContent.getScene();
            final Parent dashboardRoot = scene.getRoot();
            UserSession us = UserSession.getInstance();
            SessionManager.setSession(
                    us.isCompany() ? SessionManager.Role.COMPANY_OWNER : SessionManager.Role.STUDENT,
                    us.getUserId(), us.getEmail());
            String fxml = us.isCompany()
                    ? "/com/mindforge/company-dashboard.fxml"
                    : "/com/mindforge/student-dashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent careerRoot = loader.load();
            if (!us.isCompany()) {
                StudentController ctrl = loader.getController();
                ctrl.selectTab(0);
            }
            scene.setRoot(wrapWithBack(careerRoot, "← Back to Dashboard", scene, dashboardRoot, "#f0f2f5"));
        } catch (Exception e) {
            showError("Careers", e.getMessage());
        }
    }

    @FXML
    private void goToSocialHub() {
        try {
            Scene scene = centerContent.getScene();
            final Parent dashboardRoot = scene.getRoot();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/social_hub.fxml"));
            Parent socialRoot = loader.load();
            scene.setRoot(wrapWithBack(socialRoot, "← Back to Dashboard", scene, dashboardRoot, "#ececf2"));
        } catch (IOException e) {
            showError("Social Hub", e.getMessage());
        }
    }

    @FXML
    private void goToCommunityHub() {
        openCommunityView("/com/mindforge/fxml/community-hub-dashboard.fxml", "Community Hub — MindForge");
    }

    // ── Community card button handlers (inline in dashboard) ──────────────────

    @FXML private void openVirtualRooms()    { openCommunityModuleView("/views/RoomsView.fxml",       "Salles Virtuelles — MindForge",  ctrl -> { if (ctrl instanceof com.example.controllers.RoomsController c)      c.setMainController(buildDashboardMainController()); }); }
    @FXML private void openSendChallenge()   { openCommunityModuleView("/views/SharedTaskView.fxml",  "Envoyer un Défi — MindForge",    ctrl -> { if (ctrl instanceof com.example.controllers.SharedTaskController c) { c.setMainController(buildDashboardMainController()); c.setMode(com.example.controllers.SharedTaskController.Mode.SEND); } }); }
    @FXML private void openMyTickets()       { openCommunityModuleView("/views/ClaimView.fxml",       "Mes Tickets — MindForge",        ctrl -> { if (ctrl instanceof com.example.controllers.ClaimController c)      { c.setMainController(buildDashboardMainController()); c.setMode(com.example.controllers.ClaimController.Mode.MY_TICKETS); } }); }
    @FXML private void openChallengeInbox()  { openCommunityModuleView("/views/SharedTaskView.fxml",  "Défis Reçus — MindForge",        ctrl -> { if (ctrl instanceof com.example.controllers.SharedTaskController c) { c.setMainController(buildDashboardMainController()); c.setMode(com.example.controllers.SharedTaskController.Mode.INBOX); } }); }
    @FXML private void openSentChallenges()  { openCommunityModuleView("/views/SharedTaskView.fxml",  "Défis Envoyés — MindForge",      ctrl -> { if (ctrl instanceof com.example.controllers.SharedTaskController c) { c.setMainController(buildDashboardMainController()); c.setMode(com.example.controllers.SharedTaskController.Mode.OUTBOX); } }); }

    /** Opens a community FXML view, replacing the scene root. */
    private void openCommunityView(String fxmlPath, String title) {
        try {
            com.mindforge.community.CommunityLauncher.bridgeSession();
            try { com.example.util.SchemaMigrator.migrateAll(); } catch (Exception ignored) {}
            Scene scene = centerContent.getScene();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            scene.setRoot(root);
            Stage stage = (Stage) scene.getWindow();
            if (stage != null) stage.setTitle(title);
        } catch (Exception e) {
            showError("Community", e.getMessage());
        }
    }

    /** Opens a community module FXML (rooms/tasks/claims), wires controller, replaces scene. */
    private void openCommunityModuleView(String fxmlPath, String title,
                                         java.util.function.Consumer<Object> setup) {
        try {
            com.mindforge.community.CommunityLauncher.bridgeSession();
            try { com.example.util.SchemaMigrator.migrateAll(); } catch (Exception ignored) {}
            Scene scene = centerContent.getScene();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object ctrl = loader.getController();
            if (ctrl != null) setup.accept(ctrl);
            String css = getClass().getResource("/styles/main.css") != null
                    ? getClass().getResource("/styles/main.css").toExternalForm() : null;
            if (css != null && !scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
            scene.setRoot(root);
            Stage stage = (Stage) scene.getWindow();
            if (stage != null) { stage.setTitle(title); stage.setMaximized(true); }
        } catch (Exception e) {
            showError("Community", e.getMessage());
        }
    }

    /** Builds a MainController whose back navigation returns to this dashboard. */
    private com.example.controllers.MainController buildDashboardMainController() {
        Scene scene = centerContent.getScene();
        Stage stage = (Stage) scene.getWindow();
        return new com.example.controllers.MainController() {
            { setPrimaryStage(stage); }
            @Override public void showMainView() { returnToDashboard(); }
            @Override public void showCommunityHub() { openCommunityView("/com/mindforge/fxml/community-hub-dashboard.fxml", "Community Hub — MindForge"); }
            private void returnToDashboard() {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/dashboard.fxml"));
                    Parent dash = loader.load();
                    scene.setRoot(dash);
                    if (stage != null) stage.setTitle("MindForge — Dashboard");
                } catch (IOException ex) { ex.printStackTrace(); }
            }
        };
    }

    /** Loads the Community Hub cards directly into the center VBox (no scene swap). */
    private void loadCommunityHubInCenter() {
        try {
            com.mindforge.community.CommunityLauncher.bridgeSession();
            try { com.example.util.SchemaMigrator.migrateAll(); } catch (Exception ignored) {}

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/community-hub-dashboard.fxml"));
            Parent communityRoot = loader.load();
            Scene scene = centerContent.getScene();
            if (scene != null) {
                Stage stage = (Stage) scene.getWindow();
                scene.setRoot(communityRoot);
                if (stage != null) stage.setTitle("Community Hub — MindForge");
            }
        } catch (Exception e) {
            System.err.println("[Dashboard] Could not pre-load Community Hub: " + e.getMessage());
        }
    }

    @FXML
    private void openGuardian() {
        try {
            Scene scene = centerContent.getScene();
            final Parent dashboardRoot = scene.getRoot();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/view/guardian-hub.fxml"));
            Parent guardianRoot = loader.load();
            scene.setRoot(wrapWithBack(guardianRoot, "← Back to Dashboard", scene, dashboardRoot, "#f0f2f5"));
        } catch (IOException e) {
            showError("Guardian", e.getMessage());
        }
    }

    @FXML
    private void openFocusTimer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/focus_timer.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Focus Timer");
            stage.setScene(new Scene(root, 400, 300));
            stage.show();
        } catch (IOException e) {
            System.out.println("Focus timer not available yet");
        }
    }

    @FXML
    private void openLeaderboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/leaderboard.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Leaderboard");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
        } catch (IOException e) {
            System.out.println("Leaderboard not available yet");
        }
    }

    @FXML
    private void goToWorkspace() { /* already on workspace */ }

    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) centerContent.getScene().getWindow();
            stage.setTitle("MindForge - Profile");
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Profile", e.getMessage());
        }
    }

    @FXML
    private void logout() {
        UserSession.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) centerContent.getScene().getWindow();
            stage.setTitle("MindForge - Login");
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    /** Wraps a view with a consistent "← Back" top bar. */
    private VBox wrapWithBack(Parent content, String btnText, Scene scene, Parent returnTo, String bg) {
        Button backBtn = new Button(btnText);
        backBtn.setStyle("-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                "-fx-font-weight: bold; -fx-font-size: 13px;");
        backBtn.setOnAction(e -> scene.setRoot(returnTo));
        HBox topBar = new HBox(backBtn);
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: " + bg + ";");
        VBox.setVgrow(content, Priority.ALWAYS);
        VBox page = new VBox(topBar, content);
        page.setStyle("-fx-background-color: " + bg + ";");
        return page;
    }

    private void loadAvatarAsync(String avatar, ImageView imgView, Label fallback) {
        if (avatar == null || avatar.isBlank()) return;
        Task<Image> task = new Task<>() {
            @Override protected Image call() {
                try {
                    File f = new File(UPLOAD_DIR + avatar);
                    if (f.exists()) return new Image(f.toURI().toString(), 76, 76, false, true);
                    URL r = getClass().getResource("/com/mindforge/avatars/" + avatar);
                    if (r != null) return new Image(r.toExternalForm(), 76, 76, false, true);
                } catch (Exception ignored) {}
                return null;
            }
            @Override protected void succeeded() {
                Image img = getValue();
                if (img != null && !img.isError()) {
                    imgView.setImage(img);
                    imgView.setVisible(true);
                    fallback.setVisible(false);
                }
            }
        };
        new Thread(task, "avatar-loader").start();
    }

    private List<FriendshipJson> readFriendships() {
        if (!friendsFile.exists()) return Collections.emptyList();
        try (Reader r = new FileReader(friendsFile)) {
            List<FriendshipJson> list = gson.fromJson(r, new com.google.gson.reflect.TypeToken<List<FriendshipJson>>(){}.getType());
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg != null ? msg : "An unexpected error occurred.");
        alert.showAndWait();
    }

    private static String buildDisplayName(String fn, String ln, String email) {
        if (fn != null && ln != null && !fn.isBlank() && !ln.isBlank()) return fn + " " + ln;
        if (fn != null && !fn.isBlank()) return fn;
        if (ln != null && !ln.isBlank()) return ln;
        return email != null ? email.split("@")[0] : "User";
    }

    private static String buildInitials(String fn, String ln, String email) {
        StringBuilder sb = new StringBuilder();
        if (fn != null && !fn.isBlank()) sb.append(Character.toUpperCase(fn.charAt(0)));
        if (ln != null && !ln.isBlank()) sb.append(Character.toUpperCase(ln.charAt(0)));
        if (sb.isEmpty() && email != null && !email.isBlank())
            sb.append(Character.toUpperCase(email.charAt(0)));
        if (sb.isEmpty()) sb.append("?");
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════

    private static class UserData {
        final String firstName, lastName, email, avatar, roles;
        final int totalXp, level, streak, focusMinutes, tasksDone;
        UserData(String fn, String ln, String email, String avatar, String roles,
                 int xp, int level, int streak, int focus, int tasks) {
            this.firstName = fn; this.lastName = ln; this.email = email;
            this.avatar = avatar; this.roles = roles;
            this.totalXp = xp; this.level = level; this.streak = streak;
            this.focusMinutes = focus; this.tasksDone = tasks;
        }
    }

    private static class FriendRow {
        final int id, xp, level;
        final String firstName, lastName, email, avatar;
        FriendRow(int id, String fn, String ln, String email, String avatar, int xp, int level) {
            this.id = id; this.firstName = fn; this.lastName = ln;
            this.email = email; this.avatar = avatar; this.xp = xp; this.level = level;
        }
    }

    /** Mirrors the Friendship JSON structure from SocialHubController. */
    private static class FriendshipJson {
        long id;
        int requesterId;
        int addresseeId;
        String status;
    }
}
