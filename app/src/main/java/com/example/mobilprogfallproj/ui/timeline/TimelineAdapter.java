package com.example.mobilprogfallproj.ui.timeline;

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
import com.example.mobilprogfallproj.ui.detail.MovieDetailActivity;
import com.example.mobilprogfallproj.util.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {
    private List<TimelineFragment.TimelineItem> items;

    public TimelineAdapter(List<TimelineFragment.TimelineItem> items) {
        this.items = items;
    }

    public void updateItems(List<TimelineFragment.TimelineItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class TimelineViewHolder extends RecyclerView.ViewHolder {
        private ImageView moviePoster;
        private TextView userNameText;
        private TextView actionText;
        private TextView movieTitleText;
        private TextView dateText;

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.movie_poster);
            userNameText = itemView.findViewById(R.id.user_name_text);
            actionText = itemView.findViewById(R.id.action_text);
            movieTitleText = itemView.findViewById(R.id.movie_title_text);
            dateText = itemView.findViewById(R.id.date_text);
        }

        public void bind(TimelineFragment.TimelineItem item) {
            userNameText.setText(item.user.displayName);
            
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

            // Film posterini yükle
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

