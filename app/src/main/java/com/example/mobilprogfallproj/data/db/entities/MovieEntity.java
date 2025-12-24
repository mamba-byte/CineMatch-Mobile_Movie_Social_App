package com.example.mobilprogfallproj.data.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "movies")
public class MovieEntity {
    @PrimaryKey
    public int id;

    public String title;
    public String posterPath;
    public String overview;
    public int releaseYear;
    public double tmdbRating;
}

