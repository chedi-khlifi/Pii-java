package tn.esprit;

import com.mindforge.util.UserSession;
import example.PlannerModule;
import example.Task;
import example.TaskController;
import javafx.collections.FXCollections;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.Entity.Guardian.FocusSession;
import tn.esprit.Entity.Guardian.AiInsight;
import tn.esprit.Entity.Guardian.Resource;
import tn.esprit.Entity.Guardian.RoomMessage;
import tn.esprit.Entity.Guardian.VirtualRoom;
import tn.esprit.services.guardian.AiInsightService;
import tn.esprit.services.guardian.ExternalLearningResourceService;
import tn.esprit.services.guardian.FocusSessionService;
import tn.esprit.services.guardian.GuardianAiAssistant;
import tn.esprit.services.guardian.ResourceService;
import tn.esprit.services.guardian.RoomMessageService;
import tn.esprit.services.guardian.VirtualRoomService;
import tn.esprit.services.guardian.clients.ai.OpenAiClient;
import tn.esprit.services.guardian.clients.video.DailyCoClient;
import tn.esprit.services.guardian.clients.video.TwilioClient;
import tn.esprit.services.guardian.clients.video.AgoraClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final GuardianAiAssistant guardianAiAssistant = new GuardianAiAssistant();
    private final ExternalLearningResourceService externalLearningService = new ExternalLearningResourceService();
    private final OpenAiClient openAiClient = new OpenAiClient(); // kept for legacy chat methods
    private final DailyCoClient dailyCoClient = new DailyCoClient();
    private final TwilioClient twilioClient = new TwilioClient();
    private final AgoraClient agoraClient = new AgoraClient();
    private final AiInsightService aiInsightService = new AiInsightService();
    private final RoomMessageService roomMessageService = new RoomMessageService();

    private static Integer selectedRoomId;
    private static String selectedRoomName;
    private List<Resource> libraryAllResources = List.of();
    private static final DateTimeFormatter ROOM_CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

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
    @FXML private ComboBox<String> roomsMaxParticipantsField;
    @FXML private ComboBox<String> roomsSubjectFilter;
    @FXML private CheckBox roomsIsActiveField;
    @FXML private Label roomsMessageLabel;
    @FXML private FlowPane roomsCards;

    @FXML private TextField libraryIdField;
    @FXML private TextField libraryTitleField;
    @FXML private TextField libraryDescriptionField;
    @FXML private TextField libraryFilePathField;
    @FXML private TextField libraryRatingField;
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

    // ── Focus Timer UI (new Pomodoro layout) ─────────────────────────────────
    @FXML private Label    timerDisplay;
    @FXML private Label    timerStatus;
    @FXML private Label    labelTodaySessions;
    @FXML private Label    labelWeekMin;
    @FXML private Label    labelTotalMin;
    @FXML private Label    aiFocusTipsLabel;
    @FXML private Label    aiDailyPlanLabel;
    @FXML private Label    aiWeeklyReviewLabel;
    @FXML private CheckBox markDoneOnSave;
    @FXML private Label    sessionCountLabel;

    // ── Library new fields ────────────────────────────────────────────────────
    @FXML private ComboBox<String> librarySubjectFilter;
    @FXML private FlowPane         externalSuggestionsCards;
    @FXML private Label            externalSuggestionsLabel;
    // ── Resource upload page fields ───────────────────────────────────────────
    @FXML private TextArea         uploadStudentDemandField;
    @FXML private Label            aiModeBadge;
    // ── Room detail page fields ───────────────────────────────────────────────
    @FXML private Button           btnJoinRoom;
    @FXML private Button           btnLeaveRoom;
    @FXML private Label            roomCreatorLabel;
    @FXML private Label            roomCreatedLabel;
    @FXML private Label            roomCapacityLabel;
    @FXML private Label            roomStatusLabel;
    @FXML private Label            roomParticipantCountLabel;
    @FXML private FlowPane         roomParticipantsPane;
    @FXML private FlowPane         roomResourcesPane;

    // ── Pomodoro timer state ──────────────────────────────────────────────────
    private javafx.animation.Timeline pomodoroTimeline;
    private int pomodoroSecondsLeft = 25 * 60;
    private boolean pomodoroRunning = false;

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
        if (librarySubjectFilter != null) {
            librarySubjectFilter.getItems().setAll("-- All subjects --");
            librarySubjectFilter.getSelectionModel().selectFirst();
        }
        // ── VirtualRoomType: populate maxParticipants ComboBox with Symfony form choices ──
        if (roomsMaxParticipantsField != null) {
            if (roomsMaxParticipantsField.getItems().isEmpty()) {
                roomsMaxParticipantsField.getItems().setAll("5", "10", "15", "20");
            }
            if (roomsMaxParticipantsField.getValue() == null) {
                roomsMaxParticipantsField.getSelectionModel().select("10");
            }
        }
        // ── Resource upload page: show AI mode badge ──────────────────────────
        if (aiModeBadge != null) {
            boolean aiAvailable = guardianAiAssistant.isExternalAiAvailable();
            aiModeBadge.setText(aiAvailable ? "External AI mode active" : "Local generator mode active");
            aiModeBadge.setStyle(aiAvailable
                    ? "-fx-background-color: rgba(34,197,94,0.2); -fx-text-fill: #166534; -fx-background-radius: 10; -fx-padding: 3 10; -fx-font-size: 11px;"
                    : "-fx-background-color: rgba(245,158,11,0.2); -fx-text-fill: #92400e; -fx-background-radius: 10; -fx-padding: 3 10; -fx-font-size: 11px;");
        }
        // Initialise timer display to default duration
        updateTimerDisplay();
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
    private void onGoFocusStats(ActionEvent event) {
        loadView("guardian-focus-stats.fxml", event);
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

    /** Opens the resource upload page — mirrors guardian_resource_upload route. */
    @FXML
    private void onGoResourceUpload(ActionEvent event) {
        loadView("guardian-resource-upload.fxml", event);
    }

    /**
     * AI resource generation from the upload page.
     * Mirrors: resource_upload.html.twig "Generate Resource" button → guardian_resource_ai_generate.
     * Uses GuardianAiAssistant.generateLearningResource() + validateLearningResourceRequest().
     */
    @FXML
    private void onLibraryGenerateAi(ActionEvent event) {
        String title       = safeText(libraryTitleField);
        String description = uploadStudentDemandField != null
                ? uploadStudentDemandField.getText().trim() : "";
        String studentDemand = description; // same field used as student_demand
        String type        = libraryTypeField != null && libraryTypeField.getValue() != null
                ? libraryTypeField.getValue() : "summary";

        if (title.isBlank()) {
            setMessage(libraryMessageLabel, "Please enter a title before generating.", true);
            return;
        }
        if (description.isBlank()) {
            setMessage(libraryMessageLabel, "Please fill in the student demand field.", true);
            return;
        }

        // Validate content safety (mirrors validateLearningResourceRequest)
        java.util.Map<String, Object> validation =
                guardianAiAssistant.validateLearningResourceRequest("General", description, studentDemand, type);
        if (!Boolean.TRUE.equals(validation.get("valid"))) {
            setMessage(libraryMessageLabel, (String) validation.getOrDefault("message", "Request not allowed."), true);
            return;
        }

        setMessage(libraryMessageLabel, "Generating resource with AI...", false);

        javafx.concurrent.Task<java.util.Map<String, Object>> task = new javafx.concurrent.Task<>() {
            @Override protected java.util.Map<String, Object> call() {
                return guardianAiAssistant.generateLearningResource(java.util.Map.of(
                        "subject",        title,
                        "description",    description,
                        "student_demand", studentDemand,
                        "resource_type",  type,
                        "title_hint",     title));
            }
        };
        task.setOnSucceeded(e -> {
            java.util.Map<String, Object> result = task.getValue();
            String genTitle   = (String) result.getOrDefault("title", title);
            String content    = (String) result.getOrDefault("content", "");
            String source     = (String) result.getOrDefault("source", "local");
            String sourceLabel = "ai".equals(source) ? "External AI" : "Local Generator";

            // Write content to a temp file and set as file path
            try {
                java.io.File tmpFile = java.io.File.createTempFile(
                        genTitle.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(20, genTitle.length())), ".md");
                java.nio.file.Files.writeString(tmpFile.toPath(), content);
                if (libraryFilePathField != null) libraryFilePathField.setText(tmpFile.getAbsolutePath());
                if (libraryTitleField != null)    libraryTitleField.setText(genTitle);
                setMessage(libraryMessageLabel,
                        "Generated (" + sourceLabel + "): " + genTitle + " — review and click 'Add resource' to save.", false);
            } catch (Exception ex) {
                setMessage(libraryMessageLabel, "Generated but could not write file: " + ex.getMessage(), true);
            }
        });
        task.setOnFailed(e -> setMessage(libraryMessageLabel, "AI generation failed.", true));
        new Thread(task, "ai-resource-gen").start();
    }

    /**
     * Join the currently selected room.
     * Mirrors: guardian_room_join POST route.
     */
    @FXML
    private void onRoomJoin(ActionEvent event) {
        if (selectedRoomId == null) {
            setMessage(roomsMessageLabel, "Select a room first.", true);
            return;
        }
        try {
            int userId = requireUserId();
            if (virtualRoomService.isParticipant(selectedRoomId, userId)) {
                setMessage(roomsMessageLabel, "You are already a member of this room.", false);
                return;
            }
            VirtualRoom room = virtualRoomService.findById(selectedRoomId);
            if (room == null) { setMessage(roomsMessageLabel, "Room not found.", true); return; }
            if (!room.isActive()) { setMessage(roomsMessageLabel, "This room is closed.", true); return; }
            int count = virtualRoomService.countParticipants(selectedRoomId);
            if (count >= room.maxParticipants()) { setMessage(roomsMessageLabel, "This room is full.", true); return; }
            virtualRoomService.joinRoom(selectedRoomId, userId);
            setMessage(roomsMessageLabel, "You joined the room successfully.", false);
            loadRooms();
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to join room: " + e.getMessage(), true);
        }
    }

    /**
     * Leave the currently selected room.
     * Mirrors: guardian_room_leave POST route — if creator leaves, room is closed.
     */
    @FXML
    private void onRoomLeave(ActionEvent event) {
        if (selectedRoomId == null) {
            setMessage(roomsMessageLabel, "Select a room first.", true);
            return;
        }
        try {
            int userId = requireUserId();
            VirtualRoom room = virtualRoomService.findById(selectedRoomId);
            if (room == null) { setMessage(roomsMessageLabel, "Room not found.", true); return; }
            virtualRoomService.leaveRoom(selectedRoomId, userId);
            // If creator leaves → close the room (mirrors Symfony leaveRoom logic)
            if (Objects.equals(room.creatorId(), userId)) {
                virtualRoomService.closeRoom(selectedRoomId);
                setMessage(roomsMessageLabel, "You left. The room is now closed (you were the creator).", false);
            } else {
                setMessage(roomsMessageLabel, "You left the room.", false);
            }
            loadRooms();
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to leave room: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onIntegrationPlaceholder(ActionEvent event) {
        // Placeholder handler for unimplemented integrations.
    }

    @FXML
    private void onOpenCommunity(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/community-hub-dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            source.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to load Community Hub", e);
        }
    }

    @FXML
    private void onOpenSocialHub(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Scene scene = source.getScene();
            Parent guardianRoot = scene.getRoot();

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/social_hub.fxml"));
            Parent socialRoot = loader.load();

            Button backBtn = new Button("← Back to Guardian");
            backBtn.setStyle(
                    "-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                    "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                    "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
            backBtn.setOnAction(e -> scene.setRoot(guardianRoot));

            HBox topBar = new HBox(backBtn);
            topBar.setPadding(new Insets(10, 15, 10, 15));
            topBar.setAlignment(Pos.CENTER_LEFT);
            topBar.setStyle("-fx-background-color: #f0f2f5;");

            VBox.setVgrow(socialRoot, Priority.ALWAYS);
            VBox fullPage = new VBox(topBar, socialRoot);
            fullPage.setStyle("-fx-background-color: #ececf2;");

            scene.setRoot(fullPage);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Social Hub", e);
        }
    }

    @FXML
    private void onOpenPlanner(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Scene scene = source.getScene();
            final Parent guardianRoot = scene.getRoot();

            PlannerModule plannerModule = new PlannerModule();
            Parent plannerRoot = plannerModule.getView();

            Button backBtn = new Button("← Back to Guardian");
            backBtn.setStyle(
                    "-fx-background-color: #4e64f4; -fx-text-fill: white;" +
                    "-fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand;" +
                    "-fx-font-weight: bold; -fx-font-size: 13px;"
            );
            backBtn.setOnAction(e -> scene.setRoot(guardianRoot));

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

            // ── Mirror Symfony: update task actual_minutes + status ──────────
            if (taskId != null) {
                List<Task> tasks = TaskController.getTasksByOwner(requireUserId());
                tasks.stream().filter(t -> t.getId() == taskId).findFirst().ifPresent(task -> {
                    // Determine new status
                    boolean markDone = markDoneOnSave != null && markDoneOnSave.isSelected();
                    String newStatus = task.getStatus();
                    if (markDone) {
                        newStatus = "done";
                    } else if (!"done".equalsIgnoreCase(task.getStatus())) {
                        // Auto-progress: if estimated > 0 and actual >= estimated → done
                        int estimated = task.getEstimatedMinutes();
                        if (estimated > 0) {
                            // We don't track actual_minutes in the Java Task model yet,
                            // so just mark in_progress if it was todo
                            if ("todo".equalsIgnoreCase(task.getStatus())) {
                                newStatus = "in_progress";
                            }
                        }
                    }
                    if (!newStatus.equals(task.getStatus())) {
                        TaskController.updateTask(task.getId(), task.getTitle(),
                                task.getDescription(), newStatus, task.getPriority(),
                                task.getDueDate(), task.getEstimatedMinutes());
                    }
                });
            }

            // Stop timer after saving
            if (pomodoroTimeline != null) pomodoroTimeline.stop();
            pomodoroRunning = false;
            if (timerStatus != null) timerStatus.setText("Session saved! ✅");

            setMessage(focusMessageLabel, "Focus session saved successfully.", false);
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

    // ── Pomodoro timer controls ───────────────────────────────────────────────

    @FXML
    private void onFocusStart(ActionEvent event) {
        if (pomodoroRunning) return;
        // Read duration from field
        try {
            int mins = Integer.parseInt(focusDurationField.getText().trim());
            pomodoroSecondsLeft = mins * 60;
        } catch (NumberFormatException ignored) {
            pomodoroSecondsLeft = 25 * 60;
        }
        pomodoroRunning = true;
        updateTimerDisplay();
        if (timerStatus != null) timerStatus.setText("Focusing...");
        pomodoroTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                if (pomodoroSecondsLeft > 0) {
                    pomodoroSecondsLeft--;
                    updateTimerDisplay();
                } else {
                    pomodoroTimeline.stop();
                    pomodoroRunning = false;
                    if (timerStatus != null) timerStatus.setText("Session complete! Save it.");
                }
            })
        );
        pomodoroTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pomodoroTimeline.play();
    }

    @FXML
    private void onFocusPause(ActionEvent event) {
        if (pomodoroTimeline != null) pomodoroTimeline.pause();
        pomodoroRunning = false;
        if (timerStatus != null) timerStatus.setText("Paused.");
    }

    @FXML
    private void onFocusReset(ActionEvent event) {
        if (pomodoroTimeline != null) pomodoroTimeline.stop();
        pomodoroRunning = false;
        try {
            int mins = Integer.parseInt(focusDurationField.getText().trim());
            pomodoroSecondsLeft = mins * 60;
        } catch (NumberFormatException ignored) {
            pomodoroSecondsLeft = 25 * 60;
        }
        updateTimerDisplay();
        if (timerStatus != null) timerStatus.setText("Ready to focus");
    }

    @FXML
    private void onFocusAutoFillDuration(ActionEvent event) {
        // Auto-fill duration based on selected task priority
        if (focusTaskField != null && focusTaskField.getValue() != null) {
            TaskOption selected = focusTaskField.getValue();
            // Find the task priority and map to Pomodoro duration
            List<Task> tasks = TaskController.getTasks();
            tasks.stream()
                .filter(t -> t.getId() == selected.id)
                .findFirst()
                .ifPresent(t -> {
                    int mins = switch (t.getPriority()) {
                        case 3 -> 50; // high priority → longer session
                        case 1 -> 15; // low priority → short session
                        default -> 25; // medium → standard Pomodoro
                    };
                    if (focusDurationField != null) focusDurationField.setText(String.valueOf(mins));
                    pomodoroSecondsLeft = mins * 60;
                    updateTimerDisplay();
                });
        }
    }

    private void updateTimerDisplay() {
        if (timerDisplay == null) return;
        int mins = pomodoroSecondsLeft / 60;
        int secs = pomodoroSecondsLeft % 60;
        timerDisplay.setText(String.format("%02d:%02d", mins, secs));
    }

    @FXML
    private void onAiFocusTips(ActionEvent event) {
        // GuardianAiAssistant.getFocusTips() — structured JSON: tips[] + motivation
        Integer taskId = getSelectedTaskId();
        int userId = requireUserId();
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override protected String call() throws Exception {
                int totalMin      = focusSessionService.getTotalDurationByUser(userId);
                int todaySessions = focusSessionService.getTodaySessionCountByUser(userId);
                int weekMin       = focusSessionService.getWeekDurationByUser(userId);
                java.util.Map<String, Object> stats = statsMap(todaySessions, weekMin, totalMin);
                java.util.Map<String, Object> taskCtx = buildTaskContext(taskId);
                java.util.Map<String, Object> result  = guardianAiAssistant.getFocusTips(taskCtx, stats);
                @SuppressWarnings("unchecked")
                java.util.List<String> tips = (java.util.List<String>) result.get("tips");
                String motivation = (String) result.getOrDefault("motivation", "");
                String display = "• " + String.join("\n• ", tips);
                if (!motivation.isBlank()) display += "\n\n💬 " + motivation;
                return display;
            }
        };
        task.setOnSucceeded(e -> {
            String display = task.getValue();
            if (aiFocusTipsLabel != null) aiFocusTipsLabel.setText(display);
            else setAiMessage("AI Focus Tips", display, false);
            logAiInsight("focus_tips", display, taskId);
        });
        task.setOnFailed(e -> {
            String fallback = openAiClient.generateFocusTips(buildFocusContext());
            if (aiFocusTipsLabel != null) aiFocusTipsLabel.setText(fallback);
            else setAiMessage("AI Focus Tips", fallback, true);
        });
        new Thread(task, "ai-focus-tips").start();
    }

    @FXML
    private void onDailyPlan(ActionEvent event) {
        // GuardianAiAssistant.buildDailyPlan() — structured JSON: plan[]
        int userId = requireUserId();
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override protected String call() throws Exception {
                int totalMin      = focusSessionService.getTotalDurationByUser(userId);
                int todaySessions = focusSessionService.getTodaySessionCountByUser(userId);
                int weekMin       = focusSessionService.getWeekDurationByUser(userId);
                java.util.Map<String, Object> stats = statsMap(todaySessions, weekMin, totalMin);
                java.util.List<java.util.Map<String, Object>> taskList =
                        TaskController.getTasksByOwner(userId).stream().limit(10)
                        .map(t -> taskMap(t.getId(), t.getTitle(), t.getPriority(), t.getEstimatedMinutes()))
                        .collect(java.util.stream.Collectors.toList());
                java.util.Map<String, Object> result = guardianAiAssistant.buildDailyPlan(taskList, stats);
                @SuppressWarnings("unchecked")
                java.util.List<String> plan = (java.util.List<String>) result.get("plan");
                return "• " + String.join("\n• ", plan);
            }
        };
        task.setOnSucceeded(e -> {
            String display = task.getValue();
            if (aiDailyPlanLabel != null) aiDailyPlanLabel.setText(display);
            else setAiMessage("Your Daily Plan", display, false);
            logAiInsight("daily_plan", display, null);
        });
        task.setOnFailed(e -> {
            String fallback = openAiClient.generateDailyPlan(buildFocusContext());
            if (aiDailyPlanLabel != null) aiDailyPlanLabel.setText(fallback);
            else setAiMessage("Your Daily Plan", fallback, true);
        });
        new Thread(task, "ai-daily-plan").start();
    }

    @FXML
    private void onWeeklyReview(ActionEvent event) {
        // GuardianAiAssistant.buildWeeklyReview() — structured JSON: summary/wins/next_action
        int userId = requireUserId();
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override protected String call() throws Exception {
                int totalMin      = focusSessionService.getTotalDurationByUser(userId);
                int todaySessions = focusSessionService.getTodaySessionCountByUser(userId);
                int weekMin       = focusSessionService.getWeekDurationByUser(userId);
                java.util.Map<String, Object> stats = statsMap(todaySessions, weekMin, totalMin);
                java.util.List<FocusSession> recent = focusSessionService.findRecentByUser(userId, 8);
                java.util.List<java.util.Map<String, Object>> recentPayload = recent.stream()
                        .map(s -> {
                            java.util.Map<String, Object> m = new java.util.HashMap<>();
                            m.put("task",      s.taskId() != null ? "Task #" + s.taskId() : "No task");
                            m.put("duration",  s.duration());
                            m.put("timestamp", s.startedAt() != null ? s.startedAt().toString() : "");
                            return m;
                        })
                        .collect(java.util.stream.Collectors.toList());
                java.util.Map<String, Object> result = guardianAiAssistant.buildWeeklyReview(stats, recentPayload);
                String summary    = (String) result.getOrDefault("summary", "");
                @SuppressWarnings("unchecked")
                java.util.List<String> wins = (java.util.List<String>) result.get("wins");
                String nextAction = (String) result.getOrDefault("next_action", "");
                String display    = summary;
                if (wins != null && !wins.isEmpty()) display += "\n\n✅ " + String.join("\n✅ ", wins);
                if (!nextAction.isBlank()) display += "\n\n➡️ " + nextAction;
                return display;
            }
        };
        task.setOnSucceeded(e -> {
            String display = task.getValue();
            if (aiWeeklyReviewLabel != null) aiWeeklyReviewLabel.setText(display);
            else setAiMessage("Weekly Review Insight", display, false);
            logAiInsight("weekly_review", display, null);
        });
        task.setOnFailed(e -> {
            String fallback = openAiClient.generateWeeklyReview(buildFocusContext());
            if (aiWeeklyReviewLabel != null) aiWeeklyReviewLabel.setText(fallback);
            else setAiMessage("Weekly Review Insight", fallback, true);
        });
        new Thread(task, "ai-weekly-review").start();
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
        // ── VirtualRoomType validation (mirrors Symfony form constraints) ────
        String name = safeText(roomsNameField);
        if (name.isEmpty()) {
            setMessage(roomsMessageLabel, "Room name is required.", true); return;
        }
        if (name.length() < 3 || name.length() > 100) {
            setMessage(roomsMessageLabel, "Room name must be between 3 and 100 characters.", true); return;
        }
        if (!name.matches("[a-zA-Z0-9 \\-_]+")) {
            setMessage(roomsMessageLabel, "Allowed characters: letters, digits, spaces, hyphens, underscores.", true); return;
        }
        String description = safeText(roomsDescriptionField);
        if (description.length() > 500) {
            setMessage(roomsMessageLabel, "Description cannot exceed 500 characters.", true); return;
        }
        Integer max = parseMaxParticipants(roomsMessageLabel);
        if (max == null) return;
        if (max < 2 || max > 50) {
            setMessage(roomsMessageLabel, "Max participants must be between 2 and 50.", true); return;
        }
        try {
            // VirtualRoomType: isActive from checkbox (default true)
            boolean isActive = roomsIsActiveField == null || roomsIsActiveField.isSelected();
            VirtualRoom room = new VirtualRoom(
                    null, name, description, isActive, max,
                    LocalDateTime.now(), requireUserId(), null
            );
            virtualRoomService.insert(room);
            setMessage(roomsMessageLabel, "Room created successfully.", false);
            loadRooms();
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to create room: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onRoomsUpdate(ActionEvent event) {
        Integer id = parseRequiredInt(roomsIdField, roomsMessageLabel, "Room ID is required.");
        if (id == null) return;
        // ── VirtualRoomType validation ────────────────────────────────────────
        String name = safeText(roomsNameField);
        if (name.isEmpty()) {
            setMessage(roomsMessageLabel, "Room name is required.", true); return;
        }
        if (name.length() < 3 || name.length() > 100) {
            setMessage(roomsMessageLabel, "Room name must be between 3 and 100 characters.", true); return;
        }
        if (!name.matches("[a-zA-Z0-9 \\-_]+")) {
            setMessage(roomsMessageLabel, "Allowed characters: letters, digits, spaces, hyphens, underscores.", true); return;
        }
        String description = safeText(roomsDescriptionField);
        if (description.length() > 500) {
            setMessage(roomsMessageLabel, "Description cannot exceed 500 characters.", true); return;
        }
        Integer max = parseMaxParticipants(roomsMessageLabel);
        if (max == null) return;
        if (max < 2 || max > 50) {
            setMessage(roomsMessageLabel, "Max participants must be between 2 and 50.", true); return;
        }
        try {
            VirtualRoom existing = virtualRoomService.findById(id);
            if (existing == null) {
                setMessage(roomsMessageLabel, "Room not found.", true); return;
            }
            if (!Objects.equals(existing.creatorId(), requireUserId())) {
                setMessage(roomsMessageLabel, "You can only update your own rooms.", true); return;
            }
            VirtualRoom updated = new VirtualRoom(
                    existing.id(), name, description, existing.isActive(), max,
                    existing.createdAt(), existing.creatorId(), existing.subjectId()
            );
            virtualRoomService.update(updated);
            setMessage(roomsMessageLabel, "Room updated successfully.", false);
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

    /**
     * Filters rooms by subject — mirrors rooms.html.twig subject filter form.
     * Uses VirtualRoomService.findActiveRooms(subjectId).
     */
    @FXML
    private void onRoomsApplySubjectFilter(ActionEvent event) {
        if (roomsCards == null) return;
        try {
            Integer subjectId = null;
            if (roomsSubjectFilter != null && roomsSubjectFilter.getValue() != null
                    && !roomsSubjectFilter.getValue().startsWith("--")) {
                try { subjectId = Integer.parseInt(roomsSubjectFilter.getValue().trim()); }
                catch (NumberFormatException ignored) {}
            }
            List<VirtualRoom> rooms = virtualRoomService.findActiveRooms(subjectId);
            roomsCards.getChildren().clear();
            int currentUserId = requireUserId();
            for (VirtualRoom room : rooms) {
                boolean isOwner = Objects.equals(room.creatorId(), currentUserId);
                VBox card = buildRoomCard(room, isOwner ? " (yours)" : "");
                card.setOnMouseClicked(e -> {
                    if (roomsIdField != null)              roomsIdField.setText(String.valueOf(room.id()));
                    if (roomsNameField != null)            roomsNameField.setText(room.name() == null ? "" : room.name());
                    if (roomsDescriptionField != null)     roomsDescriptionField.setText(room.description() == null ? "" : room.description());
                    if (roomsMaxParticipantsField != null) roomsMaxParticipantsField.getSelectionModel().select(String.valueOf(room.maxParticipants()));
                    if (roomsIsActiveField != null)        roomsIsActiveField.setSelected(room.isActive());
                    selectedRoomId   = room.id();
                    selectedRoomName = room.name();
                });
                roomsCards.getChildren().add(card);
            }
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to filter rooms: " + e.getMessage(), true);
        }
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
        // ── ResourceType validation (mirrors Symfony form constraints) ────────
        String title = safeText(libraryTitleField);
        if (title.isEmpty()) {
            setMessage(libraryMessageLabel, "Title is required.", true); return;
        }
        if (title.length() < 3 || title.length() > 255) {
            setMessage(libraryMessageLabel, "Title must be between 3 and 255 characters.", true); return;
        }
        String description = safeText(libraryDescriptionField);
        if (description.length() > 2000) {
            setMessage(libraryMessageLabel, "Description cannot exceed 2000 characters.", true); return;
        }
        String filePath = safeText(libraryFilePathField);
        if (filePath.isEmpty()) {
            setMessage(libraryMessageLabel, "File path is required.", true); return;
        }
        String type = libraryTypeField == null ? "pdf" : libraryTypeField.getValue();
        if (type == null || type.isBlank()) {
            setMessage(libraryMessageLabel, "Please select a resource type.", true); return;
        }
        // ── AdminResourceEditType: rating 0–5 ────────────────────────────────
        int rating = parseRating();
        if (rating < 0) return; // validation message already set
        try {
            Resource resource = new Resource(
                    null, title, description, filePath, type,
                    0, rating, LocalDateTime.now(), LocalDateTime.now(),
                    null, requireUserId()
            );
            resourceService.insert(resource);
            setMessage(libraryMessageLabel, "Resource saved successfully.", false);
            loadResources();
        } catch (Exception e) {
            setMessage(libraryMessageLabel, "Failed to save resource: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onLibraryUpdate(ActionEvent event) {
        Integer id = parseRequiredInt(libraryIdField, libraryMessageLabel, "Resource ID is required.");
        if (id == null) return;
        // ── ResourceType + AdminResourceEditType validation ───────────────────
        String title = safeText(libraryTitleField);
        if (title.isEmpty()) {
            setMessage(libraryMessageLabel, "Title is required.", true); return;
        }
        if (title.length() < 3 || title.length() > 255) {
            setMessage(libraryMessageLabel, "Title must be between 3 and 255 characters.", true); return;
        }
        String description = safeText(libraryDescriptionField);
        if (description.length() > 2000) {
            setMessage(libraryMessageLabel, "Description cannot exceed 2000 characters.", true); return;
        }
        // ── AdminResourceEditType: rating 0–5 ────────────────────────────────
        int rating = parseRating();
        if (rating < 0) return; // validation message already set
        try {
            Resource existing = resourceService.findById(id);
            if (existing == null) {
                setMessage(libraryMessageLabel, "Resource not found.", true); return;
            }
            if (!Objects.equals(existing.uploaderId(), requireUserId())) {
                setMessage(libraryMessageLabel, "You can only update your own resources.", true); return;
            }
            String type = libraryTypeField == null ? existing.type() : libraryTypeField.getValue();
            String filePath = safeText(libraryFilePathField);
            Resource updated = new Resource(
                    existing.id(), title, description,
                    filePath.isEmpty() ? existing.filePath() : filePath,
                    type, existing.downloadCount(), rating,
                    existing.createdAt(), LocalDateTime.now(),
                    existing.subjectId(), existing.uploaderId()
            );
            resourceService.update(updated);
            setMessage(libraryMessageLabel, "Resource updated successfully.", false);
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

    /**
     * ResourceType: file field — opens a FileChooser filtered to PDF, validates max 10 MB.
     * Mirrors: FileType with accept='.pdf' and File constraint maxSize='10M'.
     */
    @FXML
    private void onLibraryBrowseFile(ActionEvent event) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select PDF Resource");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));

        javafx.stage.Stage stage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
        java.io.File selected = chooser.showOpenDialog(stage);

        if (selected == null) return; // user cancelled

        // Validate: must be PDF
        if (!selected.getName().toLowerCase().endsWith(".pdf")) {
            setMessage(libraryMessageLabel, "Only PDF files are accepted.", true);
            return;
        }
        // Validate: max 10 MB (ResourceType File constraint maxSize='10M')
        long maxBytes = 10L * 1024 * 1024;
        if (selected.length() > maxBytes) {
            setMessage(libraryMessageLabel,
                    "File is too large (" + (selected.length() / (1024 * 1024)) + " MB). Maximum: 10 MB.", true);
            return;
        }

        if (libraryFilePathField != null) {
            libraryFilePathField.setText(selected.getAbsolutePath());
        }
        setMessage(libraryMessageLabel, "File selected: " + selected.getName(), false);
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
        // ExternalLearningResourceService.fetchOpenLibrarySuggestions() — Open Library API
        String query = librarySearchField != null && !safeText(librarySearchField).isBlank()
                ? safeText(librarySearchField) : "study skills";
        if (externalSuggestionsLabel != null) externalSuggestionsLabel.setText("Query: " + query);

        javafx.concurrent.Task<java.util.List<java.util.Map<String, Object>>> task =
                new javafx.concurrent.Task<>() {
            @Override protected java.util.List<java.util.Map<String, Object>> call() {
                return externalLearningService.fetchOpenLibrarySuggestions(query, 6, 1);
            }
        };
        task.setOnSucceeded(e -> {
            java.util.List<java.util.Map<String, Object>> suggestions = task.getValue();
            if (externalSuggestionsCards != null) {
                externalSuggestionsCards.getChildren().clear();
                if (suggestions.isEmpty()) {
                    Label empty = new Label("No suggestions found for: " + query);
                    empty.setStyle("-fx-text-fill: #6d748a; -fx-font-size: 12px;");
                    externalSuggestionsCards.getChildren().add(empty);
                } else {
                    for (java.util.Map<String, Object> s : suggestions) {
                        externalSuggestionsCards.getChildren().add(buildExternalSuggestionCard(s));
                    }
                }
            }
            if (externalSuggestionsLabel != null)
                externalSuggestionsLabel.setText("Query: " + query + " — " + suggestions.size() + " result(s)");
        });
        task.setOnFailed(e -> {
            if (externalSuggestionsLabel != null)
                externalSuggestionsLabel.setText("Failed to fetch suggestions.");
        });
        new Thread(task, "external-suggestions").start();
    }

    @FXML
    private void onTrendingResources(ActionEvent event) {
        // Reuse Open Library with a "trending study" query
        if (librarySearchField != null) librarySearchField.setText("trending study");
        onAiSuggestedLinks(event);
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

            // ── Dedicated DB queries — mirrors Symfony's FocusSessionRepository ──
            int totalMinutes  = focusSessionService.getTotalDurationByUser(userId);
            int todaySessions = focusSessionService.getTodaySessionCountByUser(userId);
            int weekMinutes   = focusSessionService.getWeekDurationByUser(userId);
            List<FocusSession> recentSessions = focusSessionService.findRecentByUser(userId, 12);
            List<java.util.Map<String, Object>> perTaskTotals =
                    focusSessionService.getPerTaskTotalsByUser(userId, 6);

            // ── Stats pills ───────────────────────────────────────────────────
            if (labelTodaySessions != null)
                labelTodaySessions.setText("Today: " + todaySessions + " session" + (todaySessions == 1 ? "" : "s"));
            if (labelWeekMin != null)
                labelWeekMin.setText("This week: " + weekMinutes + " min");
            if (labelTotalMin != null)
                labelTotalMin.setText("Total: " + totalMinutes + " min");
            if (sessionCountLabel != null)
                sessionCountLabel.setText("Latest " + recentSessions.size() + " entries");

            // ── Recent sessions table ─────────────────────────────────────────
            if (focusCards != null) {
                focusCards.getChildren().clear();
                Map<Integer, String> taskTitles = TaskController.getTasksByOwner(userId).stream()
                        .collect(Collectors.toMap(Task::getId, Task::getTitle, (a, b) -> a));

                for (FocusSession session : recentSessions) {
                    String taskLabel = "No task";
                    if (session.taskId() != null) {
                        String title = taskTitles.get(session.taskId());
                        taskLabel = (title == null || title.isBlank()) ? "#" + session.taskId() : title;
                    }
                    String timestamp = session.startedAt() == null ? "-"
                            : session.startedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    VBox row = buildSessionRow(taskLabel, session.duration() + " min", timestamp);
                    final FocusSession s = session;
                    row.setOnMouseClicked(e -> {
                        if (focusIdField != null)       focusIdField.setText(String.valueOf(s.id()));
                        if (focusDurationField != null) focusDurationField.setText(String.valueOf(s.duration()));
                        if (focusTaskField != null)     selectTaskOption(s.taskId());
                    });
                    focusCards.getChildren().add(row);
                }
            }

            // ── Top focused tasks (getPerTaskTotalsByUser from DB) ────────────
            if (focusUserStatsCards != null) {
                focusUserStatsCards.getChildren().clear();
                focusUserStatsCards.getChildren().add(buildStatCard("Total Focus Minutes", String.valueOf(totalMinutes)));
                focusUserStatsCards.getChildren().add(buildStatCard("Sessions", String.valueOf(recentSessions.size())));

                if (perTaskTotals.isEmpty()) {
                    Label empty = new Label("No tracked task totals yet.");
                    empty.setStyle("-fx-text-fill: #6d748a; -fx-font-size: 12px;");
                    focusUserStatsCards.getChildren().add(empty);
                } else {
                    for (java.util.Map<String, Object> row : perTaskTotals) {
                        String title   = (String) row.get("task_title");
                        Object minutes = row.get("total_minutes");
                        String label   = (title == null || title.isBlank()) ? "Task #" + row.get("task_id") : title;
                        focusUserStatsCards.getChildren().add(buildStatCard(label, minutes + " min"));
                    }
                }
            }

            if (moduleSnapshotLabel != null)
                moduleSnapshotLabel.setText("Loaded " + recentSessions.size() + " focus sessions.");

        } catch (Exception e) {
            setMessage(focusMessageLabel, "Failed to load focus sessions: " + e.getMessage(), true);
        }
    }

    private void loadRooms() {
        if (roomsCards == null) {
            return;
        }
        try {
            // Symfony parity: findActiveRooms(null) — all active rooms, no subject filter
            List<VirtualRoom> rooms = virtualRoomService.findActiveRooms(null);
            roomsCards.getChildren().clear();
            int currentUserId = requireUserId();
            for (VirtualRoom room : rooms) {
                boolean isOwner = Objects.equals(room.creatorId(), currentUserId);
                String ownerTag = isOwner ? " (yours)" : "";
                VBox card = buildRoomCard(room, ownerTag);
                card.setOnMouseClicked(e -> {
                    if (roomsIdField != null)             roomsIdField.setText(String.valueOf(room.id()));
                    if (roomsNameField != null)           roomsNameField.setText(room.name() == null ? "" : room.name());
                    if (roomsDescriptionField != null)    roomsDescriptionField.setText(room.description() == null ? "" : room.description());
                    if (roomsMaxParticipantsField != null) roomsMaxParticipantsField.getSelectionModel().select(String.valueOf(room.maxParticipants()));
                    if (roomsIsActiveField != null)       roomsIsActiveField.setSelected(room.isActive());
                    selectedRoomId   = room.id();
                    selectedRoomName = room.name();
                });
                roomsCards.getChildren().add(card);
            }
            if (moduleSnapshotLabel != null)
                moduleSnapshotLabel.setText("Guardian module ready.");
        } catch (Exception e) {
            setMessage(roomsMessageLabel, "Failed to load rooms: " + e.getMessage(), true);
        }
    }

    private void loadResources() {
        if (libraryCards == null) {
            return;
        }
        try {
            // Symfony parity: findByFilters with no filters = all resources ordered by date
            libraryAllResources = resourceService.findByFilters(null, null, null);
            applyLibraryFilters();
        } catch (Exception e) {
            setMessage(libraryMessageLabel, "Failed to load resources: " + e.getMessage(), true);
        }
    }

    private void applyLibraryFilters() {
        if (libraryCards == null) {
            return;
        }
        String search = librarySearchField == null ? "" : safeText(librarySearchField);
        String type   = libraryFilterTypeField == null ? "All" : libraryFilterTypeField.getValue();
        String subject = librarySubjectFilter == null ? null
                : (librarySubjectFilter.getValue() == null
                   || librarySubjectFilter.getValue().startsWith("--") ? null
                   : librarySubjectFilter.getValue());

        List<Resource> filtered;
        try {
            // Use the new findByFilters() — mirrors Symfony's ResourceRepository::findByFilters()
            filtered = resourceService.findByFilters(null, type, search.isBlank() ? null : search);
        } catch (Exception e) {
            setMessage(libraryMessageLabel, "Failed to filter resources: " + e.getMessage(), true);
            return;
        }

        libraryCards.getChildren().clear();
        int currentUserId = requireUserId();
        for (Resource resource : filtered) {
            boolean isOwner = resource.uploaderId() == currentUserId;
            VBox card = buildResourceCard(resource, isOwner);
            card.setOnMouseClicked(e -> {
                if (libraryIdField != null)          libraryIdField.setText(String.valueOf(resource.id()));
                if (libraryTitleField != null)       libraryTitleField.setText(resource.title() == null ? "" : resource.title());
                if (libraryDescriptionField != null) libraryDescriptionField.setText(resource.description() == null ? "" : resource.description());
                if (libraryFilePathField != null)    libraryFilePathField.setText(resource.filePath() == null ? "" : resource.filePath());
                if (libraryTypeField != null)        libraryTypeField.getSelectionModel().select(resource.type());
                if (libraryRatingField != null)      libraryRatingField.setText(String.valueOf(resource.rating()));
            });
            libraryCards.getChildren().add(card);
        }
    }

    /** Creates a stats map for GuardianAiAssistant — avoids Map.of() type inference issues with mixed int/String. */
    private static java.util.Map<String, Object> statsMap(int todaySessions, int weekMin, int totalMin) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("today_sessions",      todaySessions);
        m.put("week_focus_minutes",  weekMin);
        m.put("total_focus_minutes", totalMin);
        return m;
    }

    /** Creates a task context map for GuardianAiAssistant. */
    private static java.util.Map<String, Object> taskMap(int id, String title, int priority, int estimatedMin) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("id",                id);
        m.put("title",             title == null ? "" : title);
        m.put("priority",          priority);
        m.put("estimated_minutes", estimatedMin);
        return m;
    }

    private String buildFocusContext() {
        Integer duration = parseOptionalInt(focusDurationField);
        Integer taskId = getSelectedTaskId();
        return "User " + requireUserId() + " focus session. Duration=" + (duration == null ? "" : duration)
                + " minutes, taskId=" + (taskId == null ? "" : taskId);
    }

    /**
     * Builds a task context map for GuardianAiAssistant calls.
     * Mirrors the taskContext array passed in Symfony's focusTimerApiTips/recommendDuration.
     */
    private java.util.Map<String, Object> buildTaskContext(Integer taskId) {
        if (taskId == null) return taskMap(0, "", 2, 0);
        return TaskController.getTasksByOwner(requireUserId()).stream()
                .filter(t -> t.getId() == taskId)
                .findFirst()
                .map(t -> taskMap(t.getId(), t.getTitle(), t.getPriority(), t.getEstimatedMinutes()))
                .orElse(taskMap(taskId, "", 2, 0));
    }

    /**
     * Builds a card for an Open Library suggestion.
     * Mirrors the external suggestions section in guardian/library.html.twig.
     */
    private VBox buildExternalSuggestionCard(java.util.Map<String, Object> suggestion) {
        String title  = String.valueOf(suggestion.getOrDefault("title", "Unknown"));
        String author = String.valueOf(suggestion.getOrDefault("author", "Unknown author"));
        Object year   = suggestion.get("year");
        String url    = String.valueOf(suggestion.getOrDefault("url", "https://openlibrary.org"));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        titleLabel.setWrapText(true);

        Label authorLabel = new Label("✍ " + author + (year != null && !year.toString().isEmpty() ? "  ·  " + year : ""));
        authorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        Button openBtn = new Button("🔗 Open Library");
        openBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;");
        openBtn.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } catch (Exception ex) {
                setMessage(libraryMessageLabel, "Could not open: " + url, true);
            }
        });

        VBox card = new VBox(5, titleLabel, authorLabel, openBtn);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12;");
        card.setMinWidth(220);
        card.setPrefWidth(260);
        return card;
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
            if (chatRoomTitle != null) chatRoomTitle.setText("Room Chat");
            return;
        }

        try {
            VirtualRoom room = virtualRoomService.findById(roomId);
            if (room != null) {
                // ── Room title ────────────────────────────────────────────────
                if (chatRoomTitle != null) {
                    String name = room.name() == null ? selectedRoomName : room.name();
                    chatRoomTitle.setText(name == null || name.isBlank() ? "Room Chat" : name);
                }

                // ── Details panel (mirrors room_detail.html.twig col-lg-4) ───
                if (roomCreatorLabel != null)
                    roomCreatorLabel.setText("User #" + room.creatorId());
                if (roomCreatedLabel != null)
                    roomCreatedLabel.setText(room.createdAt() == null ? "-"
                            : room.createdAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                if (roomStatusLabel != null) {
                    roomStatusLabel.setText(room.isActive() ? "Active room" : "Closed room");
                    roomStatusLabel.setStyle(room.isActive()
                            ? "-fx-background-color: rgba(72,198,239,0.2); -fx-text-fill: #1b4d6b; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;"
                            : "-fx-background-color: rgba(255,107,107,0.2); -fx-text-fill: #9b1c1c; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                }

                // ── Participants count + panel ────────────────────────────────
                int participantCount = virtualRoomService.countParticipants(roomId);
                if (roomCapacityLabel != null)
                    roomCapacityLabel.setText(participantCount + "/" + room.maxParticipants());
                if (roomParticipantCountLabel != null)
                    roomParticipantCountLabel.setText(String.valueOf(participantCount));

                // ── Participants pane — show user badges ──────────────────────
                if (roomParticipantsPane != null) {
                    roomParticipantsPane.getChildren().clear();
                    int currentUserId = requireUserId();
                    boolean isParticipant = virtualRoomService.isParticipant(roomId, currentUserId);
                    // Show current user status
                    Label youLabel = new Label(isParticipant ? "✅ You are a member" : "You are not a member");
                    youLabel.setStyle(isParticipant
                            ? "-fx-background-color: rgba(124,92,255,0.2); -fx-text-fill: #3f2fbf; -fx-background-radius: 10; -fx-padding: 3 8; -fx-font-size: 11px;"
                            : "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; -fx-background-radius: 10; -fx-padding: 3 8; -fx-font-size: 11px;");
                    roomParticipantsPane.getChildren().add(youLabel);
                    // Creator badge
                    Label creatorBadge = new Label("👑 Creator #" + room.creatorId());
                    creatorBadge.setStyle("-fx-background-color: rgba(255,209,102,0.3); -fx-text-fill: #8a5b00; -fx-background-radius: 10; -fx-padding: 3 8; -fx-font-size: 11px;");
                    roomParticipantsPane.getChildren().add(creatorBadge);
                }

                // ── Resources in room ─────────────────────────────────────────
                if (roomResourcesPane != null) {
                    roomResourcesPane.getChildren().clear();
                    // Load resources uploaded by the creator (mirrors room_detail creator_resources)
                    List<Resource> creatorResources = resourceService.findByUploader(room.creatorId())
                            .stream().limit(6).toList();
                    if (creatorResources.isEmpty()) {
                        Label empty = new Label("No resources added yet.");
                        empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #6d748a;");
                        roomResourcesPane.getChildren().add(empty);
                    } else {
                        for (Resource r : creatorResources) {
                            Label resLabel = new Label("📄 " + (r.title() == null ? "Resource #" + r.id() : r.title()));
                            resLabel.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-background-radius: 6; -fx-padding: 4 8; -fx-font-size: 11px; -fx-cursor: hand;");
                            resLabel.setWrapText(true);
                            roomResourcesPane.getChildren().add(resLabel);
                        }
                    }
                }

                // ── Join/Leave button visibility ──────────────────────────────
                if (btnJoinRoom != null || btnLeaveRoom != null) {
                    boolean isParticipant = virtualRoomService.isParticipant(roomId, requireUserId());
                    if (btnJoinRoom != null)  btnJoinRoom.setVisible(!isParticipant);
                    if (btnLeaveRoom != null) btnLeaveRoom.setVisible(isParticipant);
                }
            } else {
                if (chatRoomTitle != null) chatRoomTitle.setText("Room Chat");
            }
        } catch (Exception e) {
            if (chatRoomTitle != null) chatRoomTitle.setText("Room Chat");
        }

        if (chatResourceSelector != null) loadChatResources();
        refreshRoomChat();
    }

    private void refreshRoomChat() {
        if (roomChatList == null) {
            return;
        }
        Integer roomId = selectedRoomId;
        if (roomId == null) {
            roomChatList.setItems(FXCollections.observableArrayList());
            return;
        }
        try {
            List<String> formatted = roomMessageService.findByRoomId(roomId).stream()
                    .map(this::formatRoomMessage)
                    .toList();
            roomChatList.setItems(FXCollections.observableArrayList(formatted));
        } catch (Exception e) {
            roomChatList.setItems(FXCollections.observableArrayList("System: Failed to load messages."));
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
        Integer roomId = selectedRoomId;
        if (roomId == null) {
            return;
        }
        RoomMessage message = new RoomMessage(
                null,
                text,
                false,
                LocalDateTime.now(),
                null,
                resolveSenderId(),
                roomId
        );
        try {
            roomMessageService.insert(message);
            chatInputField.clear();
            refreshRoomChat();
        } catch (Exception e) {
            addRoomSystemMessage("Failed to send message.");
        }
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
        Integer roomId = selectedRoomId;
        if (roomId == null) {
            return;
        }
        RoomMessage message = new RoomMessage(
                null,
                "Shared resource: " + selected,
                false,
                LocalDateTime.now(),
                null,
                resolveSenderId(),
                roomId
        );
        try {
            roomMessageService.insert(message);
            refreshRoomChat();
        } catch (Exception e) {
            addRoomSystemMessage("Failed to share resource.");
        }
    }

    @FXML
    private void onStartDailyCoCall(ActionEvent event) {
        if (selectedRoomName == null) {
            addRoomSystemMessage("Select a room first to start a call.");
            return;
        }
        String link = dailyCoClient.createRoomSession(selectedRoomName, 10);
        addRoomSystemMessage("Daily.co Video Link: " + link);
    }

    @FXML
    private void onStartTwilioVideo(ActionEvent event) {
        if (selectedRoomName == null) {
            addRoomSystemMessage("Select a room first to start a call.");
            return;
        }
        String roomSid = twilioClient.createRoom(selectedRoomName);
        String token = twilioClient.generateParticipantToken(roomSid, resolveSenderLabel());
        addRoomSystemMessage("Twilio Room SID: " + roomSid + " | Token: " + token);
    }

    @FXML
    private void onStartAgoraVoice(ActionEvent event) {
        if (selectedRoomName == null) {
            addRoomSystemMessage("Select a room first to start a call.");
            return;
        }
        String token = agoraClient.generateAccessToken(selectedRoomName, resolveSenderLabel());
        String presence = agoraClient.trackUserPresence(selectedRoomName);
        addRoomSystemMessage("Agora Token: " + token + " | Status: " + presence);
    }

    private void addRoomSystemMessage(String message) {
        if (roomChatList == null) {
            return;
        }
        Integer roomId = selectedRoomId;
        if (roomId == null) {
            return;
        }
        RoomMessage systemMessage = new RoomMessage(
                null,
                message,
                false,
                LocalDateTime.now(),
                null,
                null,
                roomId
        );
        try {
            roomMessageService.insert(systemMessage);
            refreshRoomChat();
        } catch (Exception e) {
            roomChatList.getItems().add("System: " + message);
        }
    }

    private String formatRoomMessage(RoomMessage message) {
        String timestamp = message.createdAt() == null ? "" : message.createdAt().format(ROOM_CHAT_TIME_FORMAT);
        String prefix = timestamp.isEmpty() ? "" : "[" + timestamp + "] ";
        Integer currentUserId = resolveSenderId();
        String sender = message.senderId() == null
                ? "System"
                : (currentUserId != null && currentUserId.equals(message.senderId()) ? "Me" : "User " + message.senderId());
        String body = message.content() == null ? "" : message.content();
        return prefix + sender + ": " + body;
    }

    private Integer resolveSenderId() {
        UserSession session = UserSession.getInstance();
        return session.isLoggedIn() ? session.getUserId() : null;
    }

    private String resolveSenderLabel() {
        String email = UserSession.getInstance().getEmail();
        return email == null || email.isBlank() ? "Me" : email;
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

    /** Reads and validates the rating field (AdminResourceEditType: 0–5). Returns -1 on error. */
    private int parseRating() {
        if (libraryRatingField == null || safeText(libraryRatingField).isBlank()) {
            return 0; // optional — default to 0
        }
        try {
            int rating = Integer.parseInt(safeText(libraryRatingField));
            if (rating < 0 || rating > 5) {
                setMessage(libraryMessageLabel, "Rating must be between 0 and 5.", true);
                return -1;
            }
            return rating;
        } catch (NumberFormatException e) {
            setMessage(libraryMessageLabel, "Rating must be a number between 0 and 5.", true);
            return -1;
        }
    }

    /** Reads the selected value from the maxParticipants ComboBox. */
    private Integer parseMaxParticipants(Label messageLabel) {
        if (roomsMaxParticipantsField == null || roomsMaxParticipantsField.getValue() == null) {
            setMessage(messageLabel, "Max participants is required.", true);
            return null;
        }
        try {
            return Integer.parseInt(roomsMaxParticipantsField.getValue().trim());
        } catch (NumberFormatException e) {
            setMessage(messageLabel, "Invalid max participants value.", true);
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
        label.setVisible(true);
        label.setManaged(true);
        if (focusMessageContainer != null && label == focusMessageLabel) {
            focusMessageContainer.getChildren().clear();
        }
        label.setStyle(error
                ? "-fx-text-fill: #b91c1c; -fx-font-size: 12px;"
                : "-fx-text-fill: #15803d; -fx-font-size: 12px;");
    }

    private void setAiMessage(String title, String message, boolean error) {
        if (focusMessageContainer == null) return;
        
        focusMessageContainer.getChildren().clear();
        
        VBox aiBox = new VBox(8);
        aiBox.getStyleClass().add("guardian-ai-box");
        if (error) {
            aiBox.setStyle("-fx-border-color: #f2b2b2; -fx-background-color: #fffafb;");
        }
        
        Label titleLabel = new Label(error ? "⚠️ AI Error" : "✨ " + title);
        titleLabel.getStyleClass().add("guardian-ai-title");
        if (error) {
            titleLabel.setStyle("-fx-text-fill: #d45353;");
        }
        
        Label contentLabel = new Label(message);
        contentLabel.getStyleClass().add("guardian-ai-content");
        contentLabel.setWrapText(true);
        
        aiBox.getChildren().addAll(titleLabel, contentLabel);
        focusMessageContainer.getChildren().add(aiBox);
        
        if (focusMessageLabel != null) {
            focusMessageLabel.setText("");
            focusMessageLabel.setVisible(false);
            focusMessageLabel.setManaged(false);
        }
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

    /**
     * Builds a room card matching the Symfony rooms.html.twig card layout:
     * title, subject tag, description, creator + participant count + date, View button.
     */
    private VBox buildRoomCard(VirtualRoom room, String ownerTag) {
        // Available badge
        Label availBadge = new Label("Available");
        availBadge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46;" +
                "-fx-background-radius: 20; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Subject tag (no subject entity in Java yet — show placeholder)
        Label subjectTag = new Label("No subject assigned");
        subjectTag.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");

        // Room name
        Label nameLabel = new Label(room.name() == null ? "Unnamed Room" : room.name());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        nameLabel.setWrapText(true);

        // Description
        Label descLabel = new Label(room.description() == null || room.description().isBlank()
                ? "" : room.description());
        descLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        // Meta row: creator · live participant count / max · date
        // Mirrors rooms.html.twig: participants|length / maxParticipants
        String dateStr = room.createdAt() == null ? "-"
                : room.createdAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int liveCount = 0;
        try { liveCount = virtualRoomService.countParticipants(room.id()); } catch (Exception ignored) {}
        boolean isFull = liveCount >= room.maxParticipants();
        // Update available badge to reflect full/available state
        availBadge.setText(isFull ? "Full" : "Available");
        availBadge.setStyle(isFull
                ? "-fx-background-color: rgba(255,107,107,0.2); -fx-text-fill: #9b1c1c; -fx-background-radius: 20; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-background-radius: 20; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label metaLabel = new Label(
                "👤 creator #" + room.creatorId() + ownerTag +
                "  🔵 " + liveCount + "/" + room.maxParticipants() +
                "  📅 " + dateStr);
        metaLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        // View button
        Button viewBtn = new Button("👁 View");
        viewBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> {
            selectedRoomId = room.id();
            selectedRoomName = room.name();
            if (roomsIdField != null) roomsIdField.setText(String.valueOf(room.id()));
            if (roomsNameField != null) roomsNameField.setText(room.name() == null ? "" : room.name());
            if (roomsDescriptionField != null) roomsDescriptionField.setText(room.description() == null ? "" : room.description());
            if (roomsMaxParticipantsField != null) roomsMaxParticipantsField.getSelectionModel().select(String.valueOf(room.maxParticipants()));
            if (roomsIsActiveField != null) roomsIsActiveField.setSelected(room.isActive());
        });

        HBox headerRow = new HBox(8, nameLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        headerRow.getChildren().add(0, availBadge);

        VBox card = new VBox(6, headerRow, subjectTag, descLabel, metaLabel, viewBtn);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 14; -fx-cursor: hand;");
        card.setMinWidth(280);
        card.setPrefWidth(320);
        return card;
    }

    /**
     * Builds a session row for the "Recent Focus Sessions" table.
     * Matches Symfony's focus_timer.html.twig table: Task | Duration | Timestamp
     */
    private VBox buildSessionRow(String taskTitle, String duration, String timestamp) {
        Label taskLabel = new Label(taskTitle);
        taskLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 13px;");
        taskLabel.setMinWidth(200);

        Label durationLabel = new Label(duration);
        durationLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 13px;");
        durationLabel.setMinWidth(100);

        Label tsLabel = new Label(timestamp);
        tsLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        HBox row = new HBox(16, taskLabel, durationLabel, tsLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: transparent transparent #f3f4f6 transparent;" +
                "-fx-border-width: 0 0 1 0; -fx-padding: 8 4; -fx-cursor: hand;");
        row.setMaxWidth(Double.MAX_VALUE);

        VBox wrapper = new VBox(row);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
    }

    /**
     * Mirrors Symfony's guardian/library.html.twig resource card layout:
     * type badge, title, subject, description, uploader + download count + date, Download button.
     */
    private VBox buildResourceCard(Resource resource, boolean isOwner) {
        // Type badge
        Label typeBadge = new Label(resource.type() == null ? "file" : resource.type().toUpperCase());
        typeBadge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;" +
                "-fx-background-radius: 4; -fx-padding: 2 6; -fx-font-size: 10px; -fx-font-weight: bold;");

        // Manual badge (non-AI)
        Label manualBadge = new Label("Manual");
        manualBadge.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151;" +
                "-fx-background-radius: 4; -fx-padding: 2 6; -fx-font-size: 10px;");

        HBox badges = new HBox(4, typeBadge, manualBadge);

        // Title
        String titleText = resource.title() == null || resource.title().isBlank()
                ? "Untitled Resource" : resource.title();
        Label titleLabel = new Label(titleText);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        titleLabel.setWrapText(true);

        // Subject placeholder
        Label subjectLabel = new Label("No subject assigned");
        subjectLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");

        // Description (truncated)
        String desc = resource.description() == null ? "" : resource.description();
        if (desc.length() > 80) desc = desc.substring(0, 80) + "…";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        // Meta: uploader · downloads · date
        String dateStr = resource.createdAt() == null ? "-"
                : resource.createdAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String uploaderStr = isOwner ? "me" : "uploader #" + resource.uploaderId();
        Label metaLabel = new Label(
                "👤 " + uploaderStr +
                "  ⬇ " + resource.downloadCount() +
                "  📅 " + dateStr);
        metaLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        // Download button
        Button downloadBtn = new Button("⬇ Download");
        downloadBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
        downloadBtn.setOnAction(e -> {
            try {
                resourceService.incrementDownloadCount(resource.id());
                setMessage(libraryMessageLabel, "Download counted for: " + titleText, false);
                loadResources();
            } catch (Exception ex) {
                setMessage(libraryMessageLabel, "Download error: " + ex.getMessage(), true);
            }
        });

        VBox card = new VBox(6, badges, titleLabel, subjectLabel, descLabel, metaLabel, downloadBtn);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 14; -fx-cursor: hand;");
        card.setMinWidth(260);
        card.setPrefWidth(300);
        return card;
    }

    /**
     * Mirrors Symfony's getDurationForTaskPriority():
     *   priority 3 (high)   → 50 min
     *   priority 2 (medium) → 35 min
     *   default (low/null)  → 25 min
     */
    private int getDurationForTaskPriority(int priority) {
        return switch (priority) {
            case 3 -> 50;
            case 2 -> 35;
            default -> 25;
        };
    }
}
