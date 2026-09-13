package com.wearcast.app.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wearcast.app.R;
import com.wearcast.app.model.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> comments = new ArrayList<>();

    public void setComments(List<Comment> newComments) {
        comments.clear();
        if (newComments != null) comments.addAll(newComments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.author.setText(comment.member != null ? comment.member.uname : "");
        holder.message.setText(comment.content != null ? comment.content.message : "");
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView author;
        TextView message;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.text_comment_author);
            message = itemView.findViewById(R.id.text_comment_message);
        }
    }
}
