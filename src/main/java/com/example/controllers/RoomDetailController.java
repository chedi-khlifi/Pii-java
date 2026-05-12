package com.example.controllers;

import com.example.service.UserSession;
import com.example.service.AudioRecorder;
import com.example.service.OpenAIService;
import com.example.service.MessagingAPIService;
import com.example.virtualrooms.dao.ChatMessageDAO;
import com.example.virtualrooms.dao.VirtualRoomDAO;
import com.example.entity.ChatMessage;
import com.example.entity.VirtualRoom;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class RoomDetailController {

    @FXML private Label roomNameLabel;
    @FXML private Label roomDescriptionLabel;
    @FXML private Label roomSubjectLabel;
    @FXML private Label roomStatusLabel;
    @FXML private Label roomCreatorLabel;
    @FXML private Label roomCreatedAtLabel;
    @FXML private Label roomCapacityLabel;

    @FXML private ListView<String> participantsListView;
    @FXML private VBox messagesVBox;
    @FXML private ScrollPane messagesScrollPane;

    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button voiceButton;
    @FXML private Button leaveRoomButton;
    @FXML private Button backButton;

    private final VirtualRoomDAO roomDAO = new VirtualRoomDAO();
    private final ChatMessageDAO messageDAO = new ChatMessageDAO();
    private final MessagingAPIService messagingAPI = new MessagingAPIService();
    private final OpenAIService openAIService = new OpenAIService();
    private AudioRecorder audioRecorder;
    private boolean isRecording = false;

    private MainController mainController;
    private int roomId;
    private Timeline refreshTimeline;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setRoom(int roomId) {
        this.roomId = roomId;
        loadRoomData();
        startAutoRefresh();
    }

    @FXML
    public void initialize() {
        // Remove direct setOnAction if handleSendMessage is already assigned in FXML
    }

    @FXML
    public void handleSendMessage() {
        String content = messageField.getText() != null ? messageField.getText().trim() : "";
        if (content.isBlank()) {
            return;
        }

        int currentUserId = getCurrentUserId();
        if (currentUserId <= 0) {
            showAlert(Alert.AlertType.WARNING, "Chat", "Vous devez être connecté.");
            return;
        }

        // Always reload room participant status before sending
        if (!roomDAO.isUserParticipant(roomId, currentUserId)) {
            showAlert(Alert.AlertType.WARNING, "Chat", "Seuls les participants peuvent envoyer des messages. Cliquez d'abord sur 'Rejoindre' dans la liste des salles.");
            return;
        }

        messageDAO.insert(roomId, currentUserId, content);
        messageField.clear();
        loadMessages();
    }

    @FXML
    private void handleVoiceAction() {
        if (!isRecording) {
            // Démarrer l'enregistrement
            String fileName = "voice_msg_" + System.currentTimeMillis() + ".wav";
            audioRecorder = new AudioRecorder(fileName);
            audioRecorder.start();
            isRecording = true;
            voiceButton.setText("🛑");
            voiceButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            // Arrêter et transcrire
            audioRecorder.stop();
            isRecording = false;
            voiceButton.setText("🎤");
            voiceButton.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold;");
            voiceButton.setDisable(true); // Désactiver pendant la transcription

            new Thread(() -> {
                try {
                    File audioFile = audioRecorder.getWavFile();
                    String transcription = openAIService.transcribeAudio(audioFile);
                    
                    Platform.runLater(() -> {
                        messageField.setText(transcription);
                        voiceButton.setDisable(false);
                        // Nettoyage du fichier temporaire
                        if (audioFile.exists()) {
                            audioFile.delete();
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Chat Vocal", "La transcription a échoué : " + e.getMessage());
                        voiceButton.setDisable(false);
                    });
                }
            }).start();
        }
    }

    @FXML
    private void handleLeaveRoom() {
        int currentUserId = getCurrentUserId();
        if (currentUserId <= 0) {
            showAlert(Alert.AlertType.WARNING, "Salle", "Vous devez être connecté.");
            return;
        }
        roomDAO.leaveRoom(roomId, currentUserId);
        showAlert(Alert.AlertType.INFORMATION, "Salle", "Vous avez quitté la salle.");
        handleBack();
    }

    @FXML
    private void handleBack() {
        stopAutoRefresh();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RoomsView.fxml"));
            Parent root = loader.load();
            RoomsController controller = loader.getController();
            controller.setMainController(mainController);

            Stage stage = mainController != null ? mainController.getPrimaryStage() : (Stage) backButton.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.setTitle("Salles Virtuelles — MindForge");
            stage.setMaximized(true);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible de retourner aux salles : " + e.getMessage());
        }
    }

    private void loadRoomData() {
        Optional<VirtualRoom> roomOpt = roomDAO.findById(roomId, getCurrentUserId());
        if (roomOpt.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Salle", "Salle introuvable.");
            return;
        }
        VirtualRoom room = roomOpt.get();

        if (roomNameLabel != null) roomNameLabel.setText(room.getName());
        if (roomDescriptionLabel != null) roomDescriptionLabel.setText(room.getDescription() == null || room.getDescription().isBlank() ? "-" : room.getDescription());
        if (roomSubjectLabel != null) roomSubjectLabel.setText(room.getSubject() == null || room.getSubject().isBlank() ? "-" : room.getSubject());
        if (roomStatusLabel != null) roomStatusLabel.setText(room.getStatusLabel());
        if (roomCreatorLabel != null) roomCreatorLabel.setText(room.getCreatorName() == null || room.getCreatorName().isBlank() ? "Inconnu" : room.getCreatorName());
        if (roomCreatedAtLabel != null) roomCreatedAtLabel.setText(room.getCreatedAt() != null ? room.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
        if (roomCapacityLabel != null) roomCapacityLabel.setText(room.getCapacityLabel());

        loadParticipants(room);
        loadMessages();
    }

    private void loadParticipants(VirtualRoom room) {
        int me = getCurrentUserId();
        List<VirtualRoomDAO.ParticipantInfo> participants = roomDAO.findParticipants(roomId);
        List<String> rows = participants.stream().map(p -> {
            String suffix = "";
            if (room.getCreatorId() != null && room.getCreatorId() == p.getUserId()) {
                suffix += " [Créateur]";
            }
            if (p.getUserId() == me) {
                suffix += " [Vous]";
            }
            return p.getDisplayName() + suffix;
        }).toList();
        participantsListView.getItems().setAll(rows);
    }

    private void loadMessages() {
        List<ChatMessage> messages = messageDAO.findByRoom(roomId);
        
        // Only update if number of messages changed to avoid flickering and performance issues
        if (messages.size() == messagesVBox.getChildren().size()) {
            return;
        }

        messagesVBox.getChildren().clear();

        int me = getCurrentUserId();
        for (ChatMessage msg : messages) {
            boolean mine = msg.getSender() != null && msg.getSender().getId() == me;
            HBox row = new HBox();
            row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.setPadding(new javafx.geometry.Insets(0, mine ? 0 : 20, 0, mine ? 20 : 0));

            VBox bubble = new VBox(5);
            bubble.setMaxWidth(500);
            
            // Modern bubble styling
            String bubbleStyle = mine 
                ? "-fx-background-color: #6f42c1; -fx-background-radius: 20 20 2 20; -fx-padding: 12 18;" 
                : "-fx-background-color: #f1f3f5; -fx-background-radius: 20 20 20 2; -fx-padding: 12 18;";
            bubble.setStyle(bubbleStyle);

            if (!mine) {
                Label sender = new Label(msg.getSender() != null ? msg.getSender().getUsername() : "Inconnu");
                sender.setStyle("-fx-text-fill: #6f42c1; -fx-font-weight: 900; -fx-font-size: 11px;");
                bubble.getChildren().add(sender);
            }

            Label content = new Label(msg.getContent());
            content.setWrapText(true);
            content.setStyle(mine ? "-fx-text-fill: white; -fx-font-size: 14px;" : "-fx-text-fill: #212529; -fx-font-size: 14px;");
            
            Label time = new Label(msg.getCreatedAt() != null 
                ? msg.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) 
                : "");
            time.setStyle(mine ? "-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 10px;" : "-fx-text-fill: #adb5bd; -fx-font-size: 10px;");
            
            HBox timeRow = new HBox(time);
            timeRow.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            bubble.getChildren().addAll(content, timeRow);
            row.getChildren().add(bubble);
            messagesVBox.getChildren().add(row);
        }

        // Auto-scroll to bottom
        Platform.runLater(() -> {
            messagesScrollPane.layout();
            messagesScrollPane.setVvalue(1.0);
        });
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        // Faster refresh for "real-time" feel
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> loadMessages()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }

    private int getCurrentUserId() {
        var user = UserSession.getInstance().getCurrentUser();
        return user != null ? user.getId() : -1;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
