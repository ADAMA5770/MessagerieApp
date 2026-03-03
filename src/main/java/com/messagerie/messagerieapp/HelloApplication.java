package com.messagerie.messagerieapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/messagerie/messagerieapp/login-view.fxml")
        );
        // CORRIGE : 1000x640 au lieu de 400x300
        stage.setScene(new Scene(loader.load(), 1000, 640));
        stage.setTitle("Messagerie App");
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.show();
    }
}