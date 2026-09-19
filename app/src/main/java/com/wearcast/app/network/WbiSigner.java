package com.wearcast.app.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Signs bilibili web API requests with the Wbi signature (wts + w_rid).
 *
 * Follows the community documented algorithm (reference: xtcqinghe/bac, docs/misc/sign/wbi.md):
 * the img/sub keys are fetched from the nav API, the mixin key is derived through the fixed
 * reordering table, and w_rid is the MD5 of the sorted url-encoded query plus the mixin key.
 */
final class WbiSigner {

    private static final int[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
            61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
            36, 20, 34, 44, 52
    };

    private static final String NAV_URL = "https://api.bilibili.com/x/web-interface/nav";
    private static final long KEY_TTL_MS = 24L * 60 * 60 * 1000;
    private static final long RETRY_INTERVAL_MS = 60L * 1000;

    private static final WbiSigner INSTANCE = new WbiSigner();

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private String mixinKey;
    private long fetchedAtMs;
    private long lastAttemptMs;

    static WbiSigner get() {
        return INSTANCE;
    }

    private WbiSigner() {
    }

    /** Drops the cached signing keys so the next signature is computed with fresh ones. */
    synchronized void invalidate() {
        mixinKey = null;
        fetchedAtMs = 0L;
        lastAttemptMs = 0L;
    }

    /** Returns the signed query (sorted params + wts + w_rid), or null when keys are unavailable. */
    synchronized String signQuery(HttpUrl url) {
        String mixin = mixinKey();
        if (mixin == null) {
            return null;
        }
        TreeMap<String, String> params = new TreeMap<>();
        for (String name : url.queryParameterNames()) {
            String value = url.queryParameter(name);
            params.put(name, value == null ? "" : value);
        }
        params.put("wts", String.valueOf(System.currentTimeMillis() / 1000L));

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        String wRid = md5(query + mixin);
        if (wRid == null) {
            return null;
        }
        return query.append("&w_rid=").append(wRid).toString();
    }

    private String mixinKey() {
        long now = System.currentTimeMillis();
        if (mixinKey != null && now - fetchedAtMs < KEY_TTL_MS) {
            return mixinKey;
        }
        if (now - lastAttemptMs < RETRY_INTERVAL_MS) {
            return mixinKey;
        }
        lastAttemptMs = now;
        String fetched = fetchMixinKey();
        if (fetched != null) {
            mixinKey = fetched;
            fetchedAtMs = now;
        }
        return mixinKey;
    }

    private String fetchMixinKey() {
        Request request = new Request.Builder()
                .url(NAV_URL)
                .header("User-Agent", ApiClient.USER_AGENT)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return null;
            }
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                return null;
            }
            JsonObject data = root.getAsJsonObject("data");
            if (!data.has("wbi_img") || !data.get("wbi_img").isJsonObject()) {
                return null;
            }
            JsonObject wbiImg = data.getAsJsonObject("wbi_img");
            String raw = keyFromUrl(getStr(wbiImg, "img_url")) + keyFromUrl(getStr(wbiImg, "sub_url"));
            if (raw.length() < 64) {
                return null;
            }
            StringBuilder key = new StringBuilder(32);
            for (int i = 0; i < 32; i++) {
                key.append(raw.charAt(MIXIN_KEY_ENC_TAB[i]));
            }
            return key.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String keyFromUrl(String url) {
        if (url == null) {
            return "";
        }
        String name = url.substring(url.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String getStr(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    /** encodeURIComponent style encoding (space as %20) with the !'()* chars stripped first. */
    private static String encode(String s) {
        String filtered = s.replaceAll("[!'()*]", "");
        try {
            return URLEncoder.encode(filtered, "UTF-8").replace("+", "%20").replace("%7E", "~");
        } catch (java.io.UnsupportedEncodingException e) {
            return filtered;
        }
    }

    private static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                int value = b & 0xFF;
                if (value < 16) {
                    hex.append('0');
                }
                hex.append(Integer.toHexString(value));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
