package com.example.mobilprogfallproj.ui.dm;

import android.content.Intent;
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
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.example.mobilprogfallproj.data.repo.SocialRepository;
import com.example.mobilprogfallproj.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private SocialRepository socialRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        
        if (getContext() == null) {
            return view;
        }
        
        recyclerView = view.findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            
            adapter = new UserAdapter(new ArrayList<>(), new HashMap<>(), userId -> {
                if (getContext() != null) {
                    // userId zaten Firebase'den bir String
                    startActivity(ChatActivity.newIntent(getContext(), userId));
                }
            });
            recyclerView.setAdapter(adapter);
        }

        socialRepository = new SocialRepository(getContext());

        // Fragment görüntülendiğinde mesajları görüntülendi olarak işaretle
        String currentUserId = LoginActivity.getCurrentUserId(getContext());
        if (currentUserId != null && getActivity() instanceof com.example.mobilprogfallproj.ui.main.MainActivity) {
            com.example.mobilprogfallproj.ui.main.MainActivity mainActivity = 
                (com.example.mobilprogfallproj.ui.main.MainActivity) getActivity();
            mainActivity.markMessagesViewed(currentUserId);
        }

        loadConversationUsers();

        return view;
    }

    private void loadConversationUsers() {
        String currentUserId = LoginActivity.getCurrentUserId(getContext());
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        socialRepository.getConversationUserIds(currentUserId, 
            new SocialRepository.Callback<List<String>>() {
                @Override
                public void onSuccess(List<String> userIds) {
                    loadUserDetails(userIds);
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadUserDetails(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            if (adapter != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.updateUsers(new ArrayList<>(), new java.util.HashMap<>()));
            }
            return;
        }

        final List<com.example.mobilprogfallproj.data.db.entities.UserEntity> users = new ArrayList<>();
        final Map<Long, String> idMap = new HashMap<>(); // Long id'yi orijinal String userId'ye eşle
        final int[] loadedCount = {0};
        final int totalUsers = userIds.size();

        for (String userId : userIds) {
            final String finalUserId = userId; // Etkin olarak final yap
            if (finalUserId == null || finalUserId.isEmpty()) {
                loadedCount[0]++;
                checkAndUpdateUsers(users, idMap, loadedCount, totalUsers);
                continue;
            }
            
            socialRepository.getUserById(finalUserId, new SocialRepository.Callback<UserModel>() {
                @Override
                public void onSuccess(UserModel userModel) {
                    if (userModel != null && userModel.id != null) {
                        // Adaptör uyumluluğu için UserEntity'ye dönüştür
                        com.example.mobilprogfallproj.data.db.entities.UserEntity userEntity = 
                            new com.example.mobilprogfallproj.data.db.entities.UserEntity();
                        // Güvenli dönüştürme: long olarak parse etmeyi dene, başarısız olursa hash kodu kullan
                        long entityId;
                        try {
                            entityId = Long.parseLong(userModel.id);
                        } catch (NumberFormatException e) {
                            // ID UUID veya sayısal değilse, hash kodu kullan
                            entityId = (long) userModel.id.hashCode();
                        }
                        userEntity.id = entityId;
                        userEntity.username = userModel.username;
                        userEntity.displayName = userModel.displayName;
                        userEntity.bio = userModel.bio;
                        userEntity.profileImageUrl = userModel.profileImageUrl;
                        users.add(userEntity);
                        // Long id'den orijinal String userId'ye eşleme sakla
                        idMap.put(entityId, finalUserId);
                    }

                    loadedCount[0]++;
                    checkAndUpdateUsers(users, idMap, loadedCount, totalUsers);
                }

                @Override
                public void onError(String msg) {
                    android.util.Log.e("MessagesFragment", "Error loading user " + finalUserId + ": " + msg);
                    loadedCount[0]++;
                    checkAndUpdateUsers(users, idMap, loadedCount, totalUsers);
                }
            });
        }
    }

    private void checkAndUpdateUsers(final List<com.example.mobilprogfallproj.data.db.entities.UserEntity> users,
                                     final Map<Long, String> idMap,
                                     final int[] loadedCount,
                                     final int totalUsers) {
        if (loadedCount[0] == totalUsers && getActivity() != null && adapter != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    adapter.updateUsers(users, idMap);
                } catch (Exception e) {
                    android.util.Log.e("MessagesFragment", "Error updating adapter: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}

