package com.cookiegames.smartcookie.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

public abstract class NetworkReceiver extends BroadcastReceiver {

    public abstract void onConnectivityChange(boolean isConnected);

    @Override
    public void onReceive(@NonNull Context context, Intent intent) {
        onConnectivityChange(isConnected(context));
    }

    private static boolean isConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}