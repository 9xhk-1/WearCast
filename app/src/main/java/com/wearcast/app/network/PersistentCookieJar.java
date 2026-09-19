package com.wearcast.app.network;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/** Simple SharedPreferences backed cookie jar, keyed by cookie name so all bilibili subdomains share cookies. */
public class PersistentCookieJar implements CookieJar {

    private static final String PREFS = "wearcast_cookies";
    private final SharedPreferences prefs;
    private final Map<String, Cookie> cookieStore = new HashMap<>();

    public PersistentCookieJar(android.content.Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        restore();
    }

    private void restore() {
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> e : all.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String) {
                Cookie cookie = Cookie.parse(HttpUrl.parse("https://bilibili.com"), e.getKey() + "=" + v);
                if (cookie != null) {
                    cookieStore.put(e.getKey(), cookie);
                }
            }
        }
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Cookie cookie : cookies) {
            cookieStore.put(cookie.name(), cookie);
            editor.putString(cookie.name(), cookie.value());
        }
        editor.apply();
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        return new ArrayList<>(cookieStore.values());
    }

    public synchronized String getCookieValue(String name) {
        Cookie c = cookieStore.get(name);
        return c == null ? null : c.value();
    }

    /** Stores a cookie for the bilibili domain directly (used for device cookies obtained out of band). */
    public synchronized void put(String name, String value) {
        Cookie cookie = new Cookie.Builder()
                .name(name)
                .value(value)
                .domain("bilibili.com")
                .path("/")
                .expiresAt(System.currentTimeMillis() + 31536000000L)
                .build();
        cookieStore.put(name, cookie);
        prefs.edit().putString(name, value).apply();
    }

    public synchronized boolean hasSession() {
        return cookieStore.containsKey("SESSDATA");
    }

    public synchronized void clear() {
        cookieStore.clear();
        prefs.edit().clear().apply();
    }

    public synchronized String toCookieHeader() {
        StringBuilder sb = new StringBuilder();
        for (Cookie c : cookieStore.values()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(c.name()).append('=').append(c.value());
        }
        return sb.toString();
    }
}
