package com.mindforge.controller;

import com.mindforge.model.UserModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class MenuController implements Initializable {

    @FXML private TableView<UserModel>             usersTable;
    @FXML private TableColumn<UserModel, Integer>  idColumn;
    @FXML private TableColumn<UserModel, String>   emailColumn;
    @FXML private TableColumn<UserModel, String>   verifiedColumn;
    @FXML private TableColumn<UserModel, String>   createdColumn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idColumn.setCellValueFactory(
                cellData -> cellData.getValue().idProperty().asObject()
        );
        emailColumn.setCellValueFactory(
                cellData -> cellData.getValue().emailProperty()
        );
        verifiedColumn.setCellValueFactory(
                cellData -> cellData.getValue().verifiedProperty()
        );
        createdColumn.setCellValueFactory(
                cellData -> cellData.getValue().createdAtProperty()
        );

        loadUsers();
    }

    private void loadUsers() {
        ObservableList<UserModel> users = FXCollections.observableArrayList();
        String sql = "SELECT id, email, is_verified, created_at FROM user";

        try {
            Connection conn  = getConnection();
            Statement stmt   = conn.createStatement();
            ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {
                int    id        = result.getInt("id");
                String email     = result.getString("email");
                String verified  = result.getInt("is_verified") == 1 ? "✅ Yes" : "❌ No";
                String createdAt = result.getString("created_at");
                users.add(new UserModel(id, email, verified, createdAt));
            }

            conn.close();
            System.out.println("Total users loaded: " + users.size());

        } catch (Exception e) {
            System.out.println("❌ DB Error: " + e.getMessage());
            e.printStackTrace();
        }

        usersTable.setItems(users);
    }

    @FXML
    private void handleLogout() {
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
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setTitle("MindForge - Login");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mindforge_db", "root", ""
        );
    }
}
