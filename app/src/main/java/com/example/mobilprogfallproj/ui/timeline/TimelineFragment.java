package com.example.mobilprogfallproj.ui.timeline;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity;
import com.example.mobilprogfallproj.data.db.entities.UserEntity;
import com.example.mobilprogfallproj.data.firebase.FirebaseService;
import com.example.mobilprogfallproj.data.firebase.models.MovieModel;
import com.example.mobilprogfallproj.data.firebase.models.TimelineEventModel;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.example.mobilprogfallproj.data.repo.MovieRepository;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.login.LoginActivity;
import com.example.mobilprogfallproj.ui.social.UsersFragment;
import com.example.mobilprogfallproj.util.UiAnimations;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelineFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private TimelineAdapter adapter;
    private SocialRepository socialRepository;
    private FirebaseService firebaseService;
    private MovieRepository movieRepository;
    private boolean demoEventsAttempted = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);
        
        if (getContext() == null) {
            return view;
        }
        
        recyclerView = view.findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            
            adapter = new TimelineAdapter(new ArrayList<>());
            recyclerView.setAdapter(adapter);
        }

        // Başlıktaki kullanıcı arama butonu tıklama animasyonu ile
        ImageButton searchUsersButton = view.findViewById(R.id.btn_search_users);
        if (searchUsersButton != null) {
            UiAnimations.applyClickScale(searchUsersButton, this::openUsersSearch);
        }

        Context context = getContext();
        if (context == null) {
            return view;
        }

        socialRepository = new SocialRepository(context);
        firebaseService = FirebaseService.getInstance();
        movieRepository = new MovieRepository(context);

        // Fragment görüntülendiğinde zaman çizelgesini görüntülendi olarak işaretle
        String currentUserId = LoginActivity.getCurrentUserId(context);
        if (currentUserId != null && getActivity() instanceof com.example.mobilprogfallproj.ui.main.MainActivity) {
            com.example.mobilprogfallproj.ui.main.MainActivity mainActivity = 
                (com.example.mobilprogfallproj.ui.main.MainActivity) getActivity();
            mainActivity.markTimelineViewed(currentUserId);
        }

        loadTimelineEvents();

        return view;
    }

    private void loadTimelineEvents() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        String currentUserId = LoginActivity.getCurrentUserId(context);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        socialRepository.getTimelineEventsForFollowedUsers(currentUserId, 
            new SocialRepository.Callback<List<TimelineEventModel>>() {
                @Override
                public void onSuccess(List<TimelineEventModel> events) {
                    if (events == null || events.isEmpty()) {
                        // Gösterilecek etkinlik yok
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), "No timeline events yet", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        loadEventDetails(events);
                    }
                }

                @Override
                public void onError(String msg) {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void openUsersSearch() {
        if (getActivity() == null) return;

        // Kullanıcıların gözden geçirilebileceği/takip edilebileceği mevcut UsersFragment'e git
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, UsersFragment.newInstance(true))
                .addToBackStack(null)
                .commit();
    }

    private void loadEventDetails(List<TimelineEventModel> events) {
        if (events == null || events.isEmpty() || !isAdded() || getContext() == null) {
            return;
        }

        // Benzersiz kullanıcı ID'leri ve film ID'lerini al
        List<String> userIds = new ArrayList<>();
        List<Integer> movieIds = new ArrayList<>();
        for (TimelineEventModel event : events) {
            if (event.userId != null && !event.userId.isEmpty() && !userIds.contains(event.userId)) {
                userIds.add(event.userId);
            }
            if (!movieIds.contains(event.movieId)) {
                movieIds.add(event.movieId);
            }
        }

        // Kullanıcıları ve filmleri paralel olarak yükle
        final Map<String, UserModel> userMap = new HashMap<>();
        final Map<Integer, MovieModel> movieMap = new HashMap<>();
        final List<TimelineEventModel> finalEvents = events;
        final int[] loadedCount = {0};
        final int totalToLoad = userIds.size() + movieIds.size();

        if (totalToLoad == 0) {
            return;
        }

        // Kullanıcıları yükle
        for (String userId : userIds) {
            final String finalUserId = userId; // Etkin olarak final yap
            socialRepository.getUserById(finalUserId, new SocialRepository.Callback<UserModel>() {
                @Override
                public void onSuccess(UserModel user) {
                    if (user != null) {
                        userMap.put(finalUserId, user);
                    }
                    synchronized (loadedCount) {
                        loadedCount[0]++;
                        if (loadedCount[0] >= totalToLoad) {
                            checkAndUpdateAdapter(finalEvents, userMap, movieMap);
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    synchronized (loadedCount) {
                        loadedCount[0]++;
                        if (loadedCount[0] >= totalToLoad) {
                            checkAndUpdateAdapter(finalEvents, userMap, movieMap);
                        }
                    }
                }
            });
        }

        // Önce Firebase'den filmleri yükle, yedek olarak MovieRepository
        for (Integer movieId : movieIds) {
            final Integer finalMovieId = movieId;
            firebaseService.getMovieById(movieId, new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(com.google.android.gms.tasks.Task<QuerySnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        MovieModel movie = FirebaseService.documentToMovie(task.getResult().getDocuments().get(0));
                        if (movie != null) {
                            movieMap.put(finalMovieId, movie);
                        }
                        // Firebase başarısından sonra sayaç artır
                        synchronized (loadedCount) {
                            loadedCount[0]++;
                            if (loadedCount[0] >= totalToLoad) {
                                checkAndUpdateAdapter(finalEvents, userMap, movieMap);
                            }
                        }
                    } else {
                        // MovieRepository'ye yedek (TMDB API)
                        if (movieRepository != null) {
                            movieRepository.getMovieById(finalMovieId, new MovieRepository.Callback<MovieEntity>() {
                                @Override
                                public void onSuccess(MovieEntity movieEntity) {
                                    MovieModel movie = new MovieModel();
                                    movie.id = movieEntity.id;
                                    movie.title = movieEntity.title;
                                    movie.posterPath = movieEntity.posterPath;
                                    movie.overview = movieEntity.overview;
                                    movie.releaseYear = movieEntity.releaseYear;
                                    movie.tmdbRating = movieEntity.tmdbRating;
                                    movieMap.put(finalMovieId, movie);
                                    synchronized (loadedCount) {
                                        loadedCount[0]++;
                                        if (loadedCount[0] >= totalToLoad) {
                                            checkAndUpdateAdapter(finalEvents, userMap, movieMap);
                                        }
                                    }
                                }

                                @Override
                                public void onError(String msg) {
                                    // Son çare olarak sahte film oluştur
                                    MovieModel movie = new MovieModel();
                                    movie.id = finalMovieId;
                                    movie.title = "Movie " + finalMovieId;
                                    movieMap.put(finalMovieId, movie);
                                    synchronized (loadedCount) {
                                        loadedCount[0]++;
                                        if (loadedCount[0] >= totalToLoad) {
                                            checkAndUpdateAdapter(finalEvents, userMap, movieMap);
                                        }
                                    }
                                }
                            });
                        } else {
                            // Create dummy movie as last resort if no repository
                            MovieModel movie = new MovieModel();
                            movie.id = finalMovieId;
                            movie.title = "Movie " + finalMovieId;
                            movieMap.put(finalMovieId, movie);
                            synchronized (loadedCount) {
                                loadedCount[0]++;
                                if (loadedCount[0] >= totalToLoad) {
                                    checkAndUpdateAdapter(finalEvents, userMap, movieMap);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void checkAndUpdateAdapter(List<TimelineEventModel> events, Map<String, UserModel> userMap, Map<Integer, MovieModel> movieMap) {
        List<TimelineItem> items = new ArrayList<>();

        for (TimelineEventModel event : events) {
            UserModel user = userMap.get(event.userId);
            MovieModel movie = movieMap.get(event.movieId);

            if (user != null && movie != null) {
                // Adaptör uyumluluğu için entity'lere dönüştür
                com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity eventEntity = 
                    convertToTimelineEventEntity(event);
                com.example.mobilprogfallproj.data.db.entities.MovieEntity movieEntity = 
                    convertToMovieEntity(movie);
                com.example.mobilprogfallproj.data.db.entities.UserEntity userEntity = 
                    convertToUserEntity(user);

                items.add(new TimelineItem(eventEntity, movieEntity, userEntity));
            }
        }

        if (isAdded() && getActivity() != null && adapter != null) {
            getActivity().runOnUiThread(() -> adapter.updateItems(items));
        }
    }

    // Geçici dönüştürme yardımcıları
    private com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity convertToTimelineEventEntity(TimelineEventModel model) {
        com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity entity = 
            new com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity();
        // ID için güvenli ayrıştırma (Firebase UID'ler UUID'dir, sayısal değil)
        try {
            entity.id = Long.parseLong(model.id != null ? model.id : "0");
        } catch (NumberFormatException e) {
            entity.id = model.id != null ? model.id.hashCode() : 0;
        }
        // userId için güvenli ayrıştırma (Firebase UID'ler UUID'dir, sayısal değil)
        try {
            entity.userId = Long.parseLong(model.userId != null ? model.userId : "0");
        } catch (NumberFormatException e) {
            entity.userId = model.userId != null ? model.userId.hashCode() : 0;
        }
        entity.movieId = model.movieId;
        entity.type = model.type;
        entity.timestamp = model.timestamp;
        return entity;
    }

    private com.example.mobilprogfallproj.data.db.entities.MovieEntity convertToMovieEntity(MovieModel model) {
        com.example.mobilprogfallproj.data.db.entities.MovieEntity entity = 
            new com.example.mobilprogfallproj.data.db.entities.MovieEntity();
        entity.id = model.id;
        entity.title = model.title;
        entity.posterPath = model.posterPath;
        entity.overview = model.overview;
        entity.releaseYear = model.releaseYear;
        entity.tmdbRating = model.tmdbRating;
        return entity;
    }

    private com.example.mobilprogfallproj.data.db.entities.UserEntity convertToUserEntity(UserModel model) {
        com.example.mobilprogfallproj.data.db.entities.UserEntity entity = 
            new com.example.mobilprogfallproj.data.db.entities.UserEntity();
        // ID için güvenli ayrıştırma (Firebase UID'ler UUID'dir, sayısal değil)
        try {
            entity.id = Long.parseLong(model.id != null ? model.id : "0");
        } catch (NumberFormatException e) {
            entity.id = model.id != null ? model.id.hashCode() : 0;
        }
        entity.username = model.username;
        entity.displayName = model.displayName;
        entity.bio = model.bio;
        entity.profileImageUrl = model.profileImageUrl;
        return entity;
    }

    public static class TimelineItem {
        public TimelineEventEntity event;
        public MovieEntity movie;
        public UserEntity user;

        public TimelineItem(TimelineEventEntity event, MovieEntity movie, UserEntity user) {
            this.event = event;
            this.movie = movie;
            this.user = user;
        }
    }
}

