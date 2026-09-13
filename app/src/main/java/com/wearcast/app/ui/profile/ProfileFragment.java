package com.wearcast.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.wearcast.app.R;
import com.wearcast.app.model.UserInfo;
import com.wearcast.app.network.AuthRepository;
import com.wearcast.app.network.Callback2;
import com.wearcast.app.ui.login.LoginActivity;

public class ProfileFragment extends Fragment {

    private final AuthRepository authRepository = new AuthRepository();
    private ImageView avatar;
    private TextView name;
    private TextView level;
    private TextView loginDesc;
    private Button loginButton;
    private Button logoutButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        avatar = view.findViewById(R.id.image_avatar);
        name = view.findViewById(R.id.text_name);
        level = view.findViewById(R.id.text_level);
        loginDesc = view.findViewById(R.id.text_login_desc);
        loginButton = view.findViewById(R.id.button_login);
        logoutButton = view.findViewById(R.id.button_logout);

        loginButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), LoginActivity.class)));
        logoutButton.setOnClickListener(v -> {
            authRepository.logout();
            refresh();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (!authRepository.isLoggedIn()) {
            showLoggedOut();
            return;
        }
        authRepository.getUserInfo(new Callback2<UserInfo>() {
            @Override
            public void onSuccess(UserInfo result) {
                if (getView() == null) return;
                if (result.isLogin) {
                    showLoggedIn(result);
                } else {
                    showLoggedOut();
                }
            }

            @Override
            public void onError(String message) {
                if (getView() == null) return;
                showLoggedOut();
            }
        });
    }

    private void showLoggedIn(UserInfo info) {
        name.setText(info.uname);
        level.setText(getString(R.string.profile_level) + " " + info.level);
        level.setVisibility(View.VISIBLE);
        loginDesc.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);
        logoutButton.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(info.face)
                .apply(new RequestOptions().circleCrop().placeholder(R.drawable.ic_avatar_placeholder))
                .into(avatar);
    }

    private void showLoggedOut() {
        name.setText(R.string.profile_not_logged_in);
        level.setVisibility(View.GONE);
        loginDesc.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.GONE);
        avatar.setImageResource(R.drawable.ic_avatar_placeholder);
    }
}
