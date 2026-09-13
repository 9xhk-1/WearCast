package com.wearcast.app.ui.recommend;

import com.wearcast.app.model.VideoItem;
import com.wearcast.app.network.Callback2;
import com.wearcast.app.network.VideoRepository;
import com.wearcast.app.ui.common.BaseVideoListFragment;

import java.util.List;

public class RecommendFragment extends BaseVideoListFragment {

    private final VideoRepository repository = new VideoRepository();

    @Override
    protected void fetchData(Callback2<List<VideoItem>> callback) {
        repository.recommend(0, callback);
    }
}
