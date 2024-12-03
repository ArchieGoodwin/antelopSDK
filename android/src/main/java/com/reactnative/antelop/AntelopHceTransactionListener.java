package com.reactnative.antelop;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

import fr.antelop.sdk.AntelopError;
import fr.antelop.sdk.authentication.CustomerAuthenticationMethod;
import fr.antelop.sdk.transaction.hce.DefaultHceTransactionListener;
import fr.antelop.sdk.transaction.hce.HceTransaction;

public class AntelopHceTransactionListener extends DefaultHceTransactionListener {

    private static final String CHANNEL_ID = "hce";

    private static boolean onGoingHceTransaction = false;
    private static List<CustomerAuthenticationMethod> hceCustomerAuthenticationMethods;
    private static HceTransaction hceTransaction;

    private static Callback callback;

    public static void setCallback(AntelopHceTransactionListener.Callback callback) {
        AntelopHceTransactionListener.callback = callback;
    }

    public static List<CustomerAuthenticationMethod> getHceCustomerAuthenticationMethods() {
        return  hceCustomerAuthenticationMethods;
    }

    public static HceTransaction getHceTransaction() {
        return hceTransaction;
    }

    public static boolean hasOngoingHceTransaction() {
        return onGoingHceTransaction;
    }

    public static void cancelOngoingHceTransaction() {
        onGoingHceTransaction = false;
        hceTransaction = null;
        hceCustomerAuthenticationMethods = null;
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "hce";
            String description = "Channel used to send HCE notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onTransactionStart(@NonNull Context context) {
        onGoingHceTransaction = true;

        if(callback != null) {
            callback.onTransactionStart(context);
        }
    }

    @Override
    public void onTransactionDone(@NonNull Context context, @NonNull HceTransaction transaction) {
        onGoingHceTransaction = false;

        hceTransaction = null;
        hceCustomerAuthenticationMethods = null;

        if(callback != null) {
            callback.onTransactionDone(transaction);
        }
    }

    @Override
    public void onCredentialsRequired(@NonNull Context context, @NonNull List<CustomerAuthenticationMethod> list, @NonNull HceTransaction transaction) {
        hceCustomerAuthenticationMethods = list;
        hceTransaction = transaction;

        createNotificationChannel(context);

        String packageName = context.getApplicationContext().getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_logo)
                    .setContentTitle("Nøelse Pay")
                    .setContentText(context.getString(R.string.authenticationRequired))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(0, builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            context.startActivity(launchIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(callback != null) {
            callback.onCredentialsRequired(list, transaction);
        }
    }

    @Override
    public void onTransactionError(@NonNull Context context, @NonNull AntelopError antelopError) {
        onGoingHceTransaction = false;

        hceTransaction = null;
        hceCustomerAuthenticationMethods = null;

        if(callback != null) {
            callback.onTransactionError(antelopError);
        }
    }


    interface Callback {
        void onTransactionStart(@NonNull Context context);
        void onCredentialsRequired(@NonNull List<CustomerAuthenticationMethod> list, @NonNull HceTransaction hceTransaction);
        void onTransactionDone(@NonNull HceTransaction hceTransaction);
        void onTransactionError(@NonNull AntelopError antelopError);
    }
}
