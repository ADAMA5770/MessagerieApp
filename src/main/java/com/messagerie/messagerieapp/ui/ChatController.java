package com.messagerie.messagerieapp.ui;

import com.messagerie.messagerieapp.client.Client;
import com.messagerie.messagerieapp.util.NetworkMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatController {

    @FXML private ListView<HBox> userList;
    @FXML private ListView<VBox> messageList;
    @FXML private TextField      messageField;
    @FXML private Label          currentUserLabel;
    @FXML private Label          chatWithLabel;
    @FXML private Label          avatarLabel;

    private String username;
    private Client client;
    private String selectedReceiver = null;

    private static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public void init(String username, Client client) {
        this.username = username;
        this.client   = client;
        currentUserLabel.setText("  " + username);
        if (avatarLabel != null) {
            avatarLabel.setText(username.substring(0, 1).toUpperCase());
        }

        // Listener : message temps réel reçu
        client.setMessageListener((from, content, dateTime) ->
                Platform.runLater(() -> {
                    // Afficher seulement si la conversation est ouverte avec cet expéditeur
                    if (from.equals(selectedReceiver)) {
                        addMessage(from, content, parseTime(dateTime));
                    }
                })
        );

        // Listener : liste des utilisateurs
        client.setUserListListener(users ->
                Platform.runLater(() -> {
                    userList.getItems().clear();
                    for (String userInfo : users) {
                        String[] parts = userInfo.split("\\|");
                        String name = parts[0];
                        if (name.equals(username)) continue;
                        boolean online   = parts.length > 1 && parts[1].equals("ONLINE");
                        String lastSeen  = parts.length > 2 ? parts[2] : "";
                        userList.getItems().add(createUserRow(name, online, lastSeen));
                    }
                })
        );

        // Listener : historique de conversation
        client.setHistoryListener(messages ->
                Platform.runLater(() -> {
                    messageList.getItems().clear();
                    for (String line : messages) {
                        // Format : "sender|contenu|dateHeure"
                        String[] parts   = line.split("\\|", 3);
                        String   from    = parts[0];
                        String   content = parts.length > 1 ? parts[1] : "";
                        String   dt      = parts.length > 2 ? parts[2] : "";
                        addMessage(from, content, parseTime(dt));
                    }
                })
        );

        // Clic sur un contact → demander l'historique
        userList.setOnMouseClicked(e -> {
            HBox selected = userList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedReceiver = (String) selected.getUserData();
                chatWithLabel.setText("  " + selectedReceiver);
                messageList.getItems().clear();
                // Demander l'historique au serveur
                client.requestHistory(selectedReceiver);
            }
        });

        // Style des ListView
        messageList.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        messageList.setCellFactory(lv -> new ListCell<VBox>() {
            @Override
            protected void updateItem(VBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: white;");
                } else {
                    setGraphic(item);
                    setStyle("-fx-background-color: white; -fx-padding: 2 0 2 0;");
                }
            }
        });

        userList.setCellFactory(lv -> new ListCell<HBox>() {
            @Override
            protected void updateItem(HBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(item);
                    setStyle("-fx-background-color: transparent; -fx-padding: 2 4 2 4;");
                }
            }
        });

        client.startListening();
    }

    // Convertit la chaine dateHeure en heure affichable
    private String parseTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return LocalTime.now().format(TIME_FMT);
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateTime);
            // Si c'est aujourd'hui → afficher seulement HH:mm
            if (ldt.toLocalDate().equals(java.time.LocalDate.now())) {
                return ldt.format(TIME_FMT);
            } else {
                return ldt.format(DATETIME_FMT);
            }
        } catch (Exception e) {
            return LocalTime.now().format(TIME_FMT);
        }
    }

    private void addMessage(String from, String content, String time) {
        VBox bubble = new VBox(2);
        bubble.setPadding(new Insets(6, 12, 6, 12));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        StackPane avatarPane = new StackPane();
        Circle circle = new Circle(18);
        circle.setFill(Color.web(getColor(from)));
        Label initial = new Label(from.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
        avatarPane.getChildren().addAll(circle, initial);

        // Nom
        Label nameLabel = new Label(from);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web(from.equals(username) ? "#1264A3" : "#1D1C1D"));

        // Heure
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #717273; -fx-font-size: 11;");

        header.getChildren().addAll(avatarPane, nameLabel, timeLabel);

        // Contenu
        Label msgLabel = new Label(content);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(700);
        msgLabel.setStyle("-fx-text-fill: #1D1C1D; -fx-font-size: 14;");
        msgLabel.setPadding(new Insets(2, 0, 0, 44));

        bubble.getChildren().addAll(header, msgLabel);
        messageList.getItems().add(bubble);
        messageList.scrollTo(messageList.getItems().size() - 1);
    }

    private HBox createUserRow(String name, boolean online, String lastSeen) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(name);
        row.setPadding(new Insets(6, 8, 6, 8));

        StackPane avatar = new StackPane();
        Circle circle = new Circle(18);
        circle.setFill(Color.web(getColor(name)));
        Label initial = new Label(name.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
        avatar.getChildren().addAll(circle, initial);

        // Point de statut
        Circle statusDot = new Circle(5);
        statusDot.setFill(online ? Color.web("#22c55e") : Color.web("#6b7280"));
        StackPane avatarWithStatus = new StackPane();
        avatarWithStatus.getChildren().addAll(avatar, statusDot);
        StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);

        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #e0e0ff; -fx-font-weight: bold; -fx-font-size: 13;");

        // Statut : EN LIGNE en vert, ou "Vu a HH:mm"
        String statusText;
        String statusColor;
        if (online) {
            statusText  = "En ligne";
            statusColor = "#22c55e";
        } else if (!lastSeen.isEmpty()) {
            statusText  = "Vu a " + lastSeen;
            statusColor = "#8a8a9a";
        } else {
            statusText  = "Jamais connecte";
            statusColor = "#6b7280";
        }
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 11;");

        info.getChildren().addAll(nameLabel, statusLabel);
        row.getChildren().addAll(avatarWithStatus, info);
        return row;
    }

    private String getColor(String name) {
        String[] colors = {"#6366f1","#ec4899","#f59e0b","#10b981","#3b82f6","#8b5cf6"};
        return colors[Math.abs(name.hashCode()) % colors.length];
    }

    @FXML
    public void handleSend() {
        if (selectedReceiver == null) {
            addMessage("Systeme", "Selectionnez un destinataire !", LocalTime.now().format(TIME_FMT));
            return;
        }
        String content = messageField.getText().trim();
        if (content.isEmpty()) return;
        try {
            client.send(new NetworkMessage("SEND", new String[]{selectedReceiver, content}));
            addMessage(username, content, LocalTime.now().format(TIME_FMT));
            messageField.clear();
        } catch (Exception e) {
            addMessage("Erreur", "Impossible d'envoyer !", LocalTime.now().format(TIME_FMT));
        }
    }

    @FXML
    public void handleLogout() {
        client.logout();
        Stage stage = (Stage) messageField.getScene().getWindow();
        stage.close();
    }
}