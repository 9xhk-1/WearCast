package com.wearcast.app.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Seeds the bilibili device cookies (buvid3 / buvid4 / b_nut) that web risk control expects.
 *
 * Reference: xtcqinghe/bac, docs/misc/buvid3_4.md - buvid3/buvid4 are issued by the
 * finger/spi endpoint, and b_nut is what the main site hands out once a buvid3 exists.
 */
final class BiliDeviceCookies {

    private static final String SPI_URL = "https://api.bilibili.com/x/frontend/finger/spi";
    private static final long RETRY_INTERVAL_MS = 60L * 1000;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private final PersistentCookieJar cookieJar;
    private long lastAttemptMs;

    BiliDeviceCookies(PersistentCookieJar cookieJar) {
        this.cookieJar = cookieJar;
    }

    /** Idempotent and throttled, safe to call before every API request. */
    synchronized void ensure() {
        if (cookieJar.getCookieValue("buvid3") != null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAttemptMs < RETRY_INTERVAL_MS) {
            return;
        }
        lastAttemptMs = now;

        Request request = new Request.Builder()
                .url(SPI_URL)
                .header("User-Agent", ApiClient.USER_AGENT)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return;
            }
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (root.get("code").getAsInt() != 0) {
                return;
            }
            JsonObject data = root.getAsJsonObject("data");
            String b3 = data.has("b_3") ? data.get("b_3").getAsString() : null;
            String b4 = data.has("b_4") ? data.get("b_4").getAsString() : null;
            if (b3 == null || b3.isEmpty()) {
                return;
            }
            cookieJar.put("buvid3", b3);
            if (b4 != null && !b4.isEmpty()) {
                cookieJar.put("buvid4", b4);
            }
            cookieJar.put("b_nut", "100");
        } catch (Exception ignored) {
            // requests proceed without device cookies and may get rate limited
        }
    }
}
