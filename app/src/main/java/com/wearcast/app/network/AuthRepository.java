package com.wearcast.app.network;

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
        Request request = new Request.Builder().url(QR_GENERATE).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : null;
                    JsonObject root = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    if (root.get("code").getAsInt() != 0) {
                        callback.onError(root.has("message") ? root.get("message").getAsString() : "unknown error");
                        return;
                    }
                    JsonObject data = root.getAsJsonObject("data");
                    LoginQr qr = new LoginQr();
                    qr.url = data.get("url").getAsString();
                    qr.qrcodeKey = data.get("qrcode_key").getAsString();
                    callback.onSuccess(qr);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    /** Result code follows POLL_* constants above; message carries a human readable status. */
    public void pollQr(String qrcodeKey, final Callback2<Integer> callback) {
        Request request = new Request.Builder().url(String.format(QR_POLL, qrcodeKey)).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : null;
                    JsonObject root = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    if (root.get("code").getAsInt() != 0) {
                        callback.onError(root.has("message") ? root.get("message").getAsString() : "unknown error");
                        return;
                    }
                    JsonObject data = root.getAsJsonObject("data");
                    int status = data.get("code").getAsInt();
                    callback.onSuccess(status);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    public void getUserInfo(final Callback2<UserInfo> callback) {
        ApiClient.get().service().getNav().enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(retrofit2.Call<BiliRawResponse> call, retrofit2.Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    callback.onError(body != null ? body.message : "empty response");
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
                callback.onSuccess(info);
            }

            @Override
            public void onFailure(retrofit2.Call<BiliRawResponse> call, Throwable t) {
                callback.onError(t.getMessage());
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
