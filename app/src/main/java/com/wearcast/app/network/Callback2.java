package com.wearcast.app.network;

import android.os.Handler;
import android.os.Looper;

/** Simplified success/failure callback used by repository methods so UI code stays terse. */
public interface Callback2<T> {
    void onSuccess(T result);
    void onError(String message);

    /**
     * Wraps a callback so both callbacks run on the Android main thread.
     *
     * <p>OkHttp and Retrofit deliver their responses on background threads. Every UI touch must
     * happen on the main thread, so repositories wrap the caller's callback once and then only
     * ever invoke the wrapped instance.</p>
     */
    static <T> Callback2<T> main(final Callback2<T> delegate) {
        if (delegate == null) {
            return null;
        }
        final Handler handler = new Handler(Looper.getMainLooper());
        return new Callback2<T>() {
            @Override
            public void onSuccess(final T result) {
                handler.post(() -> delegate.onSuccess(result));
            }

            @Override
            public void onError(final String message) {
                handler.post(() -> delegate.onError(message));
            }
        };
    }
}
