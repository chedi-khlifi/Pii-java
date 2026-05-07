package com.mindforge.architect.controller;

import com.mindforge.util.UserSession;
import com.mindforge.utils.SessionManager;
import com.mindforge.controllers.carriere.StudentController;
import example.PlannerModule;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML private VBox friendsContainer;
    @FXML private VBox centerContent;   // ← new: inject the center VBox

    // Saves the original root so we can restore it later
    private Parent originalRoot;

    @FXML
    public void initialize() {
        loadFriends();
    }

    // ----------------------------------------------------------------
    // PLANNER — embed App's scene root into centerContent
    // ----------------------------------------------------------------
    @FXML
    private void goToPlanner() {
        try {
            Scene scene = centerContent.getScene();
            if (originalRoot == null) {
                originalRoot = scene.getRoot();
            }

            PlannerModule plannerModule = new PlannerModule();
            Parent plannerRoot = plannerModule.getView();

            // Wrap with a "← Back" button
            Button backBtn = new Button("← Back to Dashboard");
            backBtn.setStyle(
                    "-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                            "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
            backBtn.setOnAction(e -> {
                if (originalRoot != null) {
                    scene.setRoot(originalRoot);
                }
            });

            HBox topBar = new HBox(backBtn);
            topBar.setPadding(new Insets(10, 15, 10, 15));
            topBar.setAlignment(Pos.CENTER_LEFT);
            topBar.setStyle("-fx-background-color: #f0f2f5;"); // match planner bg

            // Make planner fill the available height
            VBox.setVgrow(plannerRoot, Priority.ALWAYS);

            // Create a full page wrapper
            VBox fullPage = new VBox(topBar, plannerRoot);
            fullPage.setStyle("-fx-background-color: #f0f2f5;");

            // Swap the scene root
            scene.setRoot(fullPage);

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Planner");
            alert.setHeaderText(null);
            alert.setContentText("Could not load planner: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void restoreOriginalContent() {
        if (originalRoot != null && centerContent != null && centerContent.getScene() != null) {
            centerContent.getScene().setRoot(originalRoot);
        }
    }

    // ----------------------------------------------------------------
    // FRIENDS
    // ----------------------------------------------------------------
    private void loadFriends() {
        friendsContainer.getChildren().clear();

        addFriendCard("Alice Johnson", 12, 3450, true,  5,  true);
        addFriendCard("Bob Smith",     8,  2100, false, 12, false);
        addFriendCard("Charlie Brown", 15, 5200, true,  2,  false);

        if (friendsContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("No friends yet. Add friends from your profile.");
            emptyLabel.setStyle("-fx-text-fill: #6d748a; -fx-font-size: 13px;");
            friendsContainer.getChildren().add(emptyLabel);
        }
    }

    private void addFriendCard(String name, int level, int xp,
                               boolean online, int leaderboardPos, boolean focusMode) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #fbfcff; -fx-border-color: #ebedf7;" +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12;");
        card.setMinWidth(400);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #21273b;");
        Label levelLabel = new Label("Level " + level + " · XP " + xp);
        levelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6d748a;");
        nameBox.getChildren().addAll(nameLabel, levelLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(online ? "Online" : "Offline");
        statusLabel.setStyle(
                "-fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: 600;" +
                        "-fx-background-color: " + (online ? "rgba(34,197,94,0.2)" : "rgba(148,163,184,0.2)") + ";" +
                        "-fx-text-fill: " + (online ? "#166534" : "#475569") + ";"
        );

        topRow.getChildren().addAll(nameBox, spacer, statusLabel);

        Label infoLabel = new Label(
                "Leaderboard: #" + leaderboardPos + " · " + (focusMode ? "In focus mode" : "Available")
        );
        infoLabel.setStyle("-fx-text-fill: #6d748a; -fx-font-size: 12px;");

        Button chatBtn = new Button("Chat from workspace");
        chatBtn.setStyle("-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
        chatBtn.setOnAction(e -> openChat(name));

        card.getChildren().addAll(topRow, infoLabel, chatBtn);
        friendsContainer.getChildren().add(card);
    }

    // ----------------------------------------------------------------
    // OTHER ACTIONS
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    // CAREERS
    // ----------------------------------------------------------------
    @FXML
    private void goToCareers() {
        try {
            Scene scene = centerContent.getScene();
            if (originalRoot == null) {
                originalRoot = scene.getRoot();
            }

            UserSession us = UserSession.getInstance();
            SessionManager.Role role = us.isCompany()
                    ? SessionManager.Role.COMPANY_OWNER
                    : SessionManager.Role.STUDENT;
            SessionManager.setSession(role, us.getUserId(), us.getEmail());

            String fxml = us.isCompany()
                    ? "/com/mindforge/company-dashboard.fxml"
                    : "/com/mindforge/student-dashboard.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent careerRoot = loader.load();

            // Hide the inner career header (redundant with dashboard chrome)
            javafx.scene.Node header = careerRoot.lookup("#careerHeader");
            if (header != null) {
                header.setVisible(false);
                header.setManaged(false);
            }

            if (!us.isCompany()) {
                StudentController ctrl = loader.getController();
                ctrl.selectTab(0);
            }

            Button backBtn = new Button("← Back to Dashboard");
            backBtn.setStyle(
                    "-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                    "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                    "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
            final Scene capturedScene = scene;
            final Parent capturedRoot = originalRoot;
            backBtn.setOnAction(e -> capturedScene.setRoot(capturedRoot));

            HBox topBar = new HBox(backBtn);
            topBar.setPadding(new Insets(10, 15, 10, 15));
            topBar.setAlignment(Pos.CENTER_LEFT);
            topBar.setStyle("-fx-background-color: #f0f2f5;");

            VBox.setVgrow(careerRoot, Priority.ALWAYS);
            VBox fullPage = new VBox(topBar, careerRoot);
            fullPage.setStyle("-fx-background-color: #f7f8fc;");

            scene.setRoot(fullPage);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Careers");
            alert.setHeaderText(null);
            alert.setContentText("Could not load Careers: " + e.getClass().getSimpleName() + " – " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void goToWorkspace() {
        System.out.println("Going to workspace...");
    }

    @FXML
    private void openFocusTimer() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/focus_timer.fxml"));
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
    private void openGuardian() {
        try {
            Scene scene = centerContent.getScene();
            if (originalRoot == null) {
                originalRoot = scene.getRoot();
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/esprit/view/guardian-hub.fxml"));
            Parent guardianRoot = loader.load();

            Button backBtn = new Button("← Back to Dashboard");
            backBtn.setStyle(
                    "-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                            "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
            backBtn.setOnAction(e -> restoreOriginalContent());

            HBox topBar = new HBox(backBtn);
            topBar.setPadding(new Insets(10, 15, 10, 15));
            topBar.setAlignment(Pos.CENTER_LEFT);
            topBar.setStyle("-fx-background-color: #f0f2f5;");

            VBox.setVgrow(guardianRoot, Priority.ALWAYS);
            VBox fullPage = new VBox(topBar, guardianRoot);
            fullPage.setStyle("-fx-background-color: #f0f2f5;");

            scene.setRoot(fullPage);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Guardian");
            alert.setHeaderText(null);
            alert.setContentText("Could not load Guardian: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openLeaderboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/leaderboard.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Leaderboard");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
        } catch (IOException e) {
            System.out.println("Leaderboard not available yet");
        }
    }

    private void openChat(String friendName) {
        System.out.println("Opening chat with: " + friendName);
    }

    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) centerContent.getScene().getWindow();
            stage.setTitle("MindForge - Profile");
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Profile");
            alert.setHeaderText(null);
            alert.setContentText("Profile page coming soon!");
            alert.showAndWait();
        }
    }

    @FXML
    private void logout() {
        UserSession.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) centerContent.getScene().getWindow();
            stage.setTitle("MindForge - Login");
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}