package com.wearcast.app.network;

import com.google.gson.JsonElement;

/** Used when the shape of "data" is not known ahead of time (kept as raw JsonElement for manual parsing). */
public class BiliRawResponse {
    public int code;
    public String message;
    public JsonElement data;

    public boolean isSuccess() {
        return code == 0;
    }
}
