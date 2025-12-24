package com.example.mobilprogfallproj.ui.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.UserEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<UserEntity> users;
    private Map<Long, String> idMap; // Long id'yi orijinal String userId'ye eşle
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(String userId);
    }

    public UserAdapter(List<UserEntity> users, Map<Long, String> idMap, OnUserClickListener listener) {
        this.users = users;
        this.idMap = idMap != null ? idMap : new HashMap<>();
        this.listener = listener;
    }

    public void updateUsers(List<UserEntity> newUsers, Map<Long, String> newIdMap) {
        this.users = newUsers;
        this.idMap = newIdMap != null ? newIdMap : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
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

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.name_text);
            usernameText = itemView.findViewById(R.id.username_text);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && position < users.size()) {
                        // Haritadan orijinal String userId'yi al veya String.valueOf'ye yedek
                        long entityId = users.get(position).id;
                        String userId = idMap.get(entityId);
                        if (userId == null) {
                            userId = String.valueOf(entityId);
                        }
                        listener.onUserClick(userId);
                    }
                }
            });
        }

        public void bind(UserEntity user) {
            nameText.setText(user.displayName);
            usernameText.setText("@" + user.username);
        }
    }
}

