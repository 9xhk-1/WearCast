package com.wearcast.app.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.wearcast.app.R;
import com.wearcast.app.model.Comment;
import com.wearcast.app.model.VideoDetail;
import com.wearcast.app.network.Callback2;
import com.wearcast.app.network.VideoRepository;
import com.wearcast.app.ui.cast.CastControlActivity;
import com.wearcast.app.ui.cast.DeviceListActivity;
import com.wearcast.app.cast.CastManager;

import java.util.List;

public class VideoDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BVID = "extra_bvid";

    private final VideoRepository repository = new VideoRepository();
    private VideoDetail currentDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        String bvid = getIntent().getStringExtra(EXTRA_BVID);

        ImageView cover = findViewById(R.id.image_cover);
        TextView title = findViewById(R.id.text_title);
        TextView author = findViewById(R.id.text_author);
        TextView stats = findViewById(R.id.text_stats);
        TextView description = findViewById(R.id.text_description);
        RecyclerView commentsView = findViewById(R.id.recycler_comments);
        ImageView playButton = findViewById(R.id.button_play_cast);

        CommentAdapter commentAdapter = new CommentAdapter();
        commentsView.setLayoutManager(new LinearLayoutManager(this));
        commentsView.setAdapter(commentAdapter);

        if (bvid == null) {
            finish();
            return;
        }

        repository.getVideoDetail(bvid, new Callback2<VideoDetail>() {
            @Override
            public void onSuccess(VideoDetail detail) {
                currentDetail = detail;
                title.setText(detail.title);
                if (detail.owner != null) {
                    author.setText(detail.owner.name);
                }
                if (detail.stat != null) {
                    stats.setText(getString(R.string.video_play_count) + " " + detail.stat.view +
                            "  ·  " + getString(R.string.video_danmaku_count) + " " + detail.stat.danmaku +
                            "  ·  " + getString(R.string.video_comment_count) + " " + detail.stat.reply);
                }
                description.setText(detail.desc);
                Glide.with(VideoDetailActivity.this)
                        .load(detail.pic)
                        .apply(new RequestOptions().placeholder(R.drawable.bg_thumb_placeholder).centerCrop())
                        .into(cover);

                repository.getComments(detail.aid, 1, new Callback2<List<Comment>>() {
                    @Override
                    public void onSuccess(List<Comment> comments) {
                        commentAdapter.setComments(comments);
                    }

                    @Override
                    public void onError(String message) {
                        // Comments are supplementary; ignore failures silently.
                    }
                });
            }

            @Override
            public void onError(String message) {
                title.setText(R.string.net_error);
            }
        });

        playButton.setOnClickListener(v -> onPlayCastClicked());
    }

    private void onPlayCastClicked() {
        if (currentDetail == null) return;
        Intent intent;
        if (CastManager.get(this).getConnectedDevice() != null) {
            intent = new Intent(this, CastControlActivity.class);
        } else {
            intent = new Intent(this, DeviceListActivity.class);
        }
        intent.putExtra(CastControlActivity.EXTRA_BVID, currentDetail.bvid);
        intent.putExtra(CastControlActivity.EXTRA_CID, currentDetail.cid);
        intent.putExtra(CastControlActivity.EXTRA_TITLE, currentDetail.title);
        startActivity(intent);
    }
}
