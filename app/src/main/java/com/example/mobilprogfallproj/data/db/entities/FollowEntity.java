package com.example.mobilprogfallproj.data.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "follows",
        primaryKeys = {"followerId", "followedId"})
public class FollowEntity {
    public long followerId;
    public long followedId;
}

