package com.mindforge.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class MainController {

    @FXML private AnchorPane contentArea;
    @FXML private Button btnHome;
    @FXML private Button btnEntreprises;
    @FXML private Button btnOpportunites;
    @FXML private Button btnDemandes;

    private Button activeButton;

    @FXML
    public void initialize() {
        navigateHome();
    }

    @FXML
    public void navigateHome() {
        loadView("home-view.fxml");
        setActive(btnHome);
    }

    @FXML
    public void navigateEntreprises() {
        loadView("entreprise-view.fxml");
        setActive(btnEntreprises);
    }

    @FXML
    public void navigateOpportunites() {
        loadView("opportunite-carriere-view.fxml");
        setActive(btnOpportunites);
    }

    @FXML
    public void navigateDemandes() {
        loadView("demande-view.fxml");
        setActive(btnDemandes);
    }

    private void loadView(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/mindforge/" + fxmlName));
            Node view = loader.load();
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.out.println("Error loading " + fxmlName + ": " + e.getMessage());
        }
    }

    private void setActive(Button btn) {
        if (activeButton != null) activeButton.getStyleClass().remove("nav-btn-active");
        activeButton = btn;
        btn.getStyleClass().add("nav-btn-active");
    }
}
