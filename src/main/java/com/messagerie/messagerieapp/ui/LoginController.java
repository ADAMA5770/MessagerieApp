package com.messagerie.messagerieapp.ui;

import com.messagerie.messagerieapp.client.Client;
import com.messagerie.messagerieapp.util.NetworkMessage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private Client client = new Client();

    @FXML
    public void handleLogin() {
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
                new NetworkMessage("LOGIN", new String[]{username, password})
        );
        if (response != null && response.getType().equals("LOGIN_OK")) {
            openChatWindow(username);
        } else {
            errorLabel.setText(response != null ? (String) response.getData() : "Erreur");
        }
    }

    @FXML
    public void handleRegister() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/messagerie/messagerieapp/register-view.fxml"));
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(loader.load()));
    }

    private void openChatWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/messagerie/messagerieapp/chat-view.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            ChatController controller = loader.getController();
            controller.init(username, client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}