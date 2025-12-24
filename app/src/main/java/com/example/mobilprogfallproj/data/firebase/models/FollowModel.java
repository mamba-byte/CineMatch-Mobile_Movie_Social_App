package com.example.mobilprogfallproj.data.firebase.models;

public class FollowModel {
    public String followerId;
    public String followedId;

    public FollowModel() {
        // Firestore için gerekli varsayılan yapıcı
    }

    public FollowModel(String followerId, String followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
    }
}

