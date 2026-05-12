package com.example.controllers;

import com.example.entity.Claim;
import com.example.entity.Claim.ClaimPriority;
import com.example.entity.Claim.ClaimStatus;
import com.example.entity.User;
import com.example.service.ClaimService;
import com.example.service.UserService;
import com.example.service.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

public class ClaimController {

    public enum Mode { CREATE, MY_TICKETS, ADMIN }

    // ── Table ──────────────────────────────────────────────────────────────
    @FXML private TableView<Claim> claimTable;
    @FXML private TableColumn<Claim, Integer> idColumn;
    @FXML private TableColumn<Claim, String>  titleColumn;
    @FXML private TableColumn<Claim, String>  statusColumn;
    @FXML private TableColumn<Claim, String>  priorityColumn;
    @FXML private TableColumn<Claim, String>  createdByColumn;
    @FXML private TableColumn<Claim, String>  assignedToColumn;
    @FXML private TableColumn<Claim, String>  createdAtColumn;
    @FXML private TableColumn<Claim, Void>    actionsColumn;

    // ── Form ───────────────────────────────────────────────────────────────
    @FXML private Label              screenTitle;
    @FXML private Label              screenSubtitle;
    @FXML private Label              modeBadge;
    @FXML private TextField          titleField;
    @FXML private TextArea           descriptionArea;
    @FXML private ComboBox<ClaimStatus>   statusComboBox;
    @FXML private ComboBox<ClaimPriority> priorityComboBox;
    @FXML private ComboBox<User>     createdByComboBox;
    @FXML private ComboBox<User>     assignedToComboBox;
    @FXML private TextArea           adminNotesArea;
    @FXML private VBox               statusSection;
    @FXML private VBox               formCard;
    @FXML private VBox               sidebarPanel;
    @FXML private VBox               tableContainer;
    @FXML private VBox               chartContainer;
    @FXML private HBox               adminViewContainer;

    // ── Admin filter bar ───────────────────────────────────────────────────
    @FXML private HBox               filterBar;
    @FXML private TextField          searchField;
    @FXML private ComboBox<ClaimStatus>   filterStatus;
    @FXML private ComboBox<ClaimPriority> filterPriority;
    @FXML private ComboBox<User>     filterAssignee;
    @FXML private Button             searchBtn;
    @FXML private Button             clearFilterBtn;

    // ── Stats bar (admin) ──────────────────────────────────────────────────
    @FXML private HBox   statsBar;
    @FXML private Label  statTotal;
    @FXML private Label  statOpen;
    @FXML private Label  statInProgress;
    @FXML private Label  statResolved;
    @FXML private Label  statClosed;

    // ── Admin-only form fields ─────────────────────────────────────────────
    @FXML private VBox   adminSection;
    @FXML private VBox   createdBySection;

    // ── Buttons ────────────────────────────────────────────────────────────
    @FXML private Button addButton;
    @FXML private Button myTicketsButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button backButton;
    @FXML private Button navNewTicket;
    @FXML private Button navMyTickets;
    @FXML private PieChart problemChart;
    @FXML private ScrollPane rootScrollPane;

    // ── State ──────────────────────────────────────────────────────────────
    private ClaimService claimService;
    private UserService  userService;
    private MainController mainController;
    private ObservableList<Claim> claimList;
    private Integer lastTicketCreatorId;
    private Mode mode = Mode.MY_TICKETS;
    private Timeline searchDebounce;

    // ── Wiring ─────────────────────────────────────────────────────────────
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
        claimService = new ClaimService();
        userService  = new UserService();
        claimList    = FXCollections.observableArrayList();

        setupTableColumns();
        setupComboBoxes();
        loadUsers();
        styleSidebarNavigation();
        setupTableSelectionListener();
        
        // Setup debounced search to remove lag
        searchDebounce = new Timeline(new KeyFrame(Duration.millis(300), e -> handleSearch()));
        searchDebounce.setCycleCount(1);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, newVal) -> {
                searchDebounce.playFromStart(); 
            });
        }
        
        if (filterStatus != null) {
            filterStatus.valueProperty().addListener((obs, old, newVal) -> handleSearch());
        }
        
        if (filterPriority != null) {
            filterPriority.valueProperty().addListener((obs, old, newVal) -> handleSearch());
        }

        // mode applied later via setMode(); default load
        applyMode();
    }

    private void styleSidebarNavigation() {
        if (sidebarPanel == null) return;
        for (var n : sidebarPanel.getChildren()) {
            if (!(n instanceof Button b)) continue;
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            boolean active = b.getText() != null && b.getText().toLowerCase().contains("community claims");
            applySidebarButtonStyle(b, active ? "active" : "base");
            if (!active) {
                b.setOnMouseEntered(e -> applySidebarButtonStyle(b, "hover"));
                b.setOnMouseExited(e -> applySidebarButtonStyle(b, "base"));
            }
        }
    }

    private void applySidebarButtonStyle(Button b, String mode) {
        if (b == null) return;
        switch (mode) {
            case "active" -> b.setStyle("-fx-background-color: linear-gradient(to right, rgba(111, 66, 193, 0.20), rgba(111, 66, 193, 0.06)); -fx-text-fill: #5f3dc4; -fx-border-color: rgba(111, 66, 193, 0.55); -fx-border-width: 0 0 0 4; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 11 14; -fx-font-weight: 800;");
            case "hover" -> b.setStyle("-fx-background-color: rgba(111, 66, 193, 0.08); -fx-text-fill: #5f3dc4; -fx-background-radius: 12; -fx-padding: 11 14; -fx-font-weight: 700;");
            default -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #4f5566; -fx-background-radius: 12; -fx-padding: 11 14; -fx-font-weight: 700;");
        }
    }

    // ── Mode logic ─────────────────────────────────────────────────────────
    private void applyMode() {
        if (screenTitle == null) return; // not yet injected
        User me = UserSession.getInstance().getCurrentUser();

        // Default visibility: hide everything first
        if (modeBadge != null) { modeBadge.setVisible(false); modeBadge.setManaged(false); }
        if (sidebarPanel != null) { sidebarPanel.setVisible(false); sidebarPanel.setManaged(false); }
        if (adminViewContainer != null) { adminViewContainer.setVisible(false); adminViewContainer.setManaged(false); }
        
        showActionsColumn(false);
        showClaimTable(false);
        if (tableContainer != null) { tableContainer.setVisible(false); tableContainer.setManaged(false); }
        if (chartContainer != null) { chartContainer.setVisible(false); chartContainer.setManaged(false); }
        
        showFilterBar(false);
        showStatsBar(false);
        showAdminSection(false);
        showCreatedBySection(false);
        showStatusSection(false);
        
        if (formCard != null) { formCard.setVisible(false); formCard.setManaged(false); }
        if (addButton != null) { addButton.setVisible(false); addButton.setManaged(false); }
        if (myTicketsButton != null) { myTicketsButton.setVisible(false); myTicketsButton.setManaged(false); }
        if (updateButton != null) { updateButton.setVisible(false); updateButton.setManaged(false); }
        if (deleteButton != null) { deleteButton.setVisible(false); deleteButton.setManaged(false); }
        if (clearButton != null) { clearButton.setVisible(false); clearButton.setManaged(false); }
        if (backButton != null) { backButton.setVisible(true); backButton.setManaged(true); }

        // Apply specific mode visibility
        switch (mode) {
            case CREATE -> {
                screenTitle.setText("Nouveau Ticket de Support");
                if (formCard != null) { formCard.setVisible(true); formCard.setManaged(true); }
                if (addButton != null) { 
                    addButton.setVisible(true); 
                    addButton.setManaged(true); 
                    addButton.setText("🚀 Créer Ticket"); 
                }
                if (clearButton != null) {
                    clearButton.setText("Annuler");
                    clearButton.setVisible(true);
                    clearButton.setManaged(true);
                }
                if (me != null && createdByComboBox != null) { 
                    createdByComboBox.setValue(me); 
                    createdByComboBox.setDisable(true); 
                }
                if (priorityComboBox != null && priorityComboBox.getValue() == null) 
                    priorityComboBox.setValue(ClaimPriority.MEDIUM);
            }
            case MY_TICKETS -> {
                 screenTitle.setText("Mes Tickets");
                 if (adminViewContainer != null) { adminViewContainer.setVisible(true); adminViewContainer.setManaged(true); }
                 showClaimTable(true);
                 if (tableContainer != null) { tableContainer.setVisible(true); tableContainer.setManaged(true); }
                 if (chartContainer != null) { chartContainer.setVisible(true); chartContainer.setManaged(true); }
                 if (myTicketsButton != null) { myTicketsButton.setVisible(true); myTicketsButton.setManaged(true); }
             }
             case ADMIN -> {
                 screenTitle.setText("Gestion des Tickets (Admin)");
                 if (sidebarPanel != null) { sidebarPanel.setVisible(true); sidebarPanel.setManaged(true); }
                 if (adminViewContainer != null) { adminViewContainer.setVisible(true); adminViewContainer.setManaged(true); }
                
                showClaimTable(true);
                showActionsColumn(true);
                if (tableContainer != null) { tableContainer.setVisible(true); tableContainer.setManaged(true); }
                if (chartContainer != null) { chartContainer.setVisible(false); chartContainer.setManaged(false); }
                
                showFilterBar(true);
                showStatsBar(true);
                showAdminSection(true);
                showStatusSection(true);
                refreshStats();
            }
        }
        
        loadClaims();
    }

    private void showClaimTable(boolean v) {
        if (claimTable != null) { claimTable.setVisible(v); claimTable.setManaged(v); }
    }

    private void showActionsColumn(boolean v) {
        if (actionsColumn != null) actionsColumn.setVisible(v);
    }

    private void showFilterBar(boolean v)  { if (filterBar    != null) { filterBar.setVisible(v);    filterBar.setManaged(v); } }
    private void showStatsBar(boolean v)   { if (statsBar     != null) { statsBar.setVisible(v);     statsBar.setManaged(v); } }
    private void showAdminSection(boolean v){ if (adminSection != null) { adminSection.setVisible(v); adminSection.setManaged(v); } }
    private void showCreatedBySection(boolean v){ if (createdBySection != null) { createdBySection.setVisible(v); createdBySection.setManaged(v); } }
    private void showStatusSection(boolean v){ if (statusSection != null) { statusSection.setVisible(v); statusSection.setManaged(v); } }

    // ── Table setup ────────────────────────────────────────────────────────
    private void setupTableColumns() {
        if (claimTable == null) return;
        claimTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        claimTable.setMinHeight(320);
        
        // Cache for badge styles to avoid string concatenation and switch in every cell update
        final String purpleBadge = "-fx-text-fill: #6f42c1; -fx-font-weight: 800; -fx-background-color: rgba(111, 66, 193, 0.1); -fx-background-radius: 20; -fx-padding: 5 12; -fx-alignment: CENTER;";
        final String blueBadge = "-fx-text-fill: #4e73df; -fx-font-weight: 800; -fx-background-color: rgba(78, 115, 223, 0.1); -fx-background-radius: 20; -fx-padding: 5 12; -fx-alignment: CENTER;";
        final String greenBadge = "-fx-text-fill: #1cc88a; -fx-font-weight: 800; -fx-background-color: rgba(28, 200, 138, 0.1); -fx-background-radius: 20; -fx-padding: 5 12; -fx-alignment: CENTER;";
        final String grayBadge = "-fx-text-fill: #858796; -fx-font-weight: 800; -fx-background-color: rgba(133, 135, 150, 0.1); -fx-background-radius: 20; -fx-padding: 5 12; -fx-alignment: CENTER;";
        
        final String redBadge = "-fx-text-fill: #e74a3b; -fx-font-weight: 800; -fx-background-color: rgba(231, 74, 59, 0.1); -fx-background-radius: 20; -fx-padding: 5 12; -fx-alignment: CENTER;";
        final String orangeBadge = "-fx-text-fill: #fd7e14; -fx-font-weight: 800; -fx-background-color: rgba(253, 126, 20, 0.1); -fx-background-radius: 20; -fx-padding: 5 12; -fx-alignment: CENTER;";
        final String yellowBadge = "-fx-text-fill: #f6c23e; -fx-font-weight: 800; -fx-background-color: rgba(246, 194, 62, 0.1); -fx-background-radius: 20; -fx-padding: 5 12; -fx-alignment: CENTER;";
        final String cyanBadge = "-fx-text-fill: #36b9cc; -fx-font-weight: 800; -fx-background-color: rgba(54, 185, 204, 0.1); -fx-background-radius: 20; -fx-padding: 5 12; -fx-alignment: CENTER;";

        claimTable.setRowFactory(tv -> {
            TableRow<Claim> row = new TableRow<>();
            row.setStyle("-fx-background-color: white; -fx-border-color: #f1f3f5; -fx-border-width: 0 0 1 0;");
            row.hoverProperty().addListener((obs, wasHover, isHover) -> {
                if (row.isEmpty() || row.isSelected()) return;
                row.setStyle(isHover ? "-fx-background-color: #f8f9fa;" : "-fx-background-color: white; -fx-border-color: #f1f3f5; -fx-border-width: 0 0 1 0;");
            });
            return row;
        });

        if (idColumn != null) idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (titleColumn != null) titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (statusColumn != null) statusColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus().name()));
        if (priorityColumn != null) priorityColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPriority().name()));
        if (createdByColumn != null) createdByColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCreatedBy() != null ? cd.getValue().getCreatedBy().getUsername() : ""));
        if (assignedToColumn != null) assignedToColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getAssignedTo() != null ? cd.getValue().getAssignedTo().getUsername() : "Non assigné"));
        if (createdAtColumn != null) createdAtColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                ValidationUtils.formatDate(cd.getValue().getCreatedAt())));

        if (actionsColumn != null) {
            actionsColumn.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn = new Button("Gérer");
                {
                    editBtn.setStyle("-fx-background-color: #f1f3f5; -fx-text-fill: #6f42c1; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 5 15; -fx-cursor: hand;");
                    editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 5 15; -fx-cursor: hand;"));
                    editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: #f1f3f5; -fx-text-fill: #6f42c1; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 5 15; -fx-cursor: hand;"));
                    editBtn.setOnAction(e -> {
                        Claim c = getTableRow().getItem();
                        if (c != null) openAdminEditor(c);
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : editBtn);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            });
        }

        if (statusColumn != null) {
            statusColumn.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    setText(item);
                    setStyle(switch (item) {
                        case "OPEN"        -> purpleBadge;
                        case "IN_PROGRESS" -> blueBadge;
                        case "RESOLVED"    -> greenBadge;
                        case "CLOSED"      -> grayBadge;
                        default            -> purpleBadge;
                    });
                }
            });
        }

        if (priorityColumn != null) {
            priorityColumn.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    setText(item);
                    setStyle(switch (item) {
                        case "CRITICAL" -> redBadge;
                        case "HIGH"     -> orangeBadge;
                        case "MEDIUM"   -> yellowBadge;
                        case "LOW"      -> cyanBadge;
                        default         -> yellowBadge;
                    });
                }
            });
        }
    }

    private String hexToRgb(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return r + "," + g + "," + b;
    }

    private void setupComboBoxes() {
        if (statusComboBox != null) statusComboBox.setItems(FXCollections.observableArrayList(ClaimStatus.values()));
        if (priorityComboBox != null) priorityComboBox.setItems(FXCollections.observableArrayList(ClaimPriority.values()));
        if (filterStatus != null) {
            ObservableList<ClaimStatus> statusItems = FXCollections.observableArrayList();
            statusItems.add(null); // null means "Tous"
            statusItems.addAll(ClaimStatus.values());
            filterStatus.setItems(statusItems);
            filterStatus.setConverter(new StringConverter<>() {
                @Override
                public String toString(ClaimStatus object) {
                    return object == null ? "Tous" : object.name();
                }

                @Override
                public ClaimStatus fromString(String string) {
                    if (string == null || string.isBlank() || "Tous".equalsIgnoreCase(string.trim())) {
                        return null;
                    }
                    return ClaimStatus.valueOf(string.trim());
                }
            });
            filterStatus.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(ClaimStatus item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null ? "Tous" : item.name()));
                }
            });
            filterStatus.getSelectionModel().selectFirst();
        }
        if (filterPriority != null) {
            ObservableList<ClaimPriority> priorityItems = FXCollections.observableArrayList();
            priorityItems.add(null); // null means "Tous"
            priorityItems.addAll(ClaimPriority.values());
            filterPriority.setItems(priorityItems);
            filterPriority.setConverter(new StringConverter<>() {
                @Override
                public String toString(ClaimPriority object) {
                    return object == null ? "Tous" : object.name();
                }

                @Override
                public ClaimPriority fromString(String string) {
                    if (string == null || string.isBlank() || "Tous".equalsIgnoreCase(string.trim())) {
                        return null;
                    }
                    return ClaimPriority.valueOf(string.trim());
                }
            });
            filterPriority.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(ClaimPriority item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null ? "Tous" : item.name()));
                }
            });
            filterPriority.getSelectionModel().selectFirst();
        }
    }

    private void loadUsers() {
        List<User> users = userService.findAll();
        if (createdByComboBox != null) createdByComboBox.setItems(FXCollections.observableArrayList(users));
        if (assignedToComboBox != null) assignedToComboBox.setItems(FXCollections.observableArrayList(users));
        if (filterAssignee != null) filterAssignee.setItems(FXCollections.observableArrayList(users));
    }

    private void loadClaims() {
        if (claimTable == null) return;
        List<Claim> claims;
        User me = resolveCurrentDbUser();
        User sessionUser = UserSession.getInstance().getCurrentUser();
        String sessionUsername = sessionUser != null ? sessionUser.getUsername() : null;
        String sessionEmail = sessionUser != null ? sessionUser.getEmail() : null;
        Integer creatorId = me != null ? me.getId() : lastTicketCreatorId;
        try {
            if (mode == Mode.ADMIN) {
                claims = claimService.search(null, null, null, null);
            } else {
                claims = claimService.findMyTicketsByIdentity(
                        creatorId,
                        sessionUsername,
                        sessionEmail);
            }
        } catch (Exception e) {
            System.err.println("loadClaims error: " + e.getMessage());
            claims = List.of();
        }
        System.out.println("Claim load: mode=" + mode + ", creatorId=" + creatorId + ", count=" + claims.size());
        claimList = FXCollections.observableArrayList(claims);
        claimTable.setItems(claimList);
        
        updateProblemChart(); // Refresh chart even in user mode
    }

    private void setupTableSelectionListener() {
        if (claimTable == null) return;
        claimTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> { 
                    if (sel != null) {
                        if (mode == Mode.ADMIN) {
                            openAdminEditor(sel);
                        } else if (mode == Mode.MY_TICKETS) {
                            openMyTicketEditor(sel);
                        } else {
                            populateForm(sel);
                        }
                    } 
                });
    }

    private void populateForm(Claim c) {
        if (titleField != null) titleField.setText(c.getTitle());
        if (descriptionArea != null) descriptionArea.setText(c.getDescription() != null ? c.getDescription() : "");
        if (statusComboBox != null) statusComboBox.setValue(c.getStatus());
        if (priorityComboBox != null) priorityComboBox.setValue(c.getPriority());
        if (createdByComboBox != null) createdByComboBox.setValue(c.getCreatedBy());
        if (assignedToComboBox != null) assignedToComboBox.setValue(c.getAssignedTo());
        if (adminNotesArea != null) adminNotesArea.setText(c.getAdminNotes() != null ? c.getAdminNotes() : "");
    }

    private void showClaimDetails(Claim c) {
        if (c == null) return;
        String creator = c.getCreatedBy() != null ? c.getCreatedBy().getUsername() : "N/A";
        String assigned = c.getAssignedTo() != null ? c.getAssignedTo().getUsername() : "Unassigned";
        String when = ValidationUtils.formatDate(c.getCreatedAt());
        String details = "ID: " + c.getId() + "\n"
                + "Title: " + c.getTitle() + "\n"
                + "Status: " + c.getStatus() + "\n"
                + "Priority: " + c.getPriority() + "\n"
                + "Creator: " + creator + "\n"
                + "Assigned: " + assigned + "\n"
                + "Created At: " + when + "\n\n"
                + "Description:\n" + (c.getDescription() != null ? c.getDescription() : "");
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Ticket Details");
        a.setHeaderText("Ticket #" + c.getId());
        a.setContentText(details);
        a.showAndWait();
    }

    private void openAdminEditor(Claim c) {
        if (c == null) return;
        claimTable.getSelectionModel().select(c);
        populateForm(c);
        if (formCard != null) {
            formCard.setVisible(true);
            formCard.setManaged(true);
        }
        showStatusSection(true);
        showAdminSection(true);
        showCreatedBySection(true);
        
        if (addButton != null) { addButton.setVisible(false); addButton.setManaged(false); }
        if (updateButton != null) { updateButton.setVisible(true); updateButton.setManaged(true); }
        if (deleteButton != null) { deleteButton.setVisible(true); deleteButton.setManaged(true); }
        if (clearButton != null) { clearButton.setVisible(true); clearButton.setManaged(true); }
    }

    private void openMyTicketEditor(Claim c) {
        if (c == null) return;
        if (!canModifyOwnClaim(c)) return;
        claimTable.getSelectionModel().select(c);
        populateForm(c);
        if (formCard != null) {
            formCard.setVisible(true);
            formCard.setManaged(true);
        }
        showStatusSection(false);
        showAdminSection(false);
        showCreatedBySection(false);
        
        if (addButton != null) { addButton.setVisible(false); addButton.setManaged(false); }
        if (updateButton != null) { updateButton.setVisible(true); updateButton.setManaged(true); }
        if (deleteButton != null) { deleteButton.setVisible(true); deleteButton.setManaged(true); }
        if (clearButton != null) { clearButton.setVisible(true); clearButton.setManaged(true); }
    }

    // ── CRUD handlers ──────────────────────────────────────────────────────
    @FXML
    public void handleAddButton() {
        if (!validateForm()) return;
        try {
            Claim claim = new Claim();
            claim.setTitle(titleField.getText().trim());
            claim.setDescription(descriptionArea.getText().trim());
            claim.setStatus(statusComboBox != null && statusComboBox.getValue() != null ? statusComboBox.getValue() : ClaimStatus.OPEN);
            claim.setPriority(priorityComboBox != null && priorityComboBox.getValue() != null ? priorityComboBox.getValue() : ClaimPriority.MEDIUM);

            // Resolve creator: admin mode uses combobox; otherwise use session user or first DB user
            User creator;
            if (mode == Mode.ADMIN && createdByComboBox != null && createdByComboBox.getValue() != null) {
                creator = createdByComboBox.getValue();
            } else {
                creator = resolveDbUser();
            }
            if (creator == null) { showError("No users found in database. Please add a user first."); return; }
            lastTicketCreatorId = creator.getId();
            claim.setCreatedBy(creator);

            if (claimService.existsActiveDuplicateTitle(claim.getTitle(), creator)) {
                showError("A similar active ticket already exists with this title.");
                return;
            }

            if (mode == Mode.ADMIN) {
                if (assignedToComboBox != null) claim.setAssignedTo(assignedToComboBox.getValue());
                if (adminNotesArea != null && ValidationUtils.isNotEmpty(adminNotesArea.getText()))
                    claim.setAdminNotes(adminNotesArea.getText().trim());
            }
            claimService.save(claim);
            if (mode != Mode.ADMIN) {
                setMode(Mode.MY_TICKETS);
                if (creator != null) {
                    lastTicketCreatorId = creator.getId();
                }
            } else {
                loadClaims();
                clearForm();
                refreshStats();
            }
            showSuccess("Ticket created successfully!");
        } catch (Exception e) { showError("Error creating ticket: " + e.getMessage()); }
    }

    /** Resolve current user in DB (id first, then username/email). No fallback to another account. */
    private User resolveDbUser() {
        return resolveCurrentDbUser();
    }

    private User resolveCurrentDbUser() {
        User me = UserSession.getInstance().getCurrentUser();
        if (me == null) return null;

        if (me.getId() > 0) {
            try {
                var found = userService.findById(me.getId());
                if (found.isPresent()) {
                    lastTicketCreatorId = found.get().getId();
                    syncSessionUserIdentity(me, found.get());
                    return found.get();
                }
            } catch (Exception ignored) {}
        }

        try {
            List<User> all = userService.findAll();
            for (User u : all) {
                boolean sameUsername = me.getUsername() != null && u.getUsername() != null
                        && me.getUsername().equalsIgnoreCase(u.getUsername());
                boolean sameEmail = me.getEmail() != null && u.getEmail() != null
                        && me.getEmail().equalsIgnoreCase(u.getEmail());
                if (sameUsername || sameEmail) {
                    lastTicketCreatorId = u.getId();
                    syncSessionUserIdentity(me, u);
                    return u;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private void syncSessionUserIdentity(User sessionUser, User dbUser) {
        if (sessionUser == null || dbUser == null) return;
        try {
            sessionUser.setId(dbUser.getId());
            if (dbUser.getUsername() != null) sessionUser.setUsername(dbUser.getUsername());
            if (dbUser.getEmail() != null) sessionUser.setEmail(dbUser.getEmail());
        } catch (Exception ignored) {
            // Session sync is best-effort; filtering still works via dbUser.
        }
    }

    @FXML
    public void handleUpdateButton() {
        if (claimTable == null) return;
        Claim sel = claimTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a ticket to update."); return; }
        if (mode == Mode.ADMIN) {
            if (!UserSession.getInstance().isAdmin()) { showError("Only admins can update tickets."); return; }
        } else if (!canModifyOwnClaim(sel)) {
            return;
        }
        if (!validateForm()) return;
        try {
            sel.setTitle(titleField.getText().trim());
            sel.setDescription(descriptionArea.getText().trim());
            if (statusComboBox != null) sel.setStatus(statusComboBox.getValue());
            if (priorityComboBox != null) sel.setPriority(priorityComboBox.getValue());
            if (mode == Mode.ADMIN) {
                if (createdByComboBox != null) sel.setCreatedBy(createdByComboBox.getValue());
                if (assignedToComboBox != null) sel.setAssignedTo(assignedToComboBox.getValue());
                if (adminNotesArea != null) sel.setAdminNotes(adminNotesArea.getText().trim());
            }
            claimService.update(sel);
            loadClaims();
            if (mode == Mode.ADMIN) refreshStats();
            showSuccess("Ticket updated!");
        } catch (Exception e) { showError("Error updating ticket: " + e.getMessage()); }
    }

    @FXML
    public void handleDeleteButton() {
        if (claimTable == null) return;
        Claim sel = claimTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Select a ticket to delete."); return; }
        if (mode == Mode.ADMIN) {
            if (!UserSession.getInstance().isAdmin()) { showError("Only admins can delete tickets."); return; }
        } else if (!canModifyOwnClaim(sel)) {
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete this ticket? This cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirm Delete");
        a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            try {
                claimService.deleteById(sel.getId());
                loadClaims(); clearForm(); refreshStats();
                if (formCard != null) { formCard.setVisible(false); formCard.setManaged(false); }
                showSuccess("Ticket deleted.");
            } catch (Exception e) { showError("Error deleting ticket: " + e.getMessage()); }
        });
    }

    @FXML
    public void handleClearButton() {
        clearForm();
        if (mode == Mode.ADMIN || mode == Mode.MY_TICKETS) {
            if (formCard != null) {
                formCard.setVisible(false);
                formCard.setManaged(false);
            }
            if (claimTable != null) claimTable.getSelectionModel().clearSelection();
        }
    }

    @FXML
    public void handleMyTicketsButton() {
        System.out.println("handleMyTicketsButton clicked");
        setMode(Mode.MY_TICKETS);
    }

    @FXML
    public void handleNewTicketButton() {
        System.out.println("handleNewTicketButton clicked");
        clearForm();
        setMode(Mode.CREATE);
    }

    @FXML
    public void handleBackButton() { mainController.showCommunityHub(); }

    // ── Admin search/filter ────────────────────────────────────────────────
    @FXML
    public void handleSearch() {
        String kw = searchField != null ? searchField.getText() : null;
        ClaimStatus st = filterStatus != null ? filterStatus.getValue() : null;
        ClaimPriority pr = filterPriority != null ? filterPriority.getValue() : null;
        User as = filterAssignee != null ? filterAssignee.getValue() : null;
        
        List<Claim> results = claimService.search(kw, st, pr, as);
        claimList.setAll(results);
        updateProblemChart();
    }

    @FXML
    public void handleClearFilter() {
        if (searchField   != null) searchField.clear();
        if (filterStatus  != null) filterStatus.setValue(null);
        if (filterPriority!= null) filterPriority.setValue(null);
        if (filterAssignee!= null) filterAssignee.setValue(null);
        loadClaims();
    }

    // ── Stats ──────────────────────────────────────────────────────────────
    private void refreshStats() {
        long total = claimService.countAll();
        long open = claimService.countByStatus(ClaimStatus.OPEN);
        long inProgress = claimService.countByStatus(ClaimStatus.IN_PROGRESS);
        long resolved = claimService.countByStatus(ClaimStatus.RESOLVED);
        long closed = claimService.countByStatus(ClaimStatus.CLOSED);
        if (statTotal != null) statTotal.setText("Total: " + total);
        if (statOpen != null) statOpen.setText("Open: " + open);
        if (statInProgress != null) statInProgress.setText("In Progress: " + inProgress);
        if (statResolved != null) statResolved.setText("Resolved: " + resolved);
        if (statClosed != null) statClosed.setText("Closed: " + closed);
        if (modeBadge != null) modeBadge.setText(inProgress + " en cours");
        
        // Ensure table is loaded with all results if in ADMIN mode
        if (mode == Mode.ADMIN && (searchField == null || searchField.getText().isEmpty())) {
            loadClaims();
        }
    }

    // ── Validation ─────────────────────────────────────────────────────────
    private boolean validateForm() {
        if (titleField == null || descriptionArea == null) return false;
        if (!ValidationUtils.isNotEmpty(titleField.getText())) { showError("Title is required."); return false; }
        if (!ValidationUtils.isValidLength(titleField.getText(), 3, 150)) { showError("Title must be 3–150 characters."); return false; }
        if (!ValidationUtils.isNotEmpty(descriptionArea.getText())) { showError("Description is required."); return false; }
        if (!ValidationUtils.isValidLength(descriptionArea.getText(), 10, 2000)) { showError("Description must be 10–2000 characters."); return false; }
        if (statusComboBox != null && statusComboBox.getValue() == null) statusComboBox.setValue(ClaimStatus.OPEN);
        if (priorityComboBox != null && priorityComboBox.getValue() == null) { showError("Select a priority."); return false; }
        if (adminNotesArea != null && ValidationUtils.isNotEmpty(adminNotesArea.getText())
                && adminNotesArea.getText().length() > 1000) { showError("Admin notes max 1000 characters."); return false; }
        return true;
    }

    private void clearForm() {
        if (titleField != null) titleField.clear(); 
        if (descriptionArea != null) descriptionArea.clear();
        if (statusComboBox != null) statusComboBox.setValue(null); 
        if (priorityComboBox != null) priorityComboBox.setValue(null);
        if (mode != Mode.CREATE && mode != Mode.MY_TICKETS) {
            if (createdByComboBox != null) createdByComboBox.setValue(null);
            if (assignedToComboBox != null) assignedToComboBox.setValue(null);
            if (adminNotesArea != null) adminNotesArea.clear();
        }
    }

    private boolean canModifyOwnClaim(Claim sel) {
        if (sel == null) { showError("Select a ticket first."); return false; }
        User me = resolveCurrentDbUser();
        boolean isOwner = me != null && sel.getCreatedBy() != null && sel.getCreatedBy().getId() == me.getId();
        if (!isOwner && !UserSession.getInstance().isAdmin()) {
            showError("You can only modify your own tickets.");
            return false;
        }
        return true;
    }

    private void deleteClaim(Claim c) {
        if (!canModifyOwnClaim(c)) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete this ticket? This cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirm Delete");
        a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            try {
                claimService.deleteById(c.getId());
                loadClaims();
                clearForm();
                if (mode == Mode.ADMIN) refreshStats();
                if (formCard != null) { formCard.setVisible(false); formCard.setManaged(false); }
                showSuccess("Ticket deleted.");
            } catch (Exception e) { showError("Error deleting ticket: " + e.getMessage()); }
        });
    }

    private void updateProblemChart() {
        if (problemChart == null || claimTable == null) return;
        
        ObservableList<Claim> currentItems = claimTable.getItems();
        
        long low = currentItems.stream().filter(c -> c.getPriority() == ClaimPriority.LOW).count();
        long medium = currentItems.stream().filter(c -> c.getPriority() == ClaimPriority.MEDIUM).count();
        long high = currentItems.stream().filter(c -> c.getPriority() == ClaimPriority.HIGH).count();
        long critical = currentItems.stream().filter(c -> c.getPriority() == ClaimPriority.CRITICAL).count();
        
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (low > 0) pieData.add(new PieChart.Data("FAIBLE (" + low + ")", low));
        if (medium > 0) pieData.add(new PieChart.Data("MOYENNE (" + medium + ")", medium));
        if (high > 0) pieData.add(new PieChart.Data("HAUTE (" + high + ")", high));
        if (critical > 0) pieData.add(new PieChart.Data("CRITIQUE (" + critical + ")", critical));
        
        problemChart.setData(pieData);
        problemChart.setTitle("Priorités des Résultats");
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Success"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Error"); a.setHeaderText("Error"); a.setContentText(msg); a.showAndWait();
    }
}
