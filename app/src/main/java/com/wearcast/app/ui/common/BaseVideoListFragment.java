package com.wearcast.app.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wearcast.app.R;
import com.wearcast.app.model.VideoItem;
import com.wearcast.app.network.Callback2;

import java.util.List;

/** Common recycler + swipe-refresh + empty-state plumbing shared by the video list tabs. */
public abstract class BaseVideoListFragment extends Fragment {

    protected VideoListAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyView;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_view);
        emptyView = view.findViewById(R.id.layout_empty);

        adapter = new VideoListAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        swipeRefresh.setOnRefreshListener(this::loadData);
        loadData();
    }

    protected void loadData() {
        swipeRefresh.setRefreshing(true);
        fetchData(new Callback2<List<VideoItem>>() {
            @Override
            public void onSuccess(List<VideoItem> result) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                adapter.setItems(result);
                emptyView.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                emptyView.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    protected abstract void fetchData(Callback2<List<VideoItem>> callback);
}
