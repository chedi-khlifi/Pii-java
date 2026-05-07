package com.mindforge.controllers;

import com.mindforge.HelloApplication;
import com.mindforge.utils.DBConnection;
import com.mindforge.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginController {

    @FXML private ComboBox<String> userIdBox;
    @FXML private Label userLoadError;

    // Maps display label → real DB user id
    private final Map<String, Integer> userMap = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        loadUsers();
    }

    private void loadUsers() {
        userMap.clear();
        userIdBox.getItems().clear();
        try {
            Connection cnx = DBConnection.getConnection();
            try (Statement st = cnx.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, email FROM user ORDER BY id")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String email = rs.getString("email");
                    String label = "#" + id + "  —  " + email;
                    userMap.put(label, id);
                    userIdBox.getItems().add(label);
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not load users from DB: " + e.getMessage());
            if (userLoadError != null) {
                userLoadError.setText("Could not load users: " + e.getMessage());
                userLoadError.setVisible(true);
                userLoadError.setManaged(true);
            }
        }
        if (!userIdBox.getItems().isEmpty()) {
            userIdBox.setValue(userIdBox.getItems().get(0));
        }
    }

    private int selectedUserId() {
        String sel = userIdBox.getValue();
        return sel != null ? userMap.getOrDefault(sel, -1) : -1;
    }

    @FXML
    private void selectStudent() {
        int userId = selectedUserId();
        if (userId == -1) return;
        SessionManager.setSession(SessionManager.Role.STUDENT, userId, userIdBox.getValue());
        navigateTo("student-dashboard.fxml");
    }

    @FXML
    private void selectCompanyOwner() {
        int userId = selectedUserId();
        if (userId == -1) return;
        SessionManager.setSession(SessionManager.Role.COMPANY_OWNER, userId, userIdBox.getValue());
        navigateTo("company-dashboard.fxml");
    }

    private void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlFile));
            Stage stage = (Stage) userIdBox.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
