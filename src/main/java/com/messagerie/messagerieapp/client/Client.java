package com.messagerie.messagerieapp.client;

import com.messagerie.messagerieapp.util.NetworkMessage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    private static final String SERVER_IP   = "192.168.1.29";
    private static final int    SERVER_PORT = 5000;

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    private MessageListener  messageListener;
    private UserListListener userListListener;
    private HistoryListener  historyListener;

    public interface MessageListener {
        void onMessage(String from, String content, String dateTime);
    }

    public interface UserListListener {
        void onUserList(String[] users);
    }

    public interface HistoryListener {
        void onHistory(String[] messages);
    }

    public void setMessageListener(MessageListener listener)   { this.messageListener  = listener; }
    public void setUserListListener(UserListListener listener) { this.userListListener = listener; }
    public void setHistoryListener(HistoryListener listener)   { this.historyListener  = listener; }

    public boolean connect() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out    = new ObjectOutputStream(socket.getOutputStream());
            in     = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (Exception e) {
            System.err.println("Connexion impossible a " + SERVER_IP + ":" + SERVER_PORT);
            e.printStackTrace();
            return false;
        }
    }

    public NetworkMessage sendRequest(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
            return (NetworkMessage) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void send(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Demande l'historique avec un contact au serveur
    public void requestHistory(String otherUser) {
        send(new NetworkMessage("GET_HISTORY", otherUser));
    }

    public void startListening() {
        new Thread(() -> {
            try {
                while (true) {
                    NetworkMessage msg = (NetworkMessage) in.readObject();
                    if (msg == null) break;
                    switch (msg.getType()) {
                        case "RECEIVE" -> {
                            if (messageListener != null) {
                                String[] data = (String[]) msg.getData();
                                // data[0]=from, data[1]=contenu, data[2]=dateHeure
                                messageListener.onMessage(data[0], data[1],
                                        data.length > 2 ? data[2] : "");
                            }
                        }
                        case "USER_LIST" -> {
                            if (userListListener != null) {
                                userListListener.onUserList((String[]) msg.getData());
                            }
                        }
                        case "HISTORY" -> {
                            if (historyListener != null) {
                                historyListener.onHistory((String[]) msg.getData());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Connexion au serveur perdue.");
            }
        }, "ClientListener").start();
    }

    public void logout() {
        try {
            send(new NetworkMessage("LOGOUT", null));
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}