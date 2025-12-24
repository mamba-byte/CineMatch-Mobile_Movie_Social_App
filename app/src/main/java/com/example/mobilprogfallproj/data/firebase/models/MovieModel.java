package com.example.mobilprogfallproj.data.firebase.models;

public class MovieModel {
    public int id;
    public String title;
    public String posterPath;
    public String overview;
    public int releaseYear;
    public double tmdbRating;

    public MovieModel() {
        // Firestore için gerekli varsayılan yapıcı
    }

    public MovieModel(int id, String title, String posterPath, String overview, int releaseYear, double tmdbRating) {
        this.id = id;
        this.title = title;
        this.posterPath = posterPath;
        this.overview = overview;
        this.releaseYear = releaseYear;
        this.tmdbRating = tmdbRating;
    }
}

