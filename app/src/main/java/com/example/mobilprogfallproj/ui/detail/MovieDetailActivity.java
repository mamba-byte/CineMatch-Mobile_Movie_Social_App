package com.example.mobilprogfallproj.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.data.firebase.models.TimelineEventModel;
import com.example.mobilprogfallproj.data.repo.MovieRepository;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.login.LoginActivity;
import com.example.mobilprogfallproj.util.Constants;

public class MovieDetailActivity extends AppCompatActivity {
    private static final String EXTRA_MOVIE_ID = "movie_id";

    private int movieId;
    private MovieRepository movieRepository;
    private SocialRepository socialRepository;

    public static Intent newIntent(Context context, int movieId) {
        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra(EXTRA_MOVIE_ID, movieId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        movieId = getIntent().getIntExtra(EXTRA_MOVIE_ID, -1);
        if (movieId == -1) {
            finish();
            return;
        }

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // Varsayılan başlığı gizle (proje/uygulama adını gösteriyordu)
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        movieRepository = new MovieRepository(this);
        socialRepository = new SocialRepository(this);

        loadMovieDetails();

        Button btnFavorite = findViewById(R.id.btn_favorite);
        Button btnWatched = findViewById(R.id.btn_watched);

        btnFavorite.setOnClickListener(v -> addToFavorites());
        btnWatched.setOnClickListener(v -> markAsWatched());
    }

    private void loadMovieDetails() {
        movieRepository.getMovieById(movieId, new MovieRepository.Callback<MovieEntity>() {
            @Override
            public void onSuccess(MovieEntity movie) {
                displayMovie(movie);
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(MovieDetailActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayMovie(MovieEntity movie) {
        TextView titleText = findViewById(R.id.title_text);
        TextView yearText = findViewById(R.id.year_text);
        TextView ratingText = findViewById(R.id.rating_text);
        TextView overviewText = findViewById(R.id.overview_text);
        ImageView posterImage = findViewById(R.id.poster_image);

        titleText.setText(movie.title);
        yearText.setText(String.valueOf(movie.releaseYear));
        ratingText.setText(String.format("⭐ %.1f", movie.tmdbRating));
        overviewText.setText(movie.overview);

        if (movie.posterPath != null && !movie.posterPath.isEmpty()) {
            String imageUrl = Constants.TMDB_IMAGE_BASE_URL + movie.posterPath;
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(posterImage);
        }
    }

    private void addToFavorites() {
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        socialRepository.addFavoriteMovie(currentUserId, movieId, new SocialRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                socialRepository.addTimelineEvent(currentUserId, movieId, "FAVORITED", 
                    new SocialRepository.Callback<TimelineEventModel>() {
                        @Override
                        public void onSuccess(TimelineEventModel data) {
                            Toast.makeText(MovieDetailActivity.this, "Added to favorites!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String msg) {
                            Toast.makeText(MovieDetailActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(MovieDetailActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAsWatched() {
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        socialRepository.addWatchedMovie(currentUserId, movieId, new SocialRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                socialRepository.addTimelineEvent(currentUserId, movieId, "WATCHED",
                    new SocialRepository.Callback<TimelineEventModel>() {
                        @Override
                        public void onSuccess(TimelineEventModel data) {
                            Toast.makeText(MovieDetailActivity.this, "Marked as watched!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String msg) {
                            Toast.makeText(MovieDetailActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(MovieDetailActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

