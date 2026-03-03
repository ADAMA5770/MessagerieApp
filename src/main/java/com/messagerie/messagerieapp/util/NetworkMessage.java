package com.messagerie.messagerieapp.util;

import java.io.Serializable;

public class NetworkMessage implements Serializable {

    private String type;
    private Object data;

    public NetworkMessage(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public Object getData() { return data; }
}