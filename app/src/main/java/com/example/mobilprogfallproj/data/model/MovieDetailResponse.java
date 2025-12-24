package com.example.mobilprogfallproj.data.model;

import java.util.List;

public class MovieDetailResponse {
    public int id;
    public String title;
    public String overview;
    public String poster_path;
    public String backdrop_path;
    public double vote_average;
    public String release_date;
    public int runtime;
    public List<GenreDto> genres;
}

