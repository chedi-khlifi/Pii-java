package com.mindforge.architect.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.ScrollPane;
import com.mindforge.util.UserSession;
import com.mindforge.utils.SessionManager;
import com.mindforge.controllers.carriere.StudentController;

public class HomeController implements Initializable {

    // ── Navbar ──────────────────────────────────────────────
    @FXML private ImageView logoImg;
    @FXML private Button    btnHome;
    @FXML private Button    btnAbout;
    @FXML private Button    btnWorkspace;
    @FXML private Button    btnLeaderboard;
    @FXML private Button    btnQuotes;
    @FXML private Button    btnLogin;
    @FXML private Button    btnAdminPanel;

    // ── Hero ────────────────────────────────────────────────
    @FXML private ImageView heroBg;
    @FXML private Button    btnGetStarted;

    // ── About ───────────────────────────────────────────────
    @FXML private ImageView aboutImg;

    // ── Courses ─────────────────────────────────────────────
    @FXML private ImageView course1Img;
    @FXML private ImageView course2Img;
    @FXML private ImageView course3Img;
    @FXML private ImageView course4Img;
    @FXML private ImageView course5Img;
    @FXML private ImageView course6Img;
    @FXML private ImageView course7Img;
    @FXML private ImageView course8Img;
    @FXML private Button    btnMoreCourses;

    // ── CTA ─────────────────────────────────────────────────
    @FXML private Button btnCtaGetStarted;
    @FXML private Button btnContactSupport;

    // ── Footer ──────────────────────────────────────────────
    @FXML private ImageView footerLogo;
    @FXML private Label     footerCopyright;

    // ── Career integration ───────────────────────────────────
    @FXML private BorderPane rootBorderPane;
    @FXML private ScrollPane homeScrollPane;

    // ────────────────────────────────────────────────────────
    //  INITIALIZE
    // ────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadImage(heroBg,     "/images/home-background.jpg");
        loadImage(aboutImg,   "/images/about.png");
        loadImage(footerLogo, "/images/logo.png");
        loadImage(logoImg,    "/images/logo.png");
        loadImage(course1Img, "/images/course01.jpg");
        loadImage(course2Img, "/images/course02.jpg");
        loadImage(course3Img, "/images/course03.jpg");
        loadImage(course4Img, "/images/course04.jpg");
        loadImage(course5Img, "/images/course05.jpg");
        loadImage(course6Img, "/images/course06.jpg");
        loadImage(course7Img, "/images/course07.jpg");
        loadImage(course8Img, "/images/course08.jpg");

        int year = java.time.Year.now().getValue();
        footerCopyright.setText("© Copyright " + year + ". All Rights Reserved. | Made by SiL0p");
    }

    // ── Helpers ─────────────────────────────────────────────
    private void loadImage(ImageView view, String path) {
        URL url = getClass().getResource(path);
        if (url != null) view.setImage(new Image(url.toExternalForm()));
    }

    private void switchScene(ActionEvent event, String fxmlFile, String title,
                             double w, double h) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/" + fxmlFile)
            );
            Parent root  = loader.load();
            Scene  scene = new Scene(root, w, h);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/com/mindforge/css/style.css")
                    ).toExternalForm()
            );
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Could not load: " + fxmlFile);
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    //  NAV HANDLERS
    // ════════════════════════════════════════════════════════

    @FXML public void onHome(ActionEvent e) {
        if (rootBorderPane != null && homeScrollPane != null) {
            rootBorderPane.setCenter(homeScrollPane);
        }
    }

    @FXML public void onAbout(ActionEvent e) {
        System.out.println("About clicked");
    }

    @FXML public void onWorkspace(ActionEvent e) {
        System.out.println("Workspace clicked");
        // TODO: switchScene(e, "Workspace.fxml", "MindForge - Workspace", 1280, 800);
    }

    @FXML public void onLeaderboard(ActionEvent e) {
        System.out.println("Leaderboard clicked");
        // TODO: switchScene(e, "Leaderboard.fxml", "MindForge - Leaderboard", 1280, 800);
    }

    @FXML public void onQuotes(ActionEvent e) {
        System.out.println("Quotes clicked");
        // TODO: switchScene(e, "Quotes.fxml", "MindForge - Quotes", 1280, 800);
    }

    @FXML public void onLogin(ActionEvent e) {
        switchScene(e, "login.fxml", "MindForge - Login", 500, 400);
    }

    @FXML public void onAdminPanel(ActionEvent e) {
        System.out.println("Admin Panel clicked");
        // TODO: switchScene(e, "AdminDashboard.fxml", "MindForge - Admin", 1280, 800);
    }

    // ════════════════════════════════════════════════════════
    //  COMMUNITY MENU HANDLERS
    // ════════════════════════════════════════════════════════

    @FXML public void onCommunityHub(ActionEvent e)     { System.out.println("Community Hub"); }
    @FXML public void onChallengeInbox(ActionEvent e)   { System.out.println("Challenge Inbox"); }
    @FXML public void onChallengeOutbox(ActionEvent e)  { System.out.println("Challenge Outbox"); }
    @FXML public void onCommunityTasks(ActionEvent e)   { System.out.println("Community Tasks"); }
    @FXML public void onMyTickets(ActionEvent e)        { System.out.println("My Tickets"); }
    @FXML public void onContactSupport(ActionEvent e)   { System.out.println("Contact Support"); }
    @FXML public void onFriends(ActionEvent e)          { System.out.println("Friends"); }
    @FXML public void onManageTickets(ActionEvent e)    { System.out.println("Manage Tickets"); }

    // ════════════════════════════════════════════════════════
    //  CAREERS MENU HANDLERS
    // ════════════════════════════════════════════════════════

    @FXML public void onBrowseOpportunities(ActionEvent e) { loadStudentCareer(1); }
    @FXML public void onCompanies(ActionEvent e)           { loadStudentCareer(0); }
    @FXML public void onMyApplications(ActionEvent e)      { loadStudentCareer(2); }
    @FXML public void onPostOpportunity(ActionEvent e)     { loadCompanyCareer(); }
    @FXML public void onMyPostings(ActionEvent e)          { loadCompanyCareer(); }
    @FXML public void onApplicationsReceived(ActionEvent e){ loadCompanyCareer(); }

    private void loadStudentCareer(int tabIndex) {
        try {
            UserSession us = UserSession.getInstance();
            SessionManager.setSession(SessionManager.Role.STUDENT, us.getUserId(), us.getEmail());
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/student-dashboard.fxml"));
            Parent root = loader.load();
            StudentController ctrl = loader.getController();
            ctrl.selectTab(tabIndex);
            rootBorderPane.setCenter(root);
        } catch (IOException e) {
            System.err.println("Could not load student-dashboard.fxml");
            e.printStackTrace();
        }
    }

    private void loadCompanyCareer() {
        try {
            UserSession us = UserSession.getInstance();
            SessionManager.setSession(SessionManager.Role.COMPANY_OWNER, us.getUserId(), us.getEmail());
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/company-dashboard.fxml"));
            Parent root = loader.load();
            rootBorderPane.setCenter(root);
        } catch (IOException e) {
            System.err.println("Could not load company-dashboard.fxml");
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    //  PROFILE MENU HANDLERS
    // ════════════════════════════════════════════════════════

    @FXML public void onViewProfile(ActionEvent e)  { System.out.println("View Profile"); }
    @FXML public void onEditProfile(ActionEvent e)  { System.out.println("Edit Profile"); }
    @FXML public void onMyRequests(ActionEvent e)   { System.out.println("My Requests"); }
    @FXML public void onStudentPlus(ActionEvent e)  { System.out.println("Student+"); }
    @FXML public void onLogout(ActionEvent e)       {
        switchScene(e, "login.fxml", "MindForge - Login", 500, 400);
    }

    // ════════════════════════════════════════════════════════
    //  HERO / COURSE / FOOTER HANDLERS
    // ════════════════════════════════════════════════════════

    @FXML public void onGetStarted(ActionEvent e)    { switchScene(e, "login.fxml", "MindForge - Login", 500, 400); }
    @FXML public void onCourse1(ActionEvent e)       { System.out.println("Course 1"); }
    @FXML public void onCourse2(ActionEvent e)       { System.out.println("Course 2"); }
    @FXML public void onCourse3(ActionEvent e)       { System.out.println("Course 3"); }
    @FXML public void onCourse4(ActionEvent e)       { System.out.println("Course 4"); }
    @FXML public void onCourse5(ActionEvent e)       { System.out.println("Course 5"); }
    @FXML public void onCourse6(ActionEvent e)       { System.out.println("Course 6"); }
    @FXML public void onCourse7(ActionEvent e)       { System.out.println("Course 7"); }
    @FXML public void onCourse8(ActionEvent e)       { System.out.println("Course 8"); }
    @FXML public void onMoreCourses(ActionEvent e)   { System.out.println("More Courses"); }
    @FXML public void onGuardian(ActionEvent e)      { System.out.println("Guardian"); }
    @FXML public void onVirtualRooms(ActionEvent e)  { System.out.println("Virtual Rooms"); }
    @FXML public void onFocusTimer(ActionEvent e)    { System.out.println("Focus Timer"); }
    @FXML public void onFacebook(ActionEvent e)      { System.out.println("Facebook"); }
    @FXML public void onTwitter(ActionEvent e)       { System.out.println("Twitter"); }
    @FXML public void onInstagram(ActionEvent e)     { System.out.println("Instagram"); }
    @FXML public void onYoutube(ActionEvent e)       { System.out.println("Youtube"); }
    @FXML public void onLinkedin(ActionEvent e)      { System.out.println("LinkedIn"); }
}
