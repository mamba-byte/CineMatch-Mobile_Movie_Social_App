package com.example.mobilprogfallproj.ui.foryou;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.data.model.MovieDto;
import com.example.mobilprogfallproj.data.repo.MovieRepository;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.detail.MovieDetailActivity;
import com.example.mobilprogfallproj.ui.home.MovieAdapter;
import com.example.mobilprogfallproj.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForYouFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private MovieRepository movieRepository;
    private SocialRepository socialRepository;

    // Öneriler için sayfalama durumu
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private final List<MovieEntity> recommendedMovies = new ArrayList<>();
    private List<Integer> favoriteMovieIds = new ArrayList<>();
    private List<Integer> watchedMovieIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_foryou, container, false);
        
        if (getContext() == null) {
            return view;
        }
        
        recyclerView = view.findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            
            adapter = new MovieAdapter(new ArrayList<>(), movieId -> {
                if (getContext() != null) {
                    startActivity(MovieDetailActivity.newIntent(getContext(), movieId));
                }
            });
            recyclerView.setAdapter(adapter);

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy <= 0 || isLoading || isLastPage) return;

                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    boolean isAtEnd = firstVisibleItemPosition + visibleItemCount >= totalItemCount - 4;
                    if (isAtEnd) {
                        loadRecommendationsPage(currentPage + 1, false);
                    }
                }
            });
        }

        movieRepository = new MovieRepository(getContext());
        socialRepository = new SocialRepository(getContext());

        loadRecommendationsPage(1, true);

        return view;
    }

    private void loadRecommendationsPage(int page, boolean reset) {
        if (movieRepository == null || socialRepository == null || getContext() == null) return;

        isLoading = true;

        // İlk sayfada, favori/izlenen ID'lerini getir; sonraki sayfalar için yeniden kullan
        if (page == 1) {
            String currentUserId = LoginActivity.getCurrentUserId(getContext());
            if (currentUserId == null || currentUserId.isEmpty()) {
                return;
            }
            
            socialRepository.getFavoriteMovies(currentUserId, new SocialRepository.Callback<List<Integer>>() {
                @Override
                public void onSuccess(List<Integer> favIds) {
                    favoriteMovieIds = favIds != null ? favIds : new ArrayList<>();
                    socialRepository.getWatchedMovies(currentUserId, new SocialRepository.Callback<List<Integer>>() {
                        @Override
                        public void onSuccess(List<Integer> watchIds) {
                            watchedMovieIds = watchIds != null ? watchIds : new ArrayList<>();
                            loadDiscoverPageInternal(page, reset);
                        }

                        @Override
                        public void onError(String msg) {
                            isLoading = false;
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                @Override
                public void onError(String msg) {
                    isLoading = false;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            loadDiscoverPageInternal(page, reset);
        }
    }

    private void loadDiscoverPageInternal(int page, boolean reset) {
        // Basitlik için varsayılan tür kombinasyonu kullan (Aksiyon, Macera)
        String genreIds = "28,12";

        movieRepository.discoverMovies(genreIds, page, new MovieRepository.Callback<List<MovieEntity>>() {
            @Override
            public void onSuccess(List<MovieEntity> movies) {
                isLoading = false;
                List<MovieEntity> filtered = new ArrayList<>();
                if (movies != null) {
                    for (MovieEntity movie : movies) {
                        if (!watchedMovieIds.contains(movie.id) &&
                                !favoriteMovieIds.contains(movie.id)) {
                            filtered.add(movie);
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    isLastPage = true;
                    return;
                }

                if (reset) {
                    recommendedMovies.clear();
                }
                recommendedMovies.addAll(filtered);
                currentPage = page;

                if (adapter != null) {
                    adapter.updateMovies(recommendedMovies);
                }
            }

            @Override
            public void onError(String msg) {
                isLoading = false;
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

