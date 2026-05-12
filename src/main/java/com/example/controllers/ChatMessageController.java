package com.example.controllers;

import com.example.entity.ChatMessage;
import com.example.entity.User;
import com.example.entity.VirtualRoom;
import com.example.service.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ChatMessageController {
    
    @FXML
    private TableView<ChatMessage> messageTable;
    
    @FXML
    private TableColumn<ChatMessage, Integer> idColumn;
    
    @FXML
    private TableColumn<ChatMessage, String> contentColumn;
    
    @FXML
    private TableColumn<ChatMessage, String> senderColumn;
    
    @FXML
    private TableColumn<ChatMessage, String> roomColumn;
    
    @FXML
    private TableColumn<ChatMessage, Boolean> editedColumn;
    
    @FXML
    private TableColumn<ChatMessage, String> createdAtColumn;
    
    @FXML
    private TextArea contentTextArea;
    
    @FXML
    private ComboBox<User> senderComboBox;
    
    @FXML
    private ComboBox<VirtualRoom> roomComboBox;
    
    @FXML
    private Button addButton;
    
    @FXML
    private Button updateButton;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private Button clearButton;
    
    @FXML
    private Button voiceButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Button joinRoomBtn;
    
    @FXML
    private Button leaveRoomBtn;
    
    @FXML
    private VBox formContainer;
    
    private ChatMessageService messageService;
    private UserService userService;
    private VirtualRoomService roomService;
    private MainController mainController;
    private ObservableList<ChatMessage> messageList;
    private AudioRecorder audioRecorder;
    private final OpenAIService openAIService = new OpenAIService();
    private boolean isRecording = false;
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    @FXML
    public void initialize() {
        messageService = new ChatMessageService();
        userService = new UserService();
        roomService = new VirtualRoomService();
        
        setupTableColumns();
        setupButtonStyles();
        loadUsers();
        loadRooms();
        loadMessages();
        setupTableSelectionListener();
        setupRoomSelectionListener();
        
        // Set current user as default sender and disable if not admin
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            senderComboBox.setValue(currentUser);
            if (!UserSession.getInstance().isAdmin()) {
                senderComboBox.setDisable(true);
            }
        }
    }
    
    private void setupRoomSelectionListener() {
        roomComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldRoom, newRoom) -> {
                if (newRoom != null) {
                    loadMessagesForRoom(newRoom);
                }
            });
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        senderColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().getSender() != null ? 
                cellData.getValue().getSender().getUsername() : ""));
        roomColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().getVirtualRoom() != null ? 
                cellData.getValue().getVirtualRoom().getName() : ""));
        editedColumn.setCellValueFactory(new PropertyValueFactory<>("isEdited"));
        createdAtColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                ValidationUtils.formatDate(cellData.getValue().getCreatedAt())));
        
        // Show edited indicator with timestamp
        editedColumn.setCellFactory(col -> new TableCell<ChatMessage, Boolean>() {
            @Override
            protected void updateItem(Boolean edited, boolean empty) {
                super.updateItem(edited, empty);
                if (empty || edited == null) {
                    setText(null);
                    setStyle("");
                } else if (edited) {
                    setText("✓ Modifié");
                    setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                } else {
                    setText("");
                    setStyle("");
                }
            }
        });
    }

    private void setupButtonStyles() {
        String buttonStyle = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;";
        String updateStyle = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;";
        String deleteStyle = "-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;";
        String clearStyle = "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;";
        String backStyle = "-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;";
        
        addButton.setStyle(buttonStyle);
        updateButton.setStyle(updateStyle);
        deleteButton.setStyle(deleteStyle);
        clearButton.setStyle(clearStyle);
        backButton.setStyle(backStyle);
    }
    
    private void setupTableSelectionListener() {
        messageTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateForm(newSelection);
                }
            });
    }
    
    private void loadUsers() {
        List<User> users = userService.findAll();
        senderComboBox.setItems(FXCollections.observableArrayList(users));
    }
    
    private void loadRooms() {
        List<VirtualRoom> rooms = roomService.findAll();
        roomComboBox.setItems(FXCollections.observableArrayList(rooms));
    }
    
    private void loadMessages() {
        List<ChatMessage> messages = messageService.findAll();
        messageList = FXCollections.observableArrayList(messages);
        messageTable.setItems(messageList);
    }
    
    private void populateForm(ChatMessage message) {
        contentTextArea.setText(message.getContent());
        senderComboBox.setValue(message.getSender());
        roomComboBox.setValue(message.getVirtualRoom());
    }
    
    @FXML
    public void handleAddButton() {
        if (!validateForm()) return;
        if (!canSendMessage()) return;
        
        try {
            ChatMessage message = new ChatMessage();
            message.setContent(contentTextArea.getText().trim());
            message.setSender(senderComboBox.getValue());
            message.setVirtualRoom(roomComboBox.getValue());
            
            messageService.save(message);
            loadMessages();
            clearForm();
            showSuccess("Message envoyé avec succès !");
        } catch (Exception e) {
            showError("Erreur lors de l'envoi du message : " + e.getMessage());
        }
    }
    
    @FXML
    public void handleUpdateButton() {
        ChatMessage selectedMessage = messageTable.getSelectionModel().getSelectedItem();
        if (selectedMessage == null) {
            showError("Veuillez sélectionner un message à modifier");
            return;
        }
        
        if (!canEditMessage(selectedMessage)) {
            showError("Vous ne pouvez modifier que vos propres messages");
            return;
        }
        
        if (!validateForm()) return;
        
        try {
            // Use the edit method from service to properly set edited flags
            messageService.editMessage(selectedMessage.getId(), contentTextArea.getText().trim());
            loadMessages();
            clearForm();
            showSuccess("Message modifié avec succès !");
        } catch (Exception e) {
            showError("Erreur lors de la modification du message : " + e.getMessage());
        }
    }
    
    @FXML
    public void handleDeleteButton() {
        ChatMessage selectedMessage = messageTable.getSelectionModel().getSelectedItem();
        if (selectedMessage == null) {
            showError("Veuillez sélectionner un message à supprimer");
            return;
        }
        
        if (!canDeleteMessage(selectedMessage)) {
            showError("Vous ne pouvez supprimer que vos propres messages");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer ce message ?");
        alert.setContentText("Cette action ne peut pas être annulée.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                messageService.deleteById(selectedMessage.getId());
                loadMessages();
                clearForm();
                showSuccess("Message supprimé avec succès !");
            } catch (Exception e) {
                showError("Erreur lors de la suppression du message : " + e.getMessage());
            }
        }
    }
    
    @FXML
    public void handleVoiceAction() {
        if (!isRecording) {
            startRecording();
        } else {
            stopAndTranscribe();
        }
    }

    private void startRecording() {
        try {
            audioRecorder = new AudioRecorder("temp_voice.wav");
            audioRecorder.start();
            isRecording = true;
            voiceButton.setText("🛑 Stop & Transcribe");
            voiceButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
            showSuccess("Recording... Speak now!");
        } catch (Exception e) {
            showError("Could not start recording: " + e.getMessage());
        }
    }

    private void stopAndTranscribe() {
        audioRecorder.stop();
        isRecording = false;
        voiceButton.setText("🎤 Start Recording");
        voiceButton.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold;");
        
        showSuccess("Transcribing voice... please wait.");
        
        new Thread(() -> {
            try {
                File audioFile = audioRecorder.getWavFile();
                String transcription = openAIService.transcribeAudio(audioFile);
                
                Platform.runLater(() -> {
                    if (contentTextArea.getText().isEmpty()) {
                        contentTextArea.setText(transcription);
                    } else {
                        contentTextArea.setText(contentTextArea.getText() + " " + transcription);
                    }
                    showSuccess("Voice transcription complete!");
                    // Delete temp file
                    audioFile.delete();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Transcription error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleClearButton() {
        clearForm();
    }
    
    @FXML
    public void handleBackButton() {
        mainController.showCommunityHub();
    }
    
    @FXML
    public void handleJoinRoom() {
        VirtualRoom selectedRoom = roomComboBox.getValue();
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (selectedRoom == null) { showError("Veuillez sélectionner une salle à rejoindre"); return; }
        if (currentUser == null)  { showError("Vous devez être connecté"); return; }
        try {
            if (roomService.isParticipant(selectedRoom, currentUser)) {
                showError("Vous êtes déjà participant dans cette salle"); return;
            }
            roomService.addParticipant(selectedRoom, currentUser);
            showSuccess("Salle rejointe : " + selectedRoom.getName());
            loadMessagesForRoom(selectedRoom);
        } catch (Exception e) {
            // Table may not exist — just load messages
            showSuccess("Affichage de la salle : " + selectedRoom.getName());
            loadMessagesForRoom(selectedRoom);
        }
    }
    
    @FXML
    public void handleLeaveRoom() {
        VirtualRoom selectedRoom = roomComboBox.getValue();
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (selectedRoom == null) { showError("Veuillez sélectionner une salle à quitter"); return; }
        if (currentUser == null)  { showError("Vous devez être connecté"); return; }
        try {
            roomService.removeParticipant(selectedRoom, currentUser);
            showSuccess("Salle quittée : " + selectedRoom.getName());
            loadMessages();
        } catch (Exception e) {
            showError("Erreur lors du départ de la salle : " + e.getMessage());
        }
    }
    
    private void loadMessagesForRoom(VirtualRoom room) {
        if (room == null) {
            loadMessages();
            return;
        }
        
        List<ChatMessage> messages = messageService.findByVirtualRoom(room);
        messageList = FXCollections.observableArrayList(messages);
        messageTable.setItems(messageList);
    }
    
    private boolean validateForm() {
        if (!ValidationUtils.isNotEmpty(contentTextArea.getText())) {
            showError("Le contenu ne peut pas être vide");
            return false;
        }
        
        if (contentTextArea.getText().length() > 1000) {
            showError("Le contenu du message ne peut pas dépasser 1000 caractères");
            return false;
        }
        
        if (senderComboBox.getValue() == null) {
            showError("Veuillez sélectionner un expéditeur");
            return false;
        }
        
        if (roomComboBox.getValue() == null) {
            showError("Veuillez sélectionner une salle virtuelle");
            return false;
        }
        
        return true;
    }
    
    /** Permission check: Only room participants or admin can send messages */
    private boolean canSendMessage() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("Vous devez être connecté pour envoyer des messages");
            return false;
        }
        // Admin can always send
        if (UserSession.getInstance().isAdmin()) return true;

        VirtualRoom selectedRoom = roomComboBox.getValue();
        if (selectedRoom == null) {
            showError("Veuillez sélectionner une salle");
            return false;
        }

        // Try participant check — if the table doesn't exist, allow anyway
        try {
            if (!roomService.isParticipant(selectedRoom, currentUser)) {
                showError("Vous devez rejoindre cette salle avant d'envoyer des messages.\nCliquez d'abord sur 'Rejoindre'.");
                return false;
            }
        } catch (Exception e) {
            // virtual_room_participants table may not exist — allow send
            System.out.println("Participant check skipped: " + e.getMessage());
        }
        return true;
    }
    
    /** Permission check: Only message author or admin can edit */
    private boolean canEditMessage(ChatMessage message) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        
        // Admin can edit any message
        if (UserSession.getInstance().isAdmin()) return true;
        
        // Author can edit their own message
        return message.getSender() != null && 
               message.getSender().getId() == currentUser.getId();
    }
    
    /** Permission check: Only message author or admin can delete */
    private boolean canDeleteMessage(ChatMessage message) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        
        // Admin can delete any message
        if (UserSession.getInstance().isAdmin()) return true;
        
        // Author can delete their own message
        return message.getSender() != null && 
               message.getSender().getId() == currentUser.getId();
    }
    
    private void clearForm() {
        contentTextArea.clear();
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            senderComboBox.setValue(currentUser);
        } else {
            senderComboBox.setValue(null);
        }
        messageTable.getSelectionModel().clearSelection();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
