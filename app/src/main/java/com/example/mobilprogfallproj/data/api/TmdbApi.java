package com.example.mobilprogfallproj.data.api;

import com.example.mobilprogfallproj.data.model.MovieDetailResponse;
import com.example.mobilprogfallproj.data.model.MovieListResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApi {
    @GET("movie/popular")
    Call<MovieListResponse> getPopular(
            @Query("api_key") String apiKey,
            @Query("page") int page
    );

    @GET("movie/top_rated")
    Call<MovieListResponse> getTopRated(
            @Query("api_key") String apiKey,
            @Query("page") int page
    );

    @GET("discover/movie")
    Call<MovieListResponse> discoverMovies(
            @Query("api_key") String apiKey,
            @Query("with_genres") String genreIds,
            @Query("sort_by") String sortBy,
            @Query("vote_count.gte") int minVotes,
            @Query("page") int page
    );

    @GET("movie/{movie_id}")
    Call<MovieDetailResponse> getMovieDetail(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey
    );

    // TMDB genelinde tam katalog araması
    @GET("search/movie")
    Call<MovieListResponse> searchMovies(
            @Query("api_key") String apiKey,
            @Query("query") String query,
            @Query("page") int page,
            @Query("include_adult") boolean includeAdult
    );
}

