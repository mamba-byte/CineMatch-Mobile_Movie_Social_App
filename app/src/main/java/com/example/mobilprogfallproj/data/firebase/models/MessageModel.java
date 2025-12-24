package com.example.mobilprogfallproj.data.firebase.models;

public class MessageModel {
    public String id;
    public String fromUserId;
    public String toUserId;
    public String text;
    public long timestamp;

    public MessageModel() {
        // Firestore için gerekli varsayılan yapıcı
    }

    public MessageModel(String id, String fromUserId, String toUserId, String text, long timestamp) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.text = text;
        this.timestamp = timestamp;
    }
}

