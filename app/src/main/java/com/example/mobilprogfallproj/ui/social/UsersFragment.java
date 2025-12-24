package com.example.mobilprogfallproj.ui.social;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.List;

public class UsersFragment extends Fragment {
    private static final String ARG_FOCUS_SEARCH = "focus_search";
    
    private RecyclerView recyclerView;
    private SocialUserAdapter adapter;
    private SocialRepository socialRepository;
    private EditText searchUsersEditText;
    private ImageButton searchToggleButton;
    private List<UserModel> allOtherUsers = new ArrayList<>();

    public static UsersFragment newInstance(boolean focusSearch) {
        UsersFragment fragment = new UsersFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FOCUS_SEARCH, focusSearch);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);
        
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchUsersEditText = view.findViewById(R.id.search_users_edit_text);
        searchToggleButton = view.findViewById(R.id.btn_toggle_user_search);

        String currentUserId = LoginActivity.getCurrentUserId(getContext());
        adapter = new SocialUserAdapter(new ArrayList<>(), currentUserId, (userId, isFollowing) -> {
            if (isFollowing) {
                unfollowUser(userId);
            } else {
                followUser(userId);
            }
        });
        recyclerView.setAdapter(adapter);

        socialRepository = new SocialRepository(getContext());
        loadUsers();

        if (searchToggleButton != null && searchUsersEditText != null) {
            searchToggleButton.setOnClickListener(v -> {
                // İlk tıklama: arama kutusunu aç. İkinci tıklama: Zaman Çizelgesi ekranına dön.
                if (searchUsersEditText.getVisibility() == View.GONE) {
                    searchUsersEditText.setVisibility(View.VISIBLE);
                    searchUsersEditText.requestFocus();
                } else {
                    // Arama metnini temizle ve önceki fragment'e dön (Zaman Çizelgesi)
                    searchUsersEditText.setText("");
                    if (getParentFragmentManager() != null) {
                        getParentFragmentManager().popBackStack();
                    }
                }
            });
        }

        if (searchUsersEditText != null) {
            searchUsersEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterUsers(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // İstenirse, arama kutusuna odaklan ve klavyeyi göster (örn. Zaman Çizelgesi'nden açıldığında)
        Bundle args = getArguments();
        if (args != null && args.getBoolean(ARG_FOCUS_SEARCH, false) && searchUsersEditText != null) {
            searchUsersEditText.setVisibility(View.VISIBLE);
            searchUsersEditText.requestFocus();
            searchUsersEditText.post(() -> {
                Context ctx = getContext();
                if (ctx != null) {
                    InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(searchUsersEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }

        return view;
    }

    private void loadUsers() {
        String currentUserId = LoginActivity.getCurrentUserId(getContext());
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        
        socialRepository.getAllUsers(new SocialRepository.Callback<List<UserModel>>() {
            @Override
            public void onSuccess(List<UserModel> users) {
                // Mevcut kullanıcıyı filtrele
                List<UserModel> otherUsers = new ArrayList<>();
                for (UserModel user : users) {
                    if (user.id != null && !user.id.equals(currentUserId)) {
                        otherUsers.add(user);
                    }
                }
                allOtherUsers = otherUsers;
                adapter.updateUsers(allOtherUsers);
                checkFollowStatus(allOtherUsers, currentUserId);
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String query) {
        if (adapter == null) return;

        if (query == null || query.trim().isEmpty()) {
            adapter.updateUsers(allOtherUsers);
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<UserModel> filtered = new ArrayList<>();

        for (UserModel user : allOtherUsers) {
            if (user.displayName != null && user.displayName.toLowerCase().contains(lowerQuery)) {
                filtered.add(user);
            } else if (user.username != null && user.username.toLowerCase().contains(lowerQuery)) {
                filtered.add(user);
            }
        }

        adapter.updateUsers(filtered);
    }

    private void checkFollowStatus(List<UserModel> users, String currentUserId) {
        for (UserModel user : users) {
            if (user.id != null) {
                socialRepository.isFollowing(currentUserId, user.id, 
                    new SocialRepository.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean isFollowing) {
                            adapter.setFollowStatus(user.id, isFollowing);
                        }

                        @Override
                        public void onError(String msg) {
                            // Yoksay
                        }
                    });
            }
        }
    }

    private void followUser(String userId) {
        String currentUserId = LoginActivity.getCurrentUserId(getContext());
        socialRepository.followUser(currentUserId, userId, 
            new SocialRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    adapter.setFollowStatus(userId, true);
                    Toast.makeText(getContext(), "Now following user", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void unfollowUser(String userId) {
        String currentUserId = LoginActivity.getCurrentUserId(getContext());
        socialRepository.unfollowUser(currentUserId, userId, 
            new SocialRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    adapter.setFollowStatus(userId, false);
                    Toast.makeText(getContext(), "Unfollowed user", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                }
            });
    }
}

