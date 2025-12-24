package com.example.mobilprogfallproj.data.firebase.models;

public class UserModel {
    public String id;
    public String username;
    public String displayName;
    public String bio;
    public String profileImageUrl;
    public String passwordHash; // Geçiş için sakla, ancak Firebase Auth kullanacağız

    public UserModel() {
        // Firestore için gerekli varsayılan yapıcı
    }

    public UserModel(String id, String username, String displayName, String bio, String profileImageUrl, String passwordHash) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.passwordHash = passwordHash;
    }
}

