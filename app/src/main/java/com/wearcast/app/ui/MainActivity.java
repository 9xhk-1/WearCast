package com.wearcast.app.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.wearcast.app.R;
import com.wearcast.app.ui.favorites.FavoritesFragment;
import com.wearcast.app.ui.profile.ProfileFragment;
import com.wearcast.app.ui.recommend.RecommendFragment;
import com.wearcast.app.ui.search.SearchFragment;

import java.util.ArrayList;
import java.util.List;

/** Hosts the four primary tabs (search / recommend / favorites / profile) in a swipeable pager. */
public class MainActivity extends AppCompatActivity {

    private final View[] dots = new View[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 pager = findViewById(R.id.view_pager);
        pager.setAdapter(new PagerAdapter(this));

        dots[0] = findViewById(R.id.dot_0);
        dots[1] = findViewById(R.id.dot_1);
        dots[2] = findViewById(R.id.dot_2);
        dots[3] = findViewById(R.id.dot_3);
        updateDots(0);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
    }

    private void updateDots(int selected) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(i == selected ? R.drawable.bg_dot_active : R.drawable.bg_dot);
        }
    }

    private static class PagerAdapter extends FragmentStateAdapter {
        PagerAdapter(FragmentActivity activity) {
            super(activity);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return new RecommendFragment();
                case 2:
                    return new FavoritesFragment();
                case 3:
                    return new ProfileFragment();
                default:
                    return new SearchFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
