package com.example.mobilprogfallproj.data.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String username;
    public String displayName;
    public String bio;
    public String profileImageUrl;
    public String passwordHash;
}

