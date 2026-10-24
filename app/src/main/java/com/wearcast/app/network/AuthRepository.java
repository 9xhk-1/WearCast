package com.wearcast.app.network;

import android.util.Log;

import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import com.wearcast.app.model.LoginQr;
import com.wearcast.app.model.UserInfo;

/** Handles the QR code login flow (generate + poll) and the logged in user's basic profile. */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private static final String QR_GENERATE = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
    private static final String QR_POLL = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=%s";

    public static final int POLL_WAITING = 86101;
    public static final int POLL_SCANNED = 86090;
    public static final int POLL_EXPIRED = 86038;
    public static final int POLL_SUCCESS = 0;

    private final okhttp3.OkHttpClient client;

    public AuthRepository() {
        this.client = ApiClient.get().httpClient();
    }

    public void generateQr(final Callback2<LoginQr> callback) {
        final Callback2<LoginQr> cb = Callback2.main(callback);
        Request request = new Request.Builder().url(QR_GENERATE).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "QR generate request failed", e);
                cb.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "QR generate HTTP " + response.code());
                        cb.onError("HTTP " + response.code());
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : null;
                    if (body == null || body.isEmpty()) {
                        cb.onError("empty response");
                        return;
                    }
                    if (!body.trim().startsWith("{")) {
                        Log.w(TAG, "QR generate unexpected body: "
                                + body.substring(0, Math.min(200, body.length())));
                        cb.onError("unexpected response");
                        return;
                    }
                    JsonObject root = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    if (root.get("code").getAsInt() != 0) {
                        cb.onError(root.has("message") ? root.get("message").getAsString() : "unknown error");
                        return;
                    }
                    JsonObject data = root.getAsJsonObject("data");
                    LoginQr qr = new LoginQr();
                    qr.url = data.get("url").getAsString();
                    qr.qrcodeKey = data.get("qrcode_key").getAsString();
                    cb.onSuccess(qr);
                } catch (Exception e) {
                    Log.w(TAG, "QR generate parse failed", e);
                    cb.onError(e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    /** Result code follows POLL_* constants above; message carries a human readable status. */
    public void pollQr(String qrcodeKey, final Callback2<Integer> callback) {
        final Callback2<Integer> cb = Callback2.main(callback);
        Request request = new Request.Builder().url(String.format(QR_POLL, qrcodeKey)).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                cb.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : null;
                    if (body == null || body.isEmpty()) {
                        cb.onError("empty response");
                        return;
                    }
                    if (!body.trim().startsWith("{")) {
                        Log.w(TAG, "QR poll unexpected body: "
                                + body.substring(0, Math.min(200, body.length())));
                        cb.onError("unexpected response");
                        return;
                    }
                    JsonObject root = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    if (root.get("code").getAsInt() != 0) {
                        cb.onError(root.has("message") ? root.get("message").getAsString() : "unknown error");
                        return;
                    }
                    JsonObject data = root.getAsJsonObject("data");
                    int status = data.get("code").getAsInt();
                    cb.onSuccess(status);
                } catch (Exception e) {
                    Log.w(TAG, "QR poll parse failed", e);
                    cb.onError(e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    public void getUserInfo(final Callback2<UserInfo> callback) {
        final Callback2<UserInfo> cb = Callback2.main(callback);
        ApiClient.get().service().getNav().enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(retrofit2.Call<BiliRawResponse> call, retrofit2.Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    cb.onError(body != null ? body.message : "empty response");
                    return;
                }
                JsonObject data = body.data.getAsJsonObject();
                UserInfo info = new UserInfo();
                info.isLogin = data.has("isLogin") && data.get("isLogin").getAsBoolean();
                if (data.has("mid")) info.mid = data.get("mid").getAsLong();
                if (data.has("uname")) info.uname = data.get("uname").getAsString();
                if (data.has("face")) info.face = data.get("face").getAsString();
                if (data.has("level_info") && data.getAsJsonObject("level_info").has("current_level")) {
                    info.level = data.getAsJsonObject("level_info").get("current_level").getAsInt();
                }
                cb.onSuccess(info);
            }

            @Override
            public void onFailure(retrofit2.Call<BiliRawResponse> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    public boolean isLoggedIn() {
        return ApiClient.get().cookieJar().hasSession();
    }

    public void logout() {
        ApiClient.get().cookieJar().clear();
    }
}
