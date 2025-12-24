package com.example.mobilprogfallproj.data.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long fromUserId;
    public long toUserId;
    public String text;
    public long timestamp;
}

