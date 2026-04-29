package com.mindforge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
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
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
