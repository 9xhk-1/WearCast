package com.wearcast.app.ui.common;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.wearcast.app.R;
import com.wearcast.app.model.VideoItem;
import com.wearcast.app.ui.detail.VideoDetailActivity;

import java.util.ArrayList;
import java.util.List;

/** Shared, uniformly styled video list used by search / recommend / favorites screens. */
public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoViewHolder> {

    private final List<VideoItem> items = new ArrayList<>();
    private final Context context;

    public VideoListAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<VideoItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void appendItems(List<VideoItem> more) {
        if (more == null || more.isEmpty()) return;
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem item = items.get(position);
        holder.title.setText(item.cleanTitle());
        holder.author.setText(item.author != null ? item.author : "");
        Glide.with(context)
                .load(item.cover)
                .apply(new RequestOptions().placeholder(R.drawable.bg_thumb_placeholder).centerCrop())
                .into(holder.cover);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, VideoDetailActivity.class);
            intent.putExtra(VideoDetailActivity.EXTRA_BVID, item.bvid);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;
        TextView author;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.image_cover);
            title = itemView.findViewById(R.id.text_title);
            author = itemView.findViewById(R.id.text_author);
        }
    }
}
