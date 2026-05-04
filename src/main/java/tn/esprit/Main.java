package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/view/guardian-hub.fxml"));
            Parent root = loader.load();

            stage.setTitle("MindForge Guardian");
            stage.setScene(new Scene(root, 1360, 820));
            stage.show();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load Guardian FXML UI", ex);
        }
    }
}
