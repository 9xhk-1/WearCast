package com.wearcast.app.ui.cast;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wearcast.app.R;
import com.wearcast.app.cast.CastManager;
import com.wearcast.app.cast.DlnaController;
import com.wearcast.app.cast.DlnaDevice;
import com.wearcast.app.model.PlayUrlResult;
import com.wearcast.app.network.Callback2;
import com.wearcast.app.network.VideoRepository;

import java.util.ArrayList;
import java.util.List;

/** Playback controls (play/pause/seek/quality/volume) for the currently connected DLNA device. */
public class CastControlActivity extends AppCompatActivity {

    public static final String EXTRA_BVID = "extra_bvid";
    public static final String EXTRA_CID = "extra_cid";
    public static final String EXTRA_TITLE = "extra_title";

    private static final int SEEK_STEP_SECONDS = 10;
    private static final int DEFAULT_QUALITY = 64;

    private final VideoRepository videoRepository = new VideoRepository();

    private String bvid;
    private long cid;
    private String title;
    private DlnaDevice device;
    private DlnaController controller;
    private PlayUrlResult currentPlayUrl;
    private boolean isPlaying = false;
    private long lastKnownPositionSeconds = 0;

    private ImageView playPauseButton;
    private TextView connectedDeviceText;
    private Spinner qualitySpinner;
    private SeekBar volumeSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cast_control);

        bvid = getIntent().getStringExtra(EXTRA_BVID);
        cid = getIntent().getLongExtra(EXTRA_CID, 0);
        title = getIntent().getStringExtra(EXTRA_TITLE);

        device = CastManager.get(this).getConnectedDevice();
        if (device == null) {
            finish();
            return;
        }
        controller = new DlnaController(device);

        connectedDeviceText = findViewById(R.id.text_connected_device);
        TextView titleText = findViewById(R.id.text_video_title);
        playPauseButton = findViewById(R.id.button_play_pause);
        ImageView rewindButton = findViewById(R.id.button_rewind);
        ImageView forwardButton = findViewById(R.id.button_forward);
        qualitySpinner = findViewById(R.id.spinner_quality);
        volumeSeekBar = findViewById(R.id.seek_volume);
        android.widget.Button switchButton = findViewById(R.id.button_switch_device);
        android.widget.Button disconnectButton = findViewById(R.id.button_disconnect);

        connectedDeviceText.setText(getString(R.string.cast_connected_to, device.friendlyName));
        titleText.setText(title);

        playPauseButton.setOnClickListener(v -> togglePlayPause());
        rewindButton.setOnClickListener(v -> seekBy(-SEEK_STEP_SECONDS));
        forwardButton.setOnClickListener(v -> seekBy(SEEK_STEP_SECONDS));
        switchButton.setOnClickListener(v -> switchDevice());
        disconnectButton.setOnClickListener(v -> disconnect());

        volumeSeekBar.setProgress(50);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    controller.setVolume(progress, new Callback2<Void>() {
                        @Override
                        public void onSuccess(Void result) { }

                        @Override
                        public void onError(String message) { }
                    });
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        loadPlayUrl(DEFAULT_QUALITY);
    }

    private void loadPlayUrl(int quality) {
        videoRepository.getPlayUrl(bvid, cid, quality, new Callback2<PlayUrlResult>() {
            @Override
            public void onSuccess(PlayUrlResult result) {
                currentPlayUrl = result;
                setupQualitySpinner(result);
                startPlaybackOnDevice(result);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CastControlActivity.this, R.string.net_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupQualitySpinner(PlayUrlResult result) {
        List<String> labels = new ArrayList<>(result.acceptDescription);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qualitySpinner.setAdapter(adapter);

        int selectedIndex = result.acceptQuality.indexOf(result.quality);
        if (selectedIndex >= 0) qualitySpinner.setSelection(selectedIndex);

        qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (currentPlayUrl == null || position >= currentPlayUrl.acceptQuality.size()) return;
                int newQuality = currentPlayUrl.acceptQuality.get(position);
                if (newQuality != currentPlayUrl.quality) {
                    loadPlayUrl(newQuality);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void startPlaybackOnDevice(PlayUrlResult result) {
        if (result.durl == null || result.durl.isEmpty()) return;
        String mediaUrl = result.durl.get(0).url;
        controller.setUrlAndPlay(mediaUrl, title, new Callback2<Void>() {
            @Override
            public void onSuccess(Void result) {
                isPlaying = true;
                playPauseButton.setImageResource(R.drawable.ic_pause);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CastControlActivity.this, R.string.net_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePlayPause() {
        Callback2<Void> callback = new Callback2<Void>() {
            @Override
            public void onSuccess(Void result) {
                isPlaying = !isPlaying;
                playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }

            @Override
            public void onError(String message) { }
        };
        if (isPlaying) {
            controller.pause(callback);
        } else {
            controller.play(callback);
        }
    }

    private void seekBy(int deltaSeconds) {
        controller.getPositionInfo(new Callback2<DlnaController.PositionInfo>() {
            @Override
            public void onSuccess(DlnaController.PositionInfo info) {
                long current = parseHmsToSeconds(info.relTime);
                long target = Math.max(0, current + deltaSeconds);
                controller.seekRelative(secondsToHms(target), new Callback2<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        lastKnownPositionSeconds = target;
                    }

                    @Override
                    public void onError(String message) { }
                });
            }

            @Override
            public void onError(String message) {
                long target = Math.max(0, lastKnownPositionSeconds + deltaSeconds);
                controller.seekRelative(secondsToHms(target), new Callback2<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        lastKnownPositionSeconds = target;
                    }

                    @Override
                    public void onError(String message) { }
                });
            }
        });
    }

    private void switchDevice() {
        Intent intent = new Intent(this, DeviceListActivity.class);
        intent.putExtra(EXTRA_BVID, bvid);
        intent.putExtra(EXTRA_CID, cid);
        intent.putExtra(EXTRA_TITLE, title);
        startActivity(intent);
        finish();
    }

    private void disconnect() {
        controller.stop(new Callback2<Void>() {
            @Override
            public void onSuccess(Void result) { }

            @Override
            public void onError(String message) { }
        });
        CastManager.get(this).forgetDevice();
        finish();
    }

    private static long parseHmsToSeconds(String hms) {
        if (hms == null) return 0;
        String[] parts = hms.split(":");
        try {
            long h = Long.parseLong(parts[0]);
            long m = Long.parseLong(parts[1]);
            long s = (long) Double.parseDouble(parts[2]);
            return h * 3600 + m * 60 + s;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String secondsToHms(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
