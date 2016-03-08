package com.jarone.litterary.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.jarone.litterary.handlers.MessageHandler;

/**
 * Created by V on 3/7/2016.
 */
public class WifiChangeReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;

    public WifiChangeReceiver(WifiManager manager) {
        wifiManager = manager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                MessageHandler.d("connected to: " + wifiManager.getConnectionInfo().getSSID());
            } else {
                MessageHandler.d("Wifi Disconnected" + wifiManager.getConnectionInfo().getSSID());
            }
        }
    }


}
