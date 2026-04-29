package com.mindforge.architect.controller;

import com.mindforge.util.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SocialHubController — LOCAL JSON STORAGE. Zero database writes for social features.
 */
public class SocialHubController implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private Label      headerTitle;
    @FXML private Label      notificationBadge;
    @FXML private Button     btnDiscover;
    @FXML private Button     btnFriends;
    @FXML private Button     btnPending;
    @FXML private Button     btnMessages;
    @FXML private Button     btnBackToProfile;
    @FXML private TextField  searchField;
    @FXML private VBox       contentBox;
    @FXML private ScrollPane contentScroll;

    private int currentUserId;
    private String currentUserEmail;
    private Timer pollTimer;
    private int activeConversationPartnerId = -1;
    private ObservableList<UserCard> allUsersCache = FXCollections.observableArrayList();

    private final File socialDir;
    private final File friendsFile;
    private final File messagesFile;
    private final File notificationsFile;
    private final Gson gson;

    private List<Friendship> friendships = new ArrayList<>();
    private List<LocalMessage> messages = new ArrayList<>();
    private List<LocalNotification> notifications = new ArrayList<>();

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/avatars/";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public SocialHubController() {
        String home = System.getProperty("user.home");
        this.socialDir = new File(home, ".mindforge/social");
        this.friendsFile = new File(socialDir, "friends.json");
        this.messagesFile = new File(socialDir, "messages.json");
        this.notificationsFile = new File(socialDir, "notifications.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUserId    = UserSession.getInstance().getUserId();
        currentUserEmail = UserSession.getInstance().getEmail();
        socialDir.mkdirs();
        loadFriendships();
        loadMessages();
        loadNotifications();
        showDiscover();
        refreshBadge();
        searchField.textProperty().addListener((obs, old, nw) -> {
            if (nw != null) filterUsers(nw.trim().toLowerCase());
        });
        startPolling();
    }

    // ═══════════════════════════════════════════════════════════════
    // JSON PERSISTENCE
    // ═══════════════════════════════════════════════════════════════
    private synchronized void loadFriendships() {
        if (!friendsFile.exists()) { friendships = new ArrayList<>(); return; }
        try (Reader r = new FileReader(friendsFile)) {
            List<Friendship> loaded = gson.fromJson(r, new TypeToken<List<Friendship>>(){}.getType());
            friendships = loaded != null ? loaded : new ArrayList<>();
        } catch (Exception e) { e.printStackTrace(); friendships = new ArrayList<>(); }
    }

    private synchronized void saveFriendships() {
        try (Writer w = new FileWriter(friendsFile)) { gson.toJson(friendships, w); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private synchronized void loadMessages() {
        if (!messagesFile.exists()) { messages = new ArrayList<>(); return; }
        try (Reader r = new FileReader(messagesFile)) {
            List<LocalMessage> loaded = gson.fromJson(r, new TypeToken<List<LocalMessage>>(){}.getType());
            messages = loaded != null ? loaded : new ArrayList<>();
        } catch (Exception e) { e.printStackTrace(); messages = new ArrayList<>(); }
    }

    private synchronized void saveMessages() {
        try (Writer w = new FileWriter(messagesFile)) { gson.toJson(messages, w); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private synchronized void loadNotifications() {
        if (!notificationsFile.exists()) { notifications = new ArrayList<>(); return; }
        try (Reader r = new FileReader(notificationsFile)) {
            List<LocalNotification> loaded = gson.fromJson(r, new TypeToken<List<LocalNotification>>(){}.getType());
            notifications = loaded != null ? loaded : new ArrayList<>();
        } catch (Exception e) { e.printStackTrace(); notifications = new ArrayList<>(); }
    }

    private synchronized void saveNotifications() {
        try (Writer w = new FileWriter(notificationsFile)) { gson.toJson(notifications, w); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════
    @FXML private void onDiscover()   { showDiscover(); }
    @FXML private void onFriends()    { showFriends(); }
    @FXML private void onPending()    { showPending(); }
    @FXML private void onMessages()   { showMessagesList(); }

    @FXML private void onBackToProfile() {
        navigateTo("/com/mindforge/fxml/profile.fxml", "MindForge - Profile");
    }

    // ═══════════════════════════════════════════════════════════════
    // DISCOVER USERS
    // ═══════════════════════════════════════════════════════════════
    private void showDiscover() {
        setActiveTab(btnDiscover);
        headerTitle.setText("Discover People");
        searchField.setPromptText("Search by name or email...");
        searchField.setVisible(true);
        contentBox.getChildren().clear();

        Task<List<UserCard>> task = new Task<>() {
            @Override protected List<UserCard> call() { return loadAllUsers(); }
            @Override protected void succeeded() {
                allUsersCache.setAll(getValue());
                renderUserGrid(allUsersCache);
            }
        };
        new Thread(task).start();
    }

    private List<UserCard> loadAllUsers() {
        List<UserCard> list = new ArrayList<>();
        String sql = """
            SELECT u.id, u.email, u.created_at,
                   p.first_name, p.last_name, p.avatar, p.bio
            FROM user u
            LEFT JOIN profile p ON p.user_id = u.id
            WHERE u.id != ?
            ORDER BY p.first_name, p.last_name, u.email
            """;
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int uid = rs.getInt("id");
                list.add(new UserCard(
                        uid,
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("avatar"),
                        rs.getString("bio"),
                        rs.getTimestamp("created_at"),
                        getFriendshipStatus(uid)
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private String getFriendshipStatus(int otherId) {
        for (Friendship f : friendships) {
            if ((f.requesterId == currentUserId && f.addresseeId == otherId) ||
                    (f.requesterId == otherId && f.addresseeId == currentUserId)) {
                return f.status;
            }
        }
        return "none";
    }

    private void renderUserGrid(List<UserCard> users) {
        contentBox.getChildren().clear();
        if (users.isEmpty()) {
            contentBox.getChildren().add(emptyState("No users found", "Try a different search."));
            return;
        }
        FlowPane grid = new FlowPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(4, 0, 20, 0));
        grid.prefWrapLengthProperty().bind(contentScroll.widthProperty().subtract(40));
        for (UserCard u : users) grid.getChildren().add(buildUserCard(u));
        contentBox.getChildren().add(grid);
    }

    private VBox buildUserCard(UserCard u) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18, 16, 18, 16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
                + "-fx-border-color: #e8e8e8; -fx-border-width: 0.8; -fx-border-radius: 14;"
                + "-fx-min-width: 220; -fx-max-width: 260;");

        StackPane avWrap = new StackPane();
        Circle bg = new Circle(36, Color.web("#EEEDFE"));
        bg.setStroke(Color.web("#534AB7")); bg.setStrokeWidth(2);
        Label ini = new Label(u.initials);
        ini.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3C3489;");
        ImageView img = new ImageView(); img.setFitWidth(72); img.setFitHeight(72);
        img.setPreserveRatio(false); img.setClip(new Circle(36, 36, 36)); img.setVisible(false);
        loadAvatarAsync(u.avatar, img, ini);
        avWrap.getChildren().addAll(bg, ini, img);

        Label nameLbl = new Label(u.displayName);
        nameLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        nameLbl.setWrapText(true); nameLbl.setAlignment(Pos.CENTER);

        Label emailLbl = new Label(u.email);
        emailLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #999999;");

        Label bioLbl = new Label(u.bio != null && !u.bio.isBlank() ? u.bio : "No bio yet.");
        bioLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaaaaa;");
        bioLbl.setWrapText(true); bioLbl.setMaxWidth(200);

        Button actionBtn = new Button();
        styleActionButton(actionBtn, u);
        actionBtn.setOnAction(e -> handleFriendAction(u, actionBtn, card));

        Button msgBtn = new Button("Message");
        msgBtn.setVisible("accepted".equals(u.friendshipStatus));
        msgBtn.setManaged("accepted".equals(u.friendshipStatus));
        msgBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #534AB7;"
                + "-fx-border-width: 0.8; -fx-border-radius: 8; -fx-text-fill: #534AB7;"
                + "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 14;");
        msgBtn.setOnAction(e -> openChatWith(u.id, u.displayName, u.avatar));

        card.getChildren().addAll(avWrap, nameLbl, emailLbl, bioLbl, actionBtn, msgBtn);
        return card;
    }

    private void styleActionButton(Button btn, UserCard u) {
        switch (u.friendshipStatus) {
            case "none" -> {
                btn.setText("+ Add Friend");
                btn.setStyle("-fx-background-color: #534AB7; -fx-text-fill: white;"
                        + "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 18;");
            }
            case "pending" -> {
                boolean iSent = didISendRequest(u.id);
                if (iSent) {
                    btn.setText("Pending..."); btn.setDisable(true);
                    btn.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #888888;"
                            + "-fx-background-radius: 8; -fx-font-size: 12px; -fx-padding: 5 18;");
                } else {
                    btn.setText("Respond");
                    btn.setStyle("-fx-background-color: #E6F1FB; -fx-text-fill: #0C447C;"
                            + "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 18;");
                }
            }
            case "accepted" -> {
                btn.setText("Friends ✓"); btn.setDisable(true);
                btn.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;"
                        + "-fx-background-radius: 8; -fx-font-size: 12px; -fx-padding: 5 18;");
            }
            default -> {
                btn.setText("+ Add Friend");
                btn.setStyle("-fx-background-color: #534AB7; -fx-text-fill: white;"
                        + "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 18;");
            }
        }
    }

    private boolean didISendRequest(int toId) {
        for (Friendship f : friendships) {
            if (f.requesterId == currentUserId && f.addresseeId == toId && "pending".equals(f.status))
                return true;
        }
        return false;
    }

    private void handleFriendAction(UserCard u, Button btn, VBox card) {
        if ("none".equals(u.friendshipStatus) || "declined".equals(u.friendshipStatus)) {
            sendFriendRequest(u.id);
            u.friendshipStatus = "pending";
            styleActionButton(btn, u);
            Button msgBtn = (Button) card.getChildren().get(card.getChildren().size() - 1);
            msgBtn.setVisible(false); msgBtn.setManaged(false);
        } else if ("pending".equals(u.friendshipStatus) && !didISendRequest(u.id)) {
            showRespondDialog(u, btn, card);
        }
    }

    private void sendFriendRequest(int toUserId) {
        Friendship f = new Friendship();
        f.id = System.currentTimeMillis();
        f.requesterId = currentUserId;
        f.addresseeId = toUserId;
        f.status = "pending";
        f.createdAt = LocalDateTime.now().format(ISO_FMT);
        friendships.add(f);
        saveFriendships();

        LocalNotification n = new LocalNotification();
        n.id = System.currentTimeMillis();
        n.userId = toUserId;
        n.type = "friend_request";
        n.fromUserId = currentUserId;
        n.message = getCurrentUserName() + " sent you a friend request";
        n.isRead = false;
        n.createdAt = LocalDateTime.now().format(ISO_FMT);
        notifications.add(n);
        saveNotifications();
    }

    private void showRespondDialog(UserCard u, Button btn, VBox card) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Friend Request");
        alert.setHeaderText(u.displayName + " wants to be friends");
        alert.setContentText("Accept or decline this request?");
        ButtonType accept = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        ButtonType decline = new ButtonType("Decline", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accept, decline);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == accept) {
            acceptFriendRequest(u.id);
            u.friendshipStatus = "accepted";
        } else {
            declineFriendRequest(u.id);
            u.friendshipStatus = "declined";
        }
        styleActionButton(btn, u);
        Button msgBtn = (Button) card.getChildren().get(card.getChildren().size() - 1);
        boolean showMsg = "accepted".equals(u.friendshipStatus);
        msgBtn.setVisible(showMsg); msgBtn.setManaged(showMsg);
    }

    private void acceptFriendRequest(int fromUserId) {
        for (Friendship f : friendships) {
            if (f.requesterId == fromUserId && f.addresseeId == currentUserId && "pending".equals(f.status)) {
                f.status = "accepted";
                f.updatedAt = LocalDateTime.now().format(ISO_FMT);
                break;
            }
        }
        saveFriendships();

        LocalNotification n = new LocalNotification();
        n.id = System.currentTimeMillis();
        n.userId = fromUserId;
        n.type = "friend_accepted";
        n.fromUserId = currentUserId;
        n.message = getCurrentUserName() + " accepted your friend request";
        n.isRead = false;
        n.createdAt = LocalDateTime.now().format(ISO_FMT);
        notifications.add(n);
        saveNotifications();
    }

    private void declineFriendRequest(int fromUserId) {
        for (Friendship f : friendships) {
            if (f.requesterId == fromUserId && f.addresseeId == currentUserId && "pending".equals(f.status)) {
                f.status = "declined";
                f.updatedAt = LocalDateTime.now().format(ISO_FMT);
                break;
            }
        }
        saveFriendships();
    }

    // ═══════════════════════════════════════════════════════════════
    // FRIENDS LIST
    // ═══════════════════════════════════════════════════════════════
    private void showFriends() {
        setActiveTab(btnFriends);
        headerTitle.setText("My Friends");
        searchField.setPromptText("Search friends...");
        searchField.setVisible(true);
        contentBox.getChildren().clear();

        Task<List<FriendInfo>> task = new Task<>() {
            @Override protected List<FriendInfo> call() { return loadFriends(); }
            @Override protected void succeeded() { renderFriendsList(getValue()); }
        };
        new Thread(task).start();
    }

    private List<FriendInfo> loadFriends() {
        List<FriendInfo> list = new ArrayList<>();
        Set<Integer> friendIds = new HashSet<>();
        for (Friendship f : friendships) {
            if (!"accepted".equals(f.status)) continue;
            int otherId = (f.requesterId == currentUserId) ? f.addresseeId : f.requesterId;
            friendIds.add(otherId);
        }
        if (friendIds.isEmpty()) return list;

        String placeholders = friendIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT u.id, u.email, p.first_name, p.last_name, p.avatar, p.bio FROM user u " +
                "LEFT JOIN profile p ON p.user_id = u.id WHERE u.id IN (" + placeholders + ")";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (int id : friendIds) ps.setInt(i++, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new FriendInfo(
                        rs.getInt("id"), rs.getString("first_name"),
                        rs.getString("last_name"), rs.getString("email"),
                        rs.getString("avatar"), rs.getString("bio"), null
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private void renderFriendsList(List<FriendInfo> friends) {
        contentBox.getChildren().clear();
        if (friends.isEmpty()) {
            contentBox.getChildren().add(emptyState("No friends yet",
                    "Go to Discover to find and connect with people."));
            return;
        }
        VBox list = new VBox(8);
        for (FriendInfo f : friends) list.getChildren().add(buildFriendRow(f));
        contentBox.getChildren().add(list);
    }

    private HBox buildFriendRow(FriendInfo f) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                + "-fx-border-color: #f0f0f0; -fx-border-width: 0.5; -fx-border-radius: 12;");

        StackPane av = new StackPane();
        Circle c = new Circle(22, Color.web("#EEEDFE"));
        c.setStroke(Color.web("#534AB7")); c.setStrokeWidth(1.5);
        Label ini = new Label(f.initials);
        ini.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3C3489;");
        ImageView iv = new ImageView(); iv.setFitWidth(44); iv.setFitHeight(44);
        iv.setPreserveRatio(false); iv.setClip(new Circle(22, 22, 22)); iv.setVisible(false);
        loadAvatarAsync(f.avatar, iv, ini);
        av.getChildren().addAll(c, ini, iv);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(f.displayName);
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label email = new Label(f.email);
        email.setStyle("-fx-font-size: 11px; -fx-text-fill: #999999;");
        info.getChildren().addAll(name, email);

        Button msgBtn = new Button("Message");
        msgBtn.setStyle("-fx-background-color: #534AB7; -fx-text-fill: white;"
                + "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 14;");
        msgBtn.setOnAction(e -> openChatWith(f.id, f.displayName, f.avatar));

        Button unfriendBtn = new Button("Unfriend");
        unfriendBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #e0e0e0;"
                + "-fx-border-radius: 8; -fx-border-width: 0.5; -fx-font-size: 11px;"
                + "-fx-text-fill: #888888; -fx-cursor: hand; -fx-padding: 4 12;");
        unfriendBtn.setOnAction(e -> { unfriendUser(f.id); showFriends(); });

        row.getChildren().addAll(av, info, msgBtn, unfriendBtn);
        return row;
    }

    private void unfriendUser(int friendId) {
        friendships.removeIf(f ->
                (f.requesterId == currentUserId && f.addresseeId == friendId && "accepted".equals(f.status)) ||
                        (f.requesterId == friendId && f.addresseeId == currentUserId && "accepted".equals(f.status))
        );
        saveFriendships();
    }

    // ═══════════════════════════════════════════════════════════════
    // PENDING REQUESTS
    // ═══════════════════════════════════════════════════════════════
    private void showPending() {
        setActiveTab(btnPending);
        headerTitle.setText("Pending Requests");
        searchField.setVisible(false);
        contentBox.getChildren().clear();

        Task<List<FriendRequest>> task = new Task<>() {
            @Override protected List<FriendRequest> call() { return loadPendingRequests(); }
            @Override protected void succeeded() { renderPendingRequests(getValue()); }
        };
        new Thread(task).start();
    }

    private List<FriendRequest> loadPendingRequests() {
        List<FriendRequest> list = new ArrayList<>();
        Set<Integer> userIds = new HashSet<>();

        for (Friendship f : friendships) {
            if (f.addresseeId == currentUserId && "pending".equals(f.status)) {
                userIds.add(f.requesterId);
            }
        }
        for (Friendship f : friendships) {
            if (f.requesterId == currentUserId && "pending".equals(f.status)) {
                userIds.add(f.addresseeId);
            }
        }
        if (userIds.isEmpty()) return list;

        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT u.id, u.email, p.first_name, p.last_name, p.avatar FROM user u " +
                "LEFT JOIN profile p ON p.user_id = u.id WHERE u.id IN (" + placeholders + ")";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (int id : userIds) ps.setInt(i++, id);
            ResultSet rs = ps.executeQuery();
            Map<Integer, FriendRequest> temp = new HashMap<>();
            while (rs.next()) {
                int uid = rs.getInt("id");
                boolean isSent = false;
                long reqId = 0;
                String createdAt = null;
                for (Friendship f : friendships) {
                    if (f.requesterId == currentUserId && f.addresseeId == uid && "pending".equals(f.status)) {
                        isSent = true; reqId = f.id; createdAt = f.createdAt; break;
                    }
                    if (f.requesterId == uid && f.addresseeId == currentUserId && "pending".equals(f.status)) {
                        isSent = false; reqId = f.id; createdAt = f.createdAt; break;
                    }
                }
                temp.put(uid, new FriendRequest(
                        reqId, uid,
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email"), rs.getString("avatar"),
                        createdAt, isSent
                ));
            }
            list.addAll(temp.values());
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private void renderPendingRequests(List<FriendRequest> requests) {
        contentBox.getChildren().clear();
        List<FriendRequest> received = requests.stream().filter(r -> !r.isSentByMe).toList();
        List<FriendRequest> sent     = requests.stream().filter(r -> r.isSentByMe).toList();

        if (!received.isEmpty()) {
            Label sec = new Label("Received (" + received.size() + ")");
            sec.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-padding: 8 0 4 0;");
            contentBox.getChildren().add(sec);
            VBox recBox = new VBox(8);
            for (FriendRequest r : received) recBox.getChildren().add(buildRequestRow(r, true));
            contentBox.getChildren().add(recBox);
        }
        if (!sent.isEmpty()) {
            Label sec = new Label("Sent (" + sent.size() + ")");
            sec.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-padding: 16 0 4 0;");
            contentBox.getChildren().add(sec);
            VBox sentBox = new VBox(8);
            for (FriendRequest r : sent) sentBox.getChildren().add(buildRequestRow(r, false));
            contentBox.getChildren().add(sentBox);
        }
        if (received.isEmpty() && sent.isEmpty()) {
            contentBox.getChildren().add(emptyState("No pending requests",
                    "Friend requests you send or receive will appear here."));
        }
    }

    private HBox buildRequestRow(FriendRequest r, boolean showActions) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                + "-fx-border-color: #f0f0f0; -fx-border-width: 0.5; -fx-border-radius: 12;");

        StackPane av = new StackPane();
        Circle c = new Circle(20, Color.web("#EEEDFE"));
        c.setStroke(Color.web("#534AB7")); c.setStrokeWidth(1.5);
        Label ini = new Label(r.initials);
        ini.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3C3489;");
        ImageView iv = new ImageView(); iv.setFitWidth(40); iv.setFitHeight(40);
        iv.setPreserveRatio(false); iv.setClip(new Circle(20, 20, 20)); iv.setVisible(false);
        loadAvatarAsync(r.avatar, iv, ini);
        av.getChildren().addAll(c, ini, iv);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(r.displayName);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label time = new Label("Sent " + formatTimeAgo(r.createdAt));
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: #bbbbbb;");
        info.getChildren().addAll(name, time);

        if (showActions) {
            Button accept = new Button("Accept");
            accept.setStyle("-fx-background-color: #534AB7; -fx-text-fill: white;"
                    + "-fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 12;");
            accept.setOnAction(e -> { acceptFriendRequest(r.userId); showPending(); });
            Button decline = new Button("Decline");
            decline.setStyle("-fx-background-color: transparent; -fx-border-color: #e0e0e0;"
                    + "-fx-border-radius: 6; -fx-border-width: 0.5; -fx-font-size: 11px;"
                    + "-fx-text-fill: #888888; -fx-cursor: hand; -fx-padding: 4 12;");
            decline.setOnAction(e -> { declineFriendRequest(r.userId); showPending(); });
            row.getChildren().addAll(av, info, accept, decline);
        } else {
            Button cancel = new Button("Cancel");
            cancel.setStyle("-fx-background-color: transparent; -fx-border-color: #e0e0e0;"
                    + "-fx-border-radius: 6; -fx-border-width: 0.5; -fx-font-size: 11px;"
                    + "-fx-text-fill: #888888; -fx-cursor: hand; -fx-padding: 4 12;");
            cancel.setOnAction(e -> { cancelFriendRequest(r.userId); showPending(); });
            row.getChildren().addAll(av, info, cancel);
        }
        return row;
    }

    private void cancelFriendRequest(int toUserId) {
        friendships.removeIf(f -> f.requesterId == currentUserId && f.addresseeId == toUserId && "pending".equals(f.status));
        saveFriendships();
    }

    // ═══════════════════════════════════════════════════════════════
    // MESSAGES LIST
    // ═══════════════════════════════════════════════════════════════
    private void showMessagesList() {
        setActiveTab(btnMessages);
        headerTitle.setText("Messages");
        searchField.setPromptText("Search conversations...");
        searchField.setVisible(true);
        contentBox.getChildren().clear();

        Task<List<Conversation>> task = new Task<>() {
            @Override protected List<Conversation> call() { return loadConversations(); }
            @Override protected void succeeded() { renderConversationsList(getValue()); }
        };
        new Thread(task).start();
    }

    private List<Conversation> loadConversations() {
        List<Conversation> list = new ArrayList<>();
        Map<Integer, Conversation> convMap = new HashMap<>();

        for (LocalMessage m : messages) {
            int otherId = (m.senderId == currentUserId) ? m.receiverId : m.senderId;
            if (!convMap.containsKey(otherId)) {
                convMap.put(otherId, new Conversation(otherId, null, null, null, null, null, null, 0));
            }
            Conversation conv = convMap.get(otherId);
            if (conv.lastTime == null || m.createdAt.compareTo(conv.lastTime) > 0) {
                conv.lastMessage = m.content;
                conv.lastTime = m.createdAt;
            }
            if (m.receiverId == currentUserId && !m.isRead) {
                conv.unreadCount++;
            }
        }

        if (convMap.isEmpty()) return list;

        Set<Integer> userIds = convMap.keySet();
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT u.id, u.email, p.first_name, p.last_name, p.avatar FROM user u " +
                "LEFT JOIN profile p ON p.user_id = u.id WHERE u.id IN (" + placeholders + ")";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (int id : userIds) ps.setInt(i++, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int uid = rs.getInt("id");
                Conversation conv = convMap.get(uid);
                conv.firstName = rs.getString("first_name");
                conv.lastName = rs.getString("last_name");
                conv.email = rs.getString("email");
                conv.avatar = rs.getString("avatar");
                conv.displayName = buildDisplayName(conv.firstName, conv.lastName, conv.email);
                conv.initials = buildInitials(conv.firstName, conv.lastName, conv.email);
                list.add(conv);
            }
        } catch (Exception e) { e.printStackTrace(); }

        list.sort((a, b) -> {
            if (a.lastTime == null) return 1;
            if (b.lastTime == null) return -1;
            return b.lastTime.compareTo(a.lastTime);
        });
        return list;
    }

    private void renderConversationsList(List<Conversation> convs) {
        contentBox.getChildren().clear();
        if (convs.isEmpty()) {
            contentBox.getChildren().add(emptyState("No messages yet",
                    "Start a conversation from Discover or Friends."));
            return;
        }
        VBox list = new VBox(6);
        for (Conversation c : convs) list.getChildren().add(buildConversationRow(c));
        contentBox.getChildren().add(list);
    }

    private HBox buildConversationRow(Conversation c) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                + "-fx-border-color: #f0f0f0; -fx-border-width: 0.5; -fx-border-radius: 12;"
                + "-fx-cursor: hand;");
        row.setOnMouseClicked(e -> openChatWith(c.userId, c.displayName, c.avatar));

        StackPane avStack = new StackPane();
        StackPane av = new StackPane();
        Circle circ = new Circle(24, Color.web("#EEEDFE"));
        circ.setStroke(Color.web("#534AB7")); circ.setStrokeWidth(1.5);
        Label ini = new Label(c.initials);
        ini.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3C3489;");
        ImageView iv = new ImageView(); iv.setFitWidth(48); iv.setFitHeight(48);
        iv.setPreserveRatio(false); iv.setClip(new Circle(24, 24, 24)); iv.setVisible(false);
        loadAvatarAsync(c.avatar, iv, ini);
        av.getChildren().addAll(circ, ini, iv);

        if (c.unreadCount > 0) {
            Label badge = new Label(String.valueOf(c.unreadCount));
            badge.setStyle("-fx-background-color: #E53935; -fx-text-fill: white;"
                    + "-fx-background-radius: 10; -fx-padding: 1 6; -fx-font-size: 10px; -fx-font-weight: bold;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(-4, -4, 0, 0));
            avStack.getChildren().addAll(av, badge);
        } else {
            avStack.getChildren().add(av);
        }

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(c.displayName);
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label preview = new Label(c.lastMessage != null ? c.lastMessage : "");
        preview.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (c.unreadCount > 0 ? "#1a1a1a" : "#999999") + ";");
        preview.setMaxWidth(280);
        info.getChildren().addAll(name, preview);

        Label time = new Label(c.lastTime != null ? formatTimeShort(c.lastTime) : "");
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: #bbbbbb;");

        row.getChildren().addAll(avStack, info, time);
        return row;
    }

    // ═══════════════════════════════════════════════════════════════
    // CHAT VIEW
    // ═══════════════════════════════════════════════════════════════
    private void openChatWith(int otherId, String otherName, String otherAvatar) {
        activeConversationPartnerId = otherId;
        headerTitle.setText(otherName);
        searchField.setVisible(false);
        contentBox.getChildren().clear();

        VBox chatContainer = new VBox();
        chatContainer.setSpacing(0);
        VBox.setVgrow(chatContainer, Priority.ALWAYS);

        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(12, 14, 12, 14));
        messagesBox.setStyle("-fx-background-color: #fafafa;");
        VBox.setVgrow(messagesBox, Priority.ALWAYS);

        ScrollPane msgScroll = new ScrollPane(messagesBox);
        msgScroll.setFitToWidth(true);
        msgScroll.setVvalue(1.0);
        msgScroll.setStyle("-fx-background: #fafafa; -fx-border-color: transparent;");
        VBox.setVgrow(msgScroll, Priority.ALWAYS);

        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setPadding(new Insets(10, 14, 14, 14));
        inputBox.setStyle("-fx-background-color: white; -fx-border-color: #f0f0f0 transparent transparent transparent;"
                + "-fx-border-width: 0.5 0 0 0;");

        TextField msgInput = new TextField();
        msgInput.setPromptText("Type a message...");
        msgInput.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #e0e0e0;"
                + "-fx-border-width: 0.5; -fx-padding: 8 14; -fx-font-size: 13px;");
        HBox.setHgrow(msgInput, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #534AB7; -fx-text-fill: white;"
                + "-fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;"
                + "-fx-cursor: hand; -fx-padding: 8 18;");

        inputBox.getChildren().addAll(msgInput, sendBtn);
        chatContainer.getChildren().addAll(msgScroll, inputBox);
        contentBox.getChildren().add(chatContainer);

        Runnable loadMessages = () -> {
            List<LocalMessage> msgs = getMessagesWith(otherId);
            Platform.runLater(() -> {
                messagesBox.getChildren().clear();
                for (LocalMessage m : msgs) {
                    messagesBox.getChildren().add(buildMessageBubble(m, m.senderId == currentUserId));
                }
                Platform.runLater(() -> msgScroll.setVvalue(1.0));
            });
        };

        loadMessages.run();

        Runnable sendMessage = () -> {
            String text = msgInput.getText().trim();
            if (text.isEmpty()) return;
            sendLocalMessage(otherId, text);
            msgInput.clear();
            loadMessages.run();
        };

        sendBtn.setOnAction(e -> sendMessage.run());
        msgInput.setOnAction(e -> sendMessage.run());

        markMessagesAsRead(otherId);
    }

    private List<LocalMessage> getMessagesWith(int otherId) {
        return messages.stream()
                .filter(m -> (m.senderId == currentUserId && m.receiverId == otherId) ||
                        (m.senderId == otherId && m.receiverId == currentUserId))
                .sorted(Comparator.comparing(m -> m.createdAt))
                .collect(Collectors.toList());
    }

    private HBox buildMessageBubble(LocalMessage m, boolean isMe) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(2, 0, 2, 0));

        VBox bubble = new VBox(2);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setMaxWidth(320);
        bubble.setStyle(isMe
                ? "-fx-background-color: #534AB7; -fx-background-radius: 14 14 2 14;"
                : "-fx-background-color: white; -fx-background-radius: 14 14 14 2;"
                + "-fx-border-color: #e8e8e8; -fx-border-width: 0.5; -fx-border-radius: 14 14 14 2;");

        Label content = new Label(m.content);
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isMe ? "white" : "#333333") + ";");

        Label time = new Label(m.createdAt != null ?
                LocalDateTime.parse(m.createdAt, ISO_FMT).format(TIME_FMT) : "");
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: " + (isMe ? "#cccccc" : "#bbbbbb") + ";");

        bubble.getChildren().addAll(content, time);
        wrapper.getChildren().add(bubble);
        return wrapper;
    }

    private void sendLocalMessage(int toId, String content) {
        LocalMessage m = new LocalMessage();
        m.id = System.currentTimeMillis();
        m.senderId = currentUserId;
        m.receiverId = toId;
        m.content = content;
        m.isRead = false;
        m.createdAt = LocalDateTime.now().format(ISO_FMT);
        messages.add(m);
        saveMessages();

        LocalNotification n = new LocalNotification();
        n.id = System.currentTimeMillis();
        n.userId = toId;
        n.type = "message";
        n.fromUserId = currentUserId;
        n.message = getCurrentUserName() + " sent you a message";
        n.isRead = false;
        n.createdAt = LocalDateTime.now().format(ISO_FMT);
        notifications.add(n);
        saveNotifications();
    }

    private void markMessagesAsRead(int fromId) {
        boolean changed = false;
        for (LocalMessage m : messages) {
            if (m.senderId == fromId && m.receiverId == currentUserId && !m.isRead) {
                m.isRead = true;
                changed = true;
            }
        }
        if (changed) saveMessages();
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════
    private int countUnreadMessages() {
        int count = 0;
        for (LocalMessage m : messages) {
            if (m.receiverId == currentUserId && !m.isRead) count++;
        }
        return count;
    }

    private int countPendingRequests() {
        int count = 0;
        for (Friendship f : friendships) {
            if (f.addresseeId == currentUserId && "pending".equals(f.status)) count++;
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════════
    // POLLING
    // ═══════════════════════════════════════════════════════════════
    private void startPolling() {
        pollTimer = new Timer(true);
        pollTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> refreshBadge());
            }
        }, 5000, 8000);
    }

    private void refreshBadge() {
        int unreadMsgs = countUnreadMessages();
        int pendingReqs = countPendingRequests();
        int total = unreadMsgs + pendingReqs;

        if (total > 0) {
            notificationBadge.setText(String.valueOf(total));
            notificationBadge.setVisible(true); notificationBadge.setManaged(true);
        } else {
            notificationBadge.setVisible(false); notificationBadge.setManaged(false);
        }
        btnPending.setText("Pending" + (pendingReqs > 0 ? " (" + pendingReqs + ")" : ""));
        btnMessages.setText("Messages" + (unreadMsgs > 0 ? " (" + unreadMsgs + ")" : ""));
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════
    private void filterUsers(String query) {
        if (allUsersCache.isEmpty()) return;
        List<UserCard> filtered = allUsersCache.stream()
                .filter(u -> u.displayName.toLowerCase().contains(query)
                        || u.email.toLowerCase().contains(query))
                .collect(Collectors.toList());
        renderUserGrid(filtered);
    }

    private void setActiveTab(Button active) {
        String activeStyle = "-fx-background-color: #534AB7; -fx-text-fill: white;"
                + "-fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold;"
                + "-fx-cursor: hand; -fx-padding: 8 16;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #666666;"
                + "-fx-background-radius: 10; -fx-font-size: 12px; -fx-cursor: hand;"
                + "-fx-padding: 8 16;";
        for (Button btn : List.of(btnDiscover, btnFriends, btnPending, btnMessages)) {
            btn.setStyle(btn == active ? activeStyle : inactiveStyle);
        }
    }

    private VBox emptyState(String title, String subtitle) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 0, 60, 0));
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #cccccc;");
        Label s = new Label(subtitle);
        s.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaaaa;");
        box.getChildren().addAll(t, s);
        return box;
    }

    private void loadAvatarAsync(String avatar, ImageView imgView, Label fallback) {
        if (avatar == null || avatar.isBlank()) {
            imgView.setVisible(false); fallback.setVisible(true); return;
        }
        Task<Image> task = new Task<>() {
            @Override protected Image call() {
                try {
                    File f = new File(UPLOAD_DIR + avatar);
                    if (f.exists()) return new Image(f.toURI().toString(), 76, 76, false, true);
                    URL r = getClass().getResource("/com/mindforge/avatars/" + avatar);
                    if (r != null) return new Image(r.toExternalForm(), 76, 76, false, true);
                } catch (Exception ignored) {}
                return null;
            }
            @Override protected void succeeded() {
                Image img = getValue();
                if (img != null && !img.isError()) {
                    imgView.setImage(img); imgView.setVisible(true); fallback.setVisible(false);
                } else {
                    imgView.setVisible(false); fallback.setVisible(true);
                }
            }
        };
        new Thread(task).start();
    }

    private String getCurrentUserName() {
        String sql = "SELECT first_name, last_name FROM profile WHERE user_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String fn = rs.getString("first_name");
                String ln = rs.getString("last_name");
                if (fn != null && !fn.isBlank()) return fn + (ln != null ? " " + ln : "");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return currentUserEmail != null ? currentUserEmail.split("@")[0] : "User";
    }

    private String formatTimeAgo(String isoTime) {
        if (isoTime == null) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(isoTime, ISO_FMT);
            Duration d = Duration.between(dt, LocalDateTime.now());
            long mins = d.toMinutes();
            if (mins < 1) return "just now";
            if (mins < 60) return mins + "m ago";
            long hrs = d.toHours();
            if (hrs < 24) return hrs + "h ago";
            long days = d.toDays();
            if (days < 7) return days + "d ago";
            return dt.format(DATE_FMT);
        } catch (Exception e) { return isoTime; }
    }

    private String formatTimeShort(String isoTime) {
        if (isoTime == null) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(isoTime, ISO_FMT);
            if (Duration.between(dt, LocalDateTime.now()).toDays() < 1)
                return dt.format(TIME_FMT);
            return dt.format(DATE_FMT);
        } catch (Exception e) { return isoTime; }
    }

    private void navigateTo(String fxml, String title) {
        if (pollTimer != null) pollTimer.cancel();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            URL css = getClass().getResource("/com/mindforge/css/style.css");
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            if (css != null && stage.getScene() != null) stage.getScene().getStylesheets().add(css.toExternalForm());
            stage.setTitle(title); 
            stage.getScene().setRoot(root);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mindforge_db", "root", "");
    }

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════
    static class Friendship {
        long id;
        int requesterId;
        int addresseeId;
        String status;
        String createdAt;
        String updatedAt;
    }

    static class LocalMessage {
        long id;
        int senderId;
        int receiverId;
        String content;
        boolean isRead;
        String createdAt;
    }

    static class LocalNotification {
        long id;
        int userId;
        String type;
        int fromUserId;
        String message;
        boolean isRead;
        String createdAt;
    }

    static class UserCard {
        int id; String firstName, lastName, email, avatar, bio, friendshipStatus;
        Timestamp memberSince;
        String displayName, initials;
        UserCard(int id, String fn, String ln, String email, String av, String bio, Timestamp since, String fs) {
            this.id = id; this.firstName = fn; this.lastName = ln; this.email = email;
            this.avatar = av; this.bio = bio; this.memberSince = since; this.friendshipStatus = fs;
            this.displayName = buildDisplayName(fn, ln, email);
            this.initials = buildInitials(fn, ln, email);
        }
    }

    static class FriendInfo {
        int id; String firstName, lastName, email, avatar, bio;
        Timestamp friendsSince;
        String displayName, initials;
        FriendInfo(int id, String fn, String ln, String email, String av, String bio, Timestamp fs) {
            this.id = id; this.firstName = fn; this.lastName = ln; this.email = email;
            this.avatar = av; this.bio = bio; this.friendsSince = fs;
            this.displayName = buildDisplayName(fn, ln, email);
            this.initials = buildInitials(fn, ln, email);
        }
    }

    static class FriendRequest {
        long requestId; int userId; String firstName, lastName, email, avatar;
        String createdAt; boolean isSentByMe;
        String displayName, initials;
        FriendRequest(long rid, int uid, String fn, String ln, String email, String av, String ca, boolean sent) {
            this.requestId = rid; this.userId = uid; this.firstName = fn; this.lastName = ln;
            this.email = email; this.avatar = av; this.createdAt = ca; this.isSentByMe = sent;
            this.displayName = buildDisplayName(fn, ln, email);
            this.initials = buildInitials(fn, ln, email);
        }
    }

    static class Conversation {
        int userId; String firstName, lastName, email, avatar;
        String lastMessage; String lastTime; int unreadCount;
        String displayName, initials;
        Conversation(int uid, String fn, String ln, String email, String av, String lm, String lt, int uc) {
            this.userId = uid; this.firstName = fn; this.lastName = ln;
            this.email = email; this.avatar = av; this.lastMessage = lm; this.lastTime = lt; this.unreadCount = uc;
        }
    }

    private static String buildDisplayName(String fn, String ln, String email) {
        if (fn != null && ln != null && !fn.isBlank() && !ln.isBlank()) return fn + " " + ln;
        if (fn != null && !fn.isBlank()) return fn;
        if (ln != null && !ln.isBlank()) return ln;
        return email != null ? email.split("@")[0] : "User";
    }

    private static String buildInitials(String fn, String ln, String email) {
        StringBuilder sb = new StringBuilder();
        if (fn != null && !fn.isBlank()) sb.append(Character.toUpperCase(fn.charAt(0)));
        if (ln != null && !ln.isBlank()) sb.append(Character.toUpperCase(ln.charAt(0)));
        if (sb.length() == 0 && email != null && !email.isBlank())
            sb.append(Character.toUpperCase(email.charAt(0)));
        if (sb.length() == 0) sb.append("?");
        return sb.toString();
    }
}