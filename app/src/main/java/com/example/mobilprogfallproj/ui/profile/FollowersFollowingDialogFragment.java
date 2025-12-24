package com.example.mobilprogfallproj.ui.profile;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.login.LoginActivity;
import com.example.mobilprogfallproj.ui.social.SocialUserAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FollowersFollowingDialogFragment extends DialogFragment {
    private static final String ARG_MODE = "mode"; // "followers" veya "following"
    private static final String ARG_USER_ID = "user_id";
    
    private RecyclerView recyclerView;
    private SocialUserAdapter adapter;
    private SocialRepository socialRepository;
    private TextView titleText;
    private String mode;
    private String userId;
    private String currentUserId;
    
    public static FollowersFollowingDialogFragment newInstance(String mode, String userId) {
        FollowersFollowingDialogFragment fragment = new FollowersFollowingDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = getArguments().getString(ARG_MODE);
            userId = getArguments().getString(ARG_USER_ID);
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_followers_following, container, false);
        
        Context context = getContext();
        if (context == null) {
            return view;
        }
        
        currentUserId = LoginActivity.getCurrentUserId(context);
        socialRepository = new SocialRepository(context);
        
        titleText = view.findViewById(R.id.title_text);
        recyclerView = view.findViewById(R.id.recycler_view);
        Button closeButton = view.findViewById(R.id.btn_close);
        
        if (titleText != null) {
            titleText.setText(mode != null && mode.equals("followers") ? "Followers" : "Following");
        }
        
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            
            adapter = new SocialUserAdapter(new ArrayList<>(), currentUserId, new SocialUserAdapter.OnFollowClickListener() {
                @Override
                public void onFollowClick(String targetUserId, boolean isFollowing) {
                    if (currentUserId == null || currentUserId.equals(targetUserId)) {
                        return;
                    }
                    
                    if (isFollowing) {
                        socialRepository.unfollowUser(currentUserId, targetUserId, new SocialRepository.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                adapter.setFollowStatus(targetUserId, false);
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Unfollowed", Toast.LENGTH_SHORT).show();
                                }
                            }
                            
                            @Override
                            public void onError(String msg) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        socialRepository.followUser(currentUserId, targetUserId, new SocialRepository.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                adapter.setFollowStatus(targetUserId, true);
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Followed", Toast.LENGTH_SHORT).show();
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
            });
            recyclerView.setAdapter(adapter);
        }
        
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }
        
        loadUsers();
        
        return view;
    }
    
    private void loadUsers() {
        if (userId == null || mode == null) {
            return;
        }
        
        if (mode.equals("followers")) {
            // Takipçileri yükle
            socialRepository.getFollowers(userId, new SocialRepository.Callback<List<String>>() {
                @Override
                public void onSuccess(List<String> followerIds) {
                    if (followerIds != null && !followerIds.isEmpty()) {
                        loadUserDetails(followerIds);
                    } else {
                        if (adapter != null) {
                            adapter.updateUsers(new ArrayList<>());
                        }
                    }
                }
                
                @Override
                public void onError(String msg) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading followers: " + msg, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // Takip edilenleri yükle
            socialRepository.getFollowedUsers(userId, new SocialRepository.Callback<List<String>>() {
                @Override
                public void onSuccess(List<String> followingIds) {
                    if (followingIds != null && !followingIds.isEmpty()) {
                        loadUserDetails(followingIds);
                    } else {
                        if (adapter != null) {
                            adapter.updateUsers(new ArrayList<>());
                        }
                    }
                }
                
                @Override
                public void onError(String msg) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading following: " + msg, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    private void loadUserDetails(List<String> userIds) {
        final List<UserModel> users = new ArrayList<>();
        final Map<String, Boolean> followStatusMap = new HashMap<>();
        final int[] loadedCount = {0};
        final int totalUsers = userIds.size();
        
        if (totalUsers == 0) {
            if (adapter != null) {
                adapter.updateUsers(new ArrayList<>());
            }
            return;
        }
        
        for (String targetUserId : userIds) {
            final String finalTargetUserId = targetUserId;
            
            // Kullanıcı detaylarını yükle
            socialRepository.getUserById(finalTargetUserId, new SocialRepository.Callback<UserModel>() {
                @Override
                public void onSuccess(UserModel user) {
                    if (user != null) {
                        users.add(user);
                        
                        // Mevcut kullanıcının bu kullanıcıyı takip edip etmediğini kontrol et
                        if (currentUserId != null && !currentUserId.equals(finalTargetUserId)) {
                            socialRepository.isFollowing(currentUserId, finalTargetUserId, new SocialRepository.Callback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean isFollowing) {
                                    followStatusMap.put(finalTargetUserId, isFollowing);
                                    loadedCount[0]++;
                                    checkAndUpdateAdapter(users, followStatusMap, loadedCount, totalUsers);
                                }
                                
                                @Override
                                public void onError(String msg) {
                                    followStatusMap.put(finalTargetUserId, false);
                                    loadedCount[0]++;
                                    checkAndUpdateAdapter(users, followStatusMap, loadedCount, totalUsers);
                                }
                            });
                        } else {
                            loadedCount[0]++;
                            checkAndUpdateAdapter(users, followStatusMap, loadedCount, totalUsers);
                        }
                    } else {
                        loadedCount[0]++;
                        checkAndUpdateAdapter(users, followStatusMap, loadedCount, totalUsers);
                    }
                }
                
                @Override
                public void onError(String msg) {
                    loadedCount[0]++;
                    checkAndUpdateAdapter(users, followStatusMap, loadedCount, totalUsers);
                }
            });
        }
    }
    
    private void checkAndUpdateAdapter(final List<UserModel> users, 
                                       final Map<String, Boolean> followStatusMap,
                                       final int[] loadedCount,
                                       final int totalUsers) {
        if (loadedCount[0] == totalUsers && getActivity() != null && adapter != null) {
            getActivity().runOnUiThread(() -> {
                adapter.updateUsers(users);
                // Her kullanıcı için takip durumunu ayarla
                for (UserModel user : users) {
                    if (user.id != null) {
                        Boolean isFollowing = followStatusMap.get(user.id);
                        if (isFollowing != null) {
                            adapter.setFollowStatus(user.id, isFollowing);
                        }
                    }
                }
            });
        }
    }
}

