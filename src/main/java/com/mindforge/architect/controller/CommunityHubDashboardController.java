package com.mindforge.architect.controller;

import com.example.controllers.ClaimController;
import com.example.controllers.CommunityHubController;
import com.example.controllers.MainController;
import com.example.controllers.SharedTaskController;
import com.example.controllers.RoomsController;
import com.example.util.SchemaMigrator;
import com.mindforge.community.CommunityLauncher;
import com.mindforge.util.UserSession;
import com.mindforge.utils.SessionManager;
import example.PlannerModule;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the Community Hub dashboard view.
 * Mirrors the Guardian hub pattern: self-contained BorderPane that replaces
 * the scene root, with its own sidebar, hero banner, module cards, and shortcuts.
 */
public class CommunityHubDashboardController {

    @FXML private Label snapshotLabel;
    @FXML private Label userLabel;
    @FXML private VBox  adminCard;
    @FXML private VBox  centerPane;

    @FXML
    public void initialize() {
        // Bridge the MindForge session into the community UserSession
        CommunityLauncher.bridgeSession();

        // Run schema migration (idempotent)
        try { SchemaMigrator.migrateAll(); }
        catch (Exception e) {
            System.err.println("[CommunityHub] Schema migration warning: " + e.getMessage());
        }

        // Show logged-in user
        UserSession us = UserSession.getInstance();
        if (userLabel != null && us.isLoggedIn()) {
            userLabel.setText("👤 " + us.getEmail());
        }

        // Show/hide admin card
        com.example.service.UserSession communitySession = com.example.service.UserSession.getInstance();
        boolean isAdmin = communitySession.isAdmin();
        if (adminCard != null) {
            adminCard.setVisible(isAdmin);
            adminCard.setManaged(isAdmin);
        }

        if (snapshotLabel != null) {
            snapshotLabel.setText("Community module ready.");
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    @FXML
    private void onBackToDashboard(ActionEvent event) {
        loadMindForgeDashboard((Node) event.getSource());
    }

    @FXML
    private void onGoHub(ActionEvent event) {
        // Already on hub — no-op
    }

    @FXML
    private void onOpenVirtualRooms(ActionEvent event) {
        openCommunityView("/views/RoomsView.fxml", "Salles Virtuelles — MindForge",
                (Node) event.getSource(), ctrl -> {
                    if (ctrl instanceof RoomsController c) c.setMainController(buildMainController(event));
                });
    }

    @FXML
    private void onSendChallenge(ActionEvent event) {
        openCommunityView("/views/SharedTaskView.fxml", "Envoyer un Défi — MindForge",
                (Node) event.getSource(), ctrl -> {
                    if (ctrl instanceof SharedTaskController c) {
                        c.setMainController(buildMainController(event));
                        c.setMode(SharedTaskController.Mode.SEND);
                    }
                });
    }

    @FXML
    private void onOpenInbox(ActionEvent event) {
        openCommunityView("/views/SharedTaskView.fxml", "Défis Reçus — MindForge",
                (Node) event.getSource(), ctrl -> {
                    if (ctrl instanceof SharedTaskController c) {
                        c.setMainController(buildMainController(event));
                        c.setMode(SharedTaskController.Mode.INBOX);
                    }
                });
    }

    @FXML
    private void onOpenOutbox(ActionEvent event) {
        openCommunityView("/views/SharedTaskView.fxml", "Défis Envoyés — MindForge",
                (Node) event.getSource(), ctrl -> {
                    if (ctrl instanceof SharedTaskController c) {
                        c.setMainController(buildMainController(event));
                        c.setMode(SharedTaskController.Mode.OUTBOX);
                    }
                });
    }

    @FXML
    private void onOpenTickets(ActionEvent event) {
        openCommunityView("/views/ClaimView.fxml", "Mes Tickets — MindForge",
                (Node) event.getSource(), ctrl -> {
                    if (ctrl instanceof ClaimController c) {
                        c.setMainController(buildMainController(event));
                        c.setMode(ClaimController.Mode.MY_TICKETS);
                    }
                });
    }

    @FXML
    private void onOpenSupport(ActionEvent event) {
        openCommunityView("/views/ClaimView.fxml", "Créer un Ticket — MindForge",
                (Node) event.getSource(), ctrl -> {
                    if (ctrl instanceof ClaimController c) {
                        c.setMainController(buildMainController(event));
                        c.setMode(ClaimController.Mode.CREATE);
                    }
                });
    }

    @FXML
    private void onOpenAdminTickets(ActionEvent event) {
        if (!com.example.service.UserSession.getInstance().isAdmin()) return;
        openCommunityView("/views/ClaimView.fxml", "Gérer les Tickets — MindForge",
                (Node) event.getSource(), ctrl -> {
                    if (ctrl instanceof ClaimController c) {
                        c.setMainController(buildMainController(event));
                        c.setMode(ClaimController.Mode.ADMIN);
                    }
                });
    }

    @FXML
    private void onOpenGuardian(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Parent root = FXMLLoader.load(
                    getClass().getResource("/tn/esprit/view/guardian-hub.fxml"));
            source.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Could not load Guardian hub: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenCareers(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Scene scene = source.getScene();
            final Parent communityRoot = scene.getRoot();

            UserSession us = UserSession.getInstance();
            SessionManager.setSession(
                    us.isCompany() ? SessionManager.Role.COMPANY_OWNER : SessionManager.Role.STUDENT,
                    us.getUserId(), us.getEmail());
            String fxml = us.isCompany()
                    ? "/com/mindforge/company-dashboard.fxml"
                    : "/com/mindforge/student-dashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent careerRoot = loader.load();
            if (!us.isCompany()) {
                com.mindforge.controllers.carriere.StudentController ctrl = loader.getController();
                ctrl.selectTab(0);
            }
            scene.setRoot(wrapWithBack(careerRoot, "← Back to Community Hub", scene, communityRoot, "#f0f2f5"));
        } catch (Exception e) {
            System.err.println("Could not load Careers: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenPlanner(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Scene scene = source.getScene();
            final Parent communityRoot = scene.getRoot();

            PlannerModule plannerModule = new PlannerModule();
            Parent plannerRoot = plannerModule.getView();

            scene.setRoot(wrapWithBack(plannerRoot, "← Back to Community Hub", scene, communityRoot, "#f0f2f5"));
        } catch (Exception e) {
            System.err.println("Could not load Planner: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenSocialHub(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Scene scene = source.getScene();
            final Parent communityRoot = scene.getRoot();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/social_hub.fxml"));
            Parent socialRoot = loader.load();
            scene.setRoot(wrapWithBack(socialRoot, "← Back to Community Hub", scene, communityRoot, "#ececf2"));
        } catch (IOException e) {
            System.err.println("Could not load Social Hub: " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Loads a community module FXML, applies CSS, wires the controller,
     * and replaces the scene root — with a "← Back to Community Hub" bar.
     */
    private void openCommunityView(String fxmlPath, String title, Node source,
                                   java.util.function.Consumer<Object> setup) {
        try {
            Scene scene = source.getScene();
            final Parent communityRoot = scene.getRoot();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            Object ctrl = loader.getController();
            if (ctrl != null) setup.accept(ctrl);

            // Apply community CSS
            String css = getClass().getResource("/styles/main.css") != null
                    ? getClass().getResource("/styles/main.css").toExternalForm() : null;
            if (css != null && !scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }

            scene.setRoot(view);
            Stage stage = (Stage) scene.getWindow();
            if (stage != null) {
                stage.setTitle(title);
                stage.setMaximized(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds a MainController whose showMainView() / showCommunityHub()
     * returns to this community hub dashboard.
     */
    private MainController buildMainController(ActionEvent event) {
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        final Parent communityRoot = scene.getRoot();
        Stage stage = (Stage) scene.getWindow();

        return new MainController() {
            { setPrimaryStage(stage); }

            @Override
            public void showMainView() {
                scene.setRoot(communityRoot);
                if (stage != null) stage.setTitle("Community Hub — MindForge");
            }

            @Override
            public void showCommunityHub() {
                scene.setRoot(communityRoot);
                if (stage != null) stage.setTitle("Community Hub — MindForge");
            }
        };
    }

    /** Navigates back to the main MindForge dashboard. */
    private void loadMindForgeDashboard(Node source) {
        try {
            Scene scene = source.getScene();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/dashboard.fxml"));
            Parent dashRoot = loader.load();
            scene.setRoot(dashRoot);
            Stage stage = (Stage) scene.getWindow();
            if (stage != null) stage.setTitle("MindForge — Dashboard");
        } catch (IOException e) {
            // Fallback: try the MindForge home
            try {
                Scene scene = source.getScene();
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/mindforge/fxml/MindForge.fxml"));
                Parent root = loader.load();
                scene.setRoot(root);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /** Wraps a view with a "← Back" top bar that restores the previous root. */
    private VBox wrapWithBack(Parent content, String btnText, Scene scene,
                              Parent returnTo, String bg) {
        Button backBtn = new Button(btnText);
        backBtn.setStyle("-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                "-fx-font-weight: bold; -fx-font-size: 13px;");
        backBtn.setOnAction(e -> scene.setRoot(returnTo));
        HBox topBar = new HBox(backBtn);
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: " + bg + ";");
        VBox.setVgrow(content, Priority.ALWAYS);
        VBox page = new VBox(topBar, content);
        page.setStyle("-fx-background-color: " + bg + ";");
        return page;
    }
}
