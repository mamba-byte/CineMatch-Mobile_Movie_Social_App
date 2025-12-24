package com.example.mobilprogfallproj.data.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "timeline_events")
public class TimelineEventEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;
    public int movieId;
    public String type; // "WATCHED" (İZLENDİ), "FAVORITED" (FAVORİLERE EKLENDİ), "RATED" (PUAN VERİLDİ)
    public long timestamp;
}

