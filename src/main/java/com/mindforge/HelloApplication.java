package com.mindforge;

import com.mindforge.utils.DBConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try {
            DBConnection.getConnection();
            System.out.println("Database connection successful.");
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }

        try (var stream = HelloApplication.class.getResourceAsStream("logo.png")) {
            if (stream != null) stage.getIcons().add(new Image(stream));
        } catch (Exception ignored) {}

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 750);
        stage.setTitle("MindForge — Career Management");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }
}
