package com.example.controllers;

import com.example.service.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class CommunityHubController {

    @FXML private Label  welcomeLabel;
    @FXML private Button adminTicketsBtn;
    @FXML private VBox   adminTicketsCard;
    @FXML private ScrollPane mainScrollPane;

    private MainController mainController;
    private Stage primaryStage;

    public void setMainController(MainController mc) {
        this.mainController  = mc;
        this.primaryStage    = mc.getPrimaryStage();
        refreshUI();
    }

    @FXML
    public void initialize() {
        if (mainScrollPane != null) {
            UIUtils.makeSmooth(mainScrollPane);
        }
    }

    private void refreshUI() {
        var user = UserSession.getInstance().getCurrentUser();
        if (user != null && welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + user.getUsername()
                    + (user.isAdmin() ? "  [Admin]" : ""));
        }
        
        boolean isAdmin = UserSession.getInstance().isAdmin();
        if (adminTicketsBtn != null) {
            adminTicketsBtn.setVisible(isAdmin);
            adminTicketsBtn.setManaged(isAdmin);
        }
        if (adminTicketsCard != null) {
            adminTicketsCard.setVisible(isAdmin);
            adminTicketsCard.setManaged(isAdmin);
        }
    }

    // ── Navigation handlers ────────────────────────────────────────────────

    @FXML public void handleSendChallenge() {
        openSharedTask(SharedTaskController.Mode.SEND, "Envoyer un Défi");
    }

    @FXML public void handleReceivedChallenges() {
        openSharedTask(SharedTaskController.Mode.INBOX, "Défis Reçus");
    }

    @FXML public void handleSentChallenges() {
        openSharedTask(SharedTaskController.Mode.OUTBOX, "Historique des Envois");
    }

    @FXML public void handleCreateTicket() {
        openClaim(ClaimController.Mode.CREATE, "Créer un Ticket de Support");
    }

    @FXML public void handleMyTickets() {
        openClaim(ClaimController.Mode.MY_TICKETS, "Mes Tickets");
    }

    @FXML public void handleAdminTickets() {
        if (!UserSession.getInstance().isAdmin()) return;
        openClaim(ClaimController.Mode.ADMIN, "Gérer tous les Tickets");
    }

    @FXML public void handleVirtualRooms() {
        load("/views/RoomsView.fxml", "Salles Virtuelles & Chat", 1100, 760, ctrl -> {
            if (ctrl instanceof RoomsController c) c.setMainController(mainController);
        });
    }

    @FXML public void handleBack() {
        mainController.showMainView();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void openSharedTask(SharedTaskController.Mode mode, String title) {
        load("/views/SharedTaskView.fxml", title, 960, 720, ctrl -> {
            if (ctrl instanceof SharedTaskController c) {
                c.setMainController(mainController);
                c.setMode(mode);
            }
        });
    }

    private void openClaim(ClaimController.Mode mode, String title) {
        load("/views/ClaimView.fxml", title, 980, 740, ctrl -> {
            if (ctrl instanceof ClaimController c) {
                c.setMainController(mainController);
                c.setMode(mode);
            }
        });
    }

    private void load(String fxml, String title, double w, double h, Consumer<Object> setup) {
        try {
            System.out.println("Loading FXML: " + fxml);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller == null) {
                System.err.println("Warning: Controller is null for " + fxml);
            } else {
                setup.accept(controller);
            }

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                try {
                    scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
                } catch (Exception e) {
                    System.err.println("CSS Load Error: " + e.getMessage());
                }
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            primaryStage.setTitle(title + " — MindForge");
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur de Navigation");
            a.setHeaderText("Impossible de charger la vue");
            a.setContentText("Fichier : " + fxml + "\nErreur : " + e.getMessage());
            a.showAndWait();
        }
    }
}
