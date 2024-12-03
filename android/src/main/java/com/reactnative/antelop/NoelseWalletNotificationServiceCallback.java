package com.reactnative.antelop;

import android.content.Context;
import android.util.Log;

import java.util.Date;
import java.util.List;

import fr.antelop.sdk.WalletLockReason;
import fr.antelop.sdk.WalletNotificationServiceCallback;
import fr.antelop.sdk.card.EmvApplicationActivationMethod;

public class NoelseWalletNotificationServiceCallback implements WalletNotificationServiceCallback {
    private static Callback callback;

    @Override
    public void onSettingsUpdated(Context context) {

    }

    @Override
    public void onCardsUpdated(Context context) {

    }

    @Override
    public void onEmvApplicationActivationRequired(Context context, String s, List<EmvApplicationActivationMethod> list) {

    }

    @Override
    public void onCountersUpdated(Context context) {

    }

    @Override
    public void onEmvApplicationCredentialsUpdated(Context context) {

    }

    @Override
    public void onWalletDeleted(Context context) {
        if(callback != null) {
            callback.onWalletDeleted();
        }
    }

    @Override
    public void onWalletLocked(Context context, WalletLockReason walletLockReason) {
        if(callback != null) {
            callback.onWalletLocked(walletLockReason);
        }
    }

    @Override
    public void onWalletUnlocked(Context context) {

    }

    @Override
    public void onLogout(Context context) {

    }

    @Override
    public void onSunsetScheduled(Context context, Date date) {

    }

    @Override
    public void onCustomerCredentialsReset(Context context) {

    }

    @Override
    public void onLostEligibility(Context context) {

    }

    @Override
    public void onWalletLoaded(Context context) {

    }

    public static void setCallback(Callback callback) {
        NoelseWalletNotificationServiceCallback.callback = callback;
    }

    interface Callback {
        void onWalletLocked(WalletLockReason walletLockReason);
        void onWalletDeleted();
    }
}
