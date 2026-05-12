package com.example.controllers;

import com.example.entity.User;
import com.example.service.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML private Label  sessionLabel;
    @FXML private Button communityButton;
    @FXML private Button chatMessageButton;
    @FXML private Button claimButton;
    @FXML private Button sharedTaskButton;
    @FXML private Button exitButton;

    private Stage primaryStage;

    public void setPrimaryStage(Stage stage) { this.primaryStage = stage; }
    public Stage getPrimaryStage()           { return primaryStage; }

    @FXML
    public void initialize() {
        // Show current user in header
        User u = UserSession.getInstance().getCurrentUser();
        if (u != null && sessionLabel != null) {
            sessionLabel.setText("Connecté en tant que : " + u.getUsername()
                    + (u.isAdmin() ? "  [Admin]" : "  [Utilisateur]"));
        }
        applyButtonStyles();
    }

    private void applyButtonStyles() {
        String base = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 8;"
                    + "-fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;";
        String hover = base.replace("#2196F3", "#1976D2");

        for (Button b : new Button[]{chatMessageButton, claimButton, sharedTaskButton}) {
            b.setStyle(base);
            b.setOnMouseEntered(e -> b.setStyle(hover));
            b.setOnMouseExited(e  -> b.setStyle(base));
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    @FXML public void handleCommunityButton()  { loadCommunityHub(); }
    @FXML public void handleExitButton()       { primaryStage.close(); }

    @FXML
    public void handleChatMessageButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RoomsView.fxml"));
            Parent root = loader.load();
            RoomsController c = loader.getController();
            c.setMainController(this);
            show(root, "Salles Virtuelles & Chat");
        } catch (IOException e) { showError(e); }
    }

    @FXML
    public void handleClaimButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ClaimView.fxml"));
            Parent root = loader.load();
            ClaimController c = loader.getController();
            c.setMainController(this);
            c.setMode(UserSession.getInstance().isAdmin()
                    ? ClaimController.Mode.ADMIN
                    : ClaimController.Mode.CREATE);
            show(root, "Tickets de Support");
        } catch (IOException e) { showError(e); }
    }

    @FXML
    public void handleSharedTaskButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SharedTaskView.fxml"));
            Parent root = loader.load();
            SharedTaskController c = loader.getController();
            c.setMainController(this);
            c.setMode(SharedTaskController.Mode.SEND);
            show(root, "Défis Partagés");
        } catch (IOException e) { showError(e); }
    }

    // ── Reusable show helpers ──────────────────────────────────────────────

    private void show(Parent root, String title) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            } catch (Exception ignored) {}
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        primaryStage.setTitle(title + " — MindForge");
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
    }

    public void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            Parent root = loader.load();
            MainController ctrl = loader.getController();
            ctrl.setPrimaryStage(primaryStage);
            show(root, "MindForge");
        } catch (IOException e) { showError(e); }
    }

    public void showCommunityHub() { loadCommunityHub(); }

    private void loadCommunityHub() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CommunityHubView.fxml"));
            Parent root = loader.load();
            CommunityHubController ctrl = loader.getController();
            ctrl.setMainController(this);
            show(root, "Community Hub");
        } catch (IOException e) { showError(e); }
    }

    private void showError(Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Navigation Error");
        a.setHeaderText("Could not load view");
        a.setContentText(e.getMessage());
        a.showAndWait();
    }
}
