package com.mindforge.controllers.carriere;

import com.mindforge.HelloApplication;
import com.mindforge.entities.carriere.*;
import com.mindforge.services.carriere.*;
import com.mindforge.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StudentController {

    @FXML private Label pageTitle;

    // Companies tab
    @FXML private HBox companiesSearchBar;
    @FXML private TextField searchCompanyKeyword;
    @FXML private ComboBox<String> searchCompanyIndustry;
    @FXML private HBox companiesNavBar;
    @FXML private Label companiesNavLabel;
    @FXML private FlowPane companiesFlow;

    // Tab pane (allows HomeController to pre-select a tab)
    @FXML private TabPane careerTabPane;

    // Opportunities tab
    @FXML private FlowPane opportunitiesFlow;
    @FXML private TextField searchKeyword;
    @FXML private ComboBox<String> searchType;
    @FXML private TextField searchLocation;

    // My Applications tab
    @FXML private FlowPane myApplicationsFlow;

    // Alerts tab
    @FXML private FlowPane alertsFlow;

    // Notifications tab
    @FXML private FlowPane notificationsFlow;
    @FXML private Label notifHeader;
    @FXML private Label notifBadge;

    private final EntrepriseService entrepriseService = new EntrepriseService();
    private final OpportuniteCarriereService oppService = new OpportuniteCarriereService();
    private final DemandeService demandeService = new DemandeService();
    private final QuizService quizService = new QuizService();
    private final JobAlertSubscriptionService alertSubService = new JobAlertSubscriptionService();
    private final JobAlertNotificationService alertNotifService = new JobAlertNotificationService();

    private Entreprise selectedCompany;
    private List<OpportuniteCarriere> allOpportunities = new ArrayList<>();
    private List<Entreprise> allCompanies = new ArrayList<>();

    @FXML
    public void initialize() {
        configFlow(companiesFlow);
        configFlow(opportunitiesFlow);
        configFlow(myApplicationsFlow);

        searchType.getItems().addAll("All Types", "Internship", "Apprenticeship", "Full-time", "Part-time", "Freelance");
        searchType.setValue("All Types");

        searchCompanyIndustry.getItems().add("All Industries");
        searchCompanyIndustry.setValue("All Industries");

        // Realtime search
        searchKeyword.textProperty().addListener((obs, o, n) -> searchOpportunities());
        searchLocation.textProperty().addListener((obs, o, n) -> searchOpportunities());
        searchType.valueProperty().addListener((obs, o, n) -> searchOpportunities());

        searchCompanyKeyword.textProperty().addListener((obs, o, n) -> searchCompanies());
        searchCompanyIndustry.valueProperty().addListener((obs, o, n) -> searchCompanies());

        loadCompanies();
        loadOpportunities();
        loadMyApplications();
        loadAlerts();
        loadNotifications();
        refreshNotifBadge();
    }

    /** Called by HomeController to jump to a specific tab (0=Companies, 1=Browse, 2=Applications). */
    public void selectTab(int index) {
        if (careerTabPane != null && index >= 0 && index < careerTabPane.getTabs().size()) {
            careerTabPane.getSelectionModel().select(index);
        }
    }

    @FXML
    private void openAlertsTab(javafx.scene.input.MouseEvent e) {
        selectTab(4);
    }

    private void configFlow(FlowPane fp) {
        fp.setHgap(20);
        fp.setVgap(20);
        fp.setPadding(new Insets(28));
    }

    // ── Companies Tab ─────────────────────────────────────────────────────────

    private void loadCompanies() {
        allCompanies = entrepriseService.getData();

        // Populate industry dropdown from actual data
        searchCompanyIndustry.getItems().clear();
        searchCompanyIndustry.getItems().add("All Industries");
        allCompanies.stream()
            .map(Entreprise::getIndustry)
            .filter(i -> i != null && !i.isBlank())
            .distinct()
            .sorted()
            .forEach(searchCompanyIndustry.getItems()::add);
        searchCompanyIndustry.setValue("All Industries");

        displayCompanies(allCompanies);
    }

    private void displayCompanies(List<Entreprise> companies) {
        companiesFlow.getChildren().clear();
        if (companies.isEmpty()) {
            companiesFlow.getChildren().add(emptyLabel("No companies match your search."));
        } else {
            for (Entreprise c : companies) {
                companiesFlow.getChildren().add(buildCompanyCard(c));
            }
        }
    }

    @FXML
    private void searchCompanies() {
        String keyword = searchCompanyKeyword.getText().trim().toLowerCase();
        String industry = searchCompanyIndustry.getValue();
        boolean anyIndustry = industry == null || industry.equals("All Industries");

        List<Entreprise> filtered = allCompanies.stream().filter(c -> {
            boolean kw = keyword.isEmpty() ||
                (c.getName() != null && c.getName().toLowerCase().contains(keyword)) ||
                (c.getDescription() != null && c.getDescription().toLowerCase().contains(keyword));
            boolean ind = anyIndustry ||
                (c.getIndustry() != null && c.getIndustry().equalsIgnoreCase(industry));
            return kw && ind;
        }).collect(Collectors.toList());

        displayCompanies(filtered);
    }

    private void showCompanyOpportunities(Entreprise company) {
        selectedCompany = company;
        companiesSearchBar.setVisible(false);
        companiesSearchBar.setManaged(false);
        companiesNavBar.setVisible(true);
        companiesNavBar.setManaged(true);
        companiesNavLabel.setText(company.getName() + "  ·  " + nvl(company.getIndustry()));

        companiesFlow.getChildren().clear();
        List<OpportuniteCarriere> opps = oppService.getByCompanyId(company.getId());
        if (opps.isEmpty()) {
            companiesFlow.getChildren().add(emptyLabel("This company has no opportunities at the moment."));
        } else {
            for (OpportuniteCarriere o : opps) {
                companiesFlow.getChildren().add(buildOpportunityCard(o));
            }
        }
    }

    @FXML
    private void goBackInCompanies() {
        companiesNavBar.setVisible(false);
        companiesNavBar.setManaged(false);
        companiesSearchBar.setVisible(true);
        companiesSearchBar.setManaged(true);
        selectedCompany = null;
        displayCompanies(allCompanies);
    }

    // ── Opportunities Tab ─────────────────────────────────────────────────────

    private void loadOpportunities() {
        allOpportunities = oppService.getData();
        displayOpportunities(allOpportunities);
    }

    private void displayOpportunities(List<OpportuniteCarriere> opps) {
        opportunitiesFlow.getChildren().clear();
        if (opps.isEmpty()) {
            opportunitiesFlow.getChildren().add(emptyLabel("No opportunities match your search."));
        } else {
            for (OpportuniteCarriere o : opps) {
                opportunitiesFlow.getChildren().add(buildOpportunityCard(o));
            }
        }
    }

    @FXML
    private void searchOpportunities() {
        String keyword = searchKeyword.getText().trim().toLowerCase();
        String typeFilter = typeFilterValue(searchType.getValue());
        String location = searchLocation.getText().trim().toLowerCase();

        List<OpportuniteCarriere> filtered = allOpportunities.stream().filter(o -> {
            boolean kw = keyword.isEmpty() ||
                (o.getTitle() != null && o.getTitle().toLowerCase().contains(keyword)) ||
                (o.getDescription() != null && o.getDescription().toLowerCase().contains(keyword));
            boolean ty = typeFilter == null ||
                (o.getType() != null && o.getType().equalsIgnoreCase(typeFilter));
            boolean loc = location.isEmpty() ||
                (o.getLocation() != null && o.getLocation().toLowerCase().contains(location));
            return kw && ty && loc;
        }).collect(Collectors.toList());

        displayOpportunities(filtered);
    }

    private String typeFilterValue(String display) {
        if (display == null || display.equals("All Types")) return null;
        return switch (display) {
            case "Internship"     -> "internship";
            case "Apprenticeship" -> "apprenticeship";
            case "Full-time"      -> "fulltime";
            case "Part-time"      -> "parttime";
            case "Freelance"      -> "freelance";
            default               -> display.toLowerCase();
        };
    }

    // ── My Applications Tab ───────────────────────────────────────────────────

    private void loadMyApplications() {
        myApplicationsFlow.getChildren().clear();
        List<Demande> demandes = demandeService.getByUserId(SessionManager.getCurrentUserId());
        if (demandes.isEmpty()) {
            myApplicationsFlow.getChildren().add(emptyLabel("You haven't applied to any opportunities yet."));
        } else {
            for (Demande d : demandes) {
                myApplicationsFlow.getChildren().add(buildApplicationCard(d));
            }
        }
    }

    // ── Card Builders ─────────────────────────────────────────────────────────

    private VBox buildCompanyCard(Entreprise c) {
        VBox card = darkCard(340);

        Label name = darkBold(nvl(c.getName()), 18);
        name.setWrapText(true);

        Label industry = darkSmall("🏭  " + nvl(c.getIndustry()));
        Label email = darkSmall("✉  " + nvl(c.getContactEmail()));
        email.setWrapText(true);
        Label website = darkSmall("🌐  " + nvl(c.getWebsite()));
        website.setWrapText(true);

        Label desc = new Label(nvl(c.getDescription()));
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.55);");
        desc.setWrapText(true);
        desc.setMaxHeight(60);

        Region sp = spacer();

        Button viewBtn = new Button("View Opportunities →");
        viewBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; " +
                         "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 20; " +
                         "-fx-background-radius: 25; -fx-cursor: hand; " +
                         "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 25;");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> showCompanyOpportunities(c));

        card.getChildren().addAll(name, industry, email, website, desc, sp, viewBtn);
        return card;
    }

    private VBox buildOpportunityCard(OpportuniteCarriere o) {
        VBox card = darkCard(300);

        Label title = darkBold(nvl(o.getTitle()), 17);
        title.setWrapText(true);
        Label company = darkSmall("🏢  " + nvl(o.getCompanyName()));
        Label location = darkSmall("📍  " + nvl(o.getLocation()));

        HBox badgesRow = new HBox(8);
        badgesRow.getChildren().add(darkBadge(nvl(o.getType()), "#3b82f6"));
        if (o.getDuration() != null && !o.getDuration().isBlank()) {
            badgesRow.getChildren().add(darkBadge(o.getDuration(), "#0ea5e9"));
        }
        if (o.getDeadline() != null) {
            String dl = "Deadline: " + o.getDeadline().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            badgesRow.getChildren().add(darkBadge(dl, "#f59e0b"));
        }

        Label desc = new Label(nvl(o.getDescription()));
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.55);");
        desc.setWrapText(true);
        desc.setMaxHeight(52);

        Region sp = spacer();

        Button applyBtn = new Button("View Details →");
        applyBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; " +
                          "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 20; " +
                          "-fx-background-radius: 25; -fx-cursor: hand; " +
                          "-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 25;");
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setOnAction(e -> handleApply(o));

        card.getChildren().addAll(title, company, location, badgesRow, desc, sp, applyBtn);
        return card;
    }

    private VBox buildApplicationCard(Demande d) {
        VBox card = baseCard(320);

        Label title = bold(nvl(d.getOpportunityTitle()), 15);
        title.setWrapText(true);

        Label statusBadge = badge(nvl(d.getStatus()), demandeStatusColor(d.getStatus()));

        Label appliedAt = small("Applied: " + (d.getAppliedAt() != null
                ? d.getAppliedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "—"));

        Label cover = new Label(d.getCoverLetter() != null && !d.getCoverLetter().isBlank()
                ? d.getCoverLetter() : "No cover letter.");
        cover.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        cover.setWrapText(true);
        cover.setMaxHeight(64);

        card.getChildren().addAll(title, statusBadge, appliedAt, cover);

        if ("pending".equalsIgnoreCase(d.getStatus())) {
            Region sp = spacer();
            Button withdrawBtn = new Button("Withdraw Application");
            withdrawBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-size: 12px; " +
                                 "-fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;");
            withdrawBtn.setMaxWidth(Double.MAX_VALUE);
            withdrawBtn.setOnAction(e -> withdrawApplication(d));
            card.getChildren().addAll(sp, withdrawBtn);
        }

        return card;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void handleApply(OpportuniteCarriere o) {
        if (demandeService.hasApplied(SessionManager.getCurrentUserId(), o.getId())) {
            Alert info = new Alert(Alert.AlertType.INFORMATION,
                    "You already have an active application for this opportunity.\n" +
                    "Check 'My Applications' to see its status.");
            info.setHeaderText(null);
            info.setTitle("Already Applied");
            info.showAndWait();
            return;
        }
        if (quizService.isPassed(o.getId())) {
            showApplyDialog(o);
        } else if (quizService.getAttempts(o.getId()) >= QuizService.MAX_ATTEMPTS) {
            showQuizExhausted(o);
        } else {
            startQuiz(o);
        }
    }

    // ── Quiz ──────────────────────────────────────────────────────────────────

    private void startQuiz(OpportuniteCarriere o) {
        Alert loading = new Alert(Alert.AlertType.INFORMATION,
                "Generating quiz questions with AI, please wait...");
        loading.setHeaderText(null);
        loading.setTitle("Loading Quiz…");
        loading.show();

        new Thread(() -> {
            try {
                List<QuizService.QuizQuestion> questions = quizService.generateQuestions(o);
                javafx.application.Platform.runLater(() -> {
                    loading.close();
                    showQuizDialog(o, questions);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loading.close();
                    Alert err = new Alert(Alert.AlertType.ERROR,
                            "Could not generate quiz questions.\nPlease try again later.\n\n" + e.getMessage());
                    err.setHeaderText(null);
                    err.showAndWait();
                });
            }
        }).start();
    }

    private void showQuizDialog(OpportuniteCarriere o, List<QuizService.QuizQuestion> questions) {
        int attempts   = quizService.getAttempts(o.getId());
        int remaining  = QuizService.MAX_ATTEMPTS - attempts;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Quiz — " + o.getTitle());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Submit Answers");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");

        Label attemptsLabel = new Label("Attempts remaining: " + remaining + " / " + QuizService.MAX_ATTEMPTS
                + "  ·  You need " + QuizService.PASSING_SCORE + "/" + QuizService.TOTAL_QUESTIONS + " to pass");
        attemptsLabel.setStyle("-fx-background-color: " + (remaining == 1 ? "#fee2e2" : "#fef9c3") + "; " +
                "-fx-text-fill: " + (remaining == 1 ? "#ef4444" : "#92400e") + "; " +
                "-fx-padding: 8 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: bold;");
        attemptsLabel.setMaxWidth(Double.MAX_VALUE);

        VBox questionsBox = new VBox(16);
        List<ToggleGroup> groups = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            QuizService.QuizQuestion q = questions.get(i);

            Label qLabel = new Label((i + 1) + ". " + q.question());
            qLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1f2e;");
            qLabel.setWrapText(true);

            VBox optionsBox = new VBox(6);
            ToggleGroup group = new ToggleGroup();
            groups.add(group);

            for (int j = 0; j < q.options().size(); j++) {
                RadioButton rb = new RadioButton(q.options().get(j));
                rb.setToggleGroup(group);
                rb.setUserData(j);
                rb.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
                optionsBox.getChildren().add(rb);
            }

            VBox qBox = new VBox(8, qLabel, optionsBox);
            qBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 16; -fx-background-radius: 8; " +
                          "-fx-border-color: #e2e8f0; -fx-border-radius: 8;");
            questionsBox.getChildren().add(qBox);
        }

        ScrollPane scroll = new ScrollPane(questionsBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(480);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(14, attemptsLabel, scroll);
        content.setPadding(new Insets(16));
        content.setPrefWidth(560);
        dialog.getDialogPane().setContent(content);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(true);
        groups.forEach(g -> g.selectedToggleProperty().addListener((obs, ov, nv) -> {
            boolean allAnswered = groups.stream().allMatch(gr -> gr.getSelectedToggle() != null);
            okBtn.setDisable(!allAnswered);
        }));

        dialog.setResultConverter(btn -> null);
        dialog.showAndWait();

        boolean submitted = groups.stream().allMatch(g -> g.getSelectedToggle() != null);
        if (!submitted) return;

        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            Toggle selected = groups.get(i).getSelectedToggle();
            if (selected != null && (int) selected.getUserData() == questions.get(i).correct()) {
                score++;
            }
        }

        quizService.incrementAttempts(o.getId());
        int newAttempts = quizService.getAttempts(o.getId());
        boolean passed = score >= QuizService.PASSING_SCORE;

        if (passed) {
            quizService.markPassed(o.getId());
            showQuizPassed(o, score);
        } else if (newAttempts >= QuizService.MAX_ATTEMPTS) {
            showQuizExhausted(o);
        } else {
            showQuizFailed(o, score, newAttempts);
        }
    }

    private void showQuizPassed(OpportuniteCarriere o, int score) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Quiz Passed!");
        alert.setHeaderText(null);
        alert.getButtonTypes().add(ButtonType.OK);
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Apply Now →");

        VBox content = new VBox(12);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 8;");

        Label icon  = new Label("✓");
        icon.setStyle("-fx-font-size: 52px; -fx-text-fill: #22c55e;");
        Label title = new Label("Congratulations!");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #15803d;");
        Label sub   = new Label("You scored " + score + "/" + QuizService.TOTAL_QUESTIONS + " — you passed the quiz!");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #166534;");
        Label hint  = new Label("You can now submit your application for " + o.getTitle() + ".");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #4ade80;");

        content.getChildren().addAll(icon, title, sub, hint);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(400);
        alert.showAndWait();
        showApplyDialog(o);
    }

    private void showQuizFailed(OpportuniteCarriere o, int score, int attempts) {
        int remaining = QuizService.MAX_ATTEMPTS - attempts;
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Quiz Failed");
        alert.setHeaderText(null);
        alert.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Try Again");
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");

        VBox content = new VBox(12);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 8;");

        Label icon  = new Label("✗");
        icon.setStyle("-fx-font-size: 52px; -fx-text-fill: #ef4444;");
        Label title = new Label("Not Quite There");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #991b1b;");
        Label sub   = new Label("You scored " + score + "/" + QuizService.TOTAL_QUESTIONS
                + " — you need at least " + QuizService.PASSING_SCORE + " to pass.");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #b91c1c;");
        Label hint  = new Label("You have " + remaining + " attempt" + (remaining != 1 ? "s" : "") + " remaining.");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc2626;");

        content.getChildren().addAll(icon, title, sub, hint);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(400);

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) startQuiz(o);
        });
    }

    private void showQuizExhausted(OpportuniteCarriere o) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "You have used all " + QuizService.MAX_ATTEMPTS + " attempts for this opportunity.\n" +
                "You cannot apply to \"" + o.getTitle() + "\" at this time.");
        alert.setTitle("All Attempts Used");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showApplyDialog(OpportuniteCarriere o) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Apply — " + o.getTitle());
        dialog.setHeaderText(null);

        Label hint = new Label("Applying to: " + nvl(o.getTitle()) +
                (o.getCompanyName() != null ? "  ·  " + o.getCompanyName() : ""));
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Label coverLabel = new Label("Cover Letter *");
        coverLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        TextArea coverLetterArea = new TextArea();
        coverLetterArea.setPromptText("Tell us why you're a great fit… (minimum 20 characters required)");
        coverLetterArea.setPrefHeight(140);
        coverLetterArea.setWrapText(true);

        Label charCount = new Label("0 / min 20 characters");
        charCount.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444;");

        VBox content = new VBox(10, hint, coverLabel, coverLetterArea, charCount);
        content.setPadding(new Insets(14));
        content.setPrefWidth(460);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Submit Application");

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(true);

        coverLetterArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.trim().length();
            boolean valid = len >= 20;
            charCount.setText(len + " / min 20 characters");
            charCount.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (valid ? "#22c55e" : "#ef4444") + ";");
            okBtn.setDisable(!valid);
        });

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? coverLetterArea.getText() : null);

        dialog.showAndWait().ifPresent(coverLetter -> {
            Demande d = new Demande(SessionManager.getCurrentUserId(), coverLetter, "pending", o.getId());
            demandeService.addEntity(d);
            loadMyApplications();
            Alert success = new Alert(Alert.AlertType.INFORMATION,
                    "Application submitted! Check 'My Applications' for status updates.");
            success.setHeaderText(null);
            success.setTitle("Applied Successfully");
            success.showAndWait();
        });
    }

    private void withdrawApplication(Demande d) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Withdraw your application for \"" + nvl(d.getOpportunityTitle()) + "\"?\n" +
                "This action cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirm Withdrawal");
        confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
            demandeService.updateStatus(d.getId(), "withdrawn");
            loadMyApplications();
        });
    }

    // ── Alerts (subscriptions) ────────────────────────────────────────────────

    private void loadAlerts() {
        alertsFlow.getChildren().clear();
        List<JobAlertSubscription> subs = alertSubService.getByUserId(SessionManager.getCurrentUserId());
        if (subs.isEmpty()) {
            alertsFlow.getChildren().add(emptyLabel("No alerts yet. Click '+ New Alert' to create one."));
        } else {
            for (JobAlertSubscription s : subs) alertsFlow.getChildren().add(buildAlertCard(s));
        }
    }

    @FXML
    private void showAddAlertDialog() {
        buildAlertDialog(null).showAndWait().ifPresent(s -> {
            s.setUserId(SessionManager.getCurrentUserId());
            alertSubService.add(s);
            loadAlerts();
        });
    }

    private VBox buildAlertCard(JobAlertSubscription s) {
        VBox card = baseCard(300);

        Label kw  = bold(s.getKeywords() != null && !s.getKeywords().isBlank()
                ? "🔍  " + s.getKeywords() : "🔍  Any keyword", 15);
        kw.setWrapText(true);

        Label type = small("Type: " + (s.getType() != null && !s.getType().isBlank() ? s.getType() : "Any"));
        Label loc  = small("📍  " + (s.getLocation() != null && !s.getLocation().isBlank() ? s.getLocation() : "Any location"));

        String badgeColor = s.isActive() ? "#22c55e" : "#94a3b8";
        Label status = badge(s.isActive() ? "Active" : "Paused", badgeColor);

        Region sp = spacer();

        HBox actions = new HBox(8);
        Button toggleBtn = actionBtn(s.isActive() ? "Pause" : "Activate",
                s.isActive() ? "#fef9c3" : "#dcfce7",
                s.isActive() ? "#92400e" : "#166534");
        toggleBtn.setOnAction(e -> { alertSubService.toggle(s.getId()); loadAlerts(); refreshNotifBadge(); });

        Button editBtn = actionBtn("Edit", "#e2e8f0", "#475569");
        editBtn.setOnAction(e -> buildAlertDialog(s).showAndWait().ifPresent(updated -> {
            updated.setUserId(SessionManager.getCurrentUserId());
            updated.setId(s.getId());
            alertSubService.update(updated);
            loadAlerts();
        }));

        Button delBtn = actionBtn("Delete", "#fee2e2", "#ef4444");
        delBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this alert?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
                alertSubService.delete(s.getId()); loadAlerts(); refreshNotifBadge();
            });
        });

        actions.getChildren().addAll(toggleBtn, editBtn, delBtn);
        card.getChildren().addAll(kw, type, loc, status, sp, actions);
        return card;
    }

    private Dialog<JobAlertSubscription> buildAlertDialog(JobAlertSubscription ex) {
        Dialog<JobAlertSubscription> dialog = new Dialog<>();
        dialog.setTitle(ex == null ? "New Job Alert" : "Edit Job Alert");
        dialog.setHeaderText(null);

        TextField kwField  = new TextField(ex != null && ex.getKeywords() != null ? ex.getKeywords() : "");
        kwField.setPromptText("e.g. Java, Python, React…");
        kwField.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("", "internship", "apprenticeship", "fulltime", "parttime", "freelance");
        typeBox.setValue(ex != null && ex.getType() != null ? ex.getType() : "");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        TextField locField = new TextField(ex != null && ex.getLocation() != null ? ex.getLocation() : "");
        locField.setPromptText("e.g. Tunis, Paris…");
        locField.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        ColumnConstraints c1 = new ColumnConstraints(100);
        ColumnConstraints c2 = new ColumnConstraints(280);
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.addRow(0, lbl("Keywords"), kwField);
        grid.addRow(1, lbl("Type"),     typeBox);
        grid.addRow(2, lbl("Location"), locField);

        Label hint = new Label("Leave fields blank to match everything.");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-padding: 0 16 8 16;");

        VBox content = new VBox(4, grid, hint);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(440);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            JobAlertSubscription s = ex != null ? ex : new JobAlertSubscription();
            s.setKeywords(kwField.getText().trim().isEmpty() ? null : kwField.getText().trim());
            s.setType(typeBox.getValue() == null || typeBox.getValue().isBlank() ? null : typeBox.getValue());
            s.setLocation(locField.getText().trim().isEmpty() ? null : locField.getText().trim());
            s.setActive(ex == null || ex.isActive());
            return s;
        });
        return dialog;
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private void loadNotifications() {
        notificationsFlow.getChildren().clear();
        List<JobAlertNotification> notifs = alertNotifService.getByUserId(SessionManager.getCurrentUserId());
        if (notifs.isEmpty()) {
            notificationsFlow.getChildren().add(emptyLabel("No notifications yet. Create a Job Alert to get started."));
        } else {
            for (JobAlertNotification n : notifs) notificationsFlow.getChildren().add(buildNotifCard(n));
        }
        int unread = (int) notifs.stream().filter(n -> !n.isRead()).count();
        notifHeader.setText("Notifications" + (unread > 0 ? "  (" + unread + " unread)" : ""));
    }

    @FXML
    private void markAllNotificationsRead() {
        alertNotifService.markAllReadForUser(SessionManager.getCurrentUserId());
        loadNotifications();
        refreshNotifBadge();
    }

    private void refreshNotifBadge() {
        int unread = alertNotifService.countUnreadForUser(SessionManager.getCurrentUserId());
        if (unread > 0) {
            notifBadge.setText("🔔 " + unread);
            notifBadge.setStyle("-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 10; " +
                    "-fx-background-color: rgba(239,68,68,0.25); -fx-text-fill: white; " +
                    "-fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            notifBadge.setText("🔔");
            notifBadge.setStyle("-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4 10; -fx-text-fill: white;");
        }
    }

    private VBox buildNotifCard(JobAlertNotification n) {
        VBox card = baseCard(300);
        String bg = n.isRead() ? "white" : "#f0f9ff";
        card.setStyle(card.getStyle().replace("white", bg));

        if (!n.isRead()) {
            Label newBadge = badge("NEW", "#3b82f6");
            card.getChildren().add(newBadge);
        }

        Label title = bold(nvl(n.getOpportunityTitle()), 15);
        title.setWrapText(true);
        Label company  = small("🏢  " + nvl(n.getCompanyName()));
        Label type     = small("Type: " + nvl(n.getOpportunityType()));
        Label location = small("📍  " + nvl(n.getOpportunityLocation()));
        Label kw       = muted("Alert: " + (n.getSubscriptionKeywords() != null ? n.getSubscriptionKeywords() : "All"));
        Label sentAt   = muted(n.getSentAt() != null
                ? n.getSentAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "");

        card.getChildren().addAll(title, company, type, location, kw, sentAt);

        if (!n.isRead()) {
            Region sp = spacer();
            Button readBtn = actionBtn("Mark as Read", "#e2e8f0", "#475569");
            readBtn.setMaxWidth(Double.MAX_VALUE);
            readBtn.setOnAction(e -> {
                alertNotifService.markRead(n.getId());
                loadNotifications();
                refreshNotifBadge();
            });
            card.getChildren().addAll(sp, readBtn);
        }
        return card;
    }

    // ── Helper for alert dialog labels / buttons ──────────────────────────────

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        return l;
    }

    private Button actionBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                   "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 14; " +
                   "-fx-background-radius: 6; -fx-cursor: hand;");
        return b;
    }

    @FXML
    private void logout() {
        SessionManager.clear();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
            Stage stage = (Stage) opportunitiesFlow.getScene().getWindow();
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToSocialHub() {
        try {
            javafx.scene.Scene scene = opportunitiesFlow.getScene();
            javafx.scene.Parent currentRoot = scene.getRoot();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/social_hub.fxml"));
            javafx.scene.Parent socialRoot = loader.load();

            javafx.scene.control.Button backBtn = new javafx.scene.control.Button("← Back to Careers");
            backBtn.setStyle(
                    "-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                    "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                    "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
            backBtn.setOnAction(e -> scene.setRoot(currentRoot));

            javafx.scene.layout.HBox topBar = new javafx.scene.layout.HBox(backBtn);
            topBar.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));
            topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            topBar.setStyle("-fx-background-color: #f0f2f5;");

            javafx.scene.layout.VBox.setVgrow(socialRoot, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.VBox fullPage = new javafx.scene.layout.VBox(topBar, socialRoot);
            fullPage.setStyle("-fx-background-color: #ececf2;");

            scene.setRoot(fullPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToCommunity() {
        try {
            javafx.scene.Scene scene = opportunitiesFlow.getScene();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/community-hub-dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox baseCard(double width) {
        VBox c = new VBox(10);
        c.setPrefWidth(width);
        c.setMinHeight(180);
        String base = "-fx-background-color: white; -fx-background-radius: 12; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 12, 0, 0, 3); -fx-padding: 22;";
        String hover = "-fx-background-color: white; -fx-background-radius: 12; " +
                       "-fx-effect: dropshadow(gaussian, rgba(79,109,245,0.22), 18, 0, 0, 4); -fx-padding: 22;";
        c.setStyle(base);
        c.setOnMouseEntered(e -> c.setStyle(hover));
        c.setOnMouseExited(e -> c.setStyle(base));
        return c;
    }

    private VBox darkCard(double width) {
        VBox c = new VBox(12);
        c.setPrefWidth(width);
        c.setMinHeight(200);
        String base = "-fx-background-color: #2d1b6e; -fx-background-radius: 14; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 16, 0, 0, 5); -fx-padding: 24;";
        String hover = "-fx-background-color: #3d2585; -fx-background-radius: 14; " +
                       "-fx-effect: dropshadow(gaussian, rgba(45,27,110,0.6), 22, 0, 0, 7); -fx-padding: 24;";
        c.setStyle(base);
        c.setOnMouseEntered(e -> c.setStyle(hover));
        c.setOnMouseExited(e -> c.setStyle(base));
        return c;
    }

    private Label darkBold(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + size + "px; -fx-font-weight: bold; -fx-text-fill: white;");
        return l;
    }

    private Label darkSmall(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.75);");
        return l;
    }

    private Label darkBadge(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                   "-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
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

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-padding: 20;");
        return l;
    }

    private Region spacer() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
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
            case "accepted"  -> "#22c55e";
            case "rejected"  -> "#ef4444";
            case "withdrawn" -> "#94a3b8";
            default          -> "#f59e0b";
        };
    }
}
