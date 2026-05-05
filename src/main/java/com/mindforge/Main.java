package com.mindforge;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main entry point for the entire project.
 * This launcher allows users to choose between different applications.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create the main launcher UI
        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 30; -fx-background-color: #f5f5f5;");
        root.setAlignment(Pos.CENTER);

        // Title
        Label title = new Label("MindForge Project Launcher");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label subtitle = new Label("Select an application to launch:");
        subtitle.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");

        // MindForge Button
        Button mindforgeBtn = new Button("Launch MindForge");
        mindforgeBtn.setStyle("-fx-font-size: 14; -fx-padding: 15; -fx-min-width: 200;");
        mindforgeBtn.setOnAction(e -> {
            primaryStage.close();
            launchMindForge();
        });

        // Guardian Button
        Button guardianBtn = new Button("Launch Guardian");
        guardianBtn.setStyle("-fx-font-size: 14; -fx-padding: 15; -fx-min-width: 200;");
        guardianBtn.setOnAction(e -> {
            primaryStage.close();
            launchGuardian();
        });

        root.getChildren().addAll(title, subtitle, mindforgeBtn, guardianBtn);

        Scene scene = new Scene(root, 500, 350);
        primaryStage.setTitle("MindForge Project Launcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Launches the MindForge application
     */
    private void launchMindForge() {
        try {
            MainApp mindforgeApp = new MainApp();
            Stage stage = new Stage();
            mindforgeApp.start(stage);
        } catch (Exception e) {
            System.err.println("Failed to launch MindForge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Launches the Guardian application
     */
    private void launchGuardian() {
        try {
            tn.esprit.Main guardianApp = new tn.esprit.Main();
            Stage stage = new Stage();
            guardianApp.start(stage);
        } catch (Exception e) {
            System.err.println("Failed to launch Guardian: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
