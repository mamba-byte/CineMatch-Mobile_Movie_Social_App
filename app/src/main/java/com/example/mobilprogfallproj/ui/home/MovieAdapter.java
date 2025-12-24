package com.example.mobilprogfallproj.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.util.Constants;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    private List<MovieEntity> movies;
    private OnMovieClickListener listener;

    public interface OnMovieClickListener {
        void onMovieClick(int movieId);
    }

    public MovieAdapter(List<MovieEntity> movies, OnMovieClickListener listener) {
        this.movies = movies;
        this.listener = listener;
    }

    public void updateMovies(List<MovieEntity> newMovies) {
        this.movies = newMovies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        MovieEntity movie = movies.get(position);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    class MovieViewHolder extends RecyclerView.ViewHolder {
        private ImageView posterImage;
        private TextView titleText;
        private TextView ratingText;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.poster_image);
            titleText = itemView.findViewById(R.id.title_text);
            ratingText = itemView.findViewById(R.id.rating_text);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMovieClick(movies.get(getAdapterPosition()).id);
                }
            });
        }

        public void bind(MovieEntity movie) {
            titleText.setText(movie.title);
            ratingText.setText(String.format("⭐ %.1f", movie.tmdbRating));
            
            TextView yearText = itemView.findViewById(R.id.year_text);
            if (yearText != null && movie.releaseYear > 0) {
                yearText.setText(String.valueOf(movie.releaseYear));
            }

            if (movie.posterPath != null && !movie.posterPath.isEmpty()) {
                String imageUrl = Constants.TMDB_IMAGE_BASE_URL + movie.posterPath;
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .centerCrop()
                        .into(posterImage);
            } else {
                posterImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }
}

