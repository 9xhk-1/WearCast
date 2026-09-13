package com.wearcast.app.ui.favorites;

import com.wearcast.app.model.FavFolder;
import com.wearcast.app.model.VideoItem;
import com.wearcast.app.network.AuthRepository;
import com.wearcast.app.network.Callback2;
import com.wearcast.app.network.VideoRepository;
import com.wearcast.app.ui.common.BaseVideoListFragment;

import java.util.ArrayList;
import java.util.List;

/** Shows the videos in the logged-in user's first favorites folder. */
public class FavoritesFragment extends BaseVideoListFragment {

    private final VideoRepository videoRepository = new VideoRepository();
    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void fetchData(Callback2<List<VideoItem>> callback) {
        if (!authRepository.isLoggedIn()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        authRepository.getUserInfo(new Callback2<com.wearcast.app.model.UserInfo>() {
            @Override
            public void onSuccess(com.wearcast.app.model.UserInfo user) {
                videoRepository.getFavFolders(user.mid, new Callback2<List<FavFolder>>() {
                    @Override
                    public void onSuccess(List<FavFolder> folders) {
                        if (folders.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                            return;
                        }
                        videoRepository.getFavResources(folders.get(0).id, 1, callback);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
