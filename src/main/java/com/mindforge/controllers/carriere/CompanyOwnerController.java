package com.mindforge.controllers.carriere;

import com.mindforge.HelloApplication;
import com.mindforge.entities.carriere.Demande;
import com.mindforge.entities.carriere.Entreprise;
import com.mindforge.entities.carriere.OpportuniteCarriere;
import com.mindforge.services.carriere.AlertMatchingService;
import com.mindforge.services.carriere.DemandeService;
import com.mindforge.services.carriere.EntrepriseService;
import com.mindforge.services.carriere.OpportuniteCarriereService;
import com.mindforge.utils.EmailService;
import com.mindforge.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CompanyOwnerController {

    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private Button backBtn;
    @FXML private Button actionBtn;
    @FXML private FlowPane cardsFlow;

    private final EntrepriseService entrepriseService = new EntrepriseService();
    private final OpportuniteCarriereService oppService = new OpportuniteCarriereService();
    private final DemandeService demandeService = new DemandeService();
    private final AlertMatchingService alertMatchingService = new AlertMatchingService();

    private enum ViewState { COMPANIES, OPPORTUNITIES, APPLICATIONS }
    private ViewState currentState = ViewState.COMPANIES;
    private Entreprise selectedCompany;
    private OpportuniteCarriere selectedOpportunity;

    @FXML
    public void initialize() {
        cardsFlow.setHgap(20);
        cardsFlow.setVgap(20);
        cardsFlow.setPadding(new Insets(28));
        showCompanies();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void showCompanies() {
        currentState = ViewState.COMPANIES;
        pageTitle.setText("My Companies");
        pageSubtitle.setText(SessionManager.getCurrentUserName() + " · Company Owner");
        backBtn.setVisible(false);
        backBtn.setManaged(false);
        actionBtn.setVisible(true);
        actionBtn.setManaged(true);
        actionBtn.setText("+ Add Company");
        actionBtn.setOnAction(e -> showAddCompanyDialog());

        cardsFlow.getChildren().clear();
        List<Entreprise> companies = entrepriseService.getByUserId(SessionManager.getCurrentUserId());
        if (companies.isEmpty()) {
            companies = entrepriseService.getData();
        }
        if (companies.isEmpty()) {
            cardsFlow.getChildren().add(emptyLabel("No companies found. Click + Add Company to create one."));
        } else {
            for (Entreprise c : companies) {
                cardsFlow.getChildren().add(buildCompanyCard(c));
            }
        }
    }

    private void showOpportunities(Entreprise company) {
        currentState = ViewState.OPPORTUNITIES;
        selectedCompany = company;
        pageTitle.setText(company.getName());
        pageSubtitle.setText((company.getIndustry() != null ? company.getIndustry() : "") +
                             " · " + (company.getContactEmail() != null ? company.getContactEmail() : ""));
        backBtn.setVisible(true);
        backBtn.setManaged(true);
        actionBtn.setVisible(true);
        actionBtn.setManaged(true);
        actionBtn.setText("+ Add Opportunity");
        actionBtn.setOnAction(e -> showAddOpportunityDialog());

        cardsFlow.getChildren().clear();
        List<OpportuniteCarriere> opps = oppService.getByCompanyId(company.getId());
        if (opps.isEmpty()) {
            cardsFlow.getChildren().add(emptyLabel("No opportunities yet. Click + Add Opportunity to create one."));
        } else {
            for (OpportuniteCarriere o : opps) {
                cardsFlow.getChildren().add(buildOpportunityCard(o));
            }
        }
    }

    private void showApplications(OpportuniteCarriere opp) {
        currentState = ViewState.APPLICATIONS;
        selectedOpportunity = opp;
        pageTitle.setText("Applications — " + opp.getTitle());
        pageSubtitle.setText((opp.getType() != null ? opp.getType() : "") +
                             " · " + (opp.getLocation() != null ? opp.getLocation() : ""));
        actionBtn.setVisible(false);
        actionBtn.setManaged(false);

        cardsFlow.getChildren().clear();
        List<Demande> demandes = demandeService.getByOpportunityId(opp.getId());
        if (demandes.isEmpty()) {
            cardsFlow.getChildren().add(emptyLabel("No applications yet for this opportunity."));
        } else {
            for (Demande d : demandes) {
                cardsFlow.getChildren().add(buildDemandeCard(d));
            }
        }
    }

    @FXML
    private void goBack() {
        if (currentState == ViewState.APPLICATIONS) {
            showOpportunities(selectedCompany);
        } else {
            showCompanies();
        }
    }

    @FXML
    private void logout() {
        SessionManager.clear();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
            Stage stage = (Stage) cardsFlow.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Card Builders ─────────────────────────────────────────────────────────

    private VBox buildCompanyCard(Entreprise c) {
        VBox card = baseCard(340);

        Label name = bold(c.getName(), 17);
        name.setWrapText(true);

        Label industry = small("🏭  " + nvl(c.getIndustry()));
        Label email = small("✉  " + nvl(c.getContactEmail()));
        email.setWrapText(true);

        Label desc = muted(nvl(c.getDescription()));
        desc.setWrapText(true);
        desc.setMaxHeight(52);

        Region sp = spacer();

        HBox actions = new HBox(8);
        Button view = actionButton("View Opportunities →", "#4f6df5", "white");
        view.setOnAction(e -> showOpportunities(c));

        Button edit = actionButton("Edit", "#e2e8f0", "#475569");
        edit.setOnAction(e -> showEditCompanyDialog(c));

        Button del = actionButton("Delete", "#fee2e2", "#ef4444");
        del.setOnAction(e -> deleteCompany(c));

        actions.getChildren().addAll(view, edit, del);
        card.getChildren().addAll(name, industry, email, desc, sp, actions);
        return card;
    }

    private VBox buildOpportunityCard(OpportuniteCarriere o) {
        VBox card = baseCard(310);

        Label typeBadge = badge(nvl(o.getType()), "#3b82f6");
        Label title = bold(nvl(o.getTitle()), 16);
        title.setWrapText(true);
        Label location = small("📍  " + nvl(o.getLocation()));
        Label deadline = small("⏰  " + (o.getDeadline() != null ? o.getDeadline().toString() : "No deadline"));
        Label statusBadge = badge(nvl(o.getStatus()), statusColor(o.getStatus()));

        Region sp = spacer();

        HBox actions = new HBox(8);
        Button view = actionButton("View Applications →", "#4f6df5", "white");
        view.setOnAction(e -> showApplications(o));

        Button edit = actionButton("Edit", "#e2e8f0", "#475569");
        edit.setOnAction(e -> showEditOpportunityDialog(o));

        Button del = actionButton("Delete", "#fee2e2", "#ef4444");
        del.setOnAction(e -> deleteOpportunity(o));

        actions.getChildren().addAll(view, edit, del);
        card.getChildren().addAll(typeBadge, title, location, deadline, statusBadge, sp, actions);
        return card;
    }

    private VBox buildDemandeCard(Demande d) {
        VBox card = baseCard(300);

        Label applicant = bold("Applicant #" + d.getUserId(), 15);
        Label appliedAt = small("Applied: " + (d.getAppliedAt() != null
                ? d.getAppliedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "—"));
        Label statusBadge = badge(nvl(d.getStatus()), demandeStatusColor(d.getStatus()));

        Label cover = new Label(d.getCoverLetter() != null ? d.getCoverLetter() : "No cover letter provided.");
        cover.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        cover.setWrapText(true);
        cover.setMaxHeight(72);

        Region sp = spacer();

        boolean decided = "accepted".equalsIgnoreCase(d.getStatus()) || "rejected".equalsIgnoreCase(d.getStatus());
        HBox actions = new HBox(10);
        actions.setVisible(!decided);
        actions.setManaged(!decided);

        Button accept = actionButton("✓  Accept", "#22c55e", "white");
        accept.setOnAction(e -> {
            demandeService.updateStatus(d.getId(), "accepted");
            new Thread(() -> {
                String email = EmailService.getUserEmail(d.getUserId());
                if (email != null) {
                    EmailService.sendAcceptanceEmail(
                        email,
                        email,
                        selectedOpportunity.getTitle(),
                        selectedOpportunity.getType() != null ? selectedOpportunity.getType() : "Opportunity",
                        selectedCompany != null ? selectedCompany.getName() : "MindForge"
                    );
                }
            }).start();
            showApplications(selectedOpportunity);
        });

        Button reject = actionButton("✗  Reject", "#ef4444", "white");
        reject.setOnAction(e -> {
            demandeService.updateStatus(d.getId(), "rejected");
            new Thread(() -> {
                String email = EmailService.getUserEmail(d.getUserId());
                if (email != null) {
                    EmailService.sendRejectionEmail(
                        email,
                        email,
                        selectedOpportunity.getTitle(),
                        selectedCompany != null ? selectedCompany.getName() : "MindForge"
                    );
                }
            }).start();
            showApplications(selectedOpportunity);
        });

        actions.getChildren().addAll(accept, reject);
        card.getChildren().addAll(applicant, appliedAt, statusBadge, cover, sp, actions);
        return card;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showAddCompanyDialog() {
        buildCompanyDialog(null).showAndWait().ifPresent(e -> {
            entrepriseService.addEntity(e);
            showCompanies();
        });
    }

    private void showEditCompanyDialog(Entreprise existing) {
        buildCompanyDialog(existing).showAndWait().ifPresent(e -> {
            entrepriseService.updateEntity(existing.getId(), e);
            showCompanies();
        });
    }

    private void deleteCompany(Entreprise c) {
        confirm("Delete \"" + c.getName() + "\"?").ifPresent(r -> {
            entrepriseService.deleteEntity(c);
            showCompanies();
        });
    }

    private void showAddOpportunityDialog() {
        buildOpportunityDialog(null).showAndWait().ifPresent(o -> {
            o.setCompanyId(selectedCompany.getId());
            oppService.addEntity(o);
            // Trigger job alert matching for all active subscriptions
            new Thread(() -> alertMatchingService.matchAndNotify(o)).start();
            showOpportunities(selectedCompany);
        });
    }

    private void showEditOpportunityDialog(OpportuniteCarriere existing) {
        buildOpportunityDialog(existing).showAndWait().ifPresent(o -> {
            o.setCompanyId(selectedCompany.getId());
            oppService.updateEntity(existing.getId(), o);
            showOpportunities(selectedCompany);
        });
    }

    private void deleteOpportunity(OpportuniteCarriere o) {
        confirm("Delete \"" + o.getTitle() + "\"?").ifPresent(r -> {
            oppService.deleteEntity(o);
            showOpportunities(selectedCompany);
        });
    }

    private Dialog<Entreprise> buildCompanyDialog(Entreprise ex) {
        Dialog<Entreprise> dialog = new Dialog<>();
        dialog.setTitle(ex == null ? "Add Company" : "Edit Company");
        dialog.setHeaderText(null);

        TextField nameF     = field(ex != null ? ex.getName() : "");
        TextField industryF = field(ex != null ? ex.getIndustry() : "");
        TextField emailF    = field(ex != null ? ex.getContactEmail() : "");
        TextField phoneF    = field(ex != null && ex.getContactPhone() > 0
                                    ? String.valueOf(ex.getContactPhone()) : "");
        TextField websiteF  = field(ex != null ? ex.getWebsite() : "");
        TextArea  descF     = area(ex != null ? ex.getDescription() : "");

        GridPane grid = formGrid();
        grid.addRow(0, lbl("Name *"),        nameF);
        grid.addRow(1, lbl("Industry"),      industryF);
        grid.addRow(2, lbl("Email"),         emailF);
        grid.addRow(3, lbl("Phone (8 digits)"), phoneF);
        grid.addRow(4, lbl("Website (https://…)"), websiteF);
        grid.addRow(5, lbl("Description"),   descF);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(480);

        // ── Validation ────────────────────────────────────────────────────────
        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        String ERR  = "-fx-border-color: #ef4444; -fx-border-radius: 6; -fx-background-radius: 6;";
        String NORM = "";

        Runnable validate = () -> {
            boolean nameOk    = !nameF.getText().trim().isEmpty();
            boolean emailOk   = emailF.getText().isEmpty()
                                || emailF.getText().matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
            boolean phoneOk   = phoneF.getText().isEmpty()
                                || phoneF.getText().matches("\\d{8}");
            boolean websiteOk = websiteF.getText().isEmpty()
                                || websiteF.getText().matches("https?://.+");

            nameF.setStyle(nameOk                                   ? NORM : ERR);
            emailF.setStyle(emailF.getText().isEmpty()  || emailOk  ? NORM : ERR);
            phoneF.setStyle(phoneF.getText().isEmpty()  || phoneOk  ? NORM : ERR);
            websiteF.setStyle(websiteF.getText().isEmpty()|| websiteOk? NORM : ERR);

            okBtn.setDisable(!nameOk || !emailOk || !phoneOk || !websiteOk);
        };

        nameF.textProperty().addListener((o, v, n)    -> validate.run());
        emailF.textProperty().addListener((o, v, n)   -> validate.run());
        phoneF.textProperty().addListener((o, v, n)   -> validate.run());
        websiteF.textProperty().addListener((o, v, n) -> validate.run());
        validate.run(); // set initial state

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Entreprise e = ex != null ? ex : new Entreprise();
            e.setName(nameF.getText().trim());
            e.setIndustry(industryF.getText().trim());
            e.setContactEmail(emailF.getText().trim());
            try { e.setContactPhone(Integer.parseInt(phoneF.getText().trim())); } catch (NumberFormatException ignored) {}
            e.setWebsite(websiteF.getText().trim());
            e.setDescription(descF.getText().trim());
            return e;
        });
        return dialog;
    }

    private Dialog<OpportuniteCarriere> buildOpportunityDialog(OpportuniteCarriere ex) {
        Dialog<OpportuniteCarriere> dialog = new Dialog<>();
        dialog.setTitle(ex == null ? "Add Opportunity" : "Edit Opportunity");
        dialog.setHeaderText(null);

        TextField titleF    = field(ex != null ? ex.getTitle() : "");
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("internship", "apprenticeship", "fulltime", "parttime", "freelance");
        typeBox.setValue(ex != null && ex.getType() != null ? ex.getType() : "internship");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        TextField locationF = field(ex != null ? ex.getLocation() : "");
        TextField durationF = field(ex != null ? ex.getDuration() : "");

        // If the saved deadline is already in the past, start blank so the user must pick a valid date
        LocalDate existingDeadline = ex != null ? ex.getDeadline() : null;
        if (existingDeadline != null && existingDeadline.isBefore(LocalDate.now())) {
            existingDeadline = null;
        }
        DatePicker deadlinePicker = new DatePicker(existingDeadline);
        deadlinePicker.setMaxWidth(Double.MAX_VALUE);
        deadlinePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #cbd5e1;");
                }
            }
        });
        // Hard guard: if the user manages to select a past date (e.g. by typing),
        // reset the picker immediately — works regardless of Dialog/window scope
        deadlinePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                javafx.application.Platform.runLater(() -> deadlinePicker.setValue(null));
            }
        });

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("open", "closed", "pending");
        statusBox.setValue(ex != null && ex.getStatus() != null ? ex.getStatus() : "open");
        statusBox.setMaxWidth(Double.MAX_VALUE);

        TextArea descF = area(ex != null ? ex.getDescription() : "");

        GridPane grid = formGrid();
        grid.addRow(0, lbl("Title *"),    titleF);
        grid.addRow(1, lbl("Type"),       typeBox);
        grid.addRow(2, lbl("Location"),   locationF);
        grid.addRow(3, lbl("Duration"),   durationF);
        grid.addRow(4, lbl("Deadline"),   deadlinePicker);
        grid.addRow(5, lbl("Status"),     statusBox);
        grid.addRow(6, lbl("Description"),descF);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(480);

        // ── Validation ────────────────────────────────────────────────────────
        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        String ERR  = "-fx-border-color: #ef4444; -fx-border-radius: 6; -fx-background-radius: 6;";
        String NORM = "";

        Runnable validate = () -> {
            boolean titleOk    = !titleF.getText().trim().isEmpty();
            boolean locationOk = !locationF.getText().trim().isEmpty();

            titleF.setStyle(titleOk       ? NORM : ERR);
            locationF.setStyle(locationOk ? NORM : ERR);

            okBtn.setDisable(!titleOk || !locationOk);
        };

        titleF.textProperty().addListener((o, v, n)    -> validate.run());
        locationF.textProperty().addListener((o, v, n) -> validate.run());
        validate.run();

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            OpportuniteCarriere o = ex != null ? ex : new OpportuniteCarriere();
            o.setTitle(titleF.getText().trim());
            o.setType(typeBox.getValue());
            o.setLocation(locationF.getText().trim());
            o.setDuration(durationF.getText().trim());
            o.setDeadline(deadlinePicker.getValue());
            o.setStatus(statusBox.getValue());
            o.setDescription(descF.getText().trim());
            return o;
        });
        return dialog;
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private VBox baseCard(double width) {
        VBox c = new VBox(10);
        c.setPrefWidth(width);
        c.setMinHeight(170);
        String base = "-fx-background-color: white; -fx-background-radius: 12; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 12, 0, 0, 3); -fx-padding: 20;";
        String hover = "-fx-background-color: white; -fx-background-radius: 12; " +
                       "-fx-effect: dropshadow(gaussian, rgba(79,109,245,0.22), 18, 0, 0, 4); -fx-padding: 20;";
        c.setStyle(base);
        c.setOnMouseEntered(e -> c.setStyle(hover));
        c.setOnMouseExited(e -> c.setStyle(base));
        return c;
    }

    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; " +
                   "-fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private Label bold(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + size + "px; -fx-font-weight: bold; -fx-text-fill: #1a1f2e;");
        return l;
    }

    private Label small(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        return l;
    }

    private Label muted(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        return l;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-padding: 20;");
        return l;
    }

    private Button actionButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                   "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 14; " +
                   "-fx-background-radius: 6; -fx-cursor: hand;");
        return b;
    }

    private Region spacer() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    private TextField field(String val) {
        TextField tf = new TextField(val != null ? val : "");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private TextArea area(String val) {
        TextArea ta = new TextArea(val != null ? val : "");
        ta.setPrefHeight(80);
        ta.setWrapText(true);
        ta.setMaxWidth(Double.MAX_VALUE);
        return ta;
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(16));
        ColumnConstraints c1 = new ColumnConstraints(100);
        ColumnConstraints c2 = new ColumnConstraints(300);
        c2.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c1, c2);
        return g;
    }

    private java.util.Optional<ButtonType> confirm(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(r -> r == ButtonType.YES);
    }

    private String nvl(String s) { return s != null ? s : "—"; }

    private String statusColor(String s) {
        if (s == null) return "#94a3b8";
        return switch (s.toLowerCase()) {
            case "open"    -> "#22c55e";
            case "closed"  -> "#94a3b8";
            case "pending" -> "#f59e0b";
            default        -> "#64748b";
        };
    }

    private String demandeStatusColor(String s) {
        if (s == null) return "#f59e0b";
        return switch (s.toLowerCase()) {
            case "accepted" -> "#22c55e";
            case "rejected" -> "#ef4444";
            default         -> "#f59e0b";
        };
    }
}
