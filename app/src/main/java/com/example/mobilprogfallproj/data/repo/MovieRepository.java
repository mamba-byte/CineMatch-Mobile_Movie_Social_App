package com.example.mobilprogfallproj.data.repo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.mobilprogfallproj.data.api.TmdbApi;
import com.example.mobilprogfallproj.data.api.TmdbClient;
import com.example.mobilprogfallproj.data.db.AppDatabase;
import com.example.mobilprogfallproj.data.db.dao.MovieDao;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.data.model.MovieDetailResponse;
import com.example.mobilprogfallproj.data.model.MovieDto;
import com.example.mobilprogfallproj.data.model.MovieListResponse;
import com.example.mobilprogfallproj.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieRepository {
    private final TmdbApi api;
    private final MovieDao movieDao;
    private final Handler mainHandler;

    public MovieRepository(Context context) {
        api = TmdbClient.getClient().create(TmdbApi.class);
        movieDao = AppDatabase.getInstance(context).movieDao();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadPopularMovies(int page, Callback<List<MovieEntity>> callback) {
        api.getPopular(Constants.TMDB_API_KEY, page)
                .enqueue(new retrofit2.Callback<MovieListResponse>() {
                    @Override
                    public void onResponse(Call<MovieListResponse> call,
                                           Response<MovieListResponse> response) {
                        if (response.body() != null) {
                            List<MovieEntity> entities = mapDtoToEntity(response.body().results);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                movieDao.insertMovies(entities);
                                mainHandler.post(() -> callback.onSuccess(entities));
                            });
                        } else {
                            mainHandler.post(() -> callback.onError("Empty body"));
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieListResponse> call, Throwable t) {
                        mainHandler.post(() -> callback.onError(t.getMessage()));
                    }
                });
    }

    public void loadTopRatedMovies(int page, Callback<List<MovieEntity>> callback) {
        api.getTopRated(Constants.TMDB_API_KEY, page)
                .enqueue(new retrofit2.Callback<MovieListResponse>() {
                    @Override
                    public void onResponse(Call<MovieListResponse> call,
                                           Response<MovieListResponse> response) {
                        if (response.body() != null) {
                            List<MovieEntity> entities = mapDtoToEntity(response.body().results);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                movieDao.insertMovies(entities);
                                mainHandler.post(() -> callback.onSuccess(entities));
                            });
                        } else {
                            mainHandler.post(() -> callback.onError("Empty body"));
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieListResponse> call, Throwable t) {
                        mainHandler.post(() -> callback.onError(t.getMessage()));
                    }
                });
    }

    public void searchMovies(String query, Callback<List<MovieEntity>> callback) {
        api.searchMovies(Constants.TMDB_API_KEY, query, 1, false)
                .enqueue(new retrofit2.Callback<MovieListResponse>() {
                    @Override
                    public void onResponse(Call<MovieListResponse> call,
                                           Response<MovieListResponse> response) {
                        if (response.body() != null) {
                            List<MovieEntity> entities = mapDtoToEntity(response.body().results);
                            // İsteğe bağlı: ayrıca veritabanına önbelleğe al
                            Executors.newSingleThreadExecutor().execute(() -> {
                                movieDao.insertMovies(entities);
                                mainHandler.post(() -> callback.onSuccess(entities));
                            });
                        } else {
                            mainHandler.post(() -> callback.onError("Empty body"));
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieListResponse> call, Throwable t) {
                        mainHandler.post(() -> callback.onError(t.getMessage()));
                    }
                });
    }

    public void getMovieById(int movieId, Callback<MovieEntity> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            MovieEntity movie = movieDao.getMovieById(movieId);
            if (movie != null) {
                mainHandler.post(() -> callback.onSuccess(movie));
            } else {
                // Veritabanında yoksa API'den yükle
                api.getMovieDetail(movieId, Constants.TMDB_API_KEY)
                        .enqueue(new retrofit2.Callback<MovieDetailResponse>() {
                            @Override
                            public void onResponse(Call<MovieDetailResponse> call,
                                                   Response<MovieDetailResponse> response) {
                                if (response.body() != null) {
                                    MovieEntity entity = mapDetailDtoToEntity(response.body());
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        movieDao.insertMovie(entity);
                                        mainHandler.post(() -> callback.onSuccess(entity));
                                    });
                                } else {
                                    mainHandler.post(() -> callback.onError("Empty body"));
                                }
                            }

                            @Override
                            public void onFailure(Call<MovieDetailResponse> call, Throwable t) {
                                mainHandler.post(() -> callback.onError(t.getMessage()));
                            }
                        });
            }
        });
    }

    public void discoverMovies(String genreIds, int page, Callback<List<MovieEntity>> callback) {
        api.discoverMovies(Constants.TMDB_API_KEY, genreIds, "vote_average.desc", 500, page)
                .enqueue(new retrofit2.Callback<MovieListResponse>() {
                    @Override
                    public void onResponse(Call<MovieListResponse> call,
                                           Response<MovieListResponse> response) {
                        if (response.body() != null) {
                            List<MovieEntity> entities = mapDtoToEntity(response.body().results);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                movieDao.insertMovies(entities);
                                mainHandler.post(() -> callback.onSuccess(entities));
                            });
                        } else {
                            mainHandler.post(() -> callback.onError("Empty body"));
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieListResponse> call, Throwable t) {
                        mainHandler.post(() -> callback.onError(t.getMessage()));
                    }
                });
    }

    // İlk sayfa için kolaylık aşırı yüklemeleri
    public void loadPopularMovies(Callback<List<MovieEntity>> callback) {
        loadPopularMovies(1, callback);
    }

    public void loadTopRatedMovies(Callback<List<MovieEntity>> callback) {
        loadTopRatedMovies(1, callback);
    }

    public void discoverMovies(String genreIds, Callback<List<MovieEntity>> callback) {
        discoverMovies(genreIds, 1, callback);
    }

    private List<MovieEntity> mapDtoToEntity(List<MovieDto> dtos) {
        List<MovieEntity> entities = new ArrayList<>();
        for (MovieDto dto : dtos) {
            MovieEntity entity = new MovieEntity();
            entity.id = dto.id;
            entity.title = dto.title;
            entity.posterPath = dto.poster_path;
            entity.overview = dto.overview;
            entity.tmdbRating = dto.vote_average;
            if (dto.release_date != null && !dto.release_date.isEmpty()) {
                try {
                    entity.releaseYear = Integer.parseInt(dto.release_date.substring(0, 4));
                } catch (Exception e) {
                    entity.releaseYear = 0;
                }
            }
            entities.add(entity);
        }
        return entities;
    }

    private MovieEntity mapDetailDtoToEntity(MovieDetailResponse dto) {
        MovieEntity entity = new MovieEntity();
        entity.id = dto.id;
        entity.title = dto.title;
        entity.posterPath = dto.poster_path;
        entity.overview = dto.overview;
        entity.tmdbRating = dto.vote_average;
        if (dto.release_date != null && !dto.release_date.isEmpty()) {
            try {
                entity.releaseYear = Integer.parseInt(dto.release_date.substring(0, 4));
            } catch (Exception e) {
                entity.releaseYear = 0;
            }
        }
        return entity;
    }

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String msg);
    }
}

