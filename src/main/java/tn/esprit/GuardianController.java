package tn.esprit;

import com.mindforge.util.UserSession;
import example.PlannerModule;
import example.Task;
import example.TaskController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.Entity.Guardian.FocusSession;
import tn.esprit.Entity.Guardian.AiInsight;
import tn.esprit.Entity.Guardian.Resource;
import tn.esprit.Entity.Guardian.VirtualRoom;
import tn.esprit.services.guardian.AiInsightService;
import tn.esprit.services.guardian.FocusSessionService;
import tn.esprit.services.guardian.ResourceService;
import tn.esprit.services.guardian.VirtualRoomService;
import tn.esprit.services.guardian.clients.ai.OpenAiClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuardianController {

    private final FocusSessionService focusSessionService = new FocusSessionService();
    private final VirtualRoomService virtualRoomService = new VirtualRoomService();
    private final ResourceService resourceService = new ResourceService();
    private final OpenAiClient openAiClient = new OpenAiClient();
    private final AiInsightService aiInsightService = new AiInsightService();

    private static Integer selectedRoomId;
    private static String selectedRoomName;
    private static final Map<Integer, ObservableList<String>> ROOM_CHAT_STORE = new HashMap<>();
    private List<Resource> libraryAllResources = List.of();

    @FXML private Label snapshotLabel;
    @FXML private Label moduleSnapshotLabel;

    @FXML private TextField focusIdField;
    @FXML private TextField focusDurationField;
    @FXML private ComboBox<TaskOption> focusTaskField;
    @FXML private FlowPane focusUserStatsCards;
    @FXML private VBox focusMessageContainer;
    @FXML private Label focusMessageLabel;
    @FXML private FlowPane focusCards;

    @FXML private TextField roomsIdField;
    @FXML private TextField roomsNameField;
    @FXML private TextField roomsDescriptionField;
    @FXML private TextField roomsMaxParticipantsField;
    @FXML private Label roomsMessageLabel;
    @FXML private FlowPane roomsCards;

    @FXML private TextField libraryIdField;
    @FXML private TextField libraryTitleField;
    @FXML private TextField libraryDescriptionField;
    @FXML private TextField libraryFilePathField;
    @FXML private ComboBox<String> libraryTypeField;
    @FXML private TextField librarySearchField;
    @FXML private ComboBox<String> libraryFilterTypeField;
    @FXML private VBox libraryMessageContainer;
    @FXML private Label libraryMessageLabel;
    @FXML private FlowPane libraryCards;

    @FXML private Label chatRoomTitle;
    @FXML private ListView<String> roomChatList;
    @FXML private TextField chatInputField;
    @FXML private ComboBox<ResourceOption> chatResourceSelector;

    @FXML
    private void initialize() {
        if (snapshotLabel != null) {
            snapshotLabel.setText("Guardian snapshot ready.");
        }
        if (moduleSnapshotLabel != null) {
            moduleSnapshotLabel.setText("Guardian module ready.");
        }
        if (libraryTypeField != null) {
            libraryTypeField.getItems().setAll("pdf", "summary", "cheat_sheet", "exercise");
            if (!libraryTypeField.getItems().isEmpty()) {
                libraryTypeField.getSelectionModel().selectFirst();
            }
        }
        if (libraryFilterTypeField != null) {
            libraryFilterTypeField.getItems().setAll("All", "pdf", "summary", "cheat_sheet", "exercise");
            libraryFilterTypeField.getSelectionModel().selectFirst();
        }
        loadTaskOptions();
        loadFocusSessions();
        loadRooms();
        loadResources();
        initRoomChat();
    }

    @FXML
    private void onGoHub(ActionEvent event) {
        loadView("guardian-hub.fxml", event);
    }

    @FXML
    private void onGoFocus(ActionEvent event) {
        loadView("guardian-focus.fxml", event);
    }

    @FXML
    private void onGoRooms(ActionEvent event) {
        loadView("guardian-rooms.fxml", event);
    }

    @FXML
    private void onGoLibrary(ActionEvent event) {
        loadView("guardian-library.fxml", event);
    }

    @FXML
    private void onStartFocus(ActionEvent event) {
        onGoFocus(event);
    }

    @FXML
    private void onOpenRooms(ActionEvent event) {
        onGoRooms(event);
    }

    @FXML
    private void onOpenLibrary(ActionEvent event) {
        onGoLibrary(event);
    }

    @FXML
    private void onIntegrationPlaceholder(ActionEvent event) {
        // Placeholder handler for unimplemented integrations.
    }

    @FXML
    private void onOpenPlanner(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Scene scene = source.getScene();

            PlannerModule plannerModule = new PlannerModule();
            Parent plannerRoot = plannerModule.getView();

            Button backBtn = new Button("← Back to Guardian");
            backBtn.setStyle(
                    "-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                            "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
            backBtn.setOnAction(e -> loadGuardianHub(scene));

            HBox topBar = new HBox(backBtn);
            topBar.setPadding(new Insets(10, 15, 10, 15));
            topBar.setAlignment(Pos.CENTER_LEFT);
            topBar.setStyle("-fx-background-color: #f0f2f5;");

            VBox.setVgrow(plannerRoot, Priority.ALWAYS);
            VBox fullPage = new VBox(topBar, plannerRoot);
            fullPage.setStyle("-fx-background-color: #f0f2f5;");

            scene.setRoot(fullPage);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load planner hub", e);
        }
    }

    @FXML
    private void onFocusCreate(ActionEvent event) {
        Integer duration = parseRequiredInt(focusDurationField, focusMessageLabel, "Duration is required.");
        if (duration == null) {
            return;
        }
        Integer taskId = getSelectedTaskId();
        try {
            FocusSession session = new FocusSession(
                    null,
                    duration,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusMinutes(duration),
                    "pomodoro",
                    requireUserId(),
                    taskId
            );
            focusSessionService.insert(session);
            setMessage(focusMessageLabel, "Focus session saved.", false);
            loadFocusSessions();
        } catch (Exception e) {
            setMessage(focusMessageLabel, "Failed to save focus session: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onFocusUpdate(ActionEvent event) {
        Integer id = parseRequiredInt(focusIdField, focusMessageLabel, "Session ID is required.");
        Integer duration = parseRequiredInt(focusDurationField, focusMessageLabel, "Duration is required.");
        if (id == null || duration == null) {
            return;
        }
        Integer taskId = getSelectedTaskId();
        try {
            FocusSession existing = focusSessionService.findById(id);
            if (existing == null) {
                setMessage(focusMessageLabel, "Session not found.", true);
                return;
            }
            LocalDateTime startedAt = existing.startedAt() == null ? LocalDateTime.now() : existing.startedAt();
            FocusSession updated = new FocusSession(
                    existing.id(),
                    duration,
                    startedAt,
                    startedAt.plusMinutes(duration),
                    existing.sessionType(),
                    existing.userId(),
                    taskId
            );
            focusSessionService.update(updated);
            setMessage(focusMessageLabel, "Focus session updated.", false);
            loadFocusSessions();
        } catch (Exception e) {
            setMessage(focusMessageLabel, "Failed to update focus session: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onFocusDelete(ActionEvent event) {
        Integer id = parseRequiredInt(focusIdField, focusMessageLabel, "Session ID is required.");
        if (id == null) {
            return;
        }
        try {
            focusSessionService.delete(id);
            setMessage(focusMessageLabel, "Focus session deleted.", false);
            loadFocusSessions();
        } catch (Exception e) {
            setMessage(focusMessageLabel, "Failed to delete focus session: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onFocusRefresh(ActionEvent event) {
        loadFocusSessions();
    }

    @FXML
    private void onAiFocusTips(ActionEvent event) {
        String context = buildFocusContext();
        String response = openAiClient.generateFocusTips(context);
        setMessage(focusMessageLabel, response, response.startsWith("Error"));
        logAiInsight("focus_tips", response, getSelectedTaskId());
    }

    @FXML
    private void onDailyPlan(ActionEvent event) {
        String context = buildFocusContext();
        String response = openAiClient.generateDailyPlan(context);
        setMessage(focusMessageLabel, response, response.startsWith("Error"));
        logAiInsight("daily_plan", response, getSelectedTaskId());
    }

    @FXML
    private void onWeeklyReview(ActionEvent event) {
        String context = buildFocusContext();
        String response = openAiClient.generateWeeklyReview(context);
        setMessage(focusMessageLabel, response, response.startsWith("Error"));
        logAiInsight("weekly_review", response, getSelectedTaskId());
    }

    @FXML
    private void onSendWeeklyDigest(ActionEvent event) {
        setMessage(focusMessageLabel, "Weekly digest email queued for future integration.", false);
    }

    @FXML
    private void onScheduleGoogleCalendar(ActionEvent event) {
        setMessage(focusMessageLabel, "Google Calendar scheduling is not configured.", true);
    }

    @FXML
    private void onFindFreeTimeGoogle(ActionEvent event) {
        setMessage(focusMessageLabel, "Free time lookup is not configured.", true);
    }

    @FXML
    private void onScheduleOutlook(ActionEvent event) {
        setMessage(focusMessageLabel, "Outlook scheduling is not configured.", true);
    }

    @FXML
    private void onCheckTeamsAvailability(ActionEvent event) {
        setMessage(focusMessageLabel, "Teams availability is not configured.", true);
    }

    @FXML
    private void onSendFocusReminder(ActionEvent event) {
        setMessage(focusMessageLabel, "Focus reminder queued for future integration.", false);
    }

    @FXML
    private void onSendSessionReport(ActionEvent event) {
        setMessage(focusMessageLabel, "Session report queued for future integration.", false);
    }

    @FXML
    private void onRoomsCreate(ActionEvent event) {
        String name = safeText(roomsNameField);
        if (name.isEmpty()) {
            setMessage(roomsMessageLabel, "Room name is required.", true);
            return;
        }
        Integer max = parseRequiredInt(roomsMaxParticipantsField, roomsMessageLabel, "Max participants is required.");
        if (max == null) {
            return;
        }
        try {
            VirtualRoom room = new VirtualRoom(
                    null,
                    name,
                    safeText(roomsDescriptionField),
                    true,
                    max,
                    LocalDateTime.now(),
                    requireUserId(),
                    null
            );
            virtualRoomService.insert(room);
            setMessage(roomsMessageLabel, "Room created.", false);
            loadRooms();
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to create room: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onRoomsUpdate(ActionEvent event) {
        Integer id = parseRequiredInt(roomsIdField, roomsMessageLabel, "Room ID is required.");
        Integer max = parseRequiredInt(roomsMaxParticipantsField, roomsMessageLabel, "Max participants is required.");
        if (id == null || max == null) {
            return;
        }
        String name = safeText(roomsNameField);
        if (name.isEmpty()) {
            setMessage(roomsMessageLabel, "Room name is required.", true);
            return;
        }
        try {
            VirtualRoom existing = virtualRoomService.findById(id);
            if (existing == null) {
                setMessage(roomsMessageLabel, "Room not found.", true);
                return;
            }
            if (!Objects.equals(existing.creatorId(), requireUserId())) {
                setMessage(roomsMessageLabel, "You can only update your own rooms.", true);
                return;
            }
            VirtualRoom updated = new VirtualRoom(
                    existing.id(),
                    name,
                    safeText(roomsDescriptionField),
                    existing.isActive(),
                    max,
                    existing.createdAt(),
                    existing.creatorId(),
                    existing.subjectId()
            );
            virtualRoomService.update(updated);
            setMessage(roomsMessageLabel, "Room updated.", false);
            loadRooms();
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to update room: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onRoomsDelete(ActionEvent event) {
        Integer id = parseRequiredInt(roomsIdField, roomsMessageLabel, "Room ID is required.");
        if (id == null) {
            return;
        }
        try {
            VirtualRoom existing = virtualRoomService.findById(id);
            if (existing == null) {
                setMessage(roomsMessageLabel, "Room not found.", true);
                return;
            }
            if (!Objects.equals(existing.creatorId(), requireUserId())) {
                setMessage(roomsMessageLabel, "You can only delete your own rooms.", true);
                return;
            }
            virtualRoomService.delete(id);
            setMessage(roomsMessageLabel, "Room deleted.", false);
            loadRooms();
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to delete room: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onRoomsRefresh(ActionEvent event) {
        loadRooms();
    }

    @FXML
    private void onSendRoomInvite(ActionEvent event) {
        setMessage(roomsMessageLabel, "Room invite queued for future integration.", false);
    }

    @FXML
    private void onOpenRoomChat(ActionEvent event) {
        Integer id = parseRequiredInt(roomsIdField, roomsMessageLabel, "Select a room first.");
        if (id == null) {
            return;
        }
        selectedRoomId = id;
        selectedRoomName = safeText(roomsNameField);
        loadView("guardian-room-chat.fxml", event);
    }

    @FXML
    private void onLibraryCreate(ActionEvent event) {
        String title = safeText(libraryTitleField);
        if (title.isEmpty()) {
            setMessage(libraryMessageLabel, "Title is required.", true);
            return;
        }
        String filePath = safeText(libraryFilePathField);
        if (filePath.isEmpty()) {
            setMessage(libraryMessageLabel, "File path is required.", true);
            return;
        }
        try {
            Resource resource = new Resource(
                    null,
                    title,
                    safeText(libraryDescriptionField),
                    filePath,
                    libraryTypeField == null ? "summary" : libraryTypeField.getValue(),
                    0,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null,
                    requireUserId()
            );
            resourceService.insert(resource);
            setMessage(libraryMessageLabel, "Resource saved.", false);
            loadResources();
        } catch (Exception e) {
            setMessage(libraryMessageLabel, "Failed to save resource: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onLibraryUpdate(ActionEvent event) {
        Integer id = parseRequiredInt(libraryIdField, libraryMessageLabel, "Resource ID is required.");
        if (id == null) {
            return;
        }
        String title = safeText(libraryTitleField);
        if (title.isEmpty()) {
            setMessage(libraryMessageLabel, "Title is required.", true);
            return;
        }
        try {
            Resource existing = resourceService.findById(id);
            if (existing == null) {
                setMessage(libraryMessageLabel, "Resource not found.", true);
                return;
            }
            if (!Objects.equals(existing.uploaderId(), requireUserId())) {
                setMessage(libraryMessageLabel, "You can only update your own resources.", true);
                return;
            }
            Resource updated = new Resource(
                    existing.id(),
                    title,
                    safeText(libraryDescriptionField),
                    safeText(libraryFilePathField),
                    libraryTypeField == null ? existing.type() : libraryTypeField.getValue(),
                    existing.downloadCount(),
                    existing.rating(),
                    existing.createdAt(),
                    LocalDateTime.now(),
                    existing.subjectId(),
                    existing.uploaderId()
            );
            resourceService.update(updated);
            setMessage(libraryMessageLabel, "Resource updated.", false);
            loadResources();
        } catch (Exception e) {
            setMessage(libraryMessageLabel, "Failed to update resource: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onLibraryDelete(ActionEvent event) {
        Integer id = parseRequiredInt(libraryIdField, libraryMessageLabel, "Resource ID is required.");
        if (id == null) {
            return;
        }
        try {
            Resource existing = resourceService.findById(id);
            if (existing == null) {
                setMessage(libraryMessageLabel, "Resource not found.", true);
                return;
            }
            if (!Objects.equals(existing.uploaderId(), requireUserId())) {
                setMessage(libraryMessageLabel, "You can only delete your own resources.", true);
                return;
            }
            resourceService.delete(id);
            setMessage(libraryMessageLabel, "Resource deleted.", false);
            loadResources();
        } catch (Exception e) {
            setMessage(libraryMessageLabel, "Failed to delete resource: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onLibraryRefresh(ActionEvent event) {
        loadResources();
    }

    @FXML
    private void onLibraryApplyFilters(ActionEvent event) {
        applyLibraryFilters();
    }

    @FXML
    private void onLibraryClearFilters(ActionEvent event) {
        if (librarySearchField != null) {
            librarySearchField.clear();
        }
        if (libraryFilterTypeField != null) {
            libraryFilterTypeField.getSelectionModel().selectFirst();
        }
        applyLibraryFilters();
    }

    @FXML
    private void onAiSuggestedLinks(ActionEvent event) {
        String response = openAiClient.generateSuggestedLinks("Study resources");
        setMessage(libraryMessageLabel, response, response.startsWith("Error"));
    }

    @FXML
    private void onTrendingResources(ActionEvent event) {
        String response = openAiClient.generateTrendingResources("Study resources");
        setMessage(libraryMessageLabel, response, response.startsWith("Error"));
    }

    @FXML
    private void onImportFromDrive(ActionEvent event) {
        setMessage(libraryMessageLabel, "Drive import is not configured.", true);
    }

    @FXML
    private void onSyncDropboxFolder(ActionEvent event) {
        setMessage(libraryMessageLabel, "Dropbox sync is not configured.", true);
    }

    private void loadView(String fxmlFile, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/view/" + fxmlFile)));
            Node source = (Node) event.getSource();
            source.getScene().setRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Guardian view: " + fxmlFile, e);
        }
    }

    private void loadGuardianHub(Scene scene) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/tn/esprit/view/guardian-hub.fxml")));
            scene.setRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Guardian hub", e);
        }
    }

    private void loadFocusSessions() {
        if (focusCards == null && focusUserStatsCards == null) {
            return;
        }
        try {
            int userId = requireUserId();
                List<FocusSession> sessions = focusSessionService.findAll().stream()
                    .filter(s -> s.userId() == userId)
                    .sorted(Comparator.comparing(
                        FocusSession::startedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .toList();

                Map<Integer, String> taskTitles = TaskController.getTasksByOwner(userId).stream()
                    .collect(Collectors.toMap(Task::getId, Task::getTitle, (a, b) -> a));

            if (focusCards != null) {
                focusCards.getChildren().clear();
                for (FocusSession session : sessions) {
                    String taskLabel = "-";
                    if (session.taskId() != null) {
                    String title = taskTitles.get(session.taskId());
                    taskLabel = title == null || title.isBlank()
                        ? "#" + session.taskId()
                        : title + " (#" + session.taskId() + ")";
                    }
                    VBox card = buildCard(
                            "Session #" + session.id(),
                            "Duration: " + session.duration() + " min",
                        "Task: " + taskLabel
                    );
                    card.setOnMouseClicked(e -> {
                        if (focusIdField != null) {
                            focusIdField.setText(String.valueOf(session.id()));
                        }
                        if (focusDurationField != null) {
                            focusDurationField.setText(String.valueOf(session.duration()));
                        }
                        if (focusTaskField != null) {
                            selectTaskOption(session.taskId());
                        }
                    });
                    focusCards.getChildren().add(card);
                }
            }

            if (focusUserStatsCards != null) {
                focusUserStatsCards.getChildren().clear();
                int totalMinutes = sessions.stream().mapToInt(FocusSession::duration).sum();
                VBox totalCard = buildStatCard("Total Focus Minutes", String.valueOf(totalMinutes));
                VBox countCard = buildStatCard("Sessions", String.valueOf(sessions.size()));
                focusUserStatsCards.getChildren().addAll(totalCard, countCard);

                Map<Integer, Integer> perTaskTotals = new HashMap<>();
                for (FocusSession session : sessions) {
                    if (session.taskId() == null) {
                        continue;
                    }
                    perTaskTotals.merge(session.taskId(), session.duration(), Integer::sum);
                }

                perTaskTotals.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .limit(3)
                        .forEach(entry -> {
                            String title = taskTitles.get(entry.getKey());
                            String label = title == null || title.isBlank()
                                    ? "Task #" + entry.getKey()
                                    : title;
                            VBox card = buildStatCard(label, entry.getValue() + " min");
                            focusUserStatsCards.getChildren().add(card);
                        });
            }

            if (moduleSnapshotLabel != null) {
                moduleSnapshotLabel.setText("Loaded " + sessions.size() + " focus sessions.");
            }
        } catch (Exception e) {
            setMessage(focusMessageLabel, "Failed to load focus sessions: " + e.getMessage(), true);
        }
    }

    private void loadRooms() {
        if (roomsCards == null) {
            return;
        }
        try {
            int userId = requireUserId();
            List<VirtualRoom> rooms = virtualRoomService.findByCreator(userId).stream()
                    .sorted(Comparator.comparing(
                            VirtualRoom::createdAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .toList();
            roomsCards.getChildren().clear();
            for (VirtualRoom room : rooms) {
                VBox card = buildCard(
                        "Room #" + room.id() + " • " + room.name(),
                        room.description() == null ? "" : room.description(),
                        "Max: " + room.maxParticipants() + " • Active: " + (room.isActive() ? "Yes" : "No")
                );
                card.setOnMouseClicked(e -> {
                    if (roomsIdField != null) {
                        roomsIdField.setText(String.valueOf(room.id()));
                    }
                    if (roomsNameField != null) {
                        roomsNameField.setText(room.name());
                    }
                    if (roomsDescriptionField != null) {
                        roomsDescriptionField.setText(room.description());
                    }
                    if (roomsMaxParticipantsField != null) {
                        roomsMaxParticipantsField.setText(String.valueOf(room.maxParticipants()));
                    }
                });
                roomsCards.getChildren().add(card);
            }
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to load rooms: " + e.getMessage(), true);
        }
    }

    private void loadResources() {
        if (libraryCards == null) {
            return;
        }
        try {
            int userId = requireUserId();
            List<Resource> resources = resourceService.findByUploader(userId).stream()
                    .sorted(Comparator.comparing(
                            Resource::createdAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .toList();
            libraryAllResources = resources;
            applyLibraryFilters();
        } catch (Exception e) {
            setMessage(libraryMessageLabel, "Failed to load resources: " + e.getMessage(), true);
        }
    }

    private void applyLibraryFilters() {
        if (libraryCards == null) {
            return;
        }
        String search = librarySearchField == null ? "" : safeText(librarySearchField).toLowerCase();
        String type = libraryFilterTypeField == null ? "All" : libraryFilterTypeField.getValue();

        List<Resource> filtered = libraryAllResources.stream()
                .filter(resource -> {
                    boolean matchesType = type == null || type.equals("All")
                            || (resource.type() != null && resource.type().equalsIgnoreCase(type));
                    if (!matchesType) {
                        return false;
                    }
                    if (search.isEmpty()) {
                        return true;
                    }
                    String title = resource.title() == null ? "" : resource.title().toLowerCase();
                    String desc = resource.description() == null ? "" : resource.description().toLowerCase();
                    String filePath = resource.filePath() == null ? "" : resource.filePath().toLowerCase();
                    return title.contains(search) || desc.contains(search) || filePath.contains(search);
                })
                .toList();

        libraryCards.getChildren().clear();
        for (Resource resource : filtered) {
            VBox card = buildCard(
                    "Resource #" + resource.id() + " • " + resource.title(),
                    resource.description() == null ? "" : resource.description(),
                    "Type: " + resource.type()
            );
            card.setOnMouseClicked(e -> {
                if (libraryIdField != null) {
                    libraryIdField.setText(String.valueOf(resource.id()));
                }
                if (libraryTitleField != null) {
                    libraryTitleField.setText(resource.title());
                }
                if (libraryDescriptionField != null) {
                    libraryDescriptionField.setText(resource.description());
                }
                if (libraryFilePathField != null) {
                    libraryFilePathField.setText(resource.filePath());
                }
                if (libraryTypeField != null) {
                    libraryTypeField.getSelectionModel().select(resource.type());
                }
            });
            libraryCards.getChildren().add(card);
        }
    }

    private String buildFocusContext() {
        Integer duration = parseOptionalInt(focusDurationField);
        Integer taskId = getSelectedTaskId();
        return "User " + requireUserId() + " focus session. Duration=" + (duration == null ? "" : duration)
                + " minutes, taskId=" + (taskId == null ? "" : taskId);
    }

    private void logAiInsight(String type, String response, Integer taskId) {
        if (response == null || response.isBlank()) {
            return;
        }
        String source = response.startsWith("Error") ? "rule" : "ai";
        AiInsight insight = new AiInsight(
                null,
                requireUserId(),
                taskId,
                type,
                source,
                response,
                0,
                0,
                LocalDateTime.now()
        );
        try {
            aiInsightService.insert(insight);
        } catch (Exception ignored) {
            // Ignore logging failures to keep the UX responsive if the table is missing.
        }
    }

    private void initRoomChat() {
        if (roomChatList == null) {
            return;
        }
        Integer roomId = selectedRoomId;
        if (roomId == null) {
            roomChatList.setItems(FXCollections.observableArrayList());
            if (chatRoomTitle != null) {
                chatRoomTitle.setText("Room Chat");
            }
            return;
        }

        ObservableList<String> messages = ROOM_CHAT_STORE.computeIfAbsent(roomId, id -> FXCollections.observableArrayList());
        roomChatList.setItems(messages);

        try {
            VirtualRoom room = virtualRoomService.findById(roomId);
            if (chatRoomTitle != null) {
                String name = room == null || room.name() == null ? selectedRoomName : room.name();
                chatRoomTitle.setText(name == null || name.isBlank() ? "Room Chat" : name);
            }
        } catch (Exception e) {
            if (chatRoomTitle != null) {
                chatRoomTitle.setText("Room Chat");
            }
        }

        if (chatResourceSelector != null) {
            loadChatResources();
        }
    }

    private void loadChatResources() {
        try {
            int userId = requireUserId();
            List<ResourceOption> options = resourceService.findByUploader(userId).stream()
                    .sorted(Comparator.comparing(Resource::createdAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(resource -> new ResourceOption(resource.id(), resource.title()))
                    .toList();
            chatResourceSelector.getItems().setAll(options);
        } catch (Exception e) {
            chatResourceSelector.getItems().clear();
        }
    }

    @FXML
    private void onSendChatMessage(ActionEvent event) {
        if (roomChatList == null || chatInputField == null) {
            return;
        }
        String text = safeText(chatInputField);
        if (text.isEmpty()) {
            return;
        }
        String sender = UserSession.getInstance().getEmail();
        String label = sender == null || sender.isBlank() ? "Me" : sender;
        roomChatList.getItems().add(label + ": " + text);
        chatInputField.clear();
    }

    @FXML
    private void onShareResource(ActionEvent event) {
        if (roomChatList == null || chatResourceSelector == null) {
            return;
        }
        ResourceOption selected = chatResourceSelector.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        roomChatList.getItems().add("Shared resource: " + selected);
    }

    @FXML
    private void onStartDailyCoCall(ActionEvent event) {
        addRoomSystemMessage("Daily.co call integration is not configured.");
    }

    @FXML
    private void onStartTwilioVideo(ActionEvent event) {
        addRoomSystemMessage("Twilio video integration is not configured.");
    }

    @FXML
    private void onStartAgoraVoice(ActionEvent event) {
        addRoomSystemMessage("Agora voice integration is not configured.");
    }

    private void addRoomSystemMessage(String message) {
        if (roomChatList == null) {
            return;
        }
        roomChatList.getItems().add("System: " + message);
    }

    private void loadTaskOptions() {
        if (focusTaskField == null) {
            return;
        }
        int userId = requireUserId();
        List<TaskOption> options = TaskController.getTasksByOwner(userId).stream()
                .sorted(Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(task -> new TaskOption(task.getId(), task.getTitle()))
                .collect(Collectors.toList());
        focusTaskField.getItems().setAll(options);
    }

    private Integer getSelectedTaskId() {
        if (focusTaskField == null) {
            return null;
        }
        TaskOption selected = focusTaskField.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.id;
    }

    private void selectTaskOption(Integer taskId) {
        if (focusTaskField == null || taskId == null) {
            return;
        }
        for (TaskOption option : focusTaskField.getItems()) {
            if (option.id == taskId) {
                focusTaskField.getSelectionModel().select(option);
                return;
            }
        }
        focusTaskField.getSelectionModel().clearSelection();
    }

    private int requireUserId() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            throw new IllegalStateException("User is not logged in.");
        }
        return session.getUserId();
    }

    private Integer parseRequiredInt(TextField field, Label messageLabel, String errorMessage) {
        String raw = safeText(field);
        if (raw.isEmpty()) {
            setMessage(messageLabel, errorMessage, true);
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            setMessage(messageLabel, "Invalid number: " + raw, true);
            return null;
        }
    }

    private Integer parseOptionalInt(TextField field) {
        if (field == null) {
            return null;
        }
        String raw = safeText(field);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class TaskOption {
        private final int id;
        private final String title;

        private TaskOption(int id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return id + " - " + title;
        }
    }

    private static final class ResourceOption {
        private final int id;
        private final String title;

        private ResourceOption(int id, String title) {
            this.id = id;
            this.title = title == null ? "" : title;
        }

        @Override
        public String toString() {
            return "#" + id + " - " + title;
        }
    }

    private String safeText(TextField field) {
        if (field == null) {
            return "";
        }
        String value = field.getText();
        return value == null ? "" : value.trim();
    }

    private void setMessage(Label label, String message, boolean error) {
        if (label == null) {
            return;
        }
        label.setText(message);
        label.setStyle(error
                ? "-fx-text-fill: #b91c1c; -fx-font-size: 12px;"
                : "-fx-text-fill: #15803d; -fx-font-size: 12px;");
    }

    private VBox buildCard(String title, String line1, String line2) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2937;");
        Label line1Label = new Label(line1);
        line1Label.setStyle("-fx-text-fill: #4b5563;");
        Label line2Label = new Label(line2);
        line2Label.setStyle("-fx-text-fill: #4b5563;");
        VBox card = new VBox(4, titleLabel, line1Label, line2Label);
        card.setStyle("-fx-background-color: #fbfcff; -fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10; -fx-cursor: hand;");
        card.setMinWidth(280);
        return card;
    }

    private VBox buildStatCard(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 16px; -fx-font-weight: bold;");
        VBox card = new VBox(4, titleLabel, valueLabel);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10;");
        card.setMinWidth(160);
        return card;
    }
}
