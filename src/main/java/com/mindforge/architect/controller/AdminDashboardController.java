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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Entity.Guardian.AiInsight;
import tn.esprit.Entity.Guardian.Resource;
import tn.esprit.Entity.Guardian.VirtualRoom;
import tn.esprit.services.guardian.AiInsightService;
import tn.esprit.services.guardian.ResourceService;
import tn.esprit.services.guardian.VirtualRoomService;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AdminDashboardController {

    // ── Stats & header ────────────────────────────────────────────────────────
    @FXML private Label adminNameLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label verifiedCountLabel;
    @FXML private Label newTodayLabel;
    @FXML private Label resultCountLabel;

    // ── AI widgets ────────────────────────────────────────────────────────────
    @FXML private Label    labelRiskScore;
    @FXML private Label    labelRiskLevel;
    @FXML private Label    labelHealthScore;
    @FXML private Label    labelHealthLabel;
    @FXML private Label    labelAiSummary;
    @FXML private VBox     vboxAlerts;
    @FXML private Label    labelAnomalyStatus;
    @FXML private Label    labelAnomalyCount;
    @FXML private VBox     vboxAnomalies;
    @FXML private VBox     vboxGrowthTrend;
    @FXML private VBox     vboxCohorts;
    @FXML private VBox     vboxRecommendations;
    @FXML private TextArea areaAiReport;

    // ── Smart Search ──────────────────────────────────────────────────────────
    @FXML private TextField smartSearchField;
    @FXML private VBox      vboxSmartSearchResults;

    // ── Tab Pane ──────────────────────────────────────────────────────────────
    @FXML private TabPane mainTabPane;

    // ── Filters ───────────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private TextField filterEmail;
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
    @FXML private TableView<RoleRequest>              requestsTable;
    @FXML private TableColumn<RoleRequest, Integer>   colReqId;
    @FXML private TableColumn<RoleRequest, String>    colReqEmail;
    @FXML private TableColumn<RoleRequest, String>    colReqMotiv;
    @FXML private TableColumn<RoleRequest, String>    colReqDate;
    @FXML private TableColumn<RoleRequest, String>    colReqStatus;
    @FXML private TableColumn<RoleRequest, Void>      colReqActions;
    @FXML private Label                               pendingCountLabel;

    // ── Guardian AI Insights tab ───────────────────────────────────────────
    @FXML private ComboBox<String> aiInsightTypeFilter;
    @FXML private ComboBox<String> aiInsightSourceFilter;
    @FXML private TableView<AiInsight> guardianAiInsightsTable;
    @FXML private TableColumn<AiInsight, String> colAiInsightDate;
    @FXML private TableColumn<AiInsight, String> colAiInsightUser;
    @FXML private TableColumn<AiInsight, String> colAiInsightTask;
    @FXML private TableColumn<AiInsight, String> colAiInsightType;
    @FXML private TableColumn<AiInsight, String> colAiInsightSource;
    @FXML private TableColumn<AiInsight, String> colAiInsightHelpful;
    @FXML private TableColumn<AiInsight, String> colAiInsightPayload;

    // ── Guardian Resources tab ─────────────────────────────────────────────
    @FXML private TableView<Resource> guardianResourcesTable;
    @FXML private TableColumn<Resource, String> colResId;
    @FXML private TableColumn<Resource, String> colResTitle;
    @FXML private TableColumn<Resource, String> colResType;
    @FXML private TableColumn<Resource, String> colResUploader;
    @FXML private TableColumn<Resource, String> colResDownloads;
    @FXML private TableColumn<Resource, String> colResRating;
    @FXML private TableColumn<Resource, String> colResCreated;
    @FXML private TableColumn<Resource, Void>   colResActions;

    // ── Guardian Rooms tab ────────────────────────────────────────────────
    @FXML private TableView<VirtualRoom> guardianRoomsTable;
    @FXML private TableColumn<VirtualRoom, String> colRoomId;
    @FXML private TableColumn<VirtualRoom, String> colRoomName;
    @FXML private TableColumn<VirtualRoom, String> colRoomCreator;
    @FXML private TableColumn<VirtualRoom, String> colRoomSubject;
    @FXML private TableColumn<VirtualRoom, String> colRoomMax;
    @FXML private TableColumn<VirtualRoom, String> colRoomActive;
    @FXML private TableColumn<VirtualRoom, String> colRoomCreated;
    @FXML private TableColumn<VirtualRoom, Void>   colRoomActions;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final ObservableList<UserRow>    usersList    = FXCollections.observableArrayList();
    private final ObservableList<RoleRequest> requestList = FXCollections.observableArrayList();
    private FilteredList<UserRow> filteredUsers;
    private final ObservableList<AiInsight>  guardianAiInsightsList = FXCollections.observableArrayList();
    private final ObservableList<Resource>   guardianResourcesList = FXCollections.observableArrayList();
    private final ObservableList<VirtualRoom> guardianRoomsList    = FXCollections.observableArrayList();

    private final AiInsightService aiInsightService = new AiInsightService();
    private final ResourceService resourceService = new ResourceService();
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
        updateStats();
        loadGuardianAiInsights();
        loadGuardianResources();
        loadGuardianRooms();
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
        String emailFilter  = filterEmail.getText().toLowerCase();
        String roleSel      = roleFilter.getValue();
        String verifiedSel  = verifiedFilter.getValue();

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

    private void updateStats() {
        try (Connection conn = getConnection()) {
            ResultSet rs;

            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM user");
            if (rs.next()) totalUsersLabel.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM user WHERE roles LIKE '%ROLE_ADMIN%'");
            if (rs.next()) adminCountLabel.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM user WHERE is_verified = 1");
            if (rs.next()) verifiedCountLabel.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM user WHERE DATE(created_at) = CURDATE()");
            if (rs.next()) newTodayLabel.setText(String.valueOf(rs.getInt(1)));

        } catch (SQLException e) {
            e.printStackTrace();
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
                    updateStats();
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
                updateStats();
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

        // Colour-coded status cell
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

        // Approve / Reject action buttons
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
            Platform.runLater(() ->
                    pendingCountLabel.setText(String.valueOf(finalPending)));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleRequestAction(RoleRequest req, String newStatus) {
        String msg = "approved".equals(newStatus)
                ? "Approve role request for " + req.getUserEmail() + "?"
                : "Reject role request for "  + req.getUserEmail() + "?";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;

            try (Connection conn = getConnection()) {
                // 1. Update request status
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE role_request SET status = ? WHERE id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, req.getId());
                    ps.executeUpdate();
                }

                // 2. If approved, promote user role
                if ("approved".equals(newStatus)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE user SET roles = ? WHERE id = ?")) {
                        ps.setString(1, "[\"ROLE_STUDENT_PLUS\"]");
                        ps.setInt(2, req.getUserId());
                        ps.executeUpdate();
                    }
                }

                loadRequests();  // refresh requests table + pending badge
                loadUsers();     // reflect role change in users table
                updateStats();   // refresh stat cards

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
        if (guardianAiInsightsTable == null) {
            return;
        }

        if (aiInsightTypeFilter != null) {
            aiInsightTypeFilter.setItems(FXCollections.observableArrayList(
                    "All Types",
                    "recommended_duration",
                    "focus_tips",
                    "daily_plan",
                    "weekly_review"
            ));
            aiInsightTypeFilter.getSelectionModel().selectFirst();
            aiInsightTypeFilter.valueProperty().addListener((obs, o, n) -> loadGuardianAiInsights());
        }

        if (aiInsightSourceFilter != null) {
            aiInsightSourceFilter.setItems(FXCollections.observableArrayList(
                    "All Sources",
                    "ai",
                    "rule"
            ));
            aiInsightSourceFilter.getSelectionModel().selectFirst();
            aiInsightSourceFilter.valueProperty().addListener((obs, o, n) -> loadGuardianAiInsights());
        }

        colAiInsightDate.setCellValueFactory(c -> new SimpleStringProperty(formatDateTime(c.getValue().createdAt())));
        colAiInsightUser.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().userId())));
        colAiInsightTask.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().taskId())));
        colAiInsightType.setCellValueFactory(c -> new SimpleStringProperty(formatAiInsightType(c.getValue().type())));
        colAiInsightSource.setCellValueFactory(c -> new SimpleStringProperty(formatAiInsightSource(c.getValue().source())));
        colAiInsightHelpful.setCellValueFactory(c -> new SimpleStringProperty(formatAiInsightFeedback(c.getValue())));
        colAiInsightPayload.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().payload())));

        colAiInsightPayload.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                AiInsight insight = getTableView().getItems().get(getIndex());
                String payload = insight == null ? "" : nullSafe(insight.payload());
                String preview = truncate(payload, 160);
                setText(preview);
                if (!payload.isEmpty()) {
                    setTooltip(new Tooltip(payload));
                } else {
                    setTooltip(null);
                }
            }
        });

        guardianAiInsightsTable.setItems(guardianAiInsightsList);
    }

    @FXML
    private void refreshGuardianAiInsights() {
        loadGuardianAiInsights();
    }

    private void loadGuardianAiInsights() {
        if (guardianAiInsightsTable == null) {
            return;
        }
        guardianAiInsightsList.clear();

        String typeFilter = normalizeFilterValue(aiInsightTypeFilter, "All Types");
        String sourceFilter = normalizeFilterValue(aiInsightSourceFilter, "All Sources");

        try {
            if (typeFilter == null && sourceFilter == null) {
                guardianAiInsightsList.setAll(aiInsightService.findAll());
            } else {
                guardianAiInsightsList.setAll(aiInsightService.findFiltered(typeFilter, sourceFilter));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load AI insights: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Guardian Resources tab
    // ═════════════════════════════════════════════════════════════════════════
    private void setupGuardianResourcesTable() {
        if (guardianResourcesTable == null) {
            return;
        }

        colResId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        colResTitle.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().title())));
        colResType.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().type())));
        colResUploader.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().uploaderId())));
        colResDownloads.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().downloadCount())));
        colResRating.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().rating())));
        colResCreated.setCellValueFactory(c -> new SimpleStringProperty(formatDateTime(c.getValue().createdAt())));

        colResActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                editBtn.setStyle(
                        "-fx-background-color: #0ea5e9; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                                "-fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: 600;");
                deleteBtn.setStyle(
                        "-fx-background-color: #dc2626; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                                "-fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: 600;");

                editBtn.setOnAction(e -> editResource(getTableView().getItems().get(getIndex())));
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

    @FXML
    private void refreshGuardianResources() {
        loadGuardianResources();
    }

    private void loadGuardianResources() {
        if (guardianResourcesTable == null) {
            return;
        }
        guardianResourcesList.clear();
        try {
            guardianResourcesList.setAll(resourceService.findAll());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load resources: " + e.getMessage());
        }
    }

    private void editResource(Resource resource) {
        if (resource == null) {
            return;
        }

        Dialog<Resource> dialog = new Dialog<>();
        dialog.setTitle("Edit Resource");
        dialog.setHeaderText("Resource #" + resource.id());

        TextField titleField = new TextField(nullSafe(resource.title()));
        TextField descriptionField = new TextField(nullSafe(resource.description()));
        TextField filePathField = new TextField(nullSafe(resource.filePath()));
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(
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
                new Label("Rating (0-5):"), ratingSpinner
        );
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new Resource(
                        resource.id(),
                        titleField.getText().trim(),
                        descriptionField.getText().trim(),
                        filePathField.getText().trim(),
                        typeBox.getValue(),
                        resource.downloadCount(),
                        ratingSpinner.getValue(),
                        resource.createdAt(),
                        LocalDateTime.now(),
                        resource.subjectId(),
                        resource.uploaderId()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (updated.title() == null || updated.title().isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Title is required.");
                return;
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
        if (resource == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Resource #" + resource.id());
        confirm.setContentText("Are you sure you want to delete '" + nullSafe(resource.title()) + "'?");

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
        if (guardianRoomsTable == null) {
            return;
        }

        colRoomId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        colRoomName.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().name())));
        colRoomCreator.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().creatorId())));
        colRoomSubject.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().subjectId())));
        colRoomMax.setCellValueFactory(c -> new SimpleStringProperty(nullSafeNumber(c.getValue().maxParticipants())));
        colRoomActive.setCellValueFactory(c -> new SimpleStringProperty(Boolean.TRUE.equals(c.getValue().isActive()) ? "Yes" : "No"));
        colRoomCreated.setCellValueFactory(c -> new SimpleStringProperty(formatDateTime(c.getValue().createdAt())));

        colRoomActions.setCellFactory(col -> new TableCell<>() {
            private final Button toggleBtn = new Button();
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(6, toggleBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                deleteBtn.setStyle(
                        "-fx-background-color: #dc2626; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                                "-fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: 600;");

                toggleBtn.setOnAction(e -> toggleRoomActive(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteRoom(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                VirtualRoom room = getTableView().getItems().get(getIndex());
                boolean active = room != null && Boolean.TRUE.equals(room.isActive());
                toggleBtn.setText(active ? "Close" : "Open");
                toggleBtn.setStyle(
                        active
                                ? "-fx-background-color: #f59e0b; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                                "-fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: 600;"
                                : "-fx-background-color: #0ea5e9; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                                "-fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: 600;"
                );
                setGraphic(box);
            }
        });

        guardianRoomsTable.setItems(guardianRoomsList);
    }

    @FXML
    private void refreshGuardianRooms() {
        loadGuardianRooms();
    }

    private void loadGuardianRooms() {
        if (guardianRoomsTable == null) {
            return;
        }
        guardianRoomsList.clear();
        try {
            guardianRoomsList.setAll(virtualRoomService.findAll());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load rooms: " + e.getMessage());
        }
    }

    private void toggleRoomActive(VirtualRoom room) {
        if (room == null || room.id() == null) {
            return;
        }
        boolean nextState = !Boolean.TRUE.equals(room.isActive());
        VirtualRoom updated = new VirtualRoom(
                room.id(),
                room.name(),
                room.description(),
                nextState,
                room.maxParticipants(),
                room.createdAt(),
                room.creatorId(),
                room.subjectId()
        );

        try {
            virtualRoomService.update(updated);
            loadGuardianRooms();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update room: " + e.getMessage());
        }
    }

    private void deleteRoom(VirtualRoom room) {
        if (room == null || room.id() == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Room #" + room.id());
        confirm.setContentText("Are you sure you want to delete '" + nullSafe(room.name()) + "'?");

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
            try {
                // Gather basic stats from the already-loaded data
                int total    = usersList.size();
                long admins  = usersList.stream().filter(u -> u.roles.contains("ROLE_ADMIN")).count();
                long verified = usersList.stream().filter(u -> u.verified).count();
                long students = usersList.stream()
                        .filter(u -> u.roles.contains("ROLE_STUDENT")).count();

                String report = String.format(
                        "📊 MindForge Weekly Admin Report%n" +
                        "════════════════════════════════%n%n" +
                        "Platform Overview%n" +
                        "  • Total users   : %d%n" +
                        "  • Admins        : %d%n" +
                        "  • Verified      : %d (%.1f%%)%n" +
                        "  • Students      : %d%n%n" +
                        "Health Assessment%n" +
                        "  • Verification rate is %s.%n" +
                        "  • Admin-to-user ratio is %s.%n%n" +
                        "Recommendations%n" +
                        "  1. %s%n" +
                        "  2. Review pending role-upgrade requests regularly.%n" +
                        "  3. Monitor anomalous login patterns weekly.%n",
                        total, admins, verified,
                        total > 0 ? (verified * 100.0 / total) : 0.0,
                        students,
                        (total > 0 && verified * 100.0 / total >= 70)
                                ? "healthy (≥ 70%%)" : "below target (< 70%%)",
                        (total > 0 && admins * 100.0 / total < 5)
                                ? "healthy (< 5%%)" : "elevated – review admin accounts",
                        (total > 0 && verified * 100.0 / total < 70)
                                ? "Run a verification-reminder email campaign."
                                : "Continue current onboarding flow."
                );

                javafx.application.Platform.runLater(() -> areaAiReport.setText(report));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                        areaAiReport.setText("Error generating report: " + ex.getMessage()));
            }
        }, "ai-report-thread").start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Navigation
    // ═════════════════════════════════════════════════════════════════════════
    @FXML private void showDashboard() { /* already here */ }

    @FXML
    private void showUsers() {
        usersTable.requestFocus();
    }

    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/profile.fxml"));
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/login.fxml"));
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
        return roles.replace("[", "").replace("]", "")
                .replace("\"", "").replace("ROLE_", "");
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
        return ts.toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String nullSafeNumber(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeFilterValue(ComboBox<String> comboBox, String allLabel) {
        if (comboBox == null) {
            return null;
        }
        String value = comboBox.getValue();
        if (value == null || value.equals(allLabel)) {
            return null;
        }
        return value.trim();
    }

    private String formatAiInsightType(String type) {
        if (type == null || type.isBlank()) return "";
        return switch (type) {
            case "recommended_duration" -> "Recommended Duration";
            case "focus_tips" -> "Focus Tips";
            case "daily_plan" -> "Daily Plan";
            case "weekly_review" -> "Weekly Review";
            default -> type;
        };
    }

    private String formatAiInsightSource(String source) {
        if (source == null || source.isBlank()) return "";
        return source.toUpperCase();
    }

    private String formatAiInsightFeedback(AiInsight insight) {
        if (insight == null) return "";
        int helpful = insight.helpfulVotes() == null ? 0 : insight.helpfulVotes();
        int unhelpful = insight.unhelpfulVotes() == null ? 0 : insight.unhelpfulVotes();
        return helpful + " / " + unhelpful;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) return trimmed;
        return trimmed.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mindforge_db", "root", "");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}