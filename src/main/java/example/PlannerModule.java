package example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import com.mindforge.config.GroqConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PlannerModule {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===================== TASK =====================
    private TableView<Task> taskTable = new TableView<>();
    private TextField taskTitle = new TextField();
    private TextField taskDesc = new TextField();
    private TextField taskEstMin = new TextField();
    private DatePicker taskDueDatePicker = new DatePicker();
    private ComboBox<String> taskDueHour = new ComboBox<>();
    private ComboBox<String> taskDueMinute = new ComboBox<>();
    private ComboBox<String> taskStatus = new ComboBox<>();
    private ComboBox<String> taskPriority = new ComboBox<>();
    private ComboBox<String> filterPriority = new ComboBox<>();
    private Label taskMsg = new Label();
    private ObservableList<Task> allTasks = FXCollections.observableArrayList();

    // ===================== EXAM =====================
    private TableView<Exam> examTable = new TableView<>();
    private TextField examTitle = new TextField();
    private TextField examDesc = new TextField();
    private DatePicker examDatePicker = new DatePicker();
    private ComboBox<String> examHour = new ComboBox<>();
    private ComboBox<String> examMinute = new ComboBox<>();
    private TextField examDuration = new TextField();
    private TextField examLocation = new TextField();
    private ComboBox<String> examImportance = new ComboBox<>();
    private ComboBox<String> filterImportance = new ComboBox<>();
    private Label examMsg = new Label();
    // Exam advanced features
    private TextArea chatArea = new TextArea();
    private ComboBox<String> examSelector = new ComboBox<>();
    private TextField chatInput = new TextField();
    private Label lblSuccessRate = new Label("0%");
    private Label lblCreationRate = new Label("-%");
    private VBox programBox = new VBox(4);
    private ObservableList<Exam> allExams = FXCollections.observableArrayList();

    // ===================== DASHBOARD LABELS =====================
    private Label lblTasksInProgress = new Label("-");
    private Label lblExamsThisWeek = new Label("-");
    private Label lblTasksOverdue = new Label("-");
    private Label lblTotalTasks = new Label("-");
    private Label lblTasksTodo = new Label("-");
    private Label lblTasksDone = new Label("-");
    private VBox notificationBox = new VBox(6);

    // ===================== MAIN LAYOUT =====================
    private BorderPane root = new BorderPane();
    private StackPane contentArea = new StackPane();

    // Pages
    private VBox hubPage;
    private VBox dashboardPage;
    private VBox tasksPage;
    private VBox examsPage;

    // Calendar state
    private YearMonth currentYearMonth = YearMonth.now();
    private GridPane calendarGrid = new GridPane();
    private Label calendarMonthLabel = new Label();

    public Parent getView() {
        // Build pages
        hubPage = buildHubPage();
        dashboardPage = buildDashboardPage();
        tasksPage = buildTasksPage();
        examsPage = buildExamsPage();

        // Sidebar
        VBox sidebar = buildSidebar();

        // Content area
        contentArea.getChildren().add(hubPage);

        root.setLeft(sidebar);
        root.setCenter(contentArea);
        root.setStyle("-fx-background-color: #f0f2f5;");

        loadTasks();
        loadExams();
        refreshDashboard();
        
        return root;
    }

    // ================================================================
    // SIDEBAR
    // ================================================================
    private VBox buildSidebar() {
        // Logo
        Label logo = new Label("  MindForge");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        logo.setTextFill(Color.web("#2d2d2d"));
        logo.setPadding(new Insets(20, 10, 20, 10));

        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 0 0;");
        sidebar.setPadding(new Insets(0, 0, 20, 0));

        sidebar.getChildren().add(logo);
        sidebar.getChildren().add(sidebarSeparator());

        sidebar.getChildren().add(sidebarItem("🏠  Hub", () -> showPage(hubPage)));
        sidebar.getChildren().add(sidebarItem("📊  Dashboard", () -> showPage(dashboardPage)));
        sidebar.getChildren().add(sidebarItem("✅  Tasks", () -> showPage(tasksPage)));
        sidebar.getChildren().add(sidebarItem("📝  Exams", () -> showPage(examsPage)));

        return sidebar;
    }

    private Button sidebarItem(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 10, 10, 18));
        btn.setStyle(
                "-fx-background-color: transparent; -fx-font-size: 13px; -fx-text-fill: #333333; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #f0eeff; -fx-font-size: 13px; -fx-text-fill: #5c3db7; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-font-size: 13px; -fx-text-fill: #333333; -fx-cursor: hand;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Label sidebarSmallItem(String text) {
        Label lbl = new Label("  " + text);
        lbl.setPadding(new Insets(6, 10, 6, 18));
        lbl.setFont(Font.font("Arial", 12));
        lbl.setTextFill(Color.web("#555555"));
        return lbl;
    }

    private Region sidebarSeparator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #eeeeee;");
        return sep;
    }

    private void showPage(VBox page) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
        if (page == dashboardPage) {
            refreshDashboard();
            buildCalendar(currentYearMonth);
        }
    }

    // ================================================================
    // HUB PAGE
    // ================================================================
    private VBox buildHubPage() {
        // Gradient header
        Label headerTitle = new Label("Planner Hub");
        headerTitle.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        headerTitle.setTextFill(Color.WHITE);

        Label headerSub = new Label("Choose your planning mode: Tasks, Exams, or Dashboard.");
        headerSub.setFont(Font.font("Arial", 14));
        headerSub.setTextFill(Color.web("#f0f0f0"));

        VBox headerText = new VBox(5, headerTitle, headerSub);
        headerText.setPadding(new Insets(30, 30, 30, 30));
        headerText.setStyle(
                "-fx-background-color: linear-gradient(to right, #a855f7, #3b82f6);" +
                        "-fx-background-radius: 12;");
        headerText.setMaxWidth(Double.MAX_VALUE);

        // "What do you want to plan?" card
        Label planLabel = new Label("Planner");
        planLabel.setStyle(
                "-fx-background-color: #ede9fe; -fx-text-fill: #7c3aed; -fx-padding: 3 10 3 10; -fx-background-radius: 20; -fx-font-size: 12px;");

        Label planTitle = new Label("What do you want to plan?");
        planTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label planSub = new Label("Open your task board, manage exams, or view the dashboard.");
        planSub.setFont(Font.font("Arial", 13));
        planSub.setTextFill(Color.web("#666666"));

        VBox planCard = new VBox(8, planLabel, planTitle, planSub);
        planCard.setPadding(new Insets(20));
        planCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");

        // 3 navigation cards
        VBox cardTasks = hubNavCard("✅", "Tasks",
                "Manage your task board and update progress by status.",
                "Open tasks →", () -> showPage(tasksPage));

        VBox cardExams = hubNavCard("🎓", "Exams",
                "Review upcoming exams, create new ones, and adjust plans.",
                "Open exams →", () -> showPage(examsPage));

        VBox cardDash = hubNavCard("📊", "Dashboard",
                "See tasks and exams stats together with a calendar view.",
                "Open dashboard →", () -> showPage(dashboardPage));

        HBox navCards = new HBox(20, cardTasks, cardExams, cardDash);
        navCards.setAlignment(Pos.CENTER_LEFT);

        VBox navWrapper = new VBox(20, planCard, navCards);
        navWrapper.setPadding(new Insets(20));
        navWrapper.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");

        VBox page = new VBox(20, headerText, navWrapper);
        page.setPadding(new Insets(25));
        page.setStyle("-fx-background-color: #f0f2f5;");
        page.setFillWidth(true);
        return page;
    }

    private VBox hubNavCard(String icon, String title, String desc, String linkText, Runnable action) {
        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(22));

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Label descLbl = new Label(desc);
        descLbl.setFont(Font.font("Arial", 12));
        descLbl.setTextFill(Color.web("#555555"));
        descLbl.setWrapText(true);

        Button link = new Button(linkText);
        link.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #7c3aed; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");
        link.setOnAction(e -> action.run());

        HBox iconRow = new HBox(10, iconLbl, titleLbl);
        iconRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, iconRow, descLbl, link);
        card.setPadding(new Insets(18));
        card.setPrefWidth(220);
        card.setStyle(
                "-fx-background-color: #fafafa; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #f5f3ff; -fx-background-radius: 10; -fx-border-color: #a855f7; -fx-border-radius: 10;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #fafafa; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;"));
        return card;
    }

    // ================================================================
    // DASHBOARD PAGE
    // ================================================================
    private VBox buildDashboardPage() {
        Label pageTitle = new Label("Dashboard");
        pageTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        // Stat cards row
        VBox c1 = dashCard("In Progress", lblTasksInProgress, "#378ADD");
        VBox c2 = dashCard("Todo", lblTasksTodo, "#EF9F27");
        VBox c3 = dashCard("Done", lblTasksDone, "#1D9E75");
        VBox c4 = dashCard("Overdue", lblTasksOverdue, "#E24B4A");
        VBox c5 = dashCard("Total Tasks", lblTotalTasks, "#888780");
        VBox c6 = dashCard("Exams/Week", lblExamsThisWeek, "#534AB7");

        HBox statsRow = new HBox(15, c1, c2, c3, c4, c5, c6);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        Button btnRefresh = navBtn("Refresh", "#378ADD");
        btnRefresh.setOnAction(e -> {
            refreshDashboard();
            buildCalendar(currentYearMonth);
            refreshNotifications();
        });

        // Notifications panel
        Label notifTitle = new Label("🔔  Notifications");
        notifTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        notifTitle.setTextFill(Color.web("#374151"));
        notificationBox.setPadding(new Insets(4));
        notificationBox.setSpacing(3);
        notificationBox.setStyle("-fx-background-color: white;");
        notificationBox.setMaxHeight(110);

        ScrollPane notifScroll = new ScrollPane(notificationBox);
        notifScroll.setFitToWidth(true);
        notifScroll.setPrefHeight(115);
        notifScroll.setMaxHeight(115);
        notifScroll.setStyle("-fx-background: white; -fx-border-color: transparent;");
        notifScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        notifScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox notifSection = new VBox(4, notifTitle, notifScroll);
        notifSection.setPadding(new Insets(8, 12, 8, 12));
        notifSection.setStyle(
                "-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Calendar
        VBox calendarBox = buildCalendarBox();

        VBox innerPage = new VBox(10, pageTitle, statsRow, btnRefresh, notifSection, calendarBox);
        innerPage.setPadding(new Insets(20));
        innerPage.setStyle("-fx-background-color: #f0f2f5;");

        ScrollPane pageScroll = new ScrollPane(innerPage);
        pageScroll.setFitToWidth(true);
        pageScroll.setStyle("-fx-background: #f0f2f5; -fx-background-color: #f0f2f5;");
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox page = new VBox(pageScroll);
        page.setStyle("-fx-background-color: #f0f2f5;");
        VBox.setVgrow(pageScroll, Priority.ALWAYS);
        return page;
    }

    // ================================================================
    // CALENDAR
    // ================================================================
    private VBox buildCalendarBox() {
        // Navigation row
        Button btnPrev = navBtn("◀", "#534AB7");
        Button btnNext = navBtn("▶", "#534AB7");

        calendarMonthLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        calendarMonthLabel.setTextFill(Color.web("#333333"));

        btnPrev.setOnAction(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            buildCalendar(currentYearMonth);
        });
        btnNext.setOnAction(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            buildCalendar(currentYearMonth);
        });

        HBox navRow = new HBox(12, btnPrev, calendarMonthLabel, btnNext);
        navRow.setAlignment(Pos.CENTER_LEFT);
        navRow.setPadding(new Insets(0, 0, 8, 0));

        // Day headers
        String[] days = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);
        calendarGrid.getColumnConstraints().clear();

        for (int i = 0; i < 7; i++) {
            // Fixed column width
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPrefWidth(90);
            cc.setMinWidth(90);
            cc.setMaxWidth(90);
            calendarGrid.getColumnConstraints().add(cc);

            Label dayHdr = new Label(days[i]);
            dayHdr.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            dayHdr.setTextFill(Color.web("#888888"));
            dayHdr.setAlignment(Pos.CENTER);
            dayHdr.setMaxWidth(Double.MAX_VALUE);
            calendarGrid.add(dayHdr, i, 0);
        }

        buildCalendar(currentYearMonth);

        VBox box = new VBox(8, navRow, calendarGrid);
        box.setPadding(new Insets(18));
        box.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        return box;
    }

    private void buildCalendar(YearMonth ym) {
        // Clear day cells (keep row 0 headers)
        calendarGrid.getChildren().removeIf(n -> {
            Integer r = GridPane.getRowIndex(n);
            return r != null && r > 0;
        });

        calendarMonthLabel.setText(ym.getMonth().toString() + " " + ym.getYear());

        LocalDate firstDay = ym.atDay(1);
        // Monday=1 ... Sunday=7, so col 0=Mon, col 6=Sun
        int startCol = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = ym.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int col = startCol;
        int row = 1;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = ym.atDay(day);

            // Collect tasks on this date
            java.util.List<Task> tasksOnDay = allTasks.stream().filter(t -> {
                if (t.getDueDate() == null || t.getDueDate().isEmpty())
                    return false;
                try {
                    return LocalDateTime.parse(t.getDueDate(), DT_FORMAT).toLocalDate().equals(date);
                } catch (Exception e) {
                    return false;
                }
            }).collect(Collectors.toList());

            // Collect exams on this date
            java.util.List<Exam> examsOnDay = allExams.stream().filter(ex -> {
                if (ex.getExamDate() == null || ex.getExamDate().isEmpty())
                    return false;
                try {
                    return LocalDateTime.parse(ex.getExamDate(), DT_FORMAT).toLocalDate().equals(date);
                } catch (Exception e) {
                    return false;
                }
            }).collect(Collectors.toList());

            VBox cell = new VBox(2);
            cell.setPrefSize(90, 60);
            cell.setMinHeight(60);
            cell.setAlignment(Pos.TOP_LEFT);
            cell.setPadding(new Insets(5));

            // Day number
            Label dayNum = new Label(String.valueOf(day));
            dayNum.setFont(Font.font("Arial", FontWeight.BOLD, 12));

            String cellStyle;
            if (date.equals(today)) {
                cellStyle = "-fx-background-color: #ede9fe; -fx-background-radius: 8; -fx-border-color: #7c3aed; -fx-border-radius: 8; -fx-border-width: 2;";
                dayNum.setTextFill(Color.web("#7c3aed"));
            } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cellStyle = "-fx-background-color: #f9fafb; -fx-background-radius: 8;";
                dayNum.setTextFill(Color.web("#aaaaaa"));
            } else {
                cellStyle = "-fx-background-color: #f3f4f6; -fx-background-radius: 8;";
                dayNum.setTextFill(Color.web("#333333"));
            }
            cell.setStyle(cellStyle);
            cell.getChildren().add(dayNum);

            // Show tasks - color by status
            for (Task t : tasksOnDay) {
                String status = t.getStatus() == null ? "" : t.getStatus().toLowerCase();
                String taskColor;
                String taskIcon;
                switch (status) {
                    case "in_progress":
                        taskColor = "#378ADD";
                        taskIcon = "🔵";
                        break; // blue
                    case "done":
                        taskColor = "#1D9E75";
                        taskIcon = "✅";
                        break; // green
                    case "todo":
                        taskColor = "#EF9F27";
                        taskIcon = "🟡";
                        break; // orange/yellow
                    default:
                        taskColor = "#888780";
                        taskIcon = "⚪";
                        break; // gray
                }
                // Overdue override - red if past due and not done
                if (isOverdue(t.getDueDate()) && !"done".equals(status)) {
                    taskColor = "#E24B4A";
                    taskIcon = "🔴";
                }
                final Task taskRef = t;
                final String tColor = taskColor;
                Label pill = new Label(taskIcon + " " + truncate(t.getTitle(), 12));
                pill.setFont(Font.font("Arial", 9));
                pill.setTextFill(Color.WHITE);
                pill.setStyle("-fx-background-color: " + taskColor
                        + "; -fx-background-radius: 4; -fx-padding: 1 4 1 4; -fx-cursor: hand;");
                pill.setMaxWidth(85);
                pill.setOnMouseEntered(e -> pill.setStyle("-fx-background-color: " + tColor
                        + "; -fx-background-radius: 4; -fx-padding: 1 4 1 4; -fx-cursor: hand; -fx-opacity: 0.8;"));
                pill.setOnMouseExited(e -> pill.setStyle("-fx-background-color: " + tColor
                        + "; -fx-background-radius: 4; -fx-padding: 1 4 1 4; -fx-cursor: hand;"));
                pill.setOnMouseClicked(e -> showTaskPopup(taskRef));
                cell.getChildren().add(pill);
            }

            // Show exams - color by importance (1-2=green, 3=orange, 4-5=red)
            for (Exam ex : examsOnDay) {
                int imp = ex.getImportance();
                String examColor;
                String examIcon;
                if (imp <= 2) {
                    examColor = "#1D9E75";
                    examIcon = "🎓";
                } else if (imp == 3) {
                    examColor = "#EF9F27";
                    examIcon = "🎓";
                } else {
                    examColor = "#E24B4A";
                    examIcon = "🎓";
                }
                final Exam examRef = ex;
                final String eColor = examColor;
                Label pill = new Label(examIcon + " " + truncate(ex.getTitle(), 12));
                pill.setFont(Font.font("Arial", 9));
                pill.setTextFill(Color.WHITE);
                pill.setStyle("-fx-background-color: " + examColor
                        + "; -fx-background-radius: 4; -fx-padding: 1 4 1 4; -fx-cursor: hand;");
                pill.setMaxWidth(85);
                pill.setOnMouseEntered(e -> pill.setStyle("-fx-background-color: " + eColor
                        + "; -fx-background-radius: 4; -fx-padding: 1 4 1 4; -fx-cursor: hand; -fx-opacity: 0.8;"));
                pill.setOnMouseExited(e -> pill.setStyle("-fx-background-color: " + eColor
                        + "; -fx-background-radius: 4; -fx-padding: 1 4 1 4; -fx-cursor: hand;"));
                pill.setOnMouseClicked(e -> showExamPopup(examRef));
                cell.getChildren().add(pill);
            }

            calendarGrid.add(cell, col, row);
            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null)
            return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // ================================================================
    // TASKS PAGE
    // ================================================================
    private VBox buildTasksPage() {
        // Header
        Label pageTitle = new Label("✅  Tasks");
        pageTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        Button btnBack = navBtn("← Hub", "#7c3aed");
        btnBack.setOnAction(e -> showPage(hubPage));

        HBox header = new HBox(20, pageTitle, btnBack);
        header.setAlignment(Pos.CENTER_LEFT);

        // Table
        TableColumn<Task, Integer> c1 = new TableColumn<>("ID");
        c1.setCellValueFactory(new PropertyValueFactory<>("id"));
        c1.setPrefWidth(45);
        TableColumn<Task, String> c2 = new TableColumn<>("Title");
        c2.setCellValueFactory(new PropertyValueFactory<>("title"));
        c2.setPrefWidth(155);
        TableColumn<Task, String> c3 = new TableColumn<>("Description");
        c3.setCellValueFactory(new PropertyValueFactory<>("description"));
        c3.setPrefWidth(145);
        TableColumn<Task, String> c4 = new TableColumn<>("Status");
        c4.setCellValueFactory(new PropertyValueFactory<>("status"));
        c4.setPrefWidth(100);
        TableColumn<Task, Integer> c5 = new TableColumn<>("Priority");
        c5.setCellValueFactory(new PropertyValueFactory<>("priority"));
        c5.setPrefWidth(70);
        TableColumn<Task, String> c6 = new TableColumn<>("Due Date");
        c6.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        c6.setPrefWidth(145);
        TableColumn<Task, Integer> c7 = new TableColumn<>("Owner ID");
        c7.setCellValueFactory(new PropertyValueFactory<>("ownerId"));
        c7.setPrefWidth(75);

        TableColumn<Task, Integer> c8 = new TableColumn<>("Duration (min)");
        c8.setCellValueFactory(new PropertyValueFactory<>("estimatedMinutes"));
        c8.setPrefWidth(110);

        taskTable.getColumns().setAll(Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8));
        taskTable.setPrefHeight(240);
        taskTable.setStyle("-fx-background-color: white;");

        taskTable.setRowFactory(tv -> new TableRow<Task>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (task == null || empty)
                    setStyle("");
                else if (isOverdue(task.getDueDate()) && !"done".equalsIgnoreCase(task.getStatus()))
                    setStyle("-fx-background-color: #ffe0e0;");
                else
                    setStyle("");
            }
        });

        // Form fields
        taskTitle.setPromptText("Title (required)");
        taskTitle.setPrefWidth(130);
        taskDesc.setPromptText("Description");
        taskDesc.setPrefWidth(130);
        // Task DatePicker + time
        taskDueDatePicker.setPromptText("Select date");
        taskDueDatePicker.setPrefWidth(150);
        taskDueDatePicker.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6;");
        taskDueDatePicker.setShowWeekNumbers(false);
        taskEstMin.setPromptText("Duration (min)");
        taskEstMin.setPrefWidth(140);
        java.util.List<String> hours = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++)
            hours.add(String.format("%02d", i));
        java.util.List<String> minutes = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i += 5)
            minutes.add(String.format("%02d", i));
        taskDueHour.getItems().addAll(hours);
        taskDueHour.setValue("08");
        taskDueHour.setPrefWidth(60);
        taskDueHour.setStyle("-fx-background-color: white;");
        taskDueMinute.getItems().addAll(minutes);
        taskDueMinute.setValue("00");
        taskDueMinute.setPrefWidth(60);
        taskDueMinute.setStyle("-fx-background-color: white;");
        taskStatus.getItems().addAll("todo", "in_progress", "done");
        taskStatus.setValue("todo");
        taskPriority.getItems().addAll("1 - Low", "2 - Medium", "3 - High");
        taskPriority.setValue("2 - Medium");
        filterPriority.getItems().addAll("All", "1 - Low", "2 - Medium", "3 - High");
        filterPriority.setValue("All");
        filterPriority.setOnAction(e -> applyTaskFilter());

        Button btnSortTask = navBtn("Sort by due date", "#534AB7");
        btnSortTask.setOnAction(e -> taskTable.setItems(taskTable.getItems().stream()
                .sorted((a, b) -> {
                    if (a.getDueDate() == null || a.getDueDate().isEmpty())
                        return 1;
                    if (b.getDueDate() == null || b.getDueDate().isEmpty())
                        return -1;
                    try {
                        return LocalDateTime.parse(a.getDueDate(), DT_FORMAT)
                                .compareTo(LocalDateTime.parse(b.getDueDate(), DT_FORMAT));
                    } catch (Exception ex) {
                        return 0;
                    }
                }).collect(Collectors.toCollection(FXCollections::observableArrayList))));

        Button btnAdd = navBtn("Add", "#1D9E75");
        Button btnEdit = navBtn("Edit", "#378ADD");
        Button btnDel = navBtn("Delete", "#E24B4A");
        Button btnRef = navBtn("Refresh", "#888780");
        Button btnClear = navBtn("New", "#534AB7");

        // Add only allowed when NO row is selected
        btnAdd.setOnAction(e -> {
            if (taskTable.getSelectionModel().getSelectedItem() != null) {
                taskMsg("Cannot Add: a row is selected. Click 'New' to deselect, or use 'Edit'.", true);
                return;
            }
            if (!validateTaskForm())
                return;
            TaskController.insertTask(taskTitle.getText().trim(), taskDesc.getText().trim(), taskStatus.getValue(),
                    taskPriorityInt(), getTaskDueDateTime(), parseEstMin());
            taskMsg("Task added successfully.", false);
            clearTaskForm();
            loadTasks();
        });

        btnEdit.setOnAction(e -> {
            Task sel = taskTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                taskMsg("Please select a task to edit.", true);
                return;
            }
            if (!validateTaskForm())
                return;
            TaskController.updateTask(sel.getId(), taskTitle.getText().trim(), taskDesc.getText().trim(),
                    taskStatus.getValue(), taskPriorityInt(), getTaskDueDateTime(), parseEstMin());
            taskMsg("Task updated successfully.", false);
            clearTaskForm();
            loadTasks();
        });

        btnDel.setOnAction(e -> {
            Task sel = taskTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                taskMsg("Please select a task to delete.", true);
                return;
            }
            TaskController.deleteTask(sel.getId());
            taskMsg("Task deleted.", false);
            clearTaskForm();
            loadTasks();
        });

        btnRef.setOnAction(e -> {
            loadTasks();
            taskMsg("Tasks refreshed.", false);
        });

        // New button: clears form and deselects row → enables Add
        btnClear.setOnAction(e -> {
            taskTable.getSelectionModel().clearSelection();
            clearTaskForm();
            taskMsg("Form cleared - you can now add a new task.", false);
        });

        // When a row is selected → disable Add, show hint
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                taskTitle.setText(n.getTitle());
                taskDesc.setText(n.getDescription());
                String td = n.getDueDate();
                if (td != null && !td.isEmpty()) {
                    try {
                        LocalDateTime ldt = LocalDateTime.parse(td, DT_FORMAT);
                        taskDueDatePicker.setValue(ldt.toLocalDate());
                        taskDueHour.setValue(String.format("%02d", ldt.getHour()));
                        taskDueMinute.setValue(String.format("%02d", (ldt.getMinute() / 15) * 15));
                    } catch (Exception ex) {
                        taskDueDatePicker.setValue(null);
                    }
                } else {
                    taskDueDatePicker.setValue(null);
                }
                taskStatus.setValue(n.getStatus());
                int p = n.getPriority();
                taskPriority.setValue(p == 1 ? "1 - Low" : p == 3 ? "3 - High" : "2 - Medium");
                taskEstMin.setText(String.valueOf(n.getEstimatedMinutes()));
                resetFieldStyles(taskTitle, taskEstMin);
                taskMsg.setText("");
                // Visual feedback: gray out Add button
                btnAdd.setStyle(
                        "-fx-background-color:#CCCCCC;-fx-text-fill:#888888;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 14 6 14;");
                btnAdd.setText("Add (select New first)");
            } else {
                // No selection: restore Add button
                btnAdd.setStyle(
                        "-fx-background-color:#1D9E75;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 14 6 14;");
                btnAdd.setText("Add");
            }
        });

        Label legend = new Label("  ■  Overdue task (due date passed)");
        legend.setStyle(
                "-fx-text-fill:#E24B4A;-fx-font-size:11px;-fx-background-color:#ffe0e0;-fx-padding:3 8 3 8;-fx-background-radius:4;");

        HBox filterRow = new HBox(10, new Label("Filter:"), filterPriority, btnSortTask);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        HBox row1 = new HBox(8, new Label("Title *:"), taskTitle, new Label("Description:"), taskDesc);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, new Label("Status *:"), taskStatus, new Label("Priority *:"), taskPriority);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label taskTimeLabel = new Label("🕐");
        taskTimeLabel.setFont(Font.font(14));
        HBox taskTimeBox = new HBox(4, taskDueHour, new Label(":"), taskDueMinute);
        taskTimeBox.setAlignment(Pos.CENTER_LEFT);
        taskTimeBox.setStyle(
                "-fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-color: #f9f9f9; -fx-background-radius: 6; -fx-padding: 2 6 2 6;");
        HBox row3 = new HBox(12, new Label("📅 Due Date:"), taskDueDatePicker, taskTimeLabel, taskTimeBox,
                new Label("⏱ Duration (min):"), taskEstMin);
        row3.setAlignment(Pos.CENTER_LEFT);
        HBox btnRow = new HBox(10, btnAdd, btnEdit, btnDel, btnRef);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        taskMsg.setStyle("-fx-font-size: 12px;");

        VBox form = new VBox(8, filterRow, legend, row1, row2, row3, btnRow, taskMsg);
        form.setPadding(new Insets(12));
        form.setStyle(
                "-fx-background-color:white;-fx-border-color:#e5e7eb;-fx-border-radius:8;-fx-background-radius:8;");

        VBox page = new VBox(15, header, taskTable, form);
        page.setPadding(new Insets(25));
        page.setStyle("-fx-background-color: #f0f2f5;");
        return page;
    }

    // ================================================================
    // EXAMS PAGE
    // ================================================================
    private VBox buildExamsPage() {
        Label pageTitle = new Label("🎓  Exams");
        pageTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        Button btnBack = navBtn("← Hub", "#7c3aed");
        btnBack.setOnAction(e -> showPage(hubPage));

        HBox header = new HBox(20, pageTitle, btnBack);
        header.setAlignment(Pos.CENTER_LEFT);

        TableColumn<Exam, Integer> c1 = new TableColumn<>("ID");
        c1.setCellValueFactory(new PropertyValueFactory<>("id"));
        c1.setPrefWidth(40);
        TableColumn<Exam, String> c2 = new TableColumn<>("Title");
        c2.setCellValueFactory(new PropertyValueFactory<>("title"));
        c2.setPrefWidth(130);
        TableColumn<Exam, String> c3 = new TableColumn<>("Description");
        c3.setCellValueFactory(new PropertyValueFactory<>("description"));
        c3.setPrefWidth(120);
        TableColumn<Exam, String> c4 = new TableColumn<>("Date");
        c4.setCellValueFactory(new PropertyValueFactory<>("examDate"));
        c4.setPrefWidth(140);
        TableColumn<Exam, Integer> c5 = new TableColumn<>("Duration (min)");
        c5.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        c5.setPrefWidth(100);
        TableColumn<Exam, String> c6 = new TableColumn<>("Location");
        c6.setCellValueFactory(new PropertyValueFactory<>("location"));
        c6.setPrefWidth(100);
        TableColumn<Exam, Integer> c7 = new TableColumn<>("Importance");
        c7.setCellValueFactory(new PropertyValueFactory<>("importance"));
        c7.setPrefWidth(80);
        TableColumn<Exam, Integer> c8 = new TableColumn<>("Owner ID");
        c8.setCellValueFactory(new PropertyValueFactory<>("ownerId"));
        c8.setPrefWidth(75);

        examTable.getColumns().setAll(Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8));
        examTable.setPrefHeight(240);
        examTable.setStyle("-fx-background-color: white;");

        examTitle.setPromptText("Title (required)");
        examTitle.setPrefWidth(130);
        examDesc.setPromptText("Description");
        examDesc.setPrefWidth(130);
        // Exam DatePicker + time
        examDatePicker.setPromptText("Select date");
        examDatePicker.setPrefWidth(150);
        examDatePicker.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6;");
        examDatePicker.setShowWeekNumbers(false);
        java.util.List<String> eHours = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++)
            eHours.add(String.format("%02d", i));
        java.util.List<String> eMinutes = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i += 5)
            eMinutes.add(String.format("%02d", i));
        examHour.getItems().addAll(eHours);
        examHour.setValue("08");
        examHour.setPrefWidth(60);
        examHour.setStyle("-fx-background-color: white;");
        examMinute.getItems().addAll(eMinutes);
        examMinute.setValue("00");
        examMinute.setPrefWidth(60);
        examMinute.setStyle("-fx-background-color: white;");
        examDuration.setPromptText("Duration in minutes");
        examDuration.setPrefWidth(120);
        examLocation.setPromptText("Location");
        examLocation.setPrefWidth(110);
        examImportance.getItems().addAll("1", "2", "3", "4", "5");
        examImportance.setValue("3");
        filterImportance.getItems().addAll("All", "1", "2", "3", "4", "5");
        filterImportance.setValue("All");
        filterImportance.setOnAction(e -> applyExamFilter());

        Button btnSort = navBtn("Sort by date", "#534AB7");
        btnSort.setOnAction(e -> examTable.setItems(examTable.getItems().stream()
                .sorted(Comparator.comparing(ex -> {
                    try {
                        return LocalDateTime.parse(ex.getExamDate(), DT_FORMAT);
                    } catch (Exception ex2) {
                        return LocalDateTime.MAX;
                    }
                })).collect(Collectors.toCollection(FXCollections::observableArrayList))));

        Button btnAdd = navBtn("Add", "#1D9E75");
        Button btnEdit = navBtn("Edit", "#378ADD");
        Button btnDel = navBtn("Delete", "#E24B4A");
        Button btnRef = navBtn("Refresh", "#888780");
        Button btnClearE = navBtn("New", "#534AB7");

        // Add only allowed when NO row is selected
        btnAdd.setOnAction(e -> {
            if (examTable.getSelectionModel().getSelectedItem() != null) {
                examMsg("Cannot Add: a row is selected. Click 'New' to deselect, or use 'Edit'.", true);
                return;
            }
            if (!validateExamForm())
                return;
            ExamController.insertExam(examTitle.getText().trim(), examDesc.getText().trim(), getExamDateTime(),
                    Integer.parseInt(examDuration.getText().trim()), examLocation.getText().trim(),
                    Integer.parseInt(examImportance.getValue()));
            examMsg("Exam added successfully.", false);
            clearExamForm();
            loadExams();
        });

        btnEdit.setOnAction(e -> {
            Exam sel = examTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                examMsg("Please select an exam to edit.", true);
                return;
            }
            if (!validateExamForm())
                return;
            ExamController.updateExam(sel.getId(), examTitle.getText().trim(), examDesc.getText().trim(),
                    getExamDateTime(), Integer.parseInt(examDuration.getText().trim()), examLocation.getText().trim(),
                    Integer.parseInt(examImportance.getValue()));
            examMsg("Exam updated successfully.", false);
            clearExamForm();
            loadExams();
        });

        btnDel.setOnAction(e -> {
            Exam sel = examTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                examMsg("Please select an exam to delete.", true);
                return;
            }
            ExamController.deleteExam(sel.getId());
            examMsg("Exam deleted.", false);
            clearExamForm();
            loadExams();
        });

        btnRef.setOnAction(e -> {
            loadExams();
            examMsg("Exams refreshed.", false);
        });

        // New button: clears form and deselects row
        btnClearE.setOnAction(e -> {
            examTable.getSelectionModel().clearSelection();
            clearExamForm();
            examMsg("Form cleared - you can now add a new exam.", false);
        });

        examTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                examTitle.setText(n.getTitle());
                examDesc.setText(n.getDescription());
                String ed = n.getExamDate();
                if (ed != null && !ed.isEmpty()) {
                    try {
                        LocalDateTime ldt = LocalDateTime.parse(ed, DT_FORMAT);
                        examDatePicker.setValue(ldt.toLocalDate());
                        examHour.setValue(String.format("%02d", ldt.getHour()));
                        examMinute.setValue(String.format("%02d", (ldt.getMinute() / 15) * 15));
                    } catch (Exception ex) {
                        examDatePicker.setValue(null);
                    }
                } else {
                    examDatePicker.setValue(null);
                }
                examDuration.setText(String.valueOf(n.getDurationMinutes()));
                examLocation.setText(n.getLocation());
                examImportance.setValue(String.valueOf(n.getImportance()));
                resetFieldStyles(examTitle, examDuration);
                examMsg.setText("");
                refreshDailyProgram(n);
                chatArea.setText("Select an exam above to start.");
                // Gray out Add button
                btnAdd.setStyle(
                        "-fx-background-color:#CCCCCC;-fx-text-fill:#888888;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 14 6 14;");
                btnAdd.setText("Add (select New first)");
            } else {
                // Restore Add button
                btnAdd.setStyle(
                        "-fx-background-color:#1D9E75;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 14 6 14;");
                btnAdd.setText("Add");
            }
        });

        HBox filterRow = new HBox(10, new Label("Filter by importance:"), filterImportance, btnSort);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        HBox row1 = new HBox(8, new Label("Title *:"), examTitle, new Label("Description:"), examDesc,
                new Label("Location:"), examLocation);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label examTimeLabel = new Label("🕐");
        examTimeLabel.setFont(Font.font(14));
        HBox examTimeBox = new HBox(4, examHour, new Label(":"), examMinute);
        examTimeBox.setAlignment(Pos.CENTER_LEFT);
        examTimeBox.setStyle(
                "-fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-color: #f9f9f9; -fx-background-radius: 6; -fx-padding: 2 6 2 6;");
        HBox row2 = new HBox(12, new Label("📅 Date *:"), examDatePicker, examTimeLabel, examTimeBox,
                new Label("Duration *:"), examDuration, new Label("Importance *:"), examImportance);
        row2.setAlignment(Pos.CENTER_LEFT);
        HBox btnRow = new HBox(10, btnAdd, btnEdit, btnDel, btnRef, btnClearE);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        examMsg.setStyle("-fx-font-size: 12px;");

        VBox form = new VBox(8, filterRow, row1, row2, btnRow, examMsg);
        form.setPadding(new Insets(12));
        form.setStyle(
                "-fx-background-color:white;-fx-border-color:#e5e7eb;-fx-border-radius:8;-fx-background-radius:8;");

        // ================================================================
        // EXAM ADVANCED FEATURES PANEL
        // ================================================================
        Label advTitle = new Label("Exam Analytics & Tools");
        advTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        advTitle.setTextFill(Color.web("#5B2D8E"));

        // --- Exam Selector ComboBox ---
        examSelector.getItems().clear();
        examSelector.setPromptText("-- Select an exam --");
        examSelector.setPrefWidth(280);
        examSelector.setStyle("-fx-font-size: 13px;");
        // Populate with exams
        for (Exam ex : allExams) {
            examSelector.getItems().add(ex.getId() + " - " + ex.getTitle() + " (" + ex.getExamDate() + ")");
        }
        Label selectorLabel = new Label("Choose Exam:");
        selectorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        HBox selectorRow = new HBox(10, selectorLabel, examSelector);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        selectorRow.setPadding(new Insets(8, 0, 8, 0));

        // --- Stats cards ---
        lblSuccessRate.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        lblSuccessRate.setTextFill(Color.web("#6B7280")); // default gray until exam selected
        Label ssTitle = new Label("Success Rate");
        ssTitle.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        ssTitle.setTextFill(Color.web("#374151"));

        VBox statSuccessCard = new VBox(6, lblSuccessRate, ssTitle);
        statSuccessCard.setAlignment(Pos.CENTER);
        statSuccessCard.setPadding(new Insets(16));
        statSuccessCard.setPrefWidth(220);
        statSuccessCard.setStyle(
                "-fx-border-color:#1D9E75;-fx-border-radius:10;-fx-background-radius:10;-fx-background-color:white;-fx-border-width:2;");

        HBox statsRow2 = new HBox(20, statSuccessCard);
        statsRow2.setAlignment(Pos.CENTER_LEFT);

        // --- Daily Program panel ---
        Label progTitle = new Label("Daily Study Program");
        progTitle.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        progTitle.setTextFill(Color.web("#1E40AF"));

        Label progHint = new Label("Select an exam above to generate your study plan.");
        progHint.setFont(Font.font("Arial", 12));
        progHint.setTextFill(Color.web("#6B7280"));

        programBox.getChildren().add(progHint);
        programBox.setPadding(new Insets(8));
        programBox.setStyle(
                "-fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE; -fx-border-radius: 6; -fx-background-radius: 6;");

        ScrollPane progScroll = new ScrollPane(programBox);
        progScroll.setFitToWidth(true);
        progScroll.setPrefHeight(200);
        progScroll.setMinHeight(200);
        progScroll.setStyle("-fx-background: #EFF6FF; -fx-border-color: transparent;");
        progScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        progScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox programSection = new VBox(8, progTitle, progScroll);
        programSection.setPadding(new Insets(12));
        programSection.setPrefWidth(460);
        programSection.setStyle(
                "-fx-background-color: white; -fx-border-color: #BFDBFE; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5;");

        // --- When exam is selected from ComboBox ---
        examSelector.setOnAction(e -> {
            String selected = examSelector.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isEmpty())
                return;
            try {
                // Extract ID from "ID - title (date)" format
                int examId = Integer.parseInt(selected.split(" - ")[0].trim());
                // Find exam by ID — always fresh from allExams
                Exam selectedExam = allExams.stream()
                        .filter(ex -> ex.getId() == examId)
                        .findFirst().orElse(null);
                if (selectedExam == null)
                    return;
                // Reset to 0% before computing
                lblSuccessRate.setText("0%");
                lblSuccessRate.setTextFill(Color.web("#6B7280"));
                // Always reload tasks fresh before computing success rate
                allTasks.setAll(TaskController.getTasks());
                refreshDailyProgram(selectedExam);
                refreshExamStatsForExam(selectedExam);
                currentChatExam = selectedExam;
                chatArea.setText("Exam selected: " + selectedExam.getTitle()
                        + "\nI only answer questions related to '" + selectedExam.getTitle() + "'."
                        + "\nAsk me anything about " + selectedExam.getTitle() + "!");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // --- AI Chatbot (Groq API) ---
        Label groqChatTitle = new Label("AI Study Assistant (Groq)");
        groqChatTitle.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        groqChatTitle.setTextFill(Color.web("#7C3AED"));

        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(200);
        chatArea.setMinHeight(200);
        chatArea.setStyle("-fx-font-size: 12px; -fx-font-family: Arial; -fx-background-color: #F5F3FF;");
        chatArea.setText("Select an exam above to start chatting.");

        chatInput.setPromptText("Ask something about the selected exam...");
        chatInput.setPrefWidth(380);
        chatInput.setStyle("-fx-font-size: 12px;");

        Button groqBtnSend = navBtn("Send", "#7c3aed");
        groqBtnSend.setOnAction(e -> handleGroqChat());
        chatInput.setOnAction(e -> handleGroqChat());

        HBox groqChatInputRow = new HBox(8, chatInput, groqBtnSend);
        groqChatInputRow.setAlignment(Pos.CENTER_LEFT);

        VBox chatBox = new VBox(8, groqChatTitle, chatArea, groqChatInputRow);
        chatBox.setPadding(new Insets(12));
        chatBox.setPrefWidth(460);
        chatBox.setStyle(
                "-fx-background-color: white; -fx-border-color: #DDD6FE; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5;");
        HBox.setHgrow(chatBox, Priority.ALWAYS);

        HBox contentRow = new HBox(15, programSection, chatBox);
        contentRow.setAlignment(Pos.TOP_LEFT);

        VBox advPanel = new VBox(12, advTitle, selectorRow, statsRow2, contentRow);
        advPanel.setPadding(new Insets(16));
        advPanel.setStyle(
                "-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-background-radius: 12;");

        VBox innerPage = new VBox(15, header, examTable, form, advPanel);
        innerPage.setPadding(new Insets(20));
        innerPage.setStyle("-fx-background-color: #f0f2f5;");

        ScrollPane pageScroll = new ScrollPane(innerPage);
        pageScroll.setFitToWidth(true);
        pageScroll.setStyle("-fx-background: #f0f2f5; -fx-background-color: #f0f2f5;");
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox page = new VBox(pageScroll);
        page.setStyle("-fx-background-color: #f0f2f5;");
        VBox.setVgrow(pageScroll, Priority.ALWAYS);
        return page;
    }

    private VBox examStatCard(String label, Label valueLabel, String color, String subtitle) {
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        valueLabel.setTextFill(Color.web(color));
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web("#374151"));
        Label sub = new Label(subtitle);
        sub.setFont(Font.font("Arial", 11));
        sub.setTextFill(Color.web("#6B7280"));
        sub.setWrapText(true);
        VBox card = new VBox(4, valueLabel, lbl, sub);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setPrefWidth(220);
        card.setStyle("-fx-border-color:" + color
                + ";-fx-border-radius:10;-fx-background-radius:10;-fx-background-color:white;-fx-border-width:2;");
        return card;
    }

    private void refreshExamStatsForExam(Exam exam) {
        // Always reload fresh from DB to get latest task titles
        ObservableList<Task> freshTasks = TaskController.getTasks();
        String examName = exam.getTitle().trim().toLowerCase();

        // STRICT: only tasks whose current title is EXACTLY the exam name
        long total = freshTasks.stream()
                .filter(t -> t.getTitle().trim().toLowerCase().equals(examName))
                .count();
        long done = freshTasks.stream()
                .filter(t -> t.getTitle().trim().toLowerCase().equals(examName))
                .filter(t -> "done".equalsIgnoreCase(t.getStatus()))
                .count();

        if (total == 0) {
            lblSuccessRate.setText("0%");
            lblSuccessRate.setTextFill(Color.web("#6B7280"));
        } else {
            double rate = (done * 100.0 / total);
            lblSuccessRate.setText(String.format("%.0f%%", rate));
            if (rate >= 100) {
                lblSuccessRate.setTextFill(Color.web("#059669")); // green
            } else if (rate >= 50) {
                lblSuccessRate.setTextFill(Color.web("#D97706")); // orange
            } else {
                lblSuccessRate.setTextFill(Color.web("#DC2626")); // red
            }
        }
    }

    // ================================================================
    // VALIDATION - TASK
    // ================================================================
    private boolean validateTaskForm() {
        boolean valid = true;
        resetFieldStyles(taskTitle, taskEstMin);

        // ---- All required fields ----
        if (taskTitle.getText().trim().isEmpty()) {
            highlightError(taskTitle);
            taskMsg("Error: Title is required.", true);
            valid = false;
        }
        if (valid && taskTitle.getText().trim().length() > 100) {
            highlightError(taskTitle);
            taskMsg("Error: Title must be 100 characters or less.", true);
            valid = false;
        }
        if (valid && taskDesc.getText().trim().isEmpty()) {
            taskMsg("Error: Description is required.", true);
            valid = false;
        }
        if (valid && taskStatus.getValue() == null) {
            taskMsg("Error: Please select a status.", true);
            valid = false;
        }
        if (valid && taskPriority.getValue() == null) {
            taskMsg("Error: Please select a priority.", true);
            valid = false;
        }
        if (valid && taskDueDatePicker.getValue() == null) {
            taskMsg("Error: Due Date is required.", true);
            valid = false;
        }
        if (valid && taskEstMin.getText().trim().isEmpty()) {
            highlightError(taskEstMin);
            taskMsg("Error: Duration (min) is required.", true);
            valid = false;
        }
        // ---- Check exact same datetime (same date + same time) - any subject ----
        if (valid && taskDueDatePicker.getValue() != null) {
            String newDateTime = getTaskDueDateTime();
            Task sel = taskTable.getSelectionModel().getSelectedItem();
            int editingId = (sel != null) ? sel.getId() : -1;

            // Block if SAME date+time regardless of subject
            boolean sameDateTime = allTasks.stream()
                    .filter(t -> t.getId() != editingId)
                    .anyMatch(t -> newDateTime.equals(t.getDueDate()));
            if (sameDateTime) {
                taskMsg("Error: Another task already exists at the same date and time ("
                        + newDateTime + "). Please choose a different time.", true);
                valid = false;
            }
        }

        // ---- Duration (min) validation (required) ----
        if (valid) {
            try {
                int em = Integer.parseInt(taskEstMin.getText().trim());
                if (em <= 0) {
                    highlightError(taskEstMin);
                    taskMsg("Error: Duration must be a positive number.", true);
                    valid = false;
                } else if (em > 180) {
                    highlightError(taskEstMin);
                    taskMsg("Error: A single task cannot exceed 180 minutes.", true);
                    valid = false;
                }
            } catch (NumberFormatException ex) {
                highlightError(taskEstMin);
                taskMsg("Error: Duration must be a valid number (e.g. 60).", true);
                valid = false;
            }
        }

        // ---- Max 3 tasks per day + max 180 min total per day (same subject/title
        // only) ----
        if (valid && taskDueDatePicker.getValue() != null) {
            LocalDate selectedDay = taskDueDatePicker.getValue();
            String newTitle = taskTitle.getText().trim().toLowerCase();
            Task selTask = taskTable.getSelectionModel().getSelectedItem();
            int editingId2 = (selTask != null) ? selTask.getId() : -1;

            // Tasks on the same day AND same subject (title) only
            java.util.List<Task> sameDaySameSubject = allTasks.stream()
                    .filter(t -> t.getId() != editingId2)
                    .filter(t -> t.getTitle().equalsIgnoreCase(newTitle))
                    .filter(t -> {
                        if (t.getDueDate() == null || t.getDueDate().isEmpty())
                            return false;
                        try {
                            return LocalDateTime.parse(t.getDueDate(), DT_FORMAT)
                                    .toLocalDate().equals(selectedDay);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            // Check: max 3 tasks per day for the same subject
            if (sameDaySameSubject.size() >= 3) {
                taskMsg("Error: You already have 3 tasks of '" + taskTitle.getText().trim()
                        + "' on " + selectedDay + ". Maximum is 3 tasks per subject per day.", true);
                valid = false;
            }

            // Check: total duration per day for same subject max 180 min
            if (valid) {
                int existingMinutes = sameDaySameSubject.stream().mapToInt(Task::getEstimatedMinutes).sum();
                int newTaskMinutes = parseEstMin();
                if (existingMinutes + newTaskMinutes > 180) {
                    highlightError(taskEstMin);
                    taskMsg("Error: Total duration for '" + taskTitle.getText().trim()
                            + "' on " + selectedDay + " would be "
                            + (existingMinutes + newTaskMinutes)
                            + " min. Max is 180 min (already: " + existingMinutes + " min).", true);
                    valid = false;
                }
            }
        }

        return valid;
    }

    // ================================================================
    // VALIDATION - EXAM
    // ================================================================
    private boolean validateExamForm() {
        boolean valid = true;
        resetFieldStyles(examTitle, examDuration);

        if (examTitle.getText().trim().isEmpty()) {
            highlightError(examTitle);
            examMsg("Error: Title is required.", true);
            valid = false;
        }
        if (valid && examTitle.getText().trim().length() > 100) {
            highlightError(examTitle);
            examMsg("Error: Title must be 100 characters or less.", true);
            valid = false;
        }
        if (valid && examDatePicker.getValue() == null) {
            examMsg("Error: Date is required.", true);
            valid = false;
        }
        if (valid && examDuration.getText().trim().isEmpty()) {
            highlightError(examDuration);
            examMsg("Error: Duration is required.", true);
            valid = false;
        }
        if (valid) {
            try {
                int dur = Integer.parseInt(examDuration.getText().trim());
                if (dur <= 0) {
                    highlightError(examDuration);
                    examMsg("Error: Duration must be a positive number.", true);
                    valid = false;
                } else if (dur > 240) {
                    highlightError(examDuration);
                    examMsg("Error: Duration cannot exceed 240 minutes.", true);
                    valid = false;
                }
            } catch (NumberFormatException ex) {
                highlightError(examDuration);
                examMsg("Error: Duration must be a valid number.", true);
                valid = false;
            }
        }
        if (valid && examImportance.getValue() == null) {
            examMsg("Error: Please select an importance level.", true);
            valid = false;
        }

        // ---- Same date+time OR same hour same day → blocked ----
        if (valid && examDatePicker.getValue() != null) {
            String newDateTime = getExamDateTime();
            Exam selE = examTable.getSelectionModel().getSelectedItem();
            int editingId = (selE != null) ? selE.getId() : -1;

            // Exact same datetime
            boolean sameExact = allExams.stream()
                    .filter(ex -> ex.getId() != editingId)
                    .anyMatch(ex -> newDateTime.equals(ex.getExamDate()));
            if (sameExact) {
                examMsg("Error: Another exam already exists at the same date and time.", true);
                valid = false;
            }

            // Same hour on same day
            if (valid) {
                try {
                    LocalDateTime newDT = LocalDateTime.parse(newDateTime, DT_FORMAT);
                    boolean sameHour = allExams.stream()
                            .filter(ex -> ex.getId() != editingId)
                            .anyMatch(ex -> {
                                try {
                                    LocalDateTime exDT = LocalDateTime.parse(ex.getExamDate(),
                                            DT_FORMAT);
                                    return exDT.toLocalDate().equals(newDT.toLocalDate())
                                            && exDT.getHour() == newDT.getHour();
                                } catch (Exception e2) {
                                    return false;
                                }
                            });
                    if (sameHour) {
                        examMsg("Error: Another exam is at the same hour on this day. Please choose a different hour.",
                                true);
                        valid = false;
                    }
                } catch (Exception e2) {
                    /* skip */ }
            }
        }
        return valid;
    }

    // ================================================================
    // HELPERS
    // ================================================================
    private void refreshDashboard() {
        lblTasksInProgress.setText(
                String.valueOf(allTasks.stream().filter(t -> "in_progress".equalsIgnoreCase(t.getStatus())).count()));
        lblTasksTodo
                .setText(String.valueOf(allTasks.stream().filter(t -> "todo".equalsIgnoreCase(t.getStatus())).count()));
        lblTasksDone
                .setText(String.valueOf(allTasks.stream().filter(t -> "done".equalsIgnoreCase(t.getStatus())).count()));
        lblTasksOverdue.setText(String.valueOf(allTasks.stream()
                .filter(t -> isOverdue(t.getDueDate()) && !"done".equalsIgnoreCase(t.getStatus())).count()));
        lblTotalTasks.setText(String.valueOf(allTasks.size()));
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);
        lblExamsThisWeek.setText(String.valueOf(allExams.stream().filter(e -> {
            if (e.getExamDate() == null || e.getExamDate().isEmpty())
                return false;
            try {
                LocalDate d = LocalDateTime.parse(e.getExamDate(), DT_FORMAT).toLocalDate();
                return !d.isBefore(today) && !d.isAfter(endOfWeek);
            } catch (Exception ex) {
                return false;
            }
        }).count()));
    }

    private VBox dashCard(String label, Label valueLabel, String color) {
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        valueLabel.setTextFill(Color.web(color));
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Arial", 12));
        lbl.setTextFill(Color.web("#555555"));
        VBox card = new VBox(4, valueLabel, lbl);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14));
        card.setPrefWidth(140);
        card.setStyle("-fx-border-color:" + color
                + ";-fx-border-radius:8;-fx-background-radius:8;-fx-background-color:white;-fx-border-width:2;");
        return card;
    }

    private Button navBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color
                + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 14 6 14;");
        return b;
    }

    private void applyTaskFilter() {
        String s = filterPriority.getValue();
        if ("All".equals(s)) {
            taskTable.setItems(allTasks);
            return;
        }
        int p = s.startsWith("1") ? 1 : s.startsWith("3") ? 3 : 2;
        taskTable.setItems(allTasks.stream().filter(t -> t.getPriority() == p)
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    private void applyExamFilter() {
        String s = filterImportance.getValue();
        if ("All".equals(s)) {
            examTable.setItems(allExams);
            return;
        }
        int imp = Integer.parseInt(s);
        examTable.setItems(allExams.stream().filter(ex -> ex.getImportance() == imp)
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    // ---- DatePicker helpers ----
    private String getTaskDueDateTime() {
        if (taskDueDatePicker.getValue() == null)
            return "";
        String h = taskDueHour.getValue() != null ? taskDueHour.getValue() : "00";
        String m = taskDueMinute.getValue() != null ? taskDueMinute.getValue() : "00";
        return taskDueDatePicker.getValue().toString() + " " + h + ":" + m + ":00";
    }

    private String getExamDateTime() {
        if (examDatePicker.getValue() == null)
            return "";
        String h = examHour.getValue() != null ? examHour.getValue() : "00";
        String m = examMinute.getValue() != null ? examMinute.getValue() : "00";
        return examDatePicker.getValue().toString() + " " + h + ":" + m + ":00";
    }

    // ================================================================
    // CALENDAR POPUPS
    // ================================================================
    private void showTaskPopup(Task t) {
        Stage popup = new Stage();
        popup.setTitle("Task Details");

        String status = t.getStatus() == null ? "-" : t.getStatus();
        String statusColor;
        switch (status.toLowerCase()) {
            case "in_progress":
                statusColor = "#378ADD";
                break;
            case "done":
                statusColor = "#1D9E75";
                break;
            case "todo":
                statusColor = "#EF9F27";
                break;
            default:
                statusColor = "#888780";
                break;
        }
        if (isOverdue(t.getDueDate()) && !"done".equalsIgnoreCase(status))
            statusColor = "#E24B4A";

        Label titleLbl = new Label(t.getTitle());
        titleLbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLbl.setTextFill(Color.web("#222222"));

        Label badge = new Label("  " + status.toUpperCase() + "  ");
        badge.setStyle("-fx-background-color: " + statusColor
                + "; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox titleRow = new HBox(10, titleLbl, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox details = new VBox(8,
                titleRow,
                detailRow("📋 Description:",
                        t.getDescription() != null && !t.getDescription().isEmpty() ? t.getDescription() : "-"),
                detailRow("📅 Due Date:", t.getDueDate() != null && !t.getDueDate().isEmpty() ? t.getDueDate() : "-"),
                detailRow("⚡ Priority:", t.getPriority() == 1 ? "Low" : t.getPriority() == 3 ? "High" : "Medium"),
                detailRow("👤 Owner ID:", String.valueOf(t.getOwnerId())));
        details.setPadding(new Insets(20));
        details.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        details.setPrefWidth(320);

        Button btnClose = new Button("Close");
        btnClose.setStyle(
                "-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 20 6 20;");
        btnClose.setOnAction(e -> popup.close());

        Button btnGoTask = new Button("Open in Tasks →");
        btnGoTask.setStyle(
                "-fx-background-color: #378ADD; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 20 6 20;");
        btnGoTask.setOnAction(e -> {
            showPage(tasksPage);
            popup.close();
        });

        HBox btnRow = new HBox(10, btnGoTask, btnClose);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 20, 15, 20));

        VBox root = new VBox(0, details, btnRow);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        popup.setScene(new Scene(root));
        popup.setResizable(false);
        popup.show();
    }

    private void showExamPopup(Exam ex) {
        Stage popup = new Stage();
        popup.setTitle("Exam Details");

        int imp = ex.getImportance();
        String impColor = imp <= 2 ? "#1D9E75" : imp == 3 ? "#EF9F27" : "#E24B4A";
        String impText = imp <= 2 ? "Low" : imp == 3 ? "Medium" : "High";

        Label titleLbl = new Label(ex.getTitle());
        titleLbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLbl.setTextFill(Color.web("#222222"));

        Label badge = new Label("  Importance: " + impText + "  ");
        badge.setStyle("-fx-background-color: " + impColor
                + "; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox titleRow = new HBox(10, titleLbl, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox details = new VBox(8,
                titleRow,
                detailRow("📋 Description:",
                        ex.getDescription() != null && !ex.getDescription().isEmpty() ? ex.getDescription() : "-"),
                detailRow("📅 Date:", ex.getExamDate() != null && !ex.getExamDate().isEmpty() ? ex.getExamDate() : "-"),
                detailRow("⏱ Duration:", ex.getDurationMinutes() + " minutes"),
                detailRow("📍 Location:",
                        ex.getLocation() != null && !ex.getLocation().isEmpty() ? ex.getLocation() : "-"),
                detailRow("👤 Owner ID:", String.valueOf(ex.getOwnerId())));
        details.setPadding(new Insets(20));
        details.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        details.setPrefWidth(340);

        Button btnClose = new Button("Close");
        btnClose.setStyle(
                "-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 20 6 20;");
        btnClose.setOnAction(e -> popup.close());

        Button btnGoExam = new Button("Open in Exams →");
        btnGoExam.setStyle(
                "-fx-background-color: #E24B4A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 20 6 20;");
        btnGoExam.setOnAction(e -> {
            showPage(examsPage);
            popup.close();
        });

        HBox btnRow = new HBox(10, btnGoExam, btnClose);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 20, 15, 20));

        VBox root = new VBox(0, details, btnRow);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        popup.setScene(new Scene(root));
        popup.setResizable(false);
        popup.show();
    }

    private HBox detailRow(String label, String value) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web("#666666"));
        lbl.setMinWidth(120);

        Label val = new Label(value);
        val.setFont(Font.font("Arial", 12));
        val.setTextFill(Color.web("#222222"));
        val.setWrapText(true);

        HBox row = new HBox(10, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent; -fx-border-width: 0 0 1 0;");
        return row;
    }

    // ================================================================
    // NOTIFICATIONS
    // ================================================================
    private void refreshNotifications() {
        notificationBox.getChildren().clear();
        LocalDate today = LocalDate.now();
        LocalDate in2Days = today.plusDays(2);
        boolean hasNotif = false;

        // ---- Tasks notifications ----
        for (Task t : allTasks) {
            String due = t.getDueDate();
            if (due == null || due.isEmpty())
                continue;
            try {
                LocalDate dueDay = LocalDateTime.parse(due, DT_FORMAT).toLocalDate();

                // Done task notification (green)
                if ("done".equalsIgnoreCase(t.getStatus())) {
                    notificationBox.getChildren().add(notifRow(
                            "[OK] Task '" + t.getTitle() + "' is completed.",
                            "#DCFCE7", "#166534"));
                    hasNotif = true;
                }
                // Task approaching in <= 2 days and not done (red)
                else if (!dueDay.isBefore(today) && !dueDay.isAfter(in2Days)) {
                    long daysLeft = today.until(dueDay, java.time.temporal.ChronoUnit.DAYS);
                    String dayText = daysLeft == 0 ? "today!" : "in " + daysLeft + " day(s)";
                    notificationBox.getChildren().add(notifRow(
                            "[!] Task '" + t.getTitle() + "' is due " + dayText + " (" + due + ")",
                            "#FEE2E2", "#991B1B"));
                    hasNotif = true;
                }
                // Overdue and not done (red)
                else if (dueDay.isBefore(today) && !"done".equalsIgnoreCase(t.getStatus())) {
                    notificationBox.getChildren().add(notifRow(
                            "[X] Task '" + t.getTitle() + "' is OVERDUE since " + due,
                            "#FEE2E2", "#991B1B"));
                    hasNotif = true;
                }
            } catch (Exception e) {
                /* skip */ }
        }

        // ---- Exams notifications ----
        for (Exam ex : allExams) {
            String examD = ex.getExamDate();
            if (examD == null || examD.isEmpty())
                continue;
            try {
                LocalDate examDay = LocalDateTime.parse(examD, DT_FORMAT).toLocalDate();

                // Exam approaching in <= 2 days (red)
                if (!examDay.isBefore(today) && !examDay.isAfter(in2Days)) {
                    long daysLeft = today.until(examDay, java.time.temporal.ChronoUnit.DAYS);
                    String dayText = daysLeft == 0 ? "today!" : "in " + daysLeft + " day(s)";
                    notificationBox.getChildren().add(notifRow(
                            "[!] Exam '" + ex.getTitle() + "' is " + dayText + " (" + examD + ")",
                            "#FEE2E2", "#991B1B"));
                    hasNotif = true;
                }
                // Exam passed (treated as done) - green
                else if (examDay.isBefore(today)) {
                    notificationBox.getChildren().add(notifRow(
                            "[OK] Exam '" + ex.getTitle() + "' has passed (" + examD + ")",
                            "#DCFCE7", "#166534"));
                    hasNotif = true;
                }
            } catch (Exception e) {
                /* skip */ }
        }

        if (!hasNotif) {
            notificationBox.getChildren().add(notifRow(
                    "✅  No urgent notifications - everything is on track!",
                    "#F0FDF4", "#166534"));
        }
    }

    private HBox notifRow(String message, String bgColor, String textColor) {
        Label lbl = new Label(message);
        lbl.setFont(Font.font("Arial", 11));
        lbl.setTextFill(Color.web(textColor));
        lbl.setWrapText(false);
        lbl.setMaxWidth(900);

        HBox row = new HBox(lbl);
        row.setPadding(new Insets(4, 10, 4, 10));
        row.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 5;");
        return row;
    }

    // ================================================================
    // EXAM ADVANCED METHODS
    // ================================================================

    // ---- AI Chatbot ----
    private void handleChatMessage() {
        String question = chatInput.getText().trim();
        if (question.isEmpty())
            return;
        chatArea.appendText("\n\n You: " + question);
        chatInput.clear();

        String response = generateAIResponse(question.toLowerCase());
        chatArea.appendText("\n Bot: " + response);
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    // Currently selected exam for chatbot context
    private Exam currentChatExam = null;

    private String generateAIResponse(String q) {
        Exam exam = currentChatExam;

        // No exam selected
        if (exam == null) {
            return "Please select an exam from the list above first. "
                    + "I only answer questions related to the selected exam subject.";
        }

        String subject = exam.getTitle();
        String subjectLower = subject.toLowerCase();

        // Compute stats fresh
        ObservableList<Task> freshTasks = TaskController.getTasks();
        long relatedTotal = freshTasks.stream()
                .filter(t -> t.getTitle().trim().equalsIgnoreCase(subject))
                .count();
        long relatedDone = freshTasks.stream()
                .filter(t -> t.getTitle().trim().equalsIgnoreCase(subject))
                .filter(t -> "done".equalsIgnoreCase(t.getStatus()))
                .count();
        long daysLeft = -1;
        try {
            LocalDate examDay = LocalDateTime.parse(exam.getExamDate(), DT_FORMAT).toLocalDate();
            daysLeft = LocalDate.now().until(examDay, java.time.temporal.ChronoUnit.DAYS);
        } catch (Exception e) {
            /* skip */ }

        String daysStr = daysLeft > 0 ? daysLeft + " day(s) left"
                : daysLeft == 0 ? "exam is TODAY!"
                        : "exam has passed";

        // Check if question is related to the selected exam subject
        boolean isAboutSubject = q.contains(subjectLower)
                || q.contains("prepare") || q.contains("preparer")
                || q.contains("study") || q.contains("etudier")
                || q.contains("success") || q.contains("reussite")
                || q.contains("rate") || q.contains("percent")
                || q.contains("task") || q.contains("tache")
                || q.contains("plan") || q.contains("program")
                || q.contains("schedule") || q.contains("planning")
                || q.contains("tip") || q.contains("conseil")
                || q.contains("advice") || q.contains("date")
                || q.contains("day") || q.contains("when")
                || q.contains("time") || q.contains("jour")
                || q.contains("hello") || q.contains("bonjour")
                || q.contains("hi") || q.contains("salut")
                || q.contains("help") || q.contains("aide");

        // If question is NOT about the exam subject → refuse politely
        if (!isAboutSubject) {
            return "I only answer questions about '" + subject + "'. "
                    + "Please ask me about: preparation, success rate, study plan, tasks, tips, or exam date for "
                    + subject + ".";
        }

        // Greetings
        if (q.contains("hello") || q.contains("bonjour") || q.contains("hi") || q.contains("salut")) {
            return "Hello! I am your assistant for '" + subject + "'. "
                    + daysStr + ". How can I help you?";
        }

        // Help
        if (q.contains("help") || q.contains("aide")) {
            return "For '" + subject + "' I can answer: "
                    + "how to prepare, success rate, study plan, tasks status, tips, exam date. "
                    + "Ask me anything about " + subject + "!";
        }

        // Prepare / Study
        if (q.contains("prepare") || q.contains("preparer") || q.contains("study") || q.contains("etudier")) {
            return "To prepare for '" + subject + "' (" + daysStr + "): "
                    + "(1) Follow the daily study program on the left. "
                    + "(2) Complete all '" + subject + "' tasks (" + relatedDone + "/" + relatedTotal + " done). "
                    + "(3) Review your notes and practice past exam questions.";
        }

        // Success rate
        if (q.contains("success") || q.contains("reussite") || q.contains("rate") || q.contains("percent")) {
            double rate = relatedTotal == 0 ? 0 : (relatedDone * 100.0 / relatedTotal);
            String result = String.format("'%s' success rate: %.0f%% (%d/%d tasks done).",
                    subject, rate, relatedDone, relatedTotal);
            if (rate >= 100)
                return result + " Excellent! You are fully ready!";
            if (rate >= 75)
                return result + " Great progress! Almost there.";
            if (rate >= 50)
                return result + " Halfway. Keep going!";
            return result + " You have work to do. Focus on completing '" + subject + "' tasks!";
        }

        // Study plan / program
        if (q.contains("plan") || q.contains("program") || q.contains("schedule") || q.contains("planning")) {
            return "Your daily study plan for '" + subject + "' is shown on the left. "
                    + daysStr + ". Follow it day by day!";
        }

        // Tasks
        if (q.contains("task") || q.contains("tache")) {
            if (relatedTotal == 0)
                return "No tasks named '" + subject + "' found. Add tasks with the same name as this exam!";
            return "'" + subject + "' tasks: " + relatedDone + " / " + relatedTotal + " done. "
                    + (relatedDone == relatedTotal ? "All done! You are ready!"
                            : "Complete the remaining " + (relatedTotal - relatedDone) + " task(s)!");
        }

        // Tips / Advice
        if (q.contains("tip") || q.contains("conseil") || q.contains("advice")) {
            return "Tips for '" + subject + "' (" + daysStr + "): "
                    + "(1) Study 2 hours minimum per day. "
                    + "(2) Rest the night before the exam. "
                    + "(3) Solve past questions on " + subject + ". "
                    + "(4) Make a concise summary sheet.";
        }

        // Date / Days
        if (q.contains("date") || q.contains("day") || q.contains("when") || q.contains("time") || q.contains("jour")) {
            return "Exam '" + subject + "' is on " + exam.getExamDate() + ". " + daysStr + ".";
        }

        // Default: refuse off-topic
        return "I only answer questions about '" + subject + "'. "
                + "Try asking: how to prepare, success rate, tasks, study plan, tips, or exam date.";
    }

    // ---- Daily Study Program (from creation date to exam date) ----
    private void refreshDailyProgram(Exam exam) {
        programBox.getChildren().clear();
        if (exam == null || exam.getExamDate() == null || exam.getExamDate().isEmpty())
            return;

        try {
            LocalDate examDay = LocalDateTime.parse(exam.getExamDate(), DT_FORMAT).toLocalDate();
            LocalDate today = LocalDate.now();
            long daysLeft = today.until(examDay, java.time.temporal.ChronoUnit.DAYS);
            long totalDays = 14; // default preparation period

            // Header
            Label header = new Label("Study Plan for: " + exam.getTitle()
                    + "  |  Exam: " + exam.getExamDate()
                    + "  |  Days left: " + Math.max(0, daysLeft));
            header.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            header.setTextFill(Color.web("#1E40AF"));
            header.setWrapText(true);
            programBox.getChildren().add(header);

            // Separator
            Region sep = new Region();
            sep.setPrefHeight(1);
            sep.setStyle("-fx-background-color: #BFDBFE;");
            programBox.getChildren().add(sep);

            if (daysLeft < 0) {
                Label done = new Label("This exam has already passed.");
                done.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                done.setTextFill(Color.web("#059669"));
                programBox.getChildren().add(done);
                return;
            }
            if (daysLeft == 0) {
                programBox.getChildren()
                        .add(progRow("TODAY IS EXAM DAY! Good luck! - " + exam.getTitle(), "#DC2626", true));
                return;
            }

            // Build plan: from today to exam day
            String[] activities = {
                    "Day 1 - Read and review course notes",
                    "Day 2 - Highlight key concepts",
                    "Day 3 - Practice exercises (chapter 1-2)",
                    "Day 4 - Practice exercises (chapter 3-4)",
                    "Day 5 - Create a summary sheet",
                    "Day 6 - Solve past exam questions",
                    "Day 7 - Review weak areas",
                    "Day 8 - Mock exam (full)",
                    "Day 9 - Correct mock exam errors",
                    "Day 10 - Light review + rest",
                    "Day 11 - Final review of summary",
                    "Day 12 - Rest and preparation",
                    "Day 13 - Light reading only",
            };

            long planDays = Math.min(daysLeft, activities.length);
            for (long i = 0; i < planDays; i++) {
                LocalDate planDay = today.plusDays(i);
                String activityLabel = i < activities.length ? activities[(int) i] : "Study and review";
                String dayLabel;
                String color;
                boolean bold;

                if (i == 0) {
                    dayLabel = "TODAY (" + planDay + ") - " + activityLabel;
                    color = "#059669";
                    bold = true;
                } else if (i == daysLeft - 1) {
                    dayLabel = "DAY BEFORE EXAM (" + planDay + ") - Rest and light review";
                    color = "#D97706";
                    bold = true;
                } else {
                    dayLabel = "Day " + (i + 1) + " (" + planDay + ") - " + activityLabel;
                    color = "#374151";
                    bold = false;
                }
                programBox.getChildren().add(progRow(dayLabel, color, bold));
            }

            // Exam day row
            programBox.getChildren().add(progRow(
                    "EXAM DAY (" + examDay + ") - " + exam.getTitle()
                            + " at " + exam.getExamDate() + " | Duration: " + exam.getDurationMinutes() + " min",
                    "#DC2626", true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox progRow(String text, String textColor) {
        return progRow(text, textColor, false);
    }

    private HBox progRow(String text, String textColor, boolean bold) {
        Label lbl = new Label("• " + text);
        lbl.setFont(bold ? Font.font("Arial", FontWeight.BOLD, 12) : Font.font("Arial", 12));
        lbl.setTextFill(Color.web(textColor));
        lbl.setWrapText(true);
        lbl.setMaxWidth(420);
        String bg = "#DC2626".equals(textColor) ? "#FEF2F2"
                : "#059669".equals(textColor) ? "#F0FDF4"
                        : "#D97706".equals(textColor) ? "#FFFBEB"
                                : "transparent";
        HBox row = new HBox(lbl);
        row.setPadding(new Insets(5, 8, 5, 8));
        row.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:5;");
        return row;
    }

    // ---- Success Rate & Creation Rate ----
    private void refreshExamStats() {
        long totalTasks = allTasks.size();
        long doneTasks = allTasks.stream().filter(t -> "done".equalsIgnoreCase(t.getStatus())).count();

        // Success rate = % of tasks done globally (exam readiness)
        double successRate = totalTasks == 0 ? 0 : (doneTasks * 100.0 / totalTasks);
        lblSuccessRate.setText(String.format("%.0f%%", successRate));

        // Creation rate per exam: % of exams where related tasks (same title) are done
        // e.g. exam "java" → tasks titled "java" done / total "java" tasks
        if (allExams.isEmpty()) {
            lblCreationRate.setText("-");
            return;
        }

        double totalRate = 0;
        int counted = 0;
        for (Exam ex : allExams) {
            String examTitle2 = ex.getTitle().toLowerCase();
            long relatedTotal = allTasks.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(examTitle2)
                            || examTitle2.contains(t.getTitle().toLowerCase()))
                    .count();
            long relatedDone = allTasks.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(examTitle2)
                            || examTitle2.contains(t.getTitle().toLowerCase()))
                    .filter(t -> "done".equalsIgnoreCase(t.getStatus()))
                    .count();
            if (relatedTotal > 0) {
                totalRate += (relatedDone * 100.0 / relatedTotal);
                counted++;
            }
        }
        double creationRate = counted == 0 ? 0 : totalRate / counted;
        lblCreationRate.setText(String.format("%.0f%%", creationRate));
    }

    // ---- Per-exam success rate popup (called from exam row click) ----
    private String getExamSuccessDetail(Exam ex) {
        String examTitle2 = ex.getTitle().toLowerCase();
        long relatedTotal = allTasks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(examTitle2)
                        || examTitle2.contains(t.getTitle().toLowerCase()))
                .count();
        long relatedDone = allTasks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(examTitle2)
                        || examTitle2.contains(t.getTitle().toLowerCase()))
                .filter(t -> "done".equalsIgnoreCase(t.getStatus()))
                .count();
        if (relatedTotal == 0)
            return "No related tasks found for exam '" + ex.getTitle() + "'.";
        double rate = relatedDone * 100.0 / relatedTotal;
        return String.format("Exam '%s': %d/%d related tasks done = %.0f%% readiness.",
                ex.getTitle(), relatedDone, relatedTotal, rate);
    }

    // ================================================================
    // GROQ AI CHATBOT
    // ================================================================
    private static final GroqConfig groqConfig = GroqConfig.getInstance();
    private static final String GROQ_API_KEY = groqConfig.getApiKey();
    private static final String GROQ_URL = groqConfig.getApiUrl();
    private static final String GROQ_MODEL = groqConfig.getModel();

    private void handleGroqChat() {
        String userMsg = chatInput.getText().trim();
        if (userMsg.isEmpty())
            return;

        if (GROQ_API_KEY == null || GROQ_API_KEY.isEmpty()) {
            chatArea.appendText("\nBot: ❌ Error: GROQ_API_KEY is not configured.\n"
                    + "Please set your Groq API key in src/main/resources/config.properties\n"
                    + "(Get your key from https://console.groq.com/keys)");
            return;
        }

        if (currentChatExam == null) {
            chatArea.appendText("\nBot: Please select an exam first.");
            return;
        }

        String examSubject = currentChatExam.getTitle();
        String examDate = currentChatExam.getExamDate();

        // --- Client-side guard: refuse if question mentions another exam's name ---
        String userMsgLower = userMsg.toLowerCase();
        for (Exam otherExam : allExams) {
            if (otherExam.getId() != currentChatExam.getId()) {
                String otherTitle = otherExam.getTitle().toLowerCase();
                if (userMsgLower.contains(otherTitle)) {
                    chatArea.appendText("\n\nYou: " + userMsg);
                    chatArea.appendText("\nBot: Sorry, I only answer questions about '"
                            + examSubject + "'. Please ask me about "
                            + examSubject + " instead.");
                    chatInput.clear();
                    chatArea.setScrollTop(Double.MAX_VALUE);
                    return;
                }
            }
        }

        chatInput.clear();
        chatArea.appendText("\n\nYou: " + userMsg);
        chatArea.appendText("\nBot: ...");
        chatArea.setScrollTop(Double.MAX_VALUE);

        long total = allTasks.stream()
                .filter(t -> t.getTitle().trim().equalsIgnoreCase(examSubject)).count();
        long done = allTasks.stream()
                .filter(t -> t.getTitle().trim().equalsIgnoreCase(examSubject))
                .filter(t -> "done".equalsIgnoreCase(t.getStatus())).count();
        double rate = total == 0 ? 0 : (done * 100.0 / total);

        String systemPrompt = "You are a dedicated study assistant ONLY for the subject: '"
                + examSubject + "'. "
                + "Exam date: " + examDate + ". "
                + "Tasks completed: " + done + " out of " + total
                + " (" + String.format("%.0f", rate) + "% done). "
                + "IMPORTANT: You MUST ONLY answer questions about '" + examSubject + "'. "
                + "If the user asks about any other subject or topic completely unrelated to '" + examSubject + "', "
                + "you MUST reply: 'Sorry, I only answer questions about " + examSubject + ".' "
                + "You should answer all questions about " + examSubject
                + ": explanations, exercises, summaries, preparation tips, and examples. "
                + "Keep answers clear, helpful, and adapted for a student.";

        javafx.concurrent.Task<String> apiTask = new javafx.concurrent.Task<String>() {
            @Override
            protected String call() throws Exception {
                // Build JSON using StringBuilder - avoid escape issues
                StringBuilder bodyBuilder = new StringBuilder();
                bodyBuilder.append("{");
                appendJson(bodyBuilder, "model", GROQ_MODEL);
                bodyBuilder.append(",").append((char) 34).append("messages").append((char) 34).append(":[");
                bodyBuilder.append("{");
                appendJson(bodyBuilder, "role", "system");
                bodyBuilder.append(",");
                appendJson(bodyBuilder, "content", systemPrompt);
                bodyBuilder.append("},{");
                appendJson(bodyBuilder, "role", "user");
                bodyBuilder.append(",");
                appendJson(bodyBuilder, "content", userMsg);
                bodyBuilder.append("}],");
                bodyBuilder.append("\"max_tokens\":512");
                bodyBuilder.append("}");

                String body = bodyBuilder.toString();

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GROQ_URL))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Authorization", "Bearer " + GROQ_API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(body, java.nio.charset.StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> resp = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                int statusCode = resp.statusCode();
                String responseBody = resp.body();
                if (statusCode != 200) {
                    // Extract Groq error message for display
                    return extractErrorMessage(responseBody, statusCode);
                }
                return extractContent(responseBody);
            }
        };

        apiTask.setOnSucceeded(ev -> {
            String answer = apiTask.getValue();
            String cur = chatArea.getText();
            int pos = cur.lastIndexOf("\nBot: ...");
            if (pos != -1)
                chatArea.setText(cur.substring(0, pos) + "\nBot: " + answer);
            else
                chatArea.appendText("\nBot: " + answer);
            chatArea.setScrollTop(Double.MAX_VALUE);
        });

        apiTask.setOnFailed(ev -> {
            String cur = chatArea.getText();
            int pos = cur.lastIndexOf("\nBot: ...");
            if (pos != -1)
                chatArea.setText(cur.substring(0, pos)
                        + "\nBot: Connection error. Check your internet.");
        });

        new Thread(apiTask).start();
    }

    private void appendJson(StringBuilder sb, String key, String value) {
        // char 34 = double-quote, char 92 = backslash
        sb.append((char) 34).append(key).append((char) 34).append(':').append((char) 34);
        for (char c : value.toCharArray()) {
            if (c == 34)
                sb.append((char) 92).append((char) 34); // escape "
            else if (c == 92)
                sb.append((char) 92).append((char) 92); // escape \
            else if (c == '\n')
                sb.append((char) 92).append('n'); // escape newline
            else if (c == '\r')
                sb.append((char) 92).append('r'); // escape CR
            else if (c == '\t')
                sb.append((char) 92).append('t'); // escape tab
            else
                sb.append(c);
        }
        sb.append((char) 34);
    }

    /** Extract a human-readable error from a non-200 Groq response body */
    private String extractErrorMessage(String json, int statusCode) {
        try {
            // Groq error: {"error":{"message":"..."}}
            int mi = json.indexOf("\"message\":");
            if (mi != -1) {
                int qi = json.indexOf('"', mi + 10) + 1;
                int qe = json.indexOf('"', qi);
                if (qi > 0 && qe > qi) {
                    return "[API Error " + statusCode + "] " + json.substring(qi, qe);
                }
            }
        } catch (Exception ignored) {
        }
        return "[API Error " + statusCode + "] Please check your API key or try again.";
    }

    private String extractContent(String json) {
        try {
            // Groq success response has "content" in the assistant message
            // We look for the LAST occurrence which is the assistant reply
            int ti = json.lastIndexOf("\"content\":");
            if (ti == -1) {
                // Fallback: check for Groq error message
                int mi = json.indexOf("\"message\":");
                if (mi != -1) {
                    int qi = json.indexOf('"', mi + 10) + 1;
                    int qe = json.indexOf('"', qi);
                    if (qi > 0 && qe > qi)
                        return "[API] " + json.substring(qi, qe);
                }
                return "No response from AI. Check internet connection.";
            }
            int qi = json.indexOf(':', ti) + 1;
            while (qi < json.length() && (json.charAt(qi) == ' ' || json.charAt(qi) == '\t'))
                qi++;
            if (qi >= json.length() || json.charAt(qi) != '"')
                return "Parse error.";
            qi++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (qi < json.length()) {
                char c = json.charAt(qi);
                if (c == '\\' && qi + 1 < json.length()) {
                    qi++;
                    char nc = json.charAt(qi);
                    if (nc == 'n')
                        sb.append('\n');
                    else if (nc == 't')
                        sb.append('\t');
                    else if (nc == 'r')
                        sb.append('\r');
                    else if (nc == '"')
                        sb.append('"');
                    else
                        sb.append(nc);
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
                qi++;
            }
            return sb.length() > 0 ? sb.toString() : "Empty response from AI.";
        } catch (Exception e) {
            return "Error parsing response: " + e.getMessage();
        }
    }

    private boolean isValidDateTime(String v) {
        try {
            LocalDateTime.parse(v, DT_FORMAT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isOverdue(String d) {
        if (d == null || d.isEmpty())
            return false;
        try {
            return LocalDateTime.parse(d, DT_FORMAT).isBefore(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void highlightError(TextField f) {
        f.setStyle("-fx-border-color:#E24B4A;-fx-border-width:2;-fx-border-radius:4;");
    }

    private void resetFieldStyles(TextField... fields) {
        for (TextField f : fields)
            f.setStyle("");
    }

    private int taskPriorityInt() {
        String v = taskPriority.getValue();
        return v.startsWith("1") ? 1 : v.startsWith("3") ? 3 : 2;
    }

    private int parseEstMin() {
        try {
            int v = Integer.parseInt(taskEstMin.getText().trim());
            return Math.max(0, v);
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    private void loadTasks() {
        allTasks.setAll(TaskController.getTasks());
        applyTaskFilter();
        refreshDashboard();
        refreshNotifications();
        refreshExamStats();
    }

    private void loadExams() {
        allExams.setAll(ExamController.getExams());
        applyExamFilter();
        refreshDashboard();
        refreshNotifications();
        refreshExamStats();
        // Refresh exam selector
        examSelector.getItems().clear();
        for (Exam ex : allExams) {
            examSelector.getItems().add(ex.getId() + " - " + ex.getTitle() + " (" + ex.getExamDate() + ")");
        }
    }

    private void clearTaskForm() {
        taskTitle.clear();
        taskDesc.clear();
        taskDueDatePicker.setValue(null);
        taskDueHour.setValue("08");
        taskDueMinute.setValue("00");
        taskStatus.setValue("todo");
        taskPriority.setValue("2 - Medium");
        taskEstMin.clear();
        resetFieldStyles(taskTitle, taskEstMin);
        taskMsg.setText("");
    }

    private void clearExamForm() {
        examTitle.clear();
        examDesc.clear();
        examDatePicker.setValue(null);
        examHour.setValue("08");
        examMinute.setValue("00");
        examDuration.clear();
        examLocation.clear();
        examImportance.setValue("3");
        resetFieldStyles(examTitle, examDuration);
        examMsg.setText("");
    }

    private void taskMsg(String msg, boolean err) {
        taskMsg.setText(msg);
        taskMsg.setStyle(
                err ? "-fx-text-fill:#E24B4A;-fx-font-size:12px;" : "-fx-text-fill:#1D9E75;-fx-font-size:12px;");
    }

    private void examMsg(String msg, boolean err) {
        examMsg.setText(msg);
        examMsg.setStyle(
                err ? "-fx-text-fill:#E24B4A;-fx-font-size:12px;" : "-fx-text-fill:#1D9E75;-fx-font-size:12px;");
    }


}