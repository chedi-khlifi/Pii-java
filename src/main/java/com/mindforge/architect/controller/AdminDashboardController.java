package com.mindforge.controller;

import com.mindforge.model.UserModel;
import com.mindforge.util.UserSession;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AdminDashboardController {

    @FXML private Label adminNameLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label verifiedCountLabel;
    @FXML private Label newTodayLabel;
    @FXML private Label resultCountLabel;

    @FXML private TextField searchField;
    @FXML private TextField filterEmail;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> verifiedFilter;

    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, String> colId;
    @FXML private TableColumn<UserRow, String> colEmail;
    @FXML private TableColumn<UserRow, String> colRoles;
    @FXML private TableColumn<UserRow, String> colVerified;
    @FXML private TableColumn<UserRow, String> colCreated;
    @FXML private TableColumn<UserRow, Void> colActions;

    private ObservableList<UserRow> usersList = FXCollections.observableArrayList();
    private FilteredList<UserRow> filteredUsers;

    public static class UserRow {
        int id;
        String email;
        String roles;
        boolean verified;
        String createdAt;

        UserRow(int id, String email, String roles, boolean verified, String createdAt) {
            this.id = id;
            this.email = email;
            this.roles = roles;
            this.verified = verified;
            this.createdAt = createdAt;
        }
    }

    @FXML
    public void initialize() {
        // Check if admin
        if (!UserSession.getInstance().isAdmin()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "You don't have admin privileges!");
            logout();
            return;
        }

        adminNameLabel.setText(UserSession.getInstance().getEmail());

        // Setup filters
        roleFilter.setItems(FXCollections.observableArrayList("All Roles", "Admin", "Student+", "Student", "User"));
        verifiedFilter.setItems(FXCollections.observableArrayList("All Status", "Verified", "Unverified"));

        // Setup table
        setupTable();

        // Load data
        loadUsers();
        updateStats();

        // Setup search
        setupSearch();
    }

    private void setupTable() {
        colId.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().id)));
        colEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().email));
        colRoles.setCellValueFactory(cell -> new SimpleStringProperty(formatRoles(cell.getValue().roles)));
        colVerified.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().verified ? "Yes" : "No"));
        colCreated.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().createdAt));

        // Actions column with buttons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("👁");
            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");

            {
                viewBtn.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 10;");
                editBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 10;");
                deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 10;");

                viewBtn.setOnAction(e -> viewUser(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> editUser(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(6, viewBtn, editBtn, deleteBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    private void setupSearch() {
        filteredUsers = new FilteredList<>(usersList, p -> true);

        filterEmail.textProperty().addListener((obs, old, newVal) -> applyFilters());
        roleFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        verifiedFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());

        usersTable.setItems(filteredUsers);
    }

    private void applyFilters() {
        String emailFilter = filterEmail.getText().toLowerCase();
        String roleSel = roleFilter.getValue();
        String verifiedSel = verifiedFilter.getValue();

        filteredUsers.setPredicate(user -> {
            boolean matchesEmail = emailFilter.isEmpty() ||
                    user.email.toLowerCase().contains(emailFilter) ||
                    String.valueOf(user.id).contains(emailFilter);

            boolean matchesRole = roleSel == null || roleSel.equals("All Roles") ||
                    (roleSel.equals("Admin") && user.roles.contains("ROLE_ADMIN")) ||
                    (roleSel.equals("Student+") && user.roles.contains("ROLE_STUDENT_PLUS")) ||
                    (roleSel.equals("Student") && user.roles.contains("ROLE_STUDENT")) ||
                    (roleSel.equals("User") && user.roles.contains("ROLE_USER"));

            boolean matchesVerified = verifiedSel == null || verifiedSel.equals("All Status") ||
                    (verifiedSel.equals("Verified") && user.verified) ||
                    (verifiedSel.equals("Unverified") && !user.verified);

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
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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
            // Total users
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM user");
            if (rs.next()) totalUsersLabel.setText(String.valueOf(rs.getInt(1)));

            // Admin count
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM user WHERE roles LIKE '%ROLE_ADMIN%'");
            if (rs.next()) adminCountLabel.setText(String.valueOf(rs.getInt(1)));

            // Verified count
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM user WHERE is_verified = 1");
            if (rs.next()) verifiedCountLabel.setText(String.valueOf(rs.getInt(1)));

            // New today
            rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM user WHERE DATE(created_at) = CURDATE()");
            if (rs.next()) newTodayLabel.setText(String.valueOf(rs.getInt(1)));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewUser(UserRow user) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Details");
        alert.setHeaderText("User #" + user.id);
        alert.setContentText("Email: " + user.email + "\n" +
                "Roles: " + formatRoles(user.roles) + "\n" +
                "Verified: " + (user.verified ? "Yes" : "No") + "\n" +
                "Created: " + user.createdAt);
        alert.showAndWait();
    }

    private void editUser(UserRow user) {
        Dialog<UserRow> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit User #" + user.id);

        // Form fields
        TextField emailField = new TextField(user.email);
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList(
                "ROLE_USER", "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_STUDENT_PLUS"
        ));
        roleBox.setValue(extractMainRole(user.roles));
        CheckBox verifiedBox = new CheckBox("Verified");
        verifiedBox.setSelected(user.verified);

        VBox content = new VBox(10,
                new Label("Email:"), emailField,
                new Label("Role:"), roleBox,
                verifiedBox
        );
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                user.email = emailField.getText();
                user.roles = "[\"" + roleBox.getValue() + "\"]";
                user.verified = verifiedBox.isSelected();
                return user;
            }
            return null;
        });

        Optional<UserRow> result = dialog.showAndWait();
        result.ifPresent(updated -> {
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

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteUserFromDB(user.id)) {
                usersList.remove(user);
                updateStats();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully!");
            }
        }
    }

    @FXML
    private void showAddUserDialog() {
        Dialog<UserRow> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create New User");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");

        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList(
                "ROLE_USER", "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_STUDENT_PLUS"
        ));
        roleBox.setValue("ROLE_USER");

        CheckBox verifiedBox = new CheckBox("Verified");
        verifiedBox.setSelected(true);

        VBox content = new VBox(10,
                new Label("Email:"), emailField,
                new Label("Password:"), passField,
                new Label("Role:"), roleBox,
                verifiedBox
        );
        content.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new UserRow(0, emailField.getText(),
                        "[\"" + roleBox.getValue() + "\"]",
                        verifiedBox.isSelected(), "");
            }
            return null;
        });

        Optional<UserRow> result = dialog.showAndWait();
        result.ifPresent(newUser -> {
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
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.email);
            pstmt.setString(2, user.roles);
            pstmt.setBoolean(3, user.verified);
            pstmt.setInt(4, user.id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteUserFromDB(int id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM user WHERE id = ?")) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean createUserInDB(UserRow user, String password) {
        String sql = "INSERT INTO user (email, password, roles, is_verified, created_at) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.email);
            pstmt.setString(2, org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt()));
            pstmt.setString(3, user.roles);
            pstmt.setBoolean(4, user.verified);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void showDashboard() {
        // Already on dashboard
    }

    @FXML
    private void showUsers() {
        usersTable.requestFocus();
    }

    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) adminNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
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
            stage.setScene(new Scene(root, 500, 400));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatRoles(String roles) {
        if (roles == null) return "User";
        return roles.replace("[", "").replace("]", "").replace("\"", "").replace("ROLE_", "");
    }

    private String extractMainRole(String roles) {
        if (roles == null) return "ROLE_USER";
        if (roles.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (roles.contains("ROLE_STUDENT_PLUS")) return "ROLE_STUDENT_PLUS";
        if (roles.contains("ROLE_STUDENT")) return "ROLE_STUDENT";
        return "ROLE_USER";
    }

    private String formatDate(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mindforge_db", "root", ""
        );
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}