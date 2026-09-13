package com.wearcast.app.ui.login;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.wearcast.app.R;
import com.wearcast.app.model.LoginQr;
import com.wearcast.app.network.AuthRepository;
import com.wearcast.app.network.Callback2;

/** Displays a QR code the user scans with the bilibili mobile app, then polls until confirmed. */
public class LoginActivity extends AppCompatActivity {

    private static final int POLL_INTERVAL_MS = 2000;
    private static final int QR_SIZE = 300;

    private final AuthRepository authRepository = new AuthRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ImageView qrImage;
    private ProgressBar progressBar;
    private TextView statusText;
    private View expiredOverlay;
    private String qrcodeKey;
    private boolean stopped = false;

    private final Runnable pollRunnable = this::poll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        qrImage = findViewById(R.id.image_qr);
        progressBar = findViewById(R.id.progress_qr);
        statusText = findViewById(R.id.text_status);
        expiredOverlay = findViewById(R.id.layout_expired);

        expiredOverlay.setOnClickListener(v -> generateQr());
        generateQr();
    }

    private void generateQr() {
        expiredOverlay.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        qrImage.setImageBitmap(null);
        statusText.setText(R.string.login_scan_hint);

        authRepository.generateQr(new Callback2<LoginQr>() {
            @Override
            public void onSuccess(LoginQr qr) {
                if (stopped) return;
                qrcodeKey = qr.qrcodeKey;
                progressBar.setVisibility(View.GONE);
                renderQr(qr.url);
                handler.removeCallbacks(pollRunnable);
                handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
            }

            @Override
            public void onError(String message) {
                if (stopped) return;
                progressBar.setVisibility(View.GONE);
                statusText.setText(getString(R.string.net_error));
            }
        });
    }

    private void renderQr(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            Bitmap bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565);
            for (int x = 0; x < QR_SIZE; x++) {
                for (int y = 0; y < QR_SIZE; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            qrImage.setImageBitmap(bitmap);
        } catch (WriterException e) {
            statusText.setText(getString(R.string.net_error));
        }
    }

    private void poll() {
        if (stopped || qrcodeKey == null) return;
        authRepository.pollQr(qrcodeKey, new Callback2<Integer>() {
            @Override
            public void onSuccess(Integer status) {
                if (stopped) return;
                if (status == AuthRepository.POLL_SUCCESS) {
                    statusText.setText(R.string.login_success);
                    handler.postDelayed(LoginActivity.this::finish, 600);
                } else if (status == AuthRepository.POLL_SCANNED) {
                    statusText.setText(R.string.login_scanned);
                    handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                } else if (status == AuthRepository.POLL_EXPIRED) {
                    statusText.setText(R.string.login_expired);
                    expiredOverlay.setVisibility(View.VISIBLE);
                } else {
                    statusText.setText(R.string.login_waiting);
                    handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                }
            }

            @Override
            public void onError(String message) {
                if (stopped) return;
                handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopped = true;
        handler.removeCallbacks(pollRunnable);
    }
}
