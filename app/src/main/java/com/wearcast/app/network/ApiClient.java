package com.wearcast.app.network;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Builds the shared Retrofit/OkHttp stack used to talk to the bilibili API. */
public final class ApiClient {

    private static final String BASE_URL = "https://api.bilibili.com/";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; WearCast) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";

    private static ApiClient instance;

    private final OkHttpClient httpClient;
    private final BiliApiService service;
    private final PersistentCookieJar cookieJar;

    private ApiClient(Context context) {
        cookieJar = new PersistentCookieJar(context);
        Interceptor headerInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws java.io.IOException {
                Request request = chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", "https://www.bilibili.com")
                        .build();
                return chain.proceed(request);
            }
        };
        httpClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor(headerInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(BiliApiService.class);
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
    }

    public static ApiClient get() {
        if (instance == null) {
            throw new IllegalStateException("ApiClient not initialized");
        }
        return instance;
    }

    public BiliApiService service() {
        return service;
    }

    public PersistentCookieJar cookieJar() {
        return cookieJar;
    }

    public OkHttpClient httpClient() {
        return httpClient;
    }
}
