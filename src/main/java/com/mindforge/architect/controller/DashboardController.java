package com.mindforge.controller;

import com.mindforge.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import com.mindforge.util.UserSession;
import java.io.IOException;

public class DashboardController {

    @FXML private VBox friendsContainer;

    @FXML
    public void initialize() {
        // Load friends when dashboard opens
        loadFriends();
    }

    private void loadFriends() {
        // Clear loading message
        friendsContainer.getChildren().clear();

        // TODO: Load real friends from database
        // For now, show sample data or empty state

        // Sample friend data (replace with database query)
        addFriendCard("Alice Johnson", 12, 3450, true, 5, true);
        addFriendCard("Bob Smith", 8, 2100, false, 12, false);
        addFriendCard("Charlie Brown", 15, 5200, true, 2, false);

        // If no friends, show empty state
        if (friendsContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("No friends yet. Add friends from your profile and come back here.");
            emptyLabel.setStyle("-fx-text-fill: #6d748a; -fx-font-size: 13px;");
            friendsContainer.getChildren().add(emptyLabel);
        }
    }

    private void addFriendCard(String name, int level, int xp, boolean online, int leaderboardPos, boolean focusMode) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #fbfcff; -fx-border-color: #ebedf7; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12;");
        card.setMinWidth(400);

        // Top row: Name/Level and Status
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #21273b;");
        Label levelLabel = new Label("Level " + level + " · XP " + xp);
        levelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6d748a;");
        nameBox.getChildren().addAll(nameLabel, levelLabel);

        Region spacer = new Region();
        spacer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Status badge
        Label statusLabel = new Label(online ? "Online" : "Offline");
        statusLabel.setStyle("-fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: 600; -fx-background-color: " +
                (online ? "rgba(34,197,94,0.2)" : "rgba(148,163,184,0.2)") + "; -fx-text-fill: " +
                (online ? "#166534" : "#475569") + ";");

        topRow.getChildren().addAll(nameBox, spacer, statusLabel);

        // Info row
        Label infoLabel = new Label("Leaderboard: #" + leaderboardPos + " · " + (focusMode ? "In focus mode" : "Available"));
        infoLabel.setStyle("-fx-text-fill: #6d748a; -fx-font-size: 12px;");

        // Chat button
        Button chatBtn = new Button("Chat from workspace");
        chatBtn.setStyle("-fx-background-color: #4e64f4; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
        chatBtn.setOnAction(e -> openChat(name));

        card.getChildren().addAll(topRow, infoLabel, chatBtn);
        friendsContainer.getChildren().add(card);
    }

    @FXML
    private void goToWorkspace() {
        // Navigate to workspace
        System.out.println("Going to workspace...");
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

    private void openChat(String friendName) {
        System.out.println("Opening chat with: " + friendName);
        // TODO: Implement chat functionality
    }
    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) friendsContainer.getScene().getWindow();
            stage.setTitle("MindForge - Profile");
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
        } catch (IOException e) {
            System.out.println("Profile page not available yet");
            // Show alert
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mindforge/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) friendsContainer.getScene().getWindow();
            stage.setTitle("MindForge - Login");
            stage.setScene(new Scene(root, 500, 400));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}