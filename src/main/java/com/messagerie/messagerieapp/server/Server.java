package com.messagerie.messagerieapp.server;

import com.messagerie.messagerieapp.dao.UserDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 5000;
    public static Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Serveur demarre sur le port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nouvelle connexion : " + socket.getInetAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastUserList() {
        UserDAO userDAO = new UserDAO();
        java.util.List<com.messagerie.messagerieapp.model.User> allUsers = userDAO.findAll();

        java.util.List<String> userInfos = new java.util.ArrayList<>();
        for (com.messagerie.messagerieapp.model.User u : allUsers) {
            String info;
            if (connectedClients.containsKey(u.getUsername())) {
                // EN LIGNE
                info = u.getUsername() + "|ONLINE";
            } else {
                // HORS LIGNE → affiche "Vu a 26/02 13:02" si lastSeen existe
                String lastSeen = "";
                if (u.getLastSeen() != null) {
                    lastSeen = u.getLastSeen().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")
                    );
                }
                info = u.getUsername() + "|OFFLINE|" + lastSeen;
            }
            userInfos.add(info);
        }

        String[] data = userInfos.toArray(new String[0]);
        for (ClientHandler handler : connectedClients.values()) {
            try {
                handler.sendMessage(
                        new com.messagerie.messagerieapp.util.NetworkMessage("USER_LIST", data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}