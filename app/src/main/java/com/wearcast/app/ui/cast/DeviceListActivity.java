package com.wearcast.app.ui.cast;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wearcast.app.R;
import com.wearcast.app.cast.CastManager;
import com.wearcast.app.cast.DlnaDevice;
import com.wearcast.app.cast.SsdpDiscovery;

/** First-time (or "switch device") device picker; discovery runs on a background thread. */
public class DeviceListActivity extends AppCompatActivity {

    private static final int DISCOVERY_TIMEOUT_MS = 6000;

    private final SsdpDiscovery discovery = new SsdpDiscovery();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DeviceAdapter adapter;
    private View searchingLayout;
    private String bvid;
    private long cid;
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        bvid = getIntent().getStringExtra(CastControlActivity.EXTRA_BVID);
        cid = getIntent().getLongExtra(CastControlActivity.EXTRA_CID, 0);
        title = getIntent().getStringExtra(CastControlActivity.EXTRA_TITLE);

        RecyclerView recyclerView = findViewById(R.id.recycler_devices);
        searchingLayout = findViewById(R.id.layout_searching);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(this::onDeviceSelected);
        recyclerView.setAdapter(adapter);

        startDiscovery();
    }

    private void startDiscovery() {
        searchingLayout.setVisibility(View.VISIBLE);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        new Thread(() -> discovery.discover(wifiManager, new SsdpDiscovery.Listener() {
            @Override
            public void onDeviceFound(DlnaDevice device) {
                mainHandler.post(() -> {
                    adapter.addDevice(device);
                    searchingLayout.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFinished() {
                mainHandler.post(() -> {
                    if (adapter.getItemCount() == 0) {
                        searchingLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        }, DISCOVERY_TIMEOUT_MS)).start();
    }

    private void onDeviceSelected(DlnaDevice device) {
        CastManager.get(this).rememberDevice(device);
        Intent intent = new Intent(this, CastControlActivity.class);
        intent.putExtra(CastControlActivity.EXTRA_BVID, bvid);
        intent.putExtra(CastControlActivity.EXTRA_CID, cid);
        intent.putExtra(CastControlActivity.EXTRA_TITLE, title);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        discovery.cancel();
    }
}
