package com.mindforge.architect.controller;

import com.mindforge.model.RoleRequest;
import com.mindforge.util.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.esprit.Entity.Guardian.AiInsight;
import tn.esprit.Entity.Guardian.Resource;
import tn.esprit.Entity.Guardian.VirtualRoom;
import tn.esprit.services.guardian.AiInsightService;
import tn.esprit.services.guardian.ResourceService;
import tn.esprit.services.guardian.VirtualRoomService;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminDashboardController {

    // ── Stats & header ────────────────────────────────────────────────────────
    @FXML private Label adminNameLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label verifiedCountLabel;
    @FXML private Label newTodayLabel;
    @FXML private Label resultCountLabel;

    // ── AI widgets ────────────────────────────────────────────────────────────
    @FXML private Label  labelRiskScore;
    @FXML private Label  labelRiskLevel;
    @FXML private Label  labelHealthScore;
    @FXML private Label  labelHealthLabel;
    @FXML private Label  labelAiSummary;
    @FXML private VBox   vboxAlerts;
    @FXML private Label  labelAnomalyStatus;
    @FXML private Label  labelAnomalyCount;
    @FXML private VBox   vboxAnomalies;
    @FXML private VBox   vboxGrowthTrend;
    @FXML private VBox   vboxCohorts;
    @FXML private VBox   vboxRecommendations;
    @FXML private TextArea areaAiReport;

    // ── Smart Search ──────────────────────────────────────────────────────────
    @FXML private TextField smartSearchField;
    @FXML private VBox      vboxSmartSearchResults;

    // ── Tab Pane ──────────────────────────────────────────────────────────────
    @FXML private TabPane mainTabPane;

    // ── Filters ───────────────────────────────────────────────────────────────
    @FXML private TextField       filterEmail;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> verifiedFilter;

    // ── Users table ───────────────────────────────────────────────────────────
    @FXML private TableView<UserRow>           usersTable;
    @FXML private TableColumn<UserRow, String> colId;
    @FXML private TableColumn<UserRow, String> colEmail;
    @FXML private TableColumn<UserRow, String> colRoles;
    @FXML private TableColumn<UserRow, String> colVerified;
    @FXML private TableColumn<UserRow, String> colCreated;
    @FXML private TableColumn<UserRow, Void>   colActions;

    // ── Requests table ────────────────────────────────────────────────────────
    @FXML private TableView<RoleRequest>             requestsTable;
    @FXML private TableColumn<RoleRequest, Integer>  colReqId;
    @FXML private TableColumn<RoleRequest, String>   colReqEmail;
    @FXML private TableColumn<RoleRequest, String>   colReqMotiv;
    @FXML private TableColumn<RoleRequest, String>   colReqDate;
    @FXML private TableColumn<RoleRequest, String>   colReqStatus;
    @FXML private TableColumn<RoleRequest, Void>     colReqActions;
    @FXML private Label                              pendingCountLabel;

    // ── Guardian AI Insights tab ──────────────────────────────────────────────
    @FXML private ComboBox<String>             aiInsightTypeFilter;
    @FXML private ComboBox<String>             aiInsightSourceFilter;
    @FXML private TableView<AiInsight>         guardianAiInsightsTable;
    @FXML private TableColumn<AiInsight, String> colAiInsightDate;
    @FXML private TableColumn<AiInsight, String> colAiInsightUser;
    @FXML private TableColumn<AiInsight, String> colAiInsightTask;
    @FXML private TableColumn<AiInsight, String> colAiInsightType;
    @FXML private TableColumn<AiInsight, String> colAiInsightSource;
    @FXML private TableColumn<AiInsight, String> colAiInsightHelpful;
    @FXML private TableColumn<AiInsight, String> colAiInsightPayload;

    // ── Guardian Resources tab ────────────────────────────────────────────────
    @FXML private TableView<Resource>          guardianResourcesTable;
    @FXML private TableColumn<Resource, String> colResId;
    @FXML private TableColumn<Resource, String> colResTitle;
    @FXML private TableColumn<Resource, String> colResType;
    @FXML private TableColumn<Resource, String> colResUploader;
    @FXML private TableColumn<Resource, String> colResDownloads;
    @FXML private TableColumn<Resource, String> colResRating;
    @FXML private TableColumn<Resource, String> colResCreated;
    @FXML private TableColumn<Resource, Void>   colResActions;

    // ── Guardian Rooms tab ────────────────────────────────────────────────────
    @FXML private TableView<VirtualRoom>           guardianRoomsTable;
    @FXML private TableColumn<VirtualRoom, String> colRoomId;
    @FXML private TableColumn<VirtualRoom, String> colRoomName;
    @FXML private TableColumn<VirtualRoom, String> colRoomCreator;
    @FXML private TableColumn<VirtualRoom, String> colRoomSubject;
    @FXML private TableColumn<VirtualRoom, String> colRoomMax;
    @FXML private TableColumn<VirtualRoom, String> colRoomActive;
    @FXML private TableColumn<VirtualRoom, String> colRoomCreated;
    @FXML private TableColumn<VirtualRoom, Void>   colRoomActions;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final ObservableList<UserRow>     usersList    = FXCollections.observableArrayList();
    private final ObservableList<RoleRequest> requestList  = FXCollections.observableArrayList();
    private FilteredList<UserRow> filteredUsers;

    private final ObservableList<AiInsight>   guardianAiInsightsList = FXCollections.observableArrayList();
    private final ObservableList<Resource>    guardianResourcesList  = FXCollections.observableArrayList();
    private final ObservableList<VirtualRoom> guardianRoomsList      = FXCollections.observableArrayList();

    private final AiInsightService   aiInsightService   = new AiInsightService();
    private final ResourceService    resourceService    = new ResourceService();
    private final VirtualRoomService virtualRoomService = new VirtualRoomService();

    // ═════════════════════════════════════════════════════════════════════════
    //  Inner model
    // ═════════════════════════════════════════════════════════════════════════
    public static class UserRow {
        int     id;
        String  email;
        String  roles;
        boolean verified;
        String  createdAt;

        UserRow(int id, String email, String roles, boolean verified, String createdAt) {
            this.id        = id;
            this.email     = email;
            this.roles     = roles;
            this.verified  = verified;
            this.createdAt = createdAt;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  initialize
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if (!UserSession.getInstance().isAdmin()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "You don't have admin privileges!");
            logout();
            return;
        }

        adminNameLabel.setText(UserSession.getInstance().getEmail());

        roleFilter.setItems(FXCollections.observableArrayList(
                "All Roles", "Admin", "Student+", "Student", "User"));
        verifiedFilter.setItems(FXCollections.observableArrayList(
                "All Status", "Verified", "Unverified"));

        setupUsersTable();
        setupRequestsTable();
        setupSearch();
        setupGuardianAiInsightsTable();
        setupGuardianResourcesTable();
        setupGuardianRoomsTable();

        loadUsers();
        loadRequests();
        loadGuardianAiInsights();
        loadGuardianResources();
        loadGuardianRooms();

        // All stat cards + AI widgets computed from in-memory data — no extra DB calls
        refreshAllDashboardWidgets();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Dashboard widget orchestrator
    //  Called after loadUsers() so usersList is populated.
    // ═════════════════════════════════════════════════════════════════════════
    private void refreshAllDashboardWidgets() {
        updateStatCards();
        computeAndShowAiWidgets();
    }

    // ── Stat cards ────────────────────────────────────────────────────────────
    private void updateStatCards() {
        int total    = usersList.size();
        long admins  = usersList.stream().filter(u -> u.roles != null && u.roles.contains("ROLE_ADMIN")).count();
        long verified= usersList.stream().filter(u -> u.verified).count();

        // "new today" — parse createdAt string (yyyy-MM-dd HH:mm)
        String today = LocalDate.now().toString(); // yyyy-MM-dd
        long newToday = usersList.stream()
                .filter(u -> u.createdAt != null && u.createdAt.startsWith(today))
                .count();

        totalUsersLabel.setText(String.valueOf(total));
        adminCountLabel.setText(String.valueOf(admins));
        verifiedCountLabel.setText(String.valueOf(verified));
        newTodayLabel.setText(String.valueOf(newToday));
    }

    // ── All AI / smart widgets ────────────────────────────────────────────────
    private void computeAndShowAiWidgets() {
        int total     = usersList.size();
        long admins   = usersList.stream().filter(u -> u.roles != null && u.roles.contains("ROLE_ADMIN")).count();
        long verified = usersList.stream().filter(u -> u.verified).count();
        long students = usersList.stream().filter(u -> u.roles != null && u.roles.contains("ROLE_STUDENT")).count();
        long stdPlus  = usersList.stream().filter(u -> u.roles != null && u.roles.contains("ROLE_STUDENT_PLUS")).count();
        long regular  = usersList.stream().filter(u -> u.roles != null && !u.roles.contains("ROLE_ADMIN")
                && !u.roles.contains("ROLE_STUDENT")).count();

        double verifyRate  = total > 0 ? (verified  * 100.0 / total) : 0;
        double adminRate   = total > 0 ? (admins    * 100.0 / total) : 0;
        long   unverified  = total - verified;

        // ── Risk Score (0-100, lower = better) ─────────────────────────────
        int riskScore = computeRiskScore(total, (int) admins, (int) unverified, verifyRate, adminRate);
        String riskLevel = riskScore <= 25 ? "LOW" : riskScore <= 50 ? "MODERATE" : riskScore <= 75 ? "HIGH" : "CRITICAL";
        String riskColor = riskScore <= 25 ? "#16a34a" : riskScore <= 50 ? "#f59e0b" : "#dc2626";

        if (labelRiskScore != null) {
            labelRiskScore.setText(String.valueOf(riskScore));
            labelRiskScore.setStyle("-fx-text-fill: " + riskColor + "; -fx-font-size: 32px; -fx-font-weight: 700;");
        }
        if (labelRiskLevel != null) {
            labelRiskLevel.setText(riskLevel);
            labelRiskLevel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + riskColor + ";");
        }

        // ── Health Score (0-100, higher = better) ──────────────────────────
        int healthScore = computeHealthScore(verifyRate, adminRate, total);
        String healthLabel = healthScore >= 80 ? "EXCELLENT" : healthScore >= 60 ? "GOOD" : healthScore >= 40 ? "FAIR" : "POOR";
        String healthColor = healthScore >= 80 ? "#16a34a" : healthScore >= 60 ? "#4f46e5" : healthScore >= 40 ? "#f59e0b" : "#dc2626";

        if (labelHealthScore != null) {
            labelHealthScore.setText(String.valueOf(healthScore));
            labelHealthScore.setStyle("-fx-text-fill: " + healthColor + "; -fx-font-size: 32px; -fx-font-weight: 700;");
        }
        if (labelHealthLabel != null) {
            labelHealthLabel.setText(healthLabel);
            labelHealthLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + healthColor + ";");
        }

        // ── AI Security Summary ─────────────────────────────────────────────
        if (labelAiSummary != null) {
            String summary = buildAiSummary(total, (int) admins, (int) verified, (int) unverified, verifyRate, adminRate, riskLevel);
            labelAiSummary.setText(summary);
        }

        // ── Alerts ─────────────────────────────────────────────────────────
        if (vboxAlerts != null) {
            vboxAlerts.getChildren().clear();
            vboxAlerts.setMaxWidth(Double.MAX_VALUE);
            vboxAlerts.setFillWidth(true);
            List<String[]> alerts = buildAlerts(verifyRate, adminRate, total, (int) unverified);
            if (alerts.isEmpty()) {
                vboxAlerts.getChildren().add(styledLabel("✅ No active alerts — platform looks healthy.", "#16a34a", 12));
            } else {
                for (String[] a : alerts) {
                    vboxAlerts.getChildren().add(alertRow(a[0], a[1]));
                }
            }
        }

        // ── Anomaly Detection ───────────────────────────────────────────────
        List<String> anomalies = detectAnomalies(total, (int) admins, verifyRate);
        if (labelAnomalyStatus != null) {
            boolean hasAnomalies = !anomalies.isEmpty();
            labelAnomalyStatus.setText(hasAnomalies ? "⚠ ANOMALIES FOUND" : "✅ CLEAN");
            labelAnomalyStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: "
                    + (hasAnomalies ? "#dc2626" : "#16a34a") + ";");
        }
        if (labelAnomalyCount != null) {
            labelAnomalyCount.setText(anomalies.size() + " issue(s)");
        }
        if (vboxAnomalies != null) {
            vboxAnomalies.getChildren().clear();
            if (anomalies.isEmpty()) {
                vboxAnomalies.getChildren().add(styledLabel("No anomalies detected in current dataset.", "#64748b", 12));
            } else {
                for (String a : anomalies) {
                    vboxAnomalies.getChildren().add(anomalyRow(a));
                }
            }
        }

        // ── 7-Day Growth Trend Chart ────────────────────────────────────────
        if (vboxGrowthTrend != null) {
            vboxGrowthTrend.getChildren().clear();
            Map<String, Long> trend = buildGrowthTrend();
            vboxGrowthTrend.getChildren().add(buildBarChart(trend, 500, 130,
                    "#4f46e5", "Registrations per day (last 7 days)"));
        }

        // ── User Cohorts Chart ──────────────────────────────────────────────
        if (vboxCohorts != null) {
            vboxCohorts.getChildren().clear();
            Map<String, Long> cohorts = new LinkedHashMap<>();
            cohorts.put("Admins",    admins);
            cohorts.put("Student+",  stdPlus);
            cohorts.put("Students",  students);
            cohorts.put("Regular",   regular);
            vboxCohorts.getChildren().add(buildHorizontalBarChart(cohorts, 400, 140, "User Cohorts Breakdown"));
        }

        // ── Recommendations ─────────────────────────────────────────────────
        if (vboxRecommendations != null) {
            vboxRecommendations.getChildren().clear();
            List<String[]> recs = buildRecommendations(verifyRate, adminRate, total);
            for (int i = 0; i < recs.size(); i++) {
                vboxRecommendations.getChildren().add(recommendationRow(i + 1, recs.get(i)[0], recs.get(i)[1]));
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Computation helpers
    // ═════════════════════════════════════════════════════════════════════════

    private int computeRiskScore(int total, int admins, int unverified, double verifyRate, double adminRate) {
        if (total == 0) return 0;
        int score = 0;
        if (verifyRate < 50)  score += 35;
        else if (verifyRate < 70) score += 20;
        else if (verifyRate < 90) score += 10;

        if (adminRate > 20) score += 30;
        else if (adminRate > 10) score += 15;
        else if (adminRate > 5)  score += 5;

        if (unverified > 10) score += 20;
        else if (unverified > 5) score += 10;
        else if (unverified > 0) score += 5;

        if (total < 5) score += 10; // very small platform, uncertain

        return Math.min(score, 100);
    }

    private int computeHealthScore(double verifyRate, double adminRate, int total) {
        if (total == 0) return 0;
        int score = 100;
        // Penalize low verify rate
        if (verifyRate < 50)  score -= 35;
        else if (verifyRate < 70) score -= 20;
        else if (verifyRate < 90) score -= 10;
        // Penalize excessive admins
        if (adminRate > 20) score -= 25;
        else if (adminRate > 10) score -= 10;
        else if (adminRate > 5)  score -= 5;
        // Reward growth
        if (total >= 50) score += 5;
        return Math.max(0, Math.min(score, 100));
    }

    private String buildAiSummary(int total, int admins, int verified, int unverified,
                                  double verifyRate, double adminRate, String riskLevel) {
        return String.format(
                "Platform has %d registered users. Verification rate is %.0f%% (%d verified, %d pending). " +
                        "Admin density is %.1f%% (%d admins). Overall risk level is assessed as %s. " +
                        "%s",
                total, verifyRate, verified, unverified, adminRate, admins, riskLevel,
                verifyRate < 70
                        ? "⚠ Verification rate is below the 70% target — consider triggering a reminder campaign."
                        : "✅ Verification rate meets the recommended threshold."
        );
    }

    private List<String[]> buildAlerts(double verifyRate, double adminRate, int total, int unverified) {
        List<String[]> alerts = new ArrayList<>();
        if (total == 0) {
            alerts.add(new String[]{"🔵", "No users in the system yet."});
            return alerts;
        }
        if (verifyRate < 50)
            alerts.add(new String[]{"🔴", String.format("Critical: only %.0f%% of users are verified.", verifyRate)});
        else if (verifyRate < 70)
            alerts.add(new String[]{"🟡", String.format("Warning: verification rate is %.0f%% (target ≥ 70%%).", verifyRate)});

        if (adminRate > 20)
            alerts.add(new String[]{"🔴", String.format("Critical: admin ratio is %.1f%% — review privileged accounts.", adminRate)});
        else if (adminRate > 10)
            alerts.add(new String[]{"🟡", String.format("Warning: admin ratio is %.1f%% (target < 10%%).", adminRate)});

        if (unverified > 10)
            alerts.add(new String[]{"🟡", unverified + " users remain unverified — send a reminder email."});

        return alerts;
    }

    private List<String> detectAnomalies(int total, int admins, double verifyRate) {
        List<String> anomalies = new ArrayList<>();
        if (total > 0 && admins == total)
            anomalies.add("All users are admins — this is unusual.");
        if (total > 5 && verifyRate == 0)
            anomalies.add("Zero verified users despite " + total + " registrations.");
        if (total > 0 && admins == 0)
            anomalies.add("No admin users detected — platform may be unmanaged.");
        if (total > 20 && verifyRate < 20)
            anomalies.add("Very low verification rate (" + String.format("%.0f", verifyRate) + "%) across a sizeable user base.");
        return anomalies;
    }

    private Map<String, Long> buildGrowthTrend() {
        // Bucket users by registration date for the last 7 days
        Map<String, Long> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 6; i >= 0; i--) {
            String day = today.minusDays(i).format(fmt);
            trend.put(day.substring(5), 0L); // show MM-dd
        }

        for (UserRow u : usersList) {
            if (u.createdAt == null || u.createdAt.length() < 10) continue;
            String dateKey = u.createdAt.substring(5, 10); // MM-dd
            if (trend.containsKey(dateKey)) {
                trend.put(dateKey, trend.get(dateKey) + 1);
            }
        }
        return trend;
    }

    private List<String[]> buildRecommendations(double verifyRate, double adminRate, int total) {
        List<String[]> recs = new ArrayList<>();
        if (verifyRate < 70)
            recs.add(new String[]{"📧 Email Campaign", "Send verification reminders to the " + (int)(total * (100 - verifyRate) / 100) + " unverified users."});
        if (adminRate > 10)
            recs.add(new String[]{"🔒 Audit Admins", "Review admin accounts — ratio is above the recommended 10% threshold."});
        recs.add(new String[]{"📋 Review Requests", "Process pending role-upgrade requests to keep students progressing."});
        recs.add(new String[]{"📊 Weekly Report", "Generate the AI report below for a full narrative summary."});
        if (total < 10)
            recs.add(new String[]{"🚀 Growth", "Platform has fewer than 10 users — focus on onboarding outreach."});
        return recs;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Chart builders using Canvas (no new imports)
    // ═════════════════════════════════════════════════════════════════════════

    /** Vertical bar chart */
    private javafx.scene.Node buildBarChart(Map<String, Long> data, double width, double height,
                                            String color, String title) {
        VBox container = new VBox(6);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        long maxVal = data.values().stream().mapToLong(Long::longValue).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        double padLeft = 30, padBottom = 24, padTop = 10, padRight = 10;
        double chartW = width - padLeft - padRight;
        double chartH = height - padBottom - padTop;
        int n = data.size();
        double barW = Math.max(4, (chartW / n) * 0.6);
        double gap  = chartW / n;

        // Background
        gc.setFill(Color.web("#f8fafc"));
        gc.fillRoundRect(0, 0, width, height, 10, 10);

        // Grid lines
        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(0.8);
        for (int i = 0; i <= 4; i++) {
            double y = padTop + chartH - (i * chartH / 4.0);
            gc.strokeLine(padLeft, y, padLeft + chartW, y);
            gc.setFill(Color.web("#94a3b8"));
            gc.fillText(String.valueOf(maxVal * i / 4), 2, y + 4);
        }

        // Bars
        int idx = 0;
        gc.setFont(javafx.scene.text.Font.font(9));
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            double barH = (entry.getValue() * chartH) / maxVal;
            double x = padLeft + idx * gap + (gap - barW) / 2.0;
            double y = padTop + chartH - barH;

            // Bar shadow
            gc.setFill(Color.web(color + "33"));
            gc.fillRoundRect(x + 2, y + 2, barW, barH, 4, 4);

            // Bar
            gc.setFill(Color.web(color));
            gc.fillRoundRect(x, y, barW, barH, 4, 4);

            // Value label on top
            if (entry.getValue() > 0) {
                gc.setFill(Color.web("#0f172a"));
                gc.fillText(String.valueOf(entry.getValue()), x + barW / 2 - 3, y - 2);
            }

            // X label
            gc.setFill(Color.web("#64748b"));
            gc.fillText(entry.getKey(), x + barW / 2 - 8, padTop + chartH + 14);

            idx++;
        }

        container.getChildren().addAll(titleLabel, canvas);
        return container;
    }

    /** Horizontal bar chart for cohorts */
    private javafx.scene.Node buildHorizontalBarChart(Map<String, Long> data, double width, double height, String title) {
        VBox container = new VBox(6);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        long maxVal = data.values().stream().mapToLong(Long::longValue).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        String[] colors = {"#dc2626", "#4f46e5", "#0ea5e9", "#16a34a"};
        double padLeft = 70, padRight = 50, padTop = 10, padBottom = 10;
        double chartW = width - padLeft - padRight;
        int n = data.size();
        double rowH = (height - padTop - padBottom) / n;
        double barH = rowH * 0.55;

        gc.setFill(Color.web("#f8fafc"));
        gc.fillRoundRect(0, 0, width, height, 10, 10);

        int idx = 0;
        gc.setFont(javafx.scene.text.Font.font(11));
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            double y = padTop + idx * rowH + (rowH - barH) / 2;
            double barW = (entry.getValue() * chartW) / maxVal;

            // Label
            gc.setFill(Color.web("#475569"));
            gc.fillText(entry.getKey(), 4, y + barH / 2 + 4);

            // Track
            gc.setFill(Color.web("#e2e8f0"));
            gc.fillRoundRect(padLeft, y, chartW, barH, 4, 4);

            // Bar
            gc.setFill(Color.web(colors[idx % colors.length]));
            gc.fillRoundRect(padLeft, y, Math.max(barW, 2), barH, 4, 4);

            // Value
            gc.setFill(Color.web("#0f172a"));
            gc.fillText(String.valueOf(entry.getValue()), padLeft + chartW + 6, y + barH / 2 + 4);

            idx++;
        }

        container.getChildren().addAll(titleLabel, canvas);
        return container;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI component helpers
    // ═════════════════════════════════════════════════════════════════════════

    private Label styledLabel(String text, String color, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px;");
        l.setWrapText(true);
        return l;
    }

    private HBox alertRow(String icon, String message) {
        Label iconLabel = new Label(icon);
        iconLabel.setMinWidth(20);
        Label msgLabel  = new Label(message);
        msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e293b;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(msgLabel, javafx.scene.layout.Priority.ALWAYS);
        HBox row = new HBox(8, iconLabel, msgLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle("-fx-background-color: #fff7ed; -fx-background-radius: 8; -fx-padding: 8 12;");
        return row;
    }

    private HBox anomalyRow(String message) {
        Label icon = new Label("⚡");
        icon.setMinWidth(20);
        Label msg  = new Label(message);
        msg.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c3aed;");
        msg.setWrapText(true);
        msg.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(msg, javafx.scene.layout.Priority.ALWAYS);
        HBox row = new HBox(8, icon, msg);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle("-fx-background-color: #faf5ff; -fx-background-radius: 8; -fx-padding: 8 12;");
        return row;
    }

    private HBox recommendationRow(int num, String title, String detail) {
        Label numLabel   = new Label(String.valueOf(num));
        numLabel.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; " +
                "-fx-font-weight: 700; -fx-font-size: 12px; " +
                "-fx-background-radius: 50%; -fx-min-width: 24; -fx-min-height: 24; " +
                "-fx-alignment: CENTER; -fx-padding: 2 6;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
        Label detailLabel = new Label(detail);
        detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        detailLabel.setWrapText(true);
        VBox text = new VBox(2, titleLabel, detailLabel);
        HBox row  = new HBox(12, numLabel, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 10 14;");
        return row;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Users tab
    // ═════════════════════════════════════════════════════════════════════════
    private void setupUsersTable() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id)));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email));
        colRoles.setCellValueFactory(c -> new SimpleStringProperty(formatRoles(c.getValue().roles)));
        colVerified.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().verified ? "Yes" : "No"));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().createdAt));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn   = new Button("👁");
            private final Button editBtn   = new Button("✏");
            private final Button deleteBtn = new Button("🗑");

            {
                viewBtn.setStyle(  "-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 10;");
                editBtn.setStyle(  "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 10;");
                deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 10;");

                viewBtn.setOnAction(e   -> viewUser(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e   -> editUser(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, viewBtn, editBtn, deleteBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });
    }

    private void setupSearch() {
        filteredUsers = new FilteredList<>(usersList, p -> true);
        filterEmail.textProperty().addListener((obs, o, n) -> applyFilters());
        roleFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        verifiedFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        usersTable.setItems(filteredUsers);
    }

    private void applyFilters() {
        String emailFilter = filterEmail.getText().toLowerCase();
        String roleSel     = roleFilter.getValue();
        String verifiedSel = verifiedFilter.getValue();

        filteredUsers.setPredicate(user -> {
            boolean matchesEmail = emailFilter.isEmpty()
                    || user.email.toLowerCase().contains(emailFilter)
                    || String.valueOf(user.id).contains(emailFilter);

            boolean matchesRole = roleSel == null || roleSel.equals("All Roles")
                    || (roleSel.equals("Admin")    && user.roles.contains("ROLE_ADMIN"))
                    || (roleSel.equals("Student+") && user.roles.contains("ROLE_STUDENT_PLUS"))
                    || (roleSel.equals("Student")  && user.roles.contains("ROLE_STUDENT"))
                    || (roleSel.equals("User")     && user.roles.contains("ROLE_USER"));

            boolean matchesVerified = verifiedSel == null || verifiedSel.equals("All Status")
                    || (verifiedSel.equals("Verified")   &&  user.verified)
                    || (verifiedSel.equals("Unverified") && !user.verified);

            return matchesEmail && matchesRole && matchesVerified;
        });

        resultCountLabel.setText("Showing " + filteredUsers.size() + " user(s)");
    }

    @FXML
    private void clearFilters() {
        filterEmail.clear();
        roleFilter.setValue("All Roles");
        verifiedFilter.setValue("All Status");
        applyFilters();
    }

    private void loadUsers() {
        usersList.clear();
        String sql = "SELECT id, email, roles, is_verified, created_at FROM user ORDER BY id DESC";

        try (Connection conn = getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                usersList.add(new UserRow(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("roles"),
                        rs.getBoolean("is_verified"),
                        formatDate(rs.getTimestamp("created_at"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void viewUser(UserRow user) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Details");
        alert.setHeaderText("User #" + user.id);
        alert.setContentText(
                "Email: "    + user.email + "\n" +
                        "Roles: "    + formatRoles(user.roles) + "\n" +
                        "Verified: " + (user.verified ? "Yes" : "No") + "\n" +
                        "Created: "  + user.createdAt);
        alert.showAndWait();
    }

    private void editUser(UserRow user) {
        Dialog<UserRow> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit User #" + user.id);

        TextField emailField = new TextField(user.email);
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList(
                "ROLE_USER", "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_STUDENT_PLUS"));
        roleBox.setValue(extractMainRole(user.roles));
        CheckBox verifiedBox = new CheckBox("Verified");
        verifiedBox.setSelected(user.verified);

        VBox content = new VBox(10,
                new Label("Email:"), emailField,
                new Label("Role:"),  roleBox,
                verifiedBox);
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                user.email    = emailField.getText();
                user.roles    = "[\"" + roleBox.getValue() + "\"]";
                user.verified = verifiedBox.isSelected();
                return user;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (updateUserInDB(updated)) {
                loadUsers();
                refreshAllDashboardWidgets();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
            }
        });
    }

    private void deleteUser(UserRow user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete User #" + user.id);
        confirm.setContentText("Are you sure you want to delete " + user.email + "?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (deleteUserFromDB(user.id)) {
                    usersList.remove(user);
                    refreshAllDashboardWidgets();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully!");
                }
            }
        });
    }

    @FXML
    private void showAddUserDialog() {
        Dialog<UserRow> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create New User");

        TextField     emailField = new TextField();   emailField.setPromptText("Email");
        PasswordField passField  = new PasswordField(); passField.setPromptText("Password");
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList(
                "ROLE_USER", "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_STUDENT_PLUS"));
        roleBox.setValue("ROLE_USER");
        CheckBox verifiedBox = new CheckBox("Verified");
        verifiedBox.setSelected(true);

        VBox content = new VBox(10,
                new Label("Email:"),    emailField,
                new Label("Password:"), passField,
                new Label("Role:"),     roleBox,
                verifiedBox);
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK
                ? new UserRow(0, emailField.getText(),
                "[\"" + roleBox.getValue() + "\"]",
                verifiedBox.isSelected(), "")
                : null);

        dialog.showAndWait().ifPresent(newUser -> {
            if (createUserInDB(newUser, passField.getText())) {
                loadUsers();
                refreshAllDashboardWidgets();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User created successfully!");
            }
        });
    }

    private boolean updateUserInDB(UserRow user) {
        String sql = "UPDATE user SET email = ?, roles = ?, is_verified = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.email);
            ps.setString(2, user.roles);
            ps.setBoolean(3, user.verified);
            ps.setInt(4, user.id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private boolean deleteUserFromDB(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM user WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private boolean createUserInDB(UserRow user, String password) {
        String sql = "INSERT INTO user (email, password, roles, is_verified, created_at) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.email);
            ps.setString(2, org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt()));
            ps.setString(3, user.roles);
            ps.setBoolean(4, user.verified);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Requests tab
    // ═════════════════════════════════════════════════════════════════════════
    private void setupRequestsTable() {
        colReqId.setCellValueFactory(
                c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colReqEmail.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getUserEmail()));
        colReqMotiv.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getMotivation()));
        colReqDate.setCellValueFactory(
                c -> new SimpleStringProperty(
                        c.getValue().getRequestedAt().toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm"))));
        colReqStatus.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getStatus().toUpperCase()));

        colReqStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                String color = switch (status) {
                    case "APPROVED" -> "#16a34a";
                    case "REJECTED" -> "#dc2626";
                    default         -> "#f59e0b";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 700;");
            }
        });

        colReqActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnApprove = new Button("✔ Approve");
            private final Button btnReject  = new Button("✘ Reject");
            private final HBox   box        = new HBox(6, btnApprove, btnReject);

            {
                box.setAlignment(Pos.CENTER);
                btnApprove.setStyle(
                        "-fx-background-color: #16a34a; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                                "-fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: 600;");
                btnReject.setStyle(
                        "-fx-background-color: #dc2626; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                                "-fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: 600;");

                btnApprove.setOnAction(e ->
                        handleRequestAction(getTableView().getItems().get(getIndex()), "approved"));
                btnReject.setOnAction(e ->
                        handleRequestAction(getTableView().getItems().get(getIndex()), "rejected"));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                boolean isPending = "pending".equals(
                        getTableView().getItems().get(getIndex()).getStatus());
                btnApprove.setDisable(!isPending);
                btnReject.setDisable(!isPending);
                setGraphic(box);
            }
        });

        requestsTable.setItems(requestList);
    }

    private void loadRequests() {
        requestList.clear();
        String sql =
                "SELECT rr.id, rr.user_id, u.email, rr.motivation, rr.status, rr.requested_at" +
                        " FROM role_request rr" +
                        " JOIN user u ON u.id = rr.user_id" +
                        " ORDER BY FIELD(rr.status,'pending','rejected','approved'), rr.requested_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int pending = 0;
            while (rs.next()) {
                RoleRequest req = new RoleRequest(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("motivation"),
                        rs.getString("status"),
                        rs.getTimestamp("requested_at")
                );
                requestList.add(req);
                if ("pending".equals(req.getStatus())) pending++;
            }

            int finalPending = pending;
            Platform.runLater(() -> pendingCountLabel.setText(String.valueOf(finalPending)));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleRequestAction(RoleRequest req, String newStatus) {
        String msg = "approved".equals(newStatus)
                ? "Approve role request for " + req.getUserEmail() + "?"
                : "Reject role request for "  + req.getUserEmail() + "?";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;

            try (Connection conn = getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE role_request SET status = ? WHERE id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, req.getId());
                    ps.executeUpdate();
                }
                if ("approved".equals(newStatus)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE user SET roles = ? WHERE id = ?")) {
                        ps.setString(1, "[\"ROLE_STUDENT_PLUS\"]");
                        ps.setInt(2, req.getUserId());
                        ps.executeUpdate();
                    }
                }

                loadRequests();
                loadUsers();
                refreshAllDashboardWidgets();

            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "DB error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Guardian AI Insights tab
    // ═════════════════════════════════════════════════════════════════════════
    private void setupGuardianAiInsightsTable() {
        if (guardianAiInsightsTable == null) return;

        if (aiInsightTypeFilter != null) {
            aiInsightTypeFilter.setItems(FXCollections.observableArrayList(
                    "All Types", "recommended_duration", "focus_tips", "daily_plan", "weekly_review"));
            aiInsightTypeFilter.getSelectionModel().selectFirst();
            aiInsightTypeFilter.valueProperty().addListener((obs, o, n) -> loadGuardianAiInsights());
        }
        if (aiInsightSourceFilter != null) {
            aiInsightSourceFilter.setItems(FXCollections.observableArrayList("All Sources", "ai", "rule"));
            aiInsightSourceFilter.getSelectionModel().selectFirst();
            aiInsightSourceFilter.valueProperty().addListener((obs, o, n) -> loadGuardianAiInsights());
        }

        colAiInsightDate.setCellValueFactory(c    -> new SimpleStringProperty(formatDateTime(c.getValue().createdAt())));
        colAiInsightUser.setCellValueFactory(c    -> new SimpleStringProperty(nullSafeNumber(c.getValue().userId())));
        colAiInsightTask.setCellValueFactory(c    -> new SimpleStringProperty(nullSafeNumber(c.getValue().taskId())));
        colAiInsightType.setCellValueFactory(c    -> new SimpleStringProperty(formatAiInsightType(c.getValue().type())));
        colAiInsightSource.setCellValueFactory(c  -> new SimpleStringProperty(formatAiInsightSource(c.getValue().source())));
        colAiInsightHelpful.setCellValueFactory(c -> new SimpleStringProperty(formatAiInsightFeedback(c.getValue())));
        colAiInsightPayload.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().payload())));

        colAiInsightPayload.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setTooltip(null); return; }
                AiInsight insight = getTableView().getItems().get(getIndex());
                String payload = insight == null ? "" : nullSafe(insight.payload());
                setText(truncate(payload, 160));
                setTooltip(payload.isEmpty() ? null : new Tooltip(payload));
            }
        });

        guardianAiInsightsTable.setItems(guardianAiInsightsList);
    }

    @FXML private void refreshGuardianAiInsights() { loadGuardianAiInsights(); }

    private void loadGuardianAiInsights() {
        if (guardianAiInsightsTable == null) return;
        guardianAiInsightsList.clear();
        String typeFilter   = normalizeFilterValue(aiInsightTypeFilter,   "All Types");
        String sourceFilter = normalizeFilterValue(aiInsightSourceFilter, "All Sources");
        try {
            if (typeFilter == null && sourceFilter == null)
                guardianAiInsightsList.setAll(aiInsightService.findAll());
            else
                guardianAiInsightsList.setAll(aiInsightService.findFiltered(typeFilter, sourceFilter));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load AI insights: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Guardian Resources tab
    // ═════════════════════════════════════════════════════════════════════════
    private void setupGuardianResourcesTable() {
        if (guardianResourcesTable == null) return;

        colResId.setCellValueFactory(c        -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        colResTitle.setCellValueFactory(c     -> new SimpleStringProperty(nullSafe(c.getValue().title())));
        colResType.setCellValueFactory(c      -> new SimpleStringProperty(nullSafe(c.getValue().type())));
        colResUploader.setCellValueFactory(c  -> new SimpleStringProperty(nullSafeNumber(c.getValue().uploaderId())));
        colResDownloads.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().downloadCount())));
        colResRating.setCellValueFactory(c    -> new SimpleStringProperty(nullSafeNumber(c.getValue().rating())));
        colResCreated.setCellValueFactory(c   -> new SimpleStringProperty(formatDateTime(c.getValue().createdAt())));

        colResActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                editBtn.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;");
                editBtn.setOnAction(e   -> editResource(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteResource(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        guardianResourcesTable.setItems(guardianResourcesList);
    }

    @FXML private void refreshGuardianResources() { loadGuardianResources(); }

    private void loadGuardianResources() {
        if (guardianResourcesTable == null) return;
        guardianResourcesList.clear();
        try { guardianResourcesList.setAll(resourceService.findAll()); }
        catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", "Failed to load resources: " + e.getMessage()); }
    }

    private void editResource(Resource resource) {
        if (resource == null) return;
        Dialog<Resource> dialog = new Dialog<>();
        dialog.setTitle("Edit Resource");
        dialog.setHeaderText("Resource #" + resource.id());

        TextField titleField       = new TextField(nullSafe(resource.title()));
        TextField descriptionField = new TextField(nullSafe(resource.description()));
        TextField filePathField    = new TextField(nullSafe(resource.filePath()));
        ComboBox<String> typeBox   = new ComboBox<>(FXCollections.observableArrayList(
                "pdf", "summary", "cheat_sheet", "exercise"));
        typeBox.setValue(resource.type() == null ? "summary" : resource.type());
        int ratingValue = resource.rating() == null ? 0 : resource.rating();
        Spinner<Integer> ratingSpinner = new Spinner<>(0, 5, Math.max(0, Math.min(5, ratingValue)));
        ratingSpinner.setEditable(true);

        VBox content = new VBox(10,
                new Label("Title:"), titleField,
                new Label("Description:"), descriptionField,
                new Label("File path:"), filePathField,
                new Label("Type:"), typeBox,
                new Label("Rating (0-5):"), ratingSpinner);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new Resource(resource.id(), titleField.getText().trim(),
                        descriptionField.getText().trim(), filePathField.getText().trim(),
                        typeBox.getValue(), resource.downloadCount(), ratingSpinner.getValue(),
                        resource.createdAt(), LocalDateTime.now(), resource.subjectId(), resource.uploaderId());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (updated.title() == null || updated.title().isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Title is required."); return;
            }
            try {
                resourceService.update(updated);
                loadGuardianResources();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Resource updated successfully!");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update resource: " + e.getMessage());
            }
        });
    }

    private void deleteResource(Resource resource) {
        if (resource == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Resource #" + resource.id());
        confirm.setContentText("Delete '" + nullSafe(resource.title()) + "'?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    resourceService.delete(resource.id());
                    loadGuardianResources();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Resource deleted successfully!");
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete resource: " + e.getMessage());
                }
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Guardian Rooms tab
    // ═════════════════════════════════════════════════════════════════════════
    private void setupGuardianRoomsTable() {
        if (guardianRoomsTable == null) return;

        colRoomId.setCellValueFactory(c      -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        colRoomName.setCellValueFactory(c    -> new SimpleStringProperty(nullSafe(c.getValue().name())));
        colRoomCreator.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().creatorId())));
        colRoomSubject.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().subjectId())));
        colRoomMax.setCellValueFactory(c     -> new SimpleStringProperty(nullSafeNumber(c.getValue().maxParticipants())));
        colRoomActive.setCellValueFactory(c  -> new SimpleStringProperty(Boolean.TRUE.equals(c.getValue().isActive()) ? "Yes" : "No"));
        colRoomCreated.setCellValueFactory(c -> new SimpleStringProperty(formatDateTime(c.getValue().createdAt())));

        colRoomActions.setCellFactory(col -> new TableCell<>() {
            private final Button toggleBtn = new Button();
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, toggleBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;");
                toggleBtn.setOnAction(e -> toggleRoomActive(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteRoom(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                VirtualRoom room = getTableView().getItems().get(getIndex());
                boolean active = room != null && Boolean.TRUE.equals(room.isActive());
                toggleBtn.setText(active ? "Close" : "Open");
                toggleBtn.setStyle(active
                        ? "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;"
                        : "-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;");
                setGraphic(box);
            }
        });

        guardianRoomsTable.setItems(guardianRoomsList);
    }

    @FXML private void refreshGuardianRooms() { loadGuardianRooms(); }

    private void loadGuardianRooms() {
        if (guardianRoomsTable == null) return;
        guardianRoomsList.clear();
        try { guardianRoomsList.setAll(virtualRoomService.findAll()); }
        catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", "Failed to load rooms: " + e.getMessage()); }
    }

    private void toggleRoomActive(VirtualRoom room) {
        if (room == null || room.id() == null) return;
        VirtualRoom updated = new VirtualRoom(room.id(), room.name(), room.description(),
                !Boolean.TRUE.equals(room.isActive()), room.maxParticipants(),
                room.createdAt(), room.creatorId(), room.subjectId());
        try { virtualRoomService.update(updated); loadGuardianRooms(); }
        catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", "Failed to update room: " + e.getMessage()); }
    }

    private void deleteRoom(VirtualRoom room) {
        if (room == null || room.id() == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Room #" + room.id());
        confirm.setContentText("Delete '" + nullSafe(room.name()) + "'?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    virtualRoomService.delete(room.id());
                    loadGuardianRooms();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Room deleted successfully!");
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete room: " + e.getMessage());
                }
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  AI Report
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void generateAiReport() {
        if (areaAiReport == null) return;
        areaAiReport.setText("Generating report…");

        new Thread(() -> {
            int    total    = usersList.size();
            long   admins   = usersList.stream().filter(u -> u.roles != null && u.roles.contains("ROLE_ADMIN")).count();
            long   verified = usersList.stream().filter(u -> u.verified).count();
            long   students = usersList.stream().filter(u -> u.roles != null && u.roles.contains("ROLE_STUDENT")).count();
            double verRate  = total > 0 ? (verified * 100.0 / total) : 0;
            double admRate  = total > 0 ? (admins   * 100.0 / total) : 0;

            int healthScore = computeHealthScore(verRate, admRate, total);
            int riskScore   = computeRiskScore(total, (int) admins, (int)(total - verified), verRate, admRate);

            String report = String.format(
                    "📊 MindForge Weekly Admin Report%n" +
                            "════════════════════════════════%n%n" +
                            "Platform Overview%n" +
                            "  • Total users    : %d%n" +
                            "  • Admins         : %d (%.1f%%)%n" +
                            "  • Verified       : %d (%.1f%%)%n" +
                            "  • Unverified     : %d%n" +
                            "  • Students       : %d%n%n" +
                            "AI Scores%n" +
                            "  • Health Score   : %d/100 — %s%n" +
                            "  • Risk Score     : %d/100 — %s%n%n" +
                            "Health Assessment%n" +
                            "  • Verification rate is %s.%n" +
                            "  • Admin-to-user ratio is %s.%n%n" +
                            "Recommendations%n" +
                            "  1. %s%n" +
                            "  2. Review pending role-upgrade requests regularly.%n" +
                            "  3. Monitor anomalous login patterns weekly.%n",
                    total, admins, admRate, verified, verRate,
                    (total - verified), students,
                    healthScore, healthScore >= 80 ? "EXCELLENT" : healthScore >= 60 ? "GOOD" : healthScore >= 40 ? "FAIR" : "POOR",
                    riskScore,   riskScore  <= 25  ? "LOW"       : riskScore  <= 50  ? "MODERATE" : riskScore <= 75 ? "HIGH" : "CRITICAL",
                    (verRate >= 70) ? "healthy (≥ 70%)" : "below target (< 70%)",
                    (admRate < 5)  ? "healthy (< 5%)"  : "elevated – review admin accounts",
                    (verRate < 70)
                            ? "Run a verification-reminder email campaign."
                            : "Continue current onboarding flow."
            );

            Platform.runLater(() -> areaAiReport.setText(report));
        }, "ai-report-thread").start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Navigation
    // ═════════════════════════════════════════════════════════════════════════
    @FXML private void showDashboard() {}
    @FXML private void showUsers()     { usersTable.requestFocus(); }

    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) adminNameLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Profile page coming soon!");
        }
    }

    @FXML
    private void logout() {
        UserSession.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) adminNameLabel.getScene().getWindow();
            stage.setTitle("MindForge - Login");
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════
    private String formatRoles(String roles) {
        if (roles == null) return "User";
        return roles.replace("[", "").replace("]", "").replace("\"", "").replace("ROLE_", "");
    }

    private String extractMainRole(String roles) {
        if (roles == null) return "ROLE_USER";
        if (roles.contains("ROLE_ADMIN"))        return "ROLE_ADMIN";
        if (roles.contains("ROLE_STUDENT_PLUS")) return "ROLE_STUDENT_PLUS";
        if (roles.contains("ROLE_STUDENT"))      return "ROLE_STUDENT";
        return "ROLE_USER";
    }

    private String formatDate(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String nullSafe(String value)         { return value == null ? "" : value; }
    private String nullSafeNumber(Integer value)  { return value == null ? "" : String.valueOf(value); }

    private String normalizeFilterValue(ComboBox<String> box, String allLabel) {
        if (box == null) return null;
        String v = box.getValue();
        return (v == null || v.equals(allLabel)) ? null : v.trim();
    }

    private String formatAiInsightType(String type) {
        if (type == null || type.isBlank()) return "";
        return switch (type) {
            case "recommended_duration" -> "Recommended Duration";
            case "focus_tips"           -> "Focus Tips";
            case "daily_plan"           -> "Daily Plan";
            case "weekly_review"        -> "Weekly Review";
            default -> type;
        };
    }

    private String formatAiInsightSource(String source) {
        return (source == null || source.isBlank()) ? "" : source.toUpperCase();
    }

    private String formatAiInsightFeedback(AiInsight insight) {
        if (insight == null) return "";
        int helpful   = insight.helpfulVotes()   == null ? 0 : insight.helpfulVotes();
        int unhelpful = insight.unhelpfulVotes()  == null ? 0 : insight.unhelpfulVotes();
        return helpful + " / " + unhelpful;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        String t = value.trim();
        return t.length() <= maxLen ? t : t.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/mindforge_db", "root", "");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}