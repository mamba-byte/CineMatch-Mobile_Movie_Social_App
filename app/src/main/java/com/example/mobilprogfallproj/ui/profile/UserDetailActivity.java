package com.example.mobilprogfallproj.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.data.firebase.FirebaseService;
import com.example.mobilprogfallproj.data.firebase.models.MovieModel;
import com.example.mobilprogfallproj.data.firebase.models.TimelineEventModel;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.example.mobilprogfallproj.data.repo.MovieRepository;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.dm.ChatActivity;
import com.example.mobilprogfallproj.ui.login.LoginActivity;
import com.example.mobilprogfallproj.ui.timeline.TimelineAdapter;
import com.example.mobilprogfallproj.ui.timeline.TimelineFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserDetailActivity extends AppCompatActivity {
    private static final String EXTRA_USER_ID = "user_id";

    private String userId;
    private Button followButton;
    private Button dmButton;
    private TextView followersCount;
    private TextView followingCount;
    private TextView bioText;
    private RecyclerView recentActivityRecycler;
    private TimelineAdapter activityAdapter;
    private SocialRepository socialRepository;
    private FirebaseService firebaseService;
    private MovieRepository movieRepository;
    private boolean isFollowing = false;

    public static Intent newIntent(Context context, String userId) {
        Intent intent = new Intent(context, UserDetailActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (userId == null || userId.equals(currentUserId)) {
            finish();
            return;
        }

        socialRepository = new SocialRepository(this);
        firebaseService = FirebaseService.getInstance();
        movieRepository = new MovieRepository(this);

        followButton = findViewById(R.id.btn_follow);
        dmButton = findViewById(R.id.btn_dm);
        followersCount = findViewById(R.id.followers_count);
        followingCount = findViewById(R.id.following_count);
        bioText = findViewById(R.id.bio_text);
        recentActivityRecycler = findViewById(R.id.recent_activity_recycler);

        recentActivityRecycler.setLayoutManager(new LinearLayoutManager(this));
        activityAdapter = new TimelineAdapter(new ArrayList<>());
        recentActivityRecycler.setAdapter(activityAdapter);

        loadUserProfile();
        loadFollowCounts();
        checkFollowStatus();
        loadRecentActivity();

        followButton.setOnClickListener(v -> {
            if (isFollowing) {
                unfollowUser();
            } else {
                followUser();
            }
        });

        dmButton.setOnClickListener(v -> {
            startActivity(ChatActivity.newIntent(this, userId));
        });
    }

    private void loadUserProfile() {
        socialRepository.getUserById(userId, new SocialRepository.Callback<UserModel>() {
            @Override
            public void onSuccess(UserModel user) {
                if (user != null) {
                    TextView usernameText = findViewById(R.id.username_text);
                    TextView displayNameText = findViewById(R.id.display_name_text);

                    usernameText.setText("@" + user.username);
                    displayNameText.setText(user.displayName);
                    
                    if (user.bio != null && !user.bio.isEmpty()) {
                        bioText.setText(user.bio);
                        bioText.setVisibility(android.view.View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(String msg) {
                // Hatayı işle
            }
        });
    }

    private void loadFollowCounts() {
        socialRepository.getFollowers(userId, new SocialRepository.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> followers) {
                followersCount.setText(String.valueOf(followers != null ? followers.size() : 0));
            }

            @Override
            public void onError(String msg) {
                followersCount.setText("0");
            }
        });

        socialRepository.getFollowedUsers(userId, new SocialRepository.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> following) {
                followingCount.setText(String.valueOf(following != null ? following.size() : 0));
            }

            @Override
            public void onError(String msg) {
                followingCount.setText("0");
            }
        });
    }

    private void checkFollowStatus() {
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        socialRepository.isFollowing(currentUserId, userId, new SocialRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean following) {
                isFollowing = following;
                followButton.setText(following ? "Unfollow" : "Follow");
            }

            @Override
            public void onError(String msg) {
                // Yoksay
            }
        });
    }

    private void loadRecentActivity() {
        socialRepository.getTimelineEventsForUser(userId, new SocialRepository.Callback<List<TimelineEventModel>>() {
            @Override
            public void onSuccess(List<TimelineEventModel> events) {
                if (events == null || events.isEmpty()) {
                    runOnUiThread(() -> activityAdapter.updateItems(new ArrayList<>()));
                    return;
                }
                
                // Kullanıcı bilgilerini al
                socialRepository.getUserById(userId, new SocialRepository.Callback<UserModel>() {
                    @Override
                    public void onSuccess(UserModel user) {
                        if (user == null) {
                            return;
                        }
                        
                        // Zaman damgasına göre azalan sırada sırala (en yeni önce)
                        events.sort((e1, e2) -> Long.compare(e2.timestamp, e1.timestamp));
                        
                        // En son 20 ile sınırla
                        List<TimelineEventModel> recentEvents = events.size() > 20 
                            ? events.subList(0, 20) 
                            : events;
                        
                        // Etkinlikler için filmleri yükle
                        loadMoviesForEvents(recentEvents, user);
                    }

                    @Override
                    public void onError(String msg) {
                        // Hatayı işle
                    }
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> activityAdapter.updateItems(new ArrayList<>()));
            }
        });
    }
    
    private void loadMoviesForEvents(List<TimelineEventModel> events, UserModel user) {
        if (events == null || events.isEmpty() || user == null) {
            return;
        }

        final List<TimelineFragment.TimelineItem> items = new ArrayList<>();
        final int[] loadedCount = {0};
        final int totalEvents = events.size();
        final UserModel finalUser = user; // Make effectively final

        for (TimelineEventModel event : events) {
            final TimelineEventModel finalEvent = event; // Make effectively final
            final int finalMovieId = event.movieId; // Make effectively final
            
            // Try to get movie from Firebase first
            firebaseService.getMovieById(finalMovieId, new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(Task<QuerySnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // Film Firebase'de bulundu
                        MovieModel movieModel = FirebaseService.documentToMovie(
                            task.getResult().getDocuments().get(0)
                        );
                        if (movieModel != null) {
                            items.add(new TimelineFragment.TimelineItem(
                                convertToTimelineEventEntity(finalEvent),
                                convertToMovieEntity(movieModel),
                                convertToUserEntity(finalUser)
                            ));
                        } else {
                            // Yedek: sahte film oluştur
                            createDummyMovieItem(finalEvent, finalMovieId, items, finalUser);
                        }
                        synchronized (loadedCount) {
                            loadedCount[0]++;
                            checkAndUpdateAdapter(items, loadedCount, totalEvents);
                        }
                    } else {
                        // Firebase'de yok, MovieRepository'yi dene
                        if (movieRepository != null) {
                            movieRepository.getMovieById(finalMovieId, new MovieRepository.Callback<MovieEntity>() {
                                @Override
                                public void onSuccess(MovieEntity movie) {
                                    items.add(new TimelineFragment.TimelineItem(
                                        convertToTimelineEventEntity(finalEvent),
                                        movie,
                                        convertToUserEntity(finalUser)
                                    ));
                                    synchronized (loadedCount) {
                                        loadedCount[0]++;
                                        checkAndUpdateAdapter(items, loadedCount, totalEvents);
                                    }
                                }

                                @Override
                                public void onError(String msg) {
                                    // Yedek olarak sahte film oluştur
                                    createDummyMovieItem(finalEvent, finalMovieId, items, finalUser);
                                    synchronized (loadedCount) {
                                        loadedCount[0]++;
                                        checkAndUpdateAdapter(items, loadedCount, totalEvents);
                                    }
                                }
                            });
                        } else {
                            // Repository yok, sahte oluştur
                            createDummyMovieItem(finalEvent, finalMovieId, items, finalUser);
                            synchronized (loadedCount) {
                                loadedCount[0]++;
                                checkAndUpdateAdapter(items, loadedCount, totalEvents);
                            }
                        }
                    }
                }
            });
        }
    }
    
    private void createDummyMovieItem(TimelineEventModel event, int movieId, List<TimelineFragment.TimelineItem> items, UserModel user) {
        MovieModel movie = new MovieModel();
        movie.id = movieId;
        movie.title = "Movie " + movieId;
        movie.posterPath = null;
        items.add(new TimelineFragment.TimelineItem(
            convertToTimelineEventEntity(event),
            convertToMovieEntity(movie),
            convertToUserEntity(user)
        ));
    }
    
    private void checkAndUpdateAdapter(final List<TimelineFragment.TimelineItem> items, 
                                       final int[] loadedCount, 
                                       final int totalEvents) {
        if (loadedCount[0] == totalEvents) {
            runOnUiThread(() -> {
                // Öğeleri zaman damgasına göre azalan sırada sırala
                items.sort((i1, i2) -> Long.compare(i2.event.timestamp, i1.event.timestamp));
                activityAdapter.updateItems(items);
            });
        }
    }

    // Geçici dönüştürme yardımcıları - adaptör güncellendiğinde kaldırılacak
    private com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity convertToTimelineEventEntity(TimelineEventModel model) {
        com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity entity = 
            new com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity();
        // ID'ler için güvenli dönüştürme
        try {
            entity.id = Long.parseLong(model.id != null ? model.id : "0");
        } catch (NumberFormatException e) {
            entity.id = (long) (model.id != null ? model.id.hashCode() : 0);
        }
        try {
            entity.userId = Long.parseLong(model.userId);
        } catch (NumberFormatException e) {
            entity.userId = (long) model.userId.hashCode();
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
        // Firebase UID (UUID string) için Long'a güvenli dönüştürme
        try {
            entity.id = Long.parseLong(model.id != null ? model.id : "0");
        } catch (NumberFormatException e) {
            // ID UUID veya sayısal değilse, hash kodu kullan
            entity.id = (long) (model.id != null ? model.id.hashCode() : 0);
        }
        entity.username = model.username;
        entity.displayName = model.displayName;
        entity.bio = model.bio;
        entity.profileImageUrl = model.profileImageUrl;
        return entity;
    }

    private void followUser() {
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        socialRepository.followUser(currentUserId, userId, new SocialRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                isFollowing = true;
                followButton.setText("Unfollow");
                loadFollowCounts();
                Toast.makeText(UserDetailActivity.this, "Now following user", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(UserDetailActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unfollowUser() {
        String currentUserId = LoginActivity.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        socialRepository.unfollowUser(currentUserId, userId, new SocialRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                isFollowing = false;
                followButton.setText("Follow");
                loadFollowCounts();
                Toast.makeText(UserDetailActivity.this, "Unfollowed user", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(UserDetailActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
