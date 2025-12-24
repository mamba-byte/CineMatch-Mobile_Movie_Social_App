package com.example.mobilprogfallproj.data.firebase.models;

public class TimelineEventModel {
    public String id;
    public String userId;
    public int movieId;
    public String type; // "WATCHED" (İZLENDİ), "FAVORITED" (FAVORİLERE EKLENDİ), "RATED" (PUAN VERİLDİ)
    public long timestamp;

    public TimelineEventModel() {
        // Firestore için gerekli varsayılan yapıcı
    }

    public TimelineEventModel(String id, String userId, int movieId, String type, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.movieId = movieId;
        this.type = type;
        this.timestamp = timestamp;
    }
}

