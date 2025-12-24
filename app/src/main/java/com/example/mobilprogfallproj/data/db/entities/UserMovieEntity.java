package com.example.mobilprogfallproj.data.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_movies",
        primaryKeys = {"userId", "movieId"})
public class UserMovieEntity {
    public long userId;
    public int movieId;
    public boolean isFavorite;
    public boolean isWatched;
    public double userScore;
}

