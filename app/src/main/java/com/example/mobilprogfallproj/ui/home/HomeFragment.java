package com.example.mobilprogfallproj.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.data.repo.MovieRepository;
import com.example.mobilprogfallproj.ui.detail.MovieDetailActivity;
import com.example.mobilprogfallproj.util.UiAnimations;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private MovieRepository movieRepository;
    private String currentTab = "popular";

    private ImageButton searchToggleButton;
    private EditText searchEditText;
    private List<MovieEntity> allMovies = new ArrayList<>();

    // Sayfalama durumu
    private int popularPage = 1;
    private int topRatedPage = 1;
    private boolean isLoadingPopular = false;
    private boolean isLoadingTopRated = false;
    private boolean isLastPopularPage = false;
    private boolean isLastTopRatedPage = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
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

            // Popüler / En İyi Puanlı için sonsuz kaydırma (arama sonuçları için değil)
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy <= 0) return;

                    String query = searchEditText != null ? searchEditText.getText().toString().trim() : "";
                    // Ararken sayfalama yapma
                    if (query.length() > 0) return;

                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    boolean isAtEnd = firstVisibleItemPosition + visibleItemCount >= totalItemCount - 4;

                    if ("popular".equals(currentTab)) {
                        if (!isAtEnd || isLoadingPopular || isLastPopularPage) return;
                        loadPopularMoviesPage(popularPage + 1, false);
                    } else {
                        if (!isAtEnd || isLoadingTopRated || isLastTopRatedPage) return;
                        loadTopRatedMoviesPage(topRatedPage + 1, false);
                    }
                }
            });
        }

        movieRepository = new MovieRepository(getContext());

        // Animasyonlu açma/kapama ile arama UI'sını ayarla
        searchToggleButton = view.findViewById(R.id.btn_search_toggle);
        searchEditText = view.findViewById(R.id.search_edit_text);

        if (searchToggleButton != null && searchEditText != null) {
            UiAnimations.applyClickScale(searchToggleButton, () -> {
                if (searchEditText.getVisibility() == View.GONE) {
                    searchEditText.setAlpha(0f);
                    searchEditText.setVisibility(View.VISIBLE);
                    searchEditText.animate().alpha(1f).setDuration(150).start();
                    searchEditText.requestFocus();
                } else {
                    searchEditText.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction(() -> {
                                searchEditText.setText("");
                                searchEditText.setVisibility(View.GONE);
                                searchEditText.setAlpha(1f);
                            })
                            .start();
                }
            });

            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    handleSearchQueryChanged(s.toString());
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        // TabLayout'u ayarla
        com.google.android.material.tabs.TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        currentTab = "popular";
                        // Popüler durumu sıfırla
                        popularPage = 1;
                        isLastPopularPage = false;
                        loadPopularMoviesPage(1, true);
                    } else {
                        currentTab = "top_rated";
                        // En iyi puanlı durumu sıfırla
                        topRatedPage = 1;
                        isLastTopRatedPage = false;
                        loadTopRatedMoviesPage(1, true);
                    }
                }

                @Override
                public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            });
        }
        
        loadPopularMoviesPage(1, true);

        return view;
    }

    private void loadPopularMoviesPage(int page, boolean reset) {
        if (movieRepository == null || getContext() == null) {
            return;
        }
        isLoadingPopular = true;
        movieRepository.loadPopularMovies(page, new MovieRepository.Callback<List<MovieEntity>>() {
            @Override
            public void onSuccess(List<MovieEntity> data) {
                isLoadingPopular = false;
                List<MovieEntity> newData = data != null ? data : new ArrayList<>();
                if (newData.isEmpty()) {
                    isLastPopularPage = true;
                    return;
                }
                if (reset) {
                    allMovies = new ArrayList<>(newData);
                } else {
                    allMovies.addAll(newData);
                }
                popularPage = page;
                if (adapter != null) {
                    adapter.updateMovies(allMovies);
                }
            }

            @Override
            public void onError(String msg) {
                isLoadingPopular = false;
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadTopRatedMoviesPage(int page, boolean reset) {
        if (movieRepository == null || getContext() == null) {
            return;
        }
        isLoadingTopRated = true;
        movieRepository.loadTopRatedMovies(page, new MovieRepository.Callback<List<MovieEntity>>() {
            @Override
            public void onSuccess(List<MovieEntity> data) {
                isLoadingTopRated = false;
                List<MovieEntity> newData = data != null ? data : new ArrayList<>();
                if (newData.isEmpty()) {
                    isLastTopRatedPage = true;
                    return;
                }
                if (reset) {
                    allMovies = new ArrayList<>(newData);
                } else {
                    allMovies.addAll(newData);
                }
                topRatedPage = page;
                if (adapter != null) {
                    adapter.updateMovies(allMovies);
                }
            }

            @Override
            public void onError(String msg) {
                isLoadingTopRated = false;
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleSearchQueryChanged(String query) {
        if (adapter == null || movieRepository == null || getContext() == null) return;

        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            // Arama temizlendiğinde, mevcut sekme listesine dön (ilk sayfa)
            popularPage = 1;
            topRatedPage = 1;
            isLastPopularPage = false;
            isLastTopRatedPage = false;
            if ("top_rated".equals(currentTab)) {
                loadTopRatedMoviesPage(1, true);
            } else {
                loadPopularMoviesPage(1, true);
            }
            return;
        }

        movieRepository.searchMovies(trimmed, new MovieRepository.Callback<List<MovieEntity>>() {
            @Override
            public void onSuccess(List<MovieEntity> data) {
                if (adapter != null) {
                    adapter.updateMovies(data != null ? data : new ArrayList<>());
                }
            }

            @Override
            public void onError(String msg) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

