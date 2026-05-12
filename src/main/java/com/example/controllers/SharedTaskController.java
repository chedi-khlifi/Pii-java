package com.example.controllers;

import com.example.entity.SharedTask;
import com.example.entity.SharedTask.TaskCategory;
import com.example.entity.SharedTask.TaskDifficulty;
import com.example.entity.SharedTask.TaskStatus;
import com.example.entity.User;
import com.example.service.SharedTaskService;
import com.example.service.UserService;
import com.example.service.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.example.service.OpenAIService;
import com.example.service.ConfigLoader;
import com.example.service.MailingService;
import javafx.application.Platform;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

public class SharedTaskController {

    public enum Mode { SEND, INBOX, OUTBOX }

    // ── Table ──────────────────────────────────────────────────────────────
    @FXML private TableView<SharedTask>          taskTable;
    @FXML private TableColumn<SharedTask, Integer> idColumn;
    @FXML private TableColumn<SharedTask, String>  titleColumn;
    @FXML private TableColumn<SharedTask, String>  statusColumn;
    @FXML private TableColumn<SharedTask, String>  categoryColumn;
    @FXML private TableColumn<SharedTask, String>  difficultyColumn;
    @FXML private TableColumn<SharedTask, String>  sharedByColumn;
    @FXML private TableColumn<SharedTask, String>  sharedWithColumn;
    @FXML private TableColumn<SharedTask, String>  createdAtColumn;
    @FXML private TableColumn<SharedTask, Void>    actionsColumn;

    // ── Charts ─────────────────────────────────────────────────────────────
    @FXML private HBox chartsContainer;
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> difficultyBarChart;

    // ── Form ───────────────────────────────────────────────────────────────
    @FXML private Label                    screenTitle;
    @FXML private TextField                titleField;
    @FXML private TextArea                 descriptionArea;
    @FXML private ComboBox<TaskStatus>     statusComboBox;
    @FXML private ComboBox<TaskCategory>   categoryComboBox;
    @FXML private ComboBox<TaskDifficulty> difficultyComboBox;
    @FXML private ComboBox<User>           sharedByComboBox;
    @FXML private ComboBox<User>           sharedWithComboBox;
    @FXML private Label                    attachmentLabel;
    @FXML private Button                   chooseFileButton;

    // ── Filter bar ─────────────────────────────────────────────────────────
    @FXML private HBox                     filterBar;
    @FXML private ComboBox<TaskStatus>     filterStatus;
    @FXML private ComboBox<TaskCategory>   filterCategory;
    @FXML private Button                   filterBtn;
    @FXML private Button                   clearFilterBtn;

    // ── Respond section (inbox only) ───────────────────────────────────────
    @FXML private HBox   respondBar;
    @FXML private Button acceptBtn;
    @FXML private Button rejectBtn;

    // ── Send form section ──────────────────────────────────────────────────
    @FXML private VBox   sendSection;
    @FXML private VBox   receivedDetailsSection;
    @FXML private Label  receivedTitleValue;
    @FXML private Label  receivedFromValue;
    @FXML private Label  receivedStatusValue;
    @FXML private Label  receivedCategoryValue;
    @FXML private Label  receivedDifficultyValue;
    @FXML private Label  receivedDateValue;
    @FXML private TextArea receivedDescriptionArea;
    @FXML private Label  receivedAttachmentValue;
    @FXML private Button openAttachmentButton;

    // ── Buttons ────────────────────────────────────────────────────────────
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button backButton;
    @FXML private Button navNewChallenge;
    @FXML private Button navInbox;
    @FXML private Button navSentHistory;
    @FXML private Button generateAIButton;
    @FXML private ScrollPane rootScrollPane;

    // ── State ──────────────────────────────────────────────────────────────
    private SharedTaskService taskService;
    private UserService       userService;
    private MainController    mainController;
    private OpenAIService     openAIService;
    private MailingService    mailingService;
    private ObservableList<SharedTask> taskList;

    private String selectedFilePath;
    private Integer lastSenderId;
    private Mode mode = Mode.SEND;
    private boolean servicesAvailable = true;

    public void setMainController(MainController mc) { this.mainController = mc; }

    public void setMode(Mode mode) {
        this.mode = mode;
        applyMode();
    }

    @FXML
    public void initialize() {
        if (rootScrollPane != null) {
            UIUtils.makeSmooth(rootScrollPane);
        }
        try {
            taskService = new SharedTaskService();
            userService = new UserService();
        } catch (Exception e) {
            servicesAvailable = false;
            taskService = null;
            userService = null;
            System.err.println("SharedTask services init failed: " + e.getMessage());
            e.printStackTrace();
        }
        openAIService = new OpenAIService();
        mailingService = new MailingService();
        setupTableColumns();
        setupComboBoxes();
        loadUsers();
        setupTableSelectionListener();
        applyMode();
        if (!servicesAvailable) {
            showError("Le module Défis ne peut pas accéder à la base de données pour le moment.");
        }
    }

    @FXML
    public void handleGenerateAI() {
        TaskCategory category = categoryComboBox.getValue();
        TaskDifficulty difficulty = difficultyComboBox.getValue();
        String currentTitle = titleField.getText() != null ? titleField.getText().trim() : "";
        
        if (category == null) {
            showError("Veuillez d'abord sélectionner une Catégorie pour guider l'IA.");
            return;
        }

        if (generateAIButton != null) generateAIButton.setDisable(true);
        addButton.setDisable(true);
        titleField.setDisable(true);
        descriptionArea.setText("L'IA prépare un défi de type " + category.name() + "...");

        final String finalCategory = category.name();
        final String finalDifficulty = (difficulty != null) ? difficulty.name() : "MEDIUM";
        final String topic = currentTitle.isEmpty() ? "un sujet aléatoire mais pertinent" : currentTitle;

        new Thread(() -> {
            try {
                String result = openAIService.generateChallenge(topic, finalCategory, finalDifficulty);
                System.out.println("Controller received result: " + result);
                
                Platform.runLater(() -> {
                    if (result == null || result.isBlank()) {
                        descriptionArea.setText("L'IA n'a retourné aucun contenu.");
                        return;
                    }

                    // On cherche plusieurs types de séparateurs au cas où l'IA varierait
                    String content = result.trim();
                    String separator = null;
                    if (content.contains("|")) separator = "\\|";
                    else if (content.contains(" : ")) separator = " : ";
                    else if (content.contains(" - ")) separator = " - ";

                    if (separator != null) {
                        String[] parts = content.split(separator, 2);
                        titleField.setText(parts[0].trim().replace("Défi : ", "").replace("Titre : ", ""));
                        descriptionArea.setText(parts[1].trim());
                    } else {
                        // Si l'IA a mis le titre sur la première ligne
                        if (content.contains("\n")) {
                            String[] lines = content.split("\n", 2);
                            titleField.setText(lines[0].trim());
                            descriptionArea.setText(lines[1].trim());
                        } else {
                            descriptionArea.setText(content);
                        }
                    }
                    showSuccess("Défi " + finalCategory + " généré !");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur IA: " + e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    if (generateAIButton != null) generateAIButton.setDisable(false);
                    addButton.setDisable(false);
                    titleField.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    public void handleNewNav() {
        setMode(Mode.SEND);
        clearForm();
    }

    @FXML
    public void handleInboxNav() {
        setMode(Mode.INBOX);
    }

    @FXML
    public void handleSentNav() {
        setMode(Mode.OUTBOX);
    }

    @FXML
    public void handleBack() {
        if (mainController != null) {
            mainController.showCommunityHub();
        }
    }

    @FXML
    public void handleBackButton() {
        handleBack();
    }
    // ── Mode logic ─────────────────────────────────────────────────────────
    private void applyMode() {
        if (screenTitle == null) return;
        User me = UserSession.getInstance().getCurrentUser();
        
        // Reset nav button styles
        if (navNewChallenge != null) navNewChallenge.getStyleClass().setAll("secondary-button");
        if (navInbox != null) navInbox.getStyleClass().setAll("secondary-button");
        if (navSentHistory != null) navSentHistory.getStyleClass().setAll("secondary-button");

        switch (mode) {
            case SEND -> {
                if (navNewChallenge != null) navNewChallenge.getStyleClass().setAll("button");
                if (screenTitle != null) screenTitle.setText("Créer un défi");
                showSendSection(true);
                showReceivedDetails(false);
                showRespondBar(false);
                showFilterBar(false);
                showTaskTable(false);
                showActionsColumn(false);
                showCharts(false);
                if (addButton != null) { addButton.setVisible(true); addButton.setManaged(true); }
                if (updateButton != null) { updateButton.setVisible(false); updateButton.setManaged(false); }
                if (deleteButton != null) { deleteButton.setVisible(false); deleteButton.setManaged(false); }
                if (me != null && sharedByComboBox != null) { sharedByComboBox.setValue(me); sharedByComboBox.setDisable(true); }
                if (statusComboBox != null) { statusComboBox.setValue(TaskStatus.PENDING); statusComboBox.setDisable(true); }
                if (categoryComboBox != null && categoryComboBox.getValue() == null) categoryComboBox.setValue(TaskCategory.TECH_SKILLS);
                if (difficultyComboBox != null && difficultyComboBox.getValue() == null) difficultyComboBox.setValue(TaskDifficulty.MEDIUM);
                loadTasks();
            }
            case INBOX -> {
                if (navInbox != null) navInbox.getStyleClass().setAll("button");
                if (screenTitle != null) screenTitle.setText("Défis reçus");
                showSendSection(false);
                showReceivedDetails(true);
                showRespondBar(true);
                showFilterBar(true);
                showTaskTable(true);
                showActionsColumn(false);
                showCharts(false);
                if (addButton != null) { addButton.setVisible(false); addButton.setManaged(false); }
                if (updateButton != null) { updateButton.setVisible(false); updateButton.setManaged(false); }
                if (deleteButton != null) { deleteButton.setVisible(false); deleteButton.setManaged(false); }
                clearReceivedDetails();
                loadTasks();
            }
            case OUTBOX -> {
                if (navSentHistory != null) navSentHistory.getStyleClass().setAll("button");
                if (screenTitle != null) screenTitle.setText("Défis envoyés - Modifier / Supprimer");
                showSendSection(true);
                showReceivedDetails(false);
                showRespondBar(false);
                showFilterBar(true);
                showTaskTable(true);
                showActionsColumn(true);
                showCharts(true);
                if (taskTable != null) taskTable.getItems().clear();
                if (addButton != null) { addButton.setVisible(false); addButton.setManaged(false); }
                if (updateButton != null) { updateButton.setVisible(true); updateButton.setManaged(true); }
                if (deleteButton != null) { deleteButton.setVisible(true); deleteButton.setManaged(true); }
                if (me != null && sharedByComboBox != null) { sharedByComboBox.setValue(me); sharedByComboBox.setDisable(true); }
                if (statusComboBox != null) { statusComboBox.setValue(TaskStatus.PENDING); statusComboBox.setDisable(true); }
                if (filterStatus != null) filterStatus.setValue(null);
                if (filterCategory != null) filterCategory.setValue(null);
                if (clearButton != null) { clearButton.setVisible(true); clearButton.setManaged(true); }
                loadTasks();
            }
        }
    }

    private void showSendSection(boolean v)  { if (sendSection  != null) { sendSection.setVisible(v);  sendSection.setManaged(v); } }
    private void showReceivedDetails(boolean v) { if (receivedDetailsSection != null) { receivedDetailsSection.setVisible(v); receivedDetailsSection.setManaged(v); } }
    private void showRespondBar(boolean v)   { if (respondBar   != null) { respondBar.setVisible(v);   respondBar.setManaged(v); } }
    private void showFilterBar(boolean v)    { if (filterBar    != null) { filterBar.setVisible(v);    filterBar.setManaged(v); } }
    private void showTaskTable(boolean v)    { if (taskTable    != null) { taskTable.setVisible(v);    taskTable.setManaged(v); } }
    private void showActionsColumn(boolean v) { if (actionsColumn != null) { actionsColumn.setVisible(v); } }
    private void showCharts(boolean v) { if (chartsContainer != null) { chartsContainer.setVisible(v); chartsContainer.setManaged(v); } }

    private void updateCharts() {
        if (taskList == null || chartsContainer == null || !chartsContainer.isVisible()) return;

        // 1. Status PieChart
        Map<TaskStatus, Long> statusCounts = taskList.stream()
                .collect(Collectors.groupingBy(SharedTask::getStatus, Collectors.counting()));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        statusCounts.forEach((status, count) -> pieData.add(new PieChart.Data(status.name() + " (" + count + ")", count)));
        if (statusPieChart != null) statusPieChart.setData(pieData);

        // 2. Difficulty BarChart
        if (difficultyBarChart != null) {
            Map<TaskDifficulty, Long> diffCounts = taskList.stream()
                    .filter(t -> t.getDifficulty() != null)
                    .collect(Collectors.groupingBy(SharedTask::getDifficulty, Collectors.counting()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Difficulté");
            for (TaskDifficulty d : TaskDifficulty.values()) {
                series.getData().add(new XYChart.Data<>(d.name(), diffCounts.getOrDefault(d, 0L)));
            }
            difficultyBarChart.getData().clear();
            difficultyBarChart.getData().add(series);
        }
    }

    // ── Table setup ────────────────────────────────────────────────────────
    private void setupTableColumns() {
        if (taskTable == null) return;
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        if (idColumn != null) idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (titleColumn != null) titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (statusColumn != null) statusColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus().name()));
        if (categoryColumn != null) categoryColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCategory() != null ? cd.getValue().getCategory().name() : ""));
        if (difficultyColumn != null) difficultyColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDifficulty() != null ? cd.getValue().getDifficulty().name() : "—"));
        if (sharedByColumn != null) sharedByColumn.setCellValueFactory(cd -> new SimpleStringProperty(
            safeUserTableText(cd.getValue().getSharedBy())));
        if (sharedWithColumn != null) sharedWithColumn.setCellValueFactory(cd -> new SimpleStringProperty(
            safeUserTableText(cd.getValue().getSharedWith())));
        if (createdAtColumn != null) createdAtColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                ValidationUtils.formatDate(cd.getValue().getCreatedAt())));

        if (actionsColumn != null) {
            actionsColumn.setCellFactory(col -> new TableCell<>() {
                private final Button modifyButton = new Button("Modifier");
                private final Button deleteButtonRow = new Button("Supprimer");
                private final HBox box = new HBox(8, modifyButton, deleteButtonRow);

                {
                    modifyButton.getStyleClass().add("module-main-action");
                    deleteButtonRow.getStyleClass().add("danger-button");

                    modifyButton.setOnAction(e -> {
                        SharedTask sel = getCurrentRowTask();
                        if (!canEditSentTask(sel)) return;
                        taskTable.getSelectionModel().select(sel);
                        populateForm(sel);
                        showSuccess("Challenge loaded for editing.");
                    });

                    deleteButtonRow.setOnAction(e -> {
                        SharedTask sel = getCurrentRowTask();
                        if (!canDeleteSentTask(sel)) return;
                        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce défi ?", ButtonType.OK, ButtonType.CANCEL);
                        a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                            try {
                                taskService.deleteById(sel.getId());
                                loadTasks();
                                clearForm();
                                showSuccess("Défi supprimé.");
                            } catch (Exception ex) {
                                showError("Erreur: " + ex.getMessage());
                            }
                        });
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || mode != Mode.OUTBOX) {
                        setGraphic(null);
                        return;
                    }
                    SharedTask sel = getCurrentRowTask();
                    boolean editable = isEditableSentTask(sel);
                    boolean deletable = isDeletableSentTask(sel);
                    modifyButton.setDisable(!editable);
                    deleteButtonRow.setDisable(!deletable);
                    setGraphic(box);
                }

                private SharedTask getCurrentRowTask() {
                    return getTableRow() == null ? null : getTableRow().getItem();
                }
            });
        }

        // Badge-style status coloring
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(switch (item) {
                    case "PENDING"   -> "EN ATTENTE";
                    case "ACCEPTED"  -> "ACCEPTÉ";
                    case "REJECTED"  -> "REFUSÉ";
                    case "COMPLETED" -> "TERMINÉ";
                    default -> item;
                });
                setStyle(switch (item) {
                    case "PENDING"   -> "-fx-text-fill: #fd7e14; -fx-font-weight: bold;";
                    case "ACCEPTED"  -> "-fx-text-fill: #28a745; -fx-font-weight: bold;";
                    case "REJECTED"  -> "-fx-text-fill: #dc3545; -fx-font-weight: bold;";
                    case "COMPLETED" -> "-fx-text-fill: #007bff; -fx-font-weight: bold;";
                    default -> "";
                });
            }
        });

        // Difficulty badge
        difficultyColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(switch (item) {
                    case "HARD"   -> "DIFFICILE";
                    case "MEDIUM" -> "MOYEN";
                    case "EASY"   -> "FACILE";
                    default -> item;
                });
                setStyle(switch (item) {
                    case "HARD"   -> "-fx-text-fill: #dc3545; -fx-font-weight: bold;";
                    case "MEDIUM" -> "-fx-text-fill: #fd7e14; -fx-font-weight: bold;";
                    case "EASY"   -> "-fx-text-fill: #28a745;";
                    default -> "";
                });
            }
        });
    }

    private void setupComboBoxes() {
        if (statusComboBox != null) statusComboBox.setItems(FXCollections.observableArrayList(TaskStatus.values()));
        if (categoryComboBox != null) categoryComboBox.setItems(FXCollections.observableArrayList(TaskCategory.values()));
        if (difficultyComboBox != null) difficultyComboBox.setItems(FXCollections.observableArrayList(TaskDifficulty.values()));
        if (filterStatus   != null) filterStatus.setItems(FXCollections.observableArrayList(TaskStatus.values()));
        if (filterCategory != null) filterCategory.setItems(FXCollections.observableArrayList(TaskCategory.values()));

        configureUserComboBox(sharedByComboBox);
        configureUserComboBox(sharedWithComboBox);
    }

    private void configureUserComboBox(ComboBox<User> comboBox) {
        if (comboBox == null) return;

        comboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatUserForDisplay(item));
            }
        });

        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatUserForDisplay(item));
            }
        });
    }

    private String formatUserForDisplay(User user) {
        return (user.getEmail() != null && !user.getEmail().isBlank())
            ? user.getEmail()
            : "no-email";
    }

    /** Safe text for TableView cells even if User is a detached/lazy proxy. */
    private String safeUserTableText(User user) {
        if (user == null) return "";
        try {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                return user.getEmail();
            }
            if (user.getUsername() != null && !user.getUsername().isBlank()) {
                return user.getUsername();
            }
        } catch (Exception ignored) {
            // Detached proxy access can fail outside session; fallback to identifier.
        }
        try {
            return "user#" + user.getId();
        } catch (Exception ignored) {
            return "user";
        }
    }

    private void loadUsers() {
        if (userService == null) {
            if (sharedByComboBox != null) sharedByComboBox.setItems(FXCollections.observableArrayList());
            if (sharedWithComboBox != null) sharedWithComboBox.setItems(FXCollections.observableArrayList());
            return;
        }
        List<User> users = userService.findAll();
        if (sharedByComboBox != null) sharedByComboBox.setItems(FXCollections.observableArrayList(users));
        if (sharedWithComboBox != null) sharedWithComboBox.setItems(FXCollections.observableArrayList(users));

        // In send mode, preselect a recipient when possible so send works out of the box.
        if (mode == Mode.SEND) {
            User me = UserSession.getInstance().getCurrentUser();
            User preferred = users.stream()
                    .filter(u -> me == null || u.getId() != me.getId())
                    .findFirst()
                    .orElse(users.isEmpty() ? null : users.get(0));
            if (preferred != null && sharedWithComboBox != null) {
                sharedWithComboBox.setValue(preferred);
            }
        }
    }

    private void loadTasks() {
        if (taskService == null) {
            taskList = FXCollections.observableArrayList();
            if (taskTable != null) taskTable.setItems(taskList);
            updateCharts();
            return;
        }
        Integer meId = resolveCurrentDbUserId();
        User sessionUser = UserSession.getInstance().getCurrentUser();
        String sessionUsername = sessionUser != null ? sessionUser.getUsername() : null;
        String sessionEmail = sessionUser != null ? sessionUser.getEmail() : null;
        List<SharedTask> tasks;
        try {
            tasks = switch (mode) {
                case INBOX -> (meId != null) ? taskService.findInboxByUserId(meId, null) : List.of();
                case SEND, OUTBOX -> taskService.findOutboxByIdentity(meId, sessionUsername, sessionEmail, null);
            };
        } catch (Exception e) {
            System.err.println("loadTasks error: " + e.getMessage());
            tasks = List.of();
        }
        System.out.println("SharedTask load: mode=" + mode + ", userId=" + meId + ", count=" + tasks.size());
        taskList = FXCollections.observableArrayList(tasks);
        if (taskTable != null) taskTable.setItems(taskList);
        updateCharts();
    }

    private void setupTableSelectionListener() {
        if (taskTable == null) return;
        taskTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) {
                        clearReceivedDetails();
                        return;
                    }
                    populateForm(sel);
                    updateReceivedDetails(sel);
                });
    }

    private void populateForm(SharedTask t) {
        if (titleField != null) titleField.setText(t.getTitle());
        if (descriptionArea != null) descriptionArea.setText(t.getDescription() != null ? t.getDescription() : "");
        if (statusComboBox != null) statusComboBox.setValue(t.getStatus());
        if (categoryComboBox != null) categoryComboBox.setValue(t.getCategory());
        if (difficultyComboBox != null) difficultyComboBox.setValue(t.getDifficulty());
        if (sharedByComboBox != null) sharedByComboBox.setValue(t.getSharedBy());
        if (sharedWithComboBox != null) sharedWithComboBox.setValue(t.getSharedWith());
        if (attachmentLabel != null) {
            if (t.getAttachment() != null && !t.getAttachment().isBlank()) {
                attachmentLabel.setText("📎 " + new File(t.getAttachment()).getName());
                selectedFilePath = t.getAttachment();
            } else {
                attachmentLabel.setText("No attachment");
                selectedFilePath = null;
            }
        }
    }

    private void updateReceivedDetails(SharedTask t) {
        if (receivedDetailsSection == null) return;
        if (receivedTitleValue != null) receivedTitleValue.setText("Titre: " + safeText(t.getTitle()));
        if (receivedFromValue != null) receivedFromValue.setText("Expéditeur: " + safeUserTableText(t.getSharedBy()));
        if (receivedStatusValue != null) receivedStatusValue.setText("Statut: " + (t.getStatus() != null ? t.getStatus().name() : "-"));
        if (receivedCategoryValue != null) receivedCategoryValue.setText("Catégorie: " + (t.getCategory() != null ? t.getCategory().name() : "-"));
        if (receivedDifficultyValue != null) receivedDifficultyValue.setText("Difficulté: " + (t.getDifficulty() != null ? t.getDifficulty().name() : "-"));
        if (receivedDateValue != null) receivedDateValue.setText("Envoyé le: " + ValidationUtils.formatDate(t.getCreatedAt()));
        if (receivedDescriptionArea != null) receivedDescriptionArea.setText(safeText(t.getDescription()));

        String attachment = t.getAttachment();
        if (attachment != null && !attachment.isBlank()) {
            if (receivedAttachmentValue != null) receivedAttachmentValue.setText("Pièce jointe: " + new File(attachment).getName());
            if (openAttachmentButton != null) openAttachmentButton.setDisable(false);
        } else {
            if (receivedAttachmentValue != null) receivedAttachmentValue.setText("Pièce jointe: Aucune");
            if (openAttachmentButton != null) openAttachmentButton.setDisable(true);
        }

        boolean pending = t.getStatus() == TaskStatus.PENDING;
        if (acceptBtn != null) acceptBtn.setDisable(!pending);
        if (rejectBtn != null) rejectBtn.setDisable(!pending);
    }

    private void clearReceivedDetails() {
        if (receivedTitleValue != null) receivedTitleValue.setText("-");
        if (receivedFromValue != null) receivedFromValue.setText("-");
        if (receivedStatusValue != null) receivedStatusValue.setText("-");
        if (receivedCategoryValue != null) receivedCategoryValue.setText("-");
        if (receivedDifficultyValue != null) receivedDifficultyValue.setText("-");
        if (receivedDateValue != null) receivedDateValue.setText("-");
        if (receivedDescriptionArea != null) receivedDescriptionArea.setText("");
        if (receivedAttachmentValue != null) receivedAttachmentValue.setText("Pièce jointe: Aucune");
        if (openAttachmentButton != null) openAttachmentButton.setDisable(true);
        if (acceptBtn != null) acceptBtn.setDisable(true);
        if (rejectBtn != null) rejectBtn.setDisable(true);
    }

    private String safeText(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    @FXML
    public void handleOpenAttachment() {
        SharedTask sel = taskTable.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getAttachment() == null || sel.getAttachment().isBlank()) {
            showError("No attachment for this challenge.");
            return;
        }

        try {
            File file = new File(sel.getAttachment());
            if (!file.exists()) {
                showError("Attachment file not found: " + file.getAbsolutePath());
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                showError("Desktop open is not supported on this system.");
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            showError("Cannot open attachment: " + e.getMessage());
        }
    }

    // ── CRUD handlers ──────────────────────────────────────────────────────
    @FXML
    public void handleAddButton() {
        if (!validateSendForm()) return;
        try {
            SharedTask task = new SharedTask();
            task.setTitle(titleField.getText().trim());
            task.setDescription(ValidationUtils.isNotEmpty(descriptionArea.getText()) ? descriptionArea.getText().trim() : null);
            task.setStatus(TaskStatus.PENDING);
            task.setCategory(categoryComboBox.getValue());
            task.setDifficulty(difficultyComboBox.getValue());

            // Resolve sender from DB
            User sender = resolveDbUser();
            if (sender == null) { showError("No users found in database."); return; }
            lastSenderId = sender.getId();
            task.setSharedBy(sender);
            task.setSharedWith(sharedWithComboBox.getValue());

            if (taskService.existsPendingDuplicate(task.getTitle(), sender, task.getSharedWith())) {
                showError("A pending challenge with the same title already exists for this recipient.");
                return;
            }

            if (selectedFilePath != null) task.setAttachment(selectedFilePath);
            SharedTask saved = taskService.save(task);

            // Defensive check: ensure the inserted row is immediately readable.
            if (saved == null || saved.getId() <= 0 || taskService.findById(saved.getId()).isEmpty()) {
                showError("Save reported success, but challenge was not confirmed in database.");
                return;
            }

            setMode(Mode.OUTBOX);
            loadTasks();
            clearForm();
            showSuccess("Défi envoyé ! Enregistré avec l'ID " + saved.getId() + ".");

            // Send email notification
            if (saved.getSharedWith() != null && saved.getSharedWith().getEmail() != null) {
                new Thread(() -> {
                    mailingService.sendChallengeEmail(
                        saved.getSharedWith().getEmail(),
                        saved.getTitle(),
                        sender.getUsername()
                    );
                }).start();
            }
        } catch (Exception e) { showError("Error sending challenge: " + e.getMessage()); }
    }

    /** Returns current DB user. No fallback to a different account. */
    private User resolveDbUser() {
        return resolveCurrentDbUser();
    }

    /** Resolve logged user against DB without falling back to another account. */
    private User resolveCurrentDbUser() {
        if (userService == null) return null;
        User sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser == null) return null;

        try {
            if (sessionUser.getId() > 0) {
                var byId = userService.findById(sessionUser.getId());
                if (byId.isPresent()) return byId.get();
            }

            List<User> users = userService.findAll();
            for (User u : users) {
                boolean sameUsername = sessionUser.getUsername() != null
                        && u.getUsername() != null
                        && sessionUser.getUsername().equalsIgnoreCase(u.getUsername());
                boolean sameEmail = sessionUser.getEmail() != null
                        && u.getEmail() != null
                        && sessionUser.getEmail().equalsIgnoreCase(u.getEmail());
                if (sameUsername || sameEmail) {
                    return u;
                }
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private Integer resolveCurrentDbUserId() {
        User u = resolveCurrentDbUser();
        if (u != null) {
            lastSenderId = u.getId();
            return u.getId();
        }
        return lastSenderId;
    }

    @FXML
    public void handleDeleteButton() {
        if (!isDataServicesReady()) return;
        SharedTask sel = taskTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Veuillez sélectionner un défi."); return; }
        if (!canDeleteSentTask(sel)) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce défi ?", ButtonType.OK, ButtonType.CANCEL);
        a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            try {
                taskService.deleteById(sel.getId());
                loadTasks();
                clearForm();
                showSuccess("Défi supprimé avec succès.");
            } catch (Exception e) {
                showError("Erreur lors de la suppression: " + e.getMessage());
            }
        });
    }

    @FXML
    public void handleUpdateButton() {
        if (!isDataServicesReady()) return;
        SharedTask sel = taskTable.getSelectionModel().getSelectedItem();
        if (!canEditSentTask(sel)) return;
        if (!validateSendForm()) return;

        try {
            sel.setTitle(titleField.getText().trim());
            sel.setDescription(ValidationUtils.isNotEmpty(descriptionArea.getText()) ? descriptionArea.getText().trim() : null);
            sel.setCategory(categoryComboBox.getValue());
            sel.setDifficulty(difficultyComboBox.getValue());
            sel.setSharedWith(sharedWithComboBox.getValue());
            if (selectedFilePath != null) {
                sel.setAttachment(selectedFilePath);
            }
            taskService.update(sel);
            loadTasks();
            clearForm();
            showSuccess("Challenge updated!");
        } catch (Exception e) {
            showError("Error updating challenge: " + e.getMessage());
        }
    }

    @FXML
    public void handleAccept() { respond(TaskStatus.ACCEPTED); }

    @FXML
    public void handleReject() { respond(TaskStatus.REJECTED); }

    private void respond(TaskStatus response) {
        if (!isDataServicesReady()) return;
        SharedTask sel = taskTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a challenge to respond to."); return; }
        User me = UserSession.getInstance().getCurrentUser();
        if (me == null || sel.getSharedWith() == null || sel.getSharedWith().getId() != me.getId()) {
            if (!UserSession.getInstance().isAdmin()) { showError("Only the recipient can respond."); return; }
        }
        if (sel.getStatus() != TaskStatus.PENDING) { showError("This challenge has already been answered."); return; }
        try {
            taskService.respond(sel.getId(), response);
            loadTasks(); clearForm();
            showSuccess("Défi " + response.name().toLowerCase() + " !");

            // Send email notification about status update
            if (sel.getSharedBy() != null && sel.getSharedBy().getEmail() != null) {
                new Thread(() -> {
                    mailingService.sendStatusUpdateEmail(
                        sel.getSharedBy().getEmail(),
                        sel.getTitle(),
                        response.name()
                    );
                }).start();
            }
        } catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    @FXML
    public void handleFilter() {
        if (!isDataServicesReady()) return;
        Integer meId = resolveCurrentDbUserId();
        User sessionUser = UserSession.getInstance().getCurrentUser();
        String sessionUsername = sessionUser != null ? sessionUser.getUsername() : null;
        String sessionEmail = sessionUser != null ? sessionUser.getEmail() : null;
        TaskStatus st = filterStatus != null ? filterStatus.getValue() : null;
        TaskCategory cat = filterCategory != null ? filterCategory.getValue() : null;
        List<SharedTask> results = switch (mode) {
            case INBOX  -> (meId != null) ? taskService.findInboxByUserId(meId, st)  : List.of();
            case SEND, OUTBOX -> taskService.findOutboxByIdentity(meId, sessionUsername, sessionEmail, st);
        };
        if (cat != null) results = results.stream().filter(t -> t.getCategory() == cat).toList();
        taskList = FXCollections.observableArrayList(results);
        taskTable.setItems(taskList);
    }

    @FXML
    public void handleClearFilter() {
        if (filterStatus   != null) filterStatus.setValue(null);
        if (filterCategory != null) filterCategory.setValue(null);
        loadTasks();
    }

    @FXML
    public void handleChooseFileButton() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Attachment");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            if (f.length() > 10 * 1024 * 1024) { showError("File too large (max 10 MB)."); return; }
            selectedFilePath = f.getAbsolutePath();
            attachmentLabel.setText("📎 " + f.getName());
        }
    }

    @FXML
    public void handleClearButton() { clearForm(); }

    // Validation
    private boolean validateSendForm() {
        if (!isDataServicesReady()) return false;
        if (!ValidationUtils.isNotEmpty(titleField.getText())) { showError("Le titre est obligatoire."); return false; }
        if (sharedWithComboBox.getValue() == null) { showError("Veuillez sélectionner un destinataire."); return false; }
        if (categoryComboBox.getValue() == null) { showError("Veuillez sélectionner une catégorie."); return false; }
        if (difficultyComboBox.getValue() == null) { showError("Veuillez sélectionner une difficulté."); return false; }
        return true;
    }

    private boolean isDataServicesReady() {
        if (taskService == null || userService == null) {
            showError("Le module Défis est indisponible: connexion base de données manquante.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        titleField.clear(); descriptionArea.clear();
        statusComboBox.setValue(mode == Mode.SEND || mode == Mode.OUTBOX ? TaskStatus.PENDING : null);
        categoryComboBox.setValue(mode == Mode.SEND ? TaskCategory.TECH_SKILLS : null);
        difficultyComboBox.setValue(mode == Mode.SEND ? TaskDifficulty.MEDIUM : null);

        User me = UserSession.getInstance().getCurrentUser();
        User preferred = sharedWithComboBox.getItems() == null ? null : sharedWithComboBox.getItems().stream()
            .filter(u -> me == null || u.getId() != me.getId())
            .findFirst()
            .orElse(sharedWithComboBox.getItems().isEmpty() ? null : sharedWithComboBox.getItems().get(0));
        sharedWithComboBox.setValue(preferred);

        attachmentLabel.setText("No attachment"); selectedFilePath = null;
        taskTable.getSelectionModel().clearSelection();
    }

    private boolean canEditSentTask(SharedTask sel) {
        if (!isEditableSentTask(sel)) {
            if (sel == null) { showError("Select a challenge to update."); }
            else if (!isOwnedByCurrentUser(sel)) { showError("You can only update your own challenges."); }
            else { showError("Only pending challenges can be updated."); }
            return false;
        }
        return true;
    }

    private boolean canDeleteSentTask(SharedTask sel) {
        if (!isDeletableSentTask(sel)) {
            if (sel == null) { showError("Select a challenge to delete."); }
            else if (!isOwnedByCurrentUser(sel)) { showError("You can only delete your own challenges."); }
            else { showError("You can only delete pending challenges you sent."); }
            return false;
        }
        return true;
    }

    private boolean isEditableSentTask(SharedTask sel) {
        return sel != null && (isOwnedByCurrentUser(sel) || UserSession.getInstance().isAdmin())
                && (sel.getStatus() == TaskStatus.PENDING || UserSession.getInstance().isAdmin());
    }

    private boolean isDeletableSentTask(SharedTask sel) {
        return sel != null && (isOwnedByCurrentUser(sel) || UserSession.getInstance().isAdmin())
                && (sel.getStatus() == TaskStatus.PENDING || UserSession.getInstance().isAdmin());
    }

    private boolean isOwnedByCurrentUser(SharedTask sel) {
        User me = UserSession.getInstance().getCurrentUser();
        return me != null && sel != null && sel.getSharedBy() != null && sel.getSharedBy().getId() == me.getId();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Succès"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Erreur"); a.setHeaderText("Erreur"); a.setContentText(msg); a.showAndWait();
    }
}
