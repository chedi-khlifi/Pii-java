package com.mindforge;

import com.example.util.SchemaMigrator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Migrate community module schema (idempotent — safe to run every startup)
        try {
            SchemaMigrator.migrateAll();
        } catch (Exception e) {
            System.err.println("[MainApp] Community schema migration warning: " + e.getMessage());
        }

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/mindforge/fxml/MindForge.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);

        // Load CSS stylesheet
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/com/mindforge/css/style.css")
                ).toExternalForm()
        );

        primaryStage.setTitle("MindForge");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
