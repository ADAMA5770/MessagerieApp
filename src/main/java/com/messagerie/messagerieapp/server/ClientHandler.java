package com.messagerie.messagerieapp.server;

import com.messagerie.messagerieapp.dao.MessageDAO;
import com.messagerie.messagerieapp.dao.UserDAO;
import com.messagerie.messagerieapp.model.Message;
import com.messagerie.messagerieapp.model.MessageStatus;
import com.messagerie.messagerieapp.model.User;
import com.messagerie.messagerieapp.model.UserStatus;
import com.messagerie.messagerieapp.util.NetworkMessage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;
    private UserDAO userDAO = new UserDAO();
    private MessageDAO messageDAO = new MessageDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input  = new ObjectInputStream(socket.getInputStream());

            NetworkMessage message;
            while ((message = (NetworkMessage) input.readObject()) != null) {
                switch (message.getType()) {
                    case "REGISTER"    -> handleRegister((String[]) message.getData());
                    case "LOGIN"       -> handleLogin((String[]) message.getData());
                    case "SEND"        -> handleSend((String[]) message.getData());
                    case "GET_HISTORY" -> handleGetHistory((String) message.getData());
                    case "LOGOUT"      -> { handleLogout(); return; }
                }
            }
        } catch (Exception e) {
            handleLogout();
        }
    }

    private void handleRegister(String[] data) throws IOException {
        String username = data[0];
        String password = data[1];
        if (userDAO.findByUsername(username) != null) {
            sendMessage(new NetworkMessage("REGISTER_FAIL", "Username deja pris"));
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setStatus(UserStatus.OFFLINE);
        user.setDateCreation(LocalDateTime.now());
        userDAO.save(user);
        sendMessage(new NetworkMessage("REGISTER_OK", "Inscription reussie"));
        System.out.println("[REGISTER] " + username);
    }

    private void handleLogin(String[] data) throws IOException {
        String username = data[0];
        String password = data[1];
        User user = userDAO.findByUsername(username);
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            sendMessage(new NetworkMessage("LOGIN_FAIL", "Identifiants incorrects"));
            return;
        }
        if (Server.connectedClients.containsKey(username)) {
            sendMessage(new NetworkMessage("LOGIN_FAIL", "Deja connecte"));
            return;
        }
        this.username = username;
        Server.connectedClients.put(username, this);
        userDAO.updateStatus(username, UserStatus.ONLINE);
        sendMessage(new NetworkMessage("LOGIN_OK", username));
        System.out.println("[LOGIN] " + username);
        Server.broadcastUserList();

        // Envoyer les messages en attente (hors-ligne)
        List<Message> pending = messageDAO.findPendingMessages(username);
        for (Message msg : pending) {
            sendMessage(new NetworkMessage("RECEIVE",
                    new String[]{msg.getSender(), msg.getContenu(),
                            msg.getDateEnvoi().toString()}));
            messageDAO.markAsReceived(msg.getSender(), username);
        }
    }

    private void handleSend(String[] data) throws IOException {
        String receiver = data[0];
        String contenu  = data[1];
        Message msg = new Message();
        msg.setSender(username);
        msg.setReceiver(receiver);
        msg.setContenu(contenu);
        msg.setDateEnvoi(LocalDateTime.now());
        msg.setStatut(MessageStatus.ENVOYE);
        messageDAO.save(msg);
        System.out.println("[MESSAGE] " + username + " -> " + receiver);

        ClientHandler receiverHandler = Server.connectedClients.get(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendMessage(new NetworkMessage("RECEIVE",
                    new String[]{username, contenu,
                            msg.getDateEnvoi().toString()}));
            messageDAO.markAsReceived(username, receiver);
        }
    }

    // Envoie l'historique de conversation au client qui le demande
    private void handleGetHistory(String otherUser) throws IOException {
        List<Message> history = messageDAO.findConversation(username, otherUser);
        // Format : "sender|contenu|dateHeure"
        String[] lines = new String[history.size()];
        for (int i = 0; i < history.size(); i++) {
            Message m = history.get(i);
            lines[i] = m.getSender() + "|" + m.getContenu() + "|" + m.getDateEnvoi().toString();
        }
        sendMessage(new NetworkMessage("HISTORY", lines));
        System.out.println("[HISTORY] " + username + " <-> " + otherUser
                + " (" + lines.length + " msgs)");
    }

    private void handleLogout() {
        if (username != null) {
            Server.connectedClients.remove(username);
            userDAO.updateStatus(username, UserStatus.OFFLINE);
            userDAO.updateLastSeen(username);
            System.out.println("[LOGOUT] " + username);
            Server.broadcastUserList();
        }
        try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
    }

    public synchronized void sendMessage(NetworkMessage message) throws IOException {
        output.writeObject(message);
        output.flush();
    }
}