package com.project.artconnect;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * L'app s'ouvre directement sur MainView (accès public).
 * Le bouton Connexion dans la barre permet de se connecter.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/project/artconnect/ui/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 800);
        stage.setTitle("ArtConnect Pro");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
