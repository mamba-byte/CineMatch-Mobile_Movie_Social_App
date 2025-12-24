package com.example.mobilprogfallproj.ui.profile;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.data.firebase.models.MovieModel;
import com.example.mobilprogfallproj.data.firebase.models.TimelineEventModel;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.example.mobilprogfallproj.data.repo.MovieRepository;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.login.LoginActivity;
import com.example.mobilprogfallproj.util.UiAnimations;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    
    private TextView usernameText;
    private TextView displayNameText;
    private TextView bioText;
    private TextView followersCount;
    private TextView followingCount;
    private Button editProfileButton;
    private Button editBioButton;
    private Button logoutButton;
    private ImageView profileHeaderImage;
    private SocialRepository socialRepository;
    private RecyclerView recentActivityRecycler;
    private ProfileActivityAdapter recentActivityAdapter;
    private MovieRepository movieRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        if (getContext() == null) {
            return view;
        }
        
        usernameText = view.findViewById(R.id.username_text);
        displayNameText = view.findViewById(R.id.display_name_text);
        bioText = view.findViewById(R.id.bio_text);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        editProfileButton = view.findViewById(R.id.btn_edit_profile);
        editBioButton = view.findViewById(R.id.btn_edit_bio);
        logoutButton = view.findViewById(R.id.btn_logout);
        profileHeaderImage = view.findViewById(R.id.profile_header_image);
        recentActivityRecycler = view.findViewById(R.id.recent_activity_recycler);
        
        socialRepository = new SocialRepository(getContext());
        movieRepository = new MovieRepository(getContext());

        if (recentActivityRecycler != null) {
            recentActivityRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            recentActivityAdapter = new ProfileActivityAdapter(new ArrayList<>(), "You");
            recentActivityRecycler.setAdapter(recentActivityAdapter);
        }
        
        // Takipçiler/takip edilenler için tıklama dinleyicilerini ayarla
        View followersContainer = view.findViewById(R.id.followers_container);
        View followingContainer = view.findViewById(R.id.following_container);
        
        if (followersContainer != null) {
            followersContainer.setOnClickListener(v -> {
                String currentUserId = LoginActivity.getCurrentUserId(getContext());
                if (currentUserId != null) {
                    FollowersFollowingDialogFragment dialog = 
                        FollowersFollowingDialogFragment.newInstance("followers", currentUserId);
                    dialog.show(getParentFragmentManager(), "followers_dialog");
                }
            });
        }
        
        if (followingContainer != null) {
            followingContainer.setOnClickListener(v -> {
                String currentUserId = LoginActivity.getCurrentUserId(getContext());
                if (currentUserId != null) {
                    FollowersFollowingDialogFragment dialog = 
                        FollowersFollowingDialogFragment.newInstance("following", currentUserId);
                    dialog.show(getParentFragmentManager(), "following_dialog");
                }
            });
        }

        // Profil düzenle ve biyografi düzenle butonlarını hafif tıklama animasyonlarıyla ayarla
        UiAnimations.applyClickScale(editProfileButton, () -> {
            startActivity(EditProfileActivity.newIntent(getContext()));
        });

        UiAnimations.applyClickScale(editBioButton, () -> {
            startActivity(EditProfileActivity.newIntent(getContext()));
        });
        
        // Çıkış butonunu ayarla
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                if (getContext() != null) {
                    LoginActivity.logout(getContext());
                    Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            });
        }
        
        loadUserProfile();
        loadFollowCounts();
        loadRecentActivity();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fragment görünür olduğunda profil verilerini yenile
        loadUserProfile();
        loadFollowCounts();
        loadRecentActivity();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isAdded()) {
            // Fragment görünür olduğunda yenile
            loadUserProfile();
            loadFollowCounts();
            loadRecentActivity();
        }
    }

    private void loadUserProfile() {
        Context context = getContext();
        if (context == null) return;
        
        String currentUserId = LoginActivity.getCurrentUserId(context);
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        socialRepository.getUserById(currentUserId, new SocialRepository.Callback<UserModel>() {
            @Override
            public void onSuccess(UserModel user) {
                if (getActivity() != null && user != null) {
                    usernameText.setText("@" + user.username);
                    displayNameText.setText(user.displayName);
                    
                    if (user.bio != null && !user.bio.isEmpty()) {
                        bioText.setText(user.bio);
                        bioText.setVisibility(View.VISIBLE);
                    } else {
                        bioText.setVisibility(View.GONE);
                    }

                    if (profileHeaderImage != null) {
                        if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                            try {
                                profileHeaderImage.setImageURI(Uri.parse(user.profileImageUrl));
                            } catch (Exception ignored) {}
                        } else {
                            profileHeaderImage.setImageResource(android.R.color.darker_gray);
                        }
                    }
                } else {
                    usernameText.setText("@user1");
                    displayNameText.setText("Current User");
                    bioText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String msg) {
                if (getActivity() != null) {
                    usernameText.setText("@user1");
                    displayNameText.setText("Current User");
                    bioText.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadFollowCounts() {
        Context context = getContext();
        if (context == null) return;
        
        String currentUserId = LoginActivity.getCurrentUserId(context);
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        socialRepository.getFollowers(currentUserId, new SocialRepository.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> followers) {
                if (getActivity() != null) {
                    followersCount.setText(String.valueOf(followers != null ? followers.size() : 0));
                }
            }

            @Override
            public void onError(String msg) {
                if (getActivity() != null) {
                    followersCount.setText("0");
                }
            }
        });

        socialRepository.getFollowedUsers(currentUserId, new SocialRepository.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> following) {
                if (getActivity() != null) {
                    followingCount.setText(String.valueOf(following != null ? following.size() : 0));
                }
            }

            @Override
            public void onError(String msg) {
                if (getActivity() != null) {
                    followingCount.setText("0");
                }
            }
        });
    }

    private void loadRecentActivity() {
        if (recentActivityAdapter == null) return;
        
        Context context = getContext();
        if (context == null) return;
        
        String currentUserId = LoginActivity.getCurrentUserId(context);
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        android.util.Log.d("ProfileFragment", "Loading timeline events for user: " + currentUserId);
        
        socialRepository.getTimelineEventsForUser(currentUserId, new SocialRepository.Callback<List<TimelineEventModel>>() {
            @Override
            public void onSuccess(List<TimelineEventModel> events) {
                android.util.Log.d("ProfileFragment", "Loaded " + (events != null ? events.size() : 0) + " timeline events");
                if (getActivity() != null && events != null && !events.isEmpty()) {
                    // Zaman damgasına göre azalan sırada sırala (en yeni önce)
                    events.sort((e1, e2) -> Long.compare(e2.timestamp, e1.timestamp));
                    
                    // En son 20 ile sınırla
                    List<TimelineEventModel> recentEvents = events.size() > 20 
                        ? events.subList(0, 20) 
                        : events;
                    
                    android.util.Log.d("ProfileFragment", "Displaying " + recentEvents.size() + " recent events");
                    
                    // ProfileItem'a dönüştür - gerçek film verilerini getir
                    loadMoviesForEvents(recentEvents);
                } else {
                    android.util.Log.d("ProfileFragment", "No timeline events found or empty list");
                    if (getActivity() != null) {
                        recentActivityAdapter.updateItems(new ArrayList<>());
                    }
                }
            }

            @Override
            public void onError(String msg) {
                android.util.Log.e("ProfileFragment", "Error loading timeline events: " + msg);
                if (getActivity() != null) {
                    // Hata durumunda boş liste göster
                    recentActivityAdapter.updateItems(new ArrayList<>());
                }
            }
        });
    }

    // Geçici dönüştürme yöntemleri - adaptör güncellendiğinde kaldırılacak
    private com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity convertToTimelineEventEntity(TimelineEventModel model) {
        com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity entity = new com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity();
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
        com.example.mobilprogfallproj.data.db.entities.MovieEntity entity = new com.example.mobilprogfallproj.data.db.entities.MovieEntity();
        entity.id = model.id;
        entity.title = model.title;
        entity.posterPath = model.posterPath;
        entity.overview = model.overview;
        entity.releaseYear = model.releaseYear;
        entity.tmdbRating = model.tmdbRating;
        return entity;
    }

    private void loadMoviesForEvents(List<TimelineEventModel> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        final List<ProfileActivityAdapter.ProfileItem> items = new ArrayList<>();
        final int[] loadedCount = {0};
        final int totalEvents = events.size();

        for (TimelineEventModel event : events) {
            final TimelineEventModel finalEvent = event; // Etkin olarak final yap
            final int finalMovieId = event.movieId; // Etkin olarak final yap
            
            // Önce Firebase'den filmi almaya çalış
            com.example.mobilprogfallproj.data.firebase.FirebaseService.getInstance()
                .getMovieById(finalMovieId, new com.google.android.gms.tasks.OnCompleteListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            // Film Firebase'de bulundu
                            MovieModel movieModel = com.example.mobilprogfallproj.data.firebase.FirebaseService.documentToMovie(
                                task.getResult().getDocuments().get(0)
                            );
                            if (movieModel != null) {
                                items.add(new ProfileActivityAdapter.ProfileItem(
                                    convertToTimelineEventEntity(finalEvent),
                                    convertToMovieEntity(movieModel)
                                ));
                            } else {
                                // Yedek: sahte film oluştur
                                createDummyMovieItem(finalEvent, finalMovieId, items);
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
                                        items.add(new ProfileActivityAdapter.ProfileItem(
                                            convertToTimelineEventEntity(finalEvent),
                                            movie
                                        ));
                                        synchronized (loadedCount) {
                                            loadedCount[0]++;
                                            checkAndUpdateAdapter(items, loadedCount, totalEvents);
                                        }
                                    }

                                    @Override
                                    public void onError(String msg) {
                                        // Yedek olarak sahte film oluştur
                                        createDummyMovieItem(finalEvent, finalMovieId, items);
                                        synchronized (loadedCount) {
                                            loadedCount[0]++;
                                            checkAndUpdateAdapter(items, loadedCount, totalEvents);
                                        }
                                    }
                                });
                            } else {
                                // Repository yok, sahte oluştur
                                createDummyMovieItem(finalEvent, finalMovieId, items);
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

    private void createDummyMovieItem(TimelineEventModel event, int movieId, List<ProfileActivityAdapter.ProfileItem> items) {
        MovieModel movie = new MovieModel();
        movie.id = movieId;
        movie.title = "Movie " + movieId;
        movie.posterPath = null;
        items.add(new ProfileActivityAdapter.ProfileItem(
            convertToTimelineEventEntity(event),
            convertToMovieEntity(movie)
        ));
    }

    private void checkAndUpdateAdapter(final List<ProfileActivityAdapter.ProfileItem> items, 
                                       final int[] loadedCount, 
                                       final int totalEvents) {
        if (loadedCount[0] == totalEvents && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Öğeleri zaman damgasına göre azalan sırada sırala
                items.sort((i1, i2) -> Long.compare(i2.event.timestamp, i1.event.timestamp));
                recentActivityAdapter.updateItems(items);
            });
        }
    }
}
