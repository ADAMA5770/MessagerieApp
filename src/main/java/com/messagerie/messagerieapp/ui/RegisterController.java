package com.messagerie.messagerieapp.ui;

import com.messagerie.messagerieapp.client.Client;
import com.messagerie.messagerieapp.util.NetworkMessage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private Client client = new Client();

    @FXML

    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Remplissez tous les champs !");
            return;
        }
        if (!client.connect()) {
            errorLabel.setText("Impossible de se connecter au serveur !");
            return;
        }
        NetworkMessage response = client.sendRequest(
                new NetworkMessage("REGISTER", new String[]{username, password})
        );
        if (response != null && response.getType().equals("REGISTER_OK")) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Inscription réussie ! Redirection...");
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        try { handleBackToLogin(); } catch (Exception e) { e.printStackTrace(); }
                    });
                } catch (InterruptedException e) { e.printStackTrace(); }
            }).start();
        } else {
            errorLabel.setText(response != null ? (String) response.getData() : "Erreur");
        }
    }

    @FXML
    public void handleBackToLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/messagerie/messagerieapp/login-view.fxml"));
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(loader.load()));
    }
}