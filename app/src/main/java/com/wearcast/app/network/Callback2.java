package com.wearcast.app.network;

/** Simplified success/failure callback used by repository methods so UI code stays terse. */
public interface Callback2<T> {
    void onSuccess(T result);
    void onError(String message);
}
