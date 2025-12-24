package com.example.mobilprogfallproj.ui.social;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.firebase.models.UserModel;
import com.example.mobilprogfallproj.ui.profile.UserDetailActivity;
import com.example.mobilprogfallproj.util.UiAnimations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialUserAdapter extends RecyclerView.Adapter<SocialUserAdapter.UserViewHolder> {
    private List<UserModel> users;
    private String currentUserId;
    private Map<String, Boolean> followStatus;
    private OnFollowClickListener listener;
    private Context context;

    public interface OnFollowClickListener {
        void onFollowClick(String userId, boolean isFollowing);
    }

    public SocialUserAdapter(List<UserModel> users, String currentUserId, OnFollowClickListener listener) {
        this.users = users;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.followStatus = new HashMap<>();
    }

    public void updateUsers(List<UserModel> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    public void setFollowStatus(String userId, boolean isFollowing) {
        followStatus.put(userId, isFollowing);
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).id != null && users.get(i).id.equals(userId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_social_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView usernameText;
        private Button followButton;
        private View itemContainer;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.name_text);
            usernameText = itemView.findViewById(R.id.username_text);
            followButton = itemView.findViewById(R.id.follow_button);
            itemContainer = itemView.findViewById(R.id.item_container);

            // Kullanıcı profilini açmak için tüm öğeye tıkla
            if (itemContainer != null) {
                itemContainer.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && context != null) {
                        UserModel user = users.get(pos);
                        if (user.id != null) {
                            Intent intent = UserDetailActivity.newIntent(context, user.id);
                            context.startActivity(intent);
                        }
                    }
                });
            } else {
                // Yedek: tüm itemView'i tıklanabilir yap
                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && context != null) {
                        UserModel user = users.get(pos);
                        if (user.id != null) {
                            Intent intent = UserDetailActivity.newIntent(context, user.id);
                            context.startActivity(intent);
                        }
                    }
                });
            }

            // Takip et/takipten çık için takip butonuna tıkla (olay yayılımını durdur)
            followButton.setOnClickListener(v -> {
                v.setClickable(false); // Çift tıklamaları önle
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        UserModel user = users.get(pos);
                        if (user.id != null) {
                            boolean isFollowing = followStatus.getOrDefault(user.id, false);
                            listener.onFollowClick(user.id, isFollowing);
                        }
                    }
                }
                v.postDelayed(() -> v.setClickable(true), 500);
            });
        }

        public void bind(UserModel user) {
            nameText.setText(user.displayName);
            usernameText.setText("@" + user.username);
            
            if (user.id != null) {
                boolean isFollowing = followStatus.getOrDefault(user.id, false);
                followButton.setText(isFollowing ? "Unfollow" : "Follow");
            }
        }
    }
}

