package com.reactnative.antelop;

import android.content.Context;

import androidx.annotation.NonNull;

import fr.antelop.sdk.sca.CancellationReason;
import fr.antelop.sdk.sca.PushAuthenticationRequest;
import fr.antelop.sdk.sca.PushAuthenticationRequestListener;

public class AntelopPushAuthenticationRequestListener implements PushAuthenticationRequestListener {

    private static Callback callback;

    @Override
    public void onRequestReceived(@NonNull Context context, @NonNull PushAuthenticationRequest pushAuthenticationRequest) {
        if(callback != null) {
            callback.onRequestReceived(pushAuthenticationRequest);
        }
    }

    @Override
    public void onRequestCancelled(@NonNull Context context, @NonNull String requestId, @NonNull CancellationReason cancellationReason) {
        if(callback != null) {
            callback.onRequestCancelled(requestId, cancellationReason);
        }
    }

    public static void setCallback(Callback callback) {
        AntelopPushAuthenticationRequestListener.callback = callback;
    }

    interface Callback {
        void onRequestReceived(PushAuthenticationRequest pushAuthenticationRequestListener);
        void onRequestCancelled(String requestId, CancellationReason cancellationReason);
    }
}
