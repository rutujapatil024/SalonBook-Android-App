package com.salonbook.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NetworkReceiver extends BroadcastReceiver {

    public static final String NETWORK_STATUS_ACTION = "com.salonbook.app.NETWORK_STATUS";
    public static final String EXTRA_IS_CONNECTED = "isConnected";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = isNetworkAvailable(context);

        // Send local broadcast
        Intent localIntent = new Intent(NETWORK_STATUS_ACTION);
        localIntent.putExtra(EXTRA_IS_CONNECTED, isConnected);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
}
