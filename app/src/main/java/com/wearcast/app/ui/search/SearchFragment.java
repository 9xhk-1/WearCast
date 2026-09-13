package com.wearcast.app.ui.search;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.wearcast.app.R;
import com.wearcast.app.model.VideoItem;
import com.wearcast.app.network.Callback2;
import com.wearcast.app.network.VideoRepository;
import com.wearcast.app.ui.common.BaseVideoListFragment;

import java.util.List;

public class SearchFragment extends Fragment {

    private String keyword = "";
    private ResultsFragment resultsFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText editKeyword = view.findViewById(R.id.edit_keyword);

        resultsFragment = new ResultsFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.container_results, resultsFragment)
                .commit();

        editKeyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                keyword = editKeyword.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    resultsFragment.search(keyword);
                }
                return true;
            }
            return false;
        });
    }

    public static class ResultsFragment extends BaseVideoListFragment {
        private final VideoRepository repository = new VideoRepository();
        private String pendingKeyword;

        public void search(String keyword) {
            this.pendingKeyword = keyword;
            loadData();
        }

        @Override
        protected void fetchData(Callback2<List<VideoItem>> callback) {
            if (pendingKeyword == null || pendingKeyword.isEmpty()) {
                callback.onSuccess(new java.util.ArrayList<>());
                return;
            }
            repository.search(pendingKeyword, 1, callback);
        }
    }
}
