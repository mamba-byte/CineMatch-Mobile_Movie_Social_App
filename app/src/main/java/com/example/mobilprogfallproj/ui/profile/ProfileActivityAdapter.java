package com.example.mobilprogfallproj.ui.profile;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mobilprogfallproj.R;
import com.example.mobilprogfallproj.data.db.entities.MovieEntity;
import com.example.mobilprogfallproj.data.db.entities.TimelineEventEntity;
import com.example.mobilprogfallproj.ui.detail.MovieDetailActivity;
import com.example.mobilprogfallproj.util.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivityAdapter extends RecyclerView.Adapter<ProfileActivityAdapter.ActivityViewHolder> {

    public static class ProfileItem {
        public final TimelineEventEntity event;
        public final MovieEntity movie;

        public ProfileItem(TimelineEventEntity event, MovieEntity movie) {
            this.event = event;
            this.movie = movie;
        }
    }

    private List<ProfileItem> items;
    private String userDisplayName;

    public ProfileActivityAdapter(List<ProfileItem> items, String userDisplayName) {
        this.items = items != null ? items : new ArrayList<>();
        this.userDisplayName = userDisplayName;
    }

    public void updateItems(List<ProfileItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setUserDisplayName(String name) {
        this.userDisplayName = name;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        holder.bind(items.get(position), userDisplayName);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final ImageView moviePoster;
        private final TextView userNameText;
        private final TextView actionText;
        private final TextView movieTitleText;
        private final TextView dateText;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.movie_poster);
            userNameText = itemView.findViewById(R.id.user_name_text);
            actionText = itemView.findViewById(R.id.action_text);
            movieTitleText = itemView.findViewById(R.id.movie_title_text);
            dateText = itemView.findViewById(R.id.date_text);
        }

        void bind(ProfileItem item, String userDisplayName) {
            userNameText.setText(userDisplayName != null ? userDisplayName : "You");

            String action = "";
            String actionEmoji = "";
            switch (item.event.type) {
                case "WATCHED":
                    action = "watched";
                    actionEmoji = "👀";
                    break;
                case "FAVORITED":
                    action = "favorited";
                    actionEmoji = "❤️";
                    break;
                case "RATED":
                    action = "rated";
                    actionEmoji = "⭐";
                    break;
            }

            actionText.setText(actionEmoji + " " + action);
            movieTitleText.setText(item.movie.title);

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            String date = sdf.format(new Date(item.event.timestamp));
            dateText.setText(date);

            if (item.movie.posterPath != null && !item.movie.posterPath.isEmpty()) {
                String imageUrl = Constants.TMDB_IMAGE_BASE_URL + item.movie.posterPath;
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(moviePoster);
            } else {
                moviePoster.setImageResource(R.drawable.ic_launcher_background);
            }

            // Film detaylarını açmak için tıklama dinleyicisi ekle
            itemView.setOnClickListener(v -> {
                Intent intent = MovieDetailActivity.newIntent(itemView.getContext(), item.movie.id);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}


