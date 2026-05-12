package com.example.controllers;

import com.example.service.UserSession;
import com.example.virtualrooms.dao.VirtualRoomDAO;
import com.example.entity.VirtualRoom;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class RoomsController {

    @FXML private FlowPane roomsCardsPane;
    @FXML private Button backButton;
    @FXML private ScrollPane roomsScrollPane;

    private final VirtualRoomDAO roomDAO = new VirtualRoomDAO();
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        if (roomsScrollPane != null) {
            UIUtils.makeSmooth(roomsScrollPane);
        }
        loadRooms();
    }

    @FXML
    private void handleRefresh() {
        loadRooms();
    }

    @FXML
    private void handleBackButton() {
        if (mainController != null) {
            mainController.showCommunityHub();
        }
    }

    private void loadRooms() {
        int currentUserId = getCurrentUserId();
        List<VirtualRoom> rooms = roomDAO.findAllWithStats(currentUserId);
        renderRoomCards(rooms);
    }

    private void renderRoomCards(List<VirtualRoom> rooms) {
        roomsCardsPane.getChildren().clear();

        if (rooms == null || rooms.isEmpty()) {
            Label empty = new Label("Aucune salle virtuelle disponible.");
            empty.getStyleClass().add("muted-label");
            roomsCardsPane.getChildren().add(empty);
            return;
        }

        for (VirtualRoom room : rooms) {
            roomsCardsPane.getChildren().add(buildRoomCard(room));
        }
    }

    private VBox buildRoomCard(VirtualRoom room) {
        VBox card = new VBox(12);
        card.getStyleClass().add("room-card");
        card.setPrefWidth(320);
        card.setMinWidth(320);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(nullToDash(room.getName()));
        title.getStyleClass().add("room-card-title");

        Label description = new Label(nullToDash(room.getDescription()));
        description.getStyleClass().add("room-card-description");
        description.setWrapText(true);
        description.setMinHeight(45);

        Label subject = new Label("Sujet : " + nullToDash(room.getSubject()));
        subject.getStyleClass().add("room-card-meta");
        subject.setStyle("-fx-font-weight: bold;");

        HBox stats = new HBox(10);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label status = new Label(room.getStatusLabel());
        status.getStyleClass().add("role-badge");
        Label cap = new Label(room.getCapacityLabel());
        cap.getStyleClass().add("room-capacity-badge");
        stats.getChildren().addAll(status, cap);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("room-card-actions");
        
        Button joinButton = new Button();
        joinButton.getStyleClass().add("module-main-action");
        joinButton.setPrefWidth(120);

        int capacity = room.getCapacity() <= 0 ? 10 : room.getCapacity();
        boolean full = room.getParticipantCount() >= capacity;
        boolean joined = room.isCurrentUserJoined();
        joinButton.setDisable(full || joined);
        joinButton.setText(joined ? "Rejoint" : (full ? "Plein" : "Rejoindre"));
        joinButton.setOnAction(e -> joinRoom(room));

        Button openButton = new Button("Ouvrir");
        openButton.getStyleClass().add("module-secondary-action");
        openButton.setPrefWidth(100);
        openButton.setOnAction(e -> openRoomDetails(room));

        actions.getChildren().addAll(joinButton, openButton);
        card.getChildren().addAll(title, description, subject, stats, spacer, actions);
        return card;
    }

    private void joinRoom(VirtualRoom room) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            showAlert(Alert.AlertType.WARNING, "Salle", "Vous devez être connecté.");
            return;
        }

        if (roomDAO.isUserParticipant(room.getId(), userId)) {
            showAlert(Alert.AlertType.WARNING, "Salle", "Vous êtes déjà participant de cette salle.");
            loadRooms();
            return;
        }

        int capacity = room.getCapacity() <= 0 ? 10 : room.getCapacity();
        if (roomDAO.countParticipants(room.getId()) >= capacity) {
            showAlert(Alert.AlertType.WARNING, "Salle", "Salle complète. Impossible de rejoindre.");
            loadRooms();
            return;
        }

        roomDAO.joinRoom(room.getId(), userId);
        showAlert(Alert.AlertType.INFORMATION, "Salle", "Vous avez rejoint la salle.");
        loadRooms();
    }

    private void openRoomDetails(VirtualRoom room) {
        try {
            System.out.println("Opening room: " + room.getName() + " (ID: " + room.getId() + ")");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RoomDetailView.fxml"));
            Parent root = loader.load();
            RoomDetailController controller = loader.getController();
            
            if (controller == null) {
                throw new IOException("Erreur interne : le contrôleur de la salle n'a pas pu être chargé.");
            }
            
            controller.setMainController(mainController);
            controller.setRoom(room.getId());

            Stage stage = mainController != null ? mainController.getPrimaryStage() : (Stage) roomsCardsPane.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.setTitle("Salle Virtuelle : " + room.getName() + " — MindForge");
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir les détails de la salle : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur inattendue est survenue : " + e.getMessage());
        }
    }

    private int getCurrentUserId() {
        var user = UserSession.getInstance().getCurrentUser();
        return user != null ? user.getId() : -1;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
