package com.jarone.litterary.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;

import java.util.ArrayList;

/**
 * Created by V on 3/7/2016.
 */
public class WifiScanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (ContextManager.getMainActivityInstance() != null && ContextManager.getMainActivityInstance().isWantResults()) {

            MessageHandler.d("Received wifi list...");

            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            ArrayList<ScanResult> results = (ArrayList<ScanResult>) wifiManager.getScanResults();
            String phantomWifi = "";
            for (ScanResult result : results) {
                if (result.SSID.contains("Phantom")) {
                    phantomWifi = result.SSID;
                    break;
                }
            }
            if (!phantomWifi.equals("")) {
                MessageHandler.d("Found phantom wifi");
                ContextManager.getMainActivityInstance().connectWithSSID(phantomWifi);
            }
            ContextManager.getMainActivityInstance().setWantResults(false);

//            context.unregisterReceiver(this);
        }
    }
}