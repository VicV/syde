package com.jarone.litterary.helpers;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.text.TextUtils;

import com.jarone.litterary.handlers.MessageHandler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by V on 3/7/2016.
 */
public class WifiHelper {

    /*
 *  Max priority of network to be associated.
 */
    private static final int MAX_PRIORITY = 999999;

    /**
     * Allow a previously configured network to be associated with.
     */
    public static boolean enableNetwork(String SSID, WifiManager wifiManager) {
        boolean state = false;
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();


        if (list != null && list.size() > 0) {
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals(convertToQuotedString(SSID))) {
                    forgetWifi(i, wifiManager);
                    wifiManager.disconnect();
                    int newPri = getMaxPriority(wifiManager) + 1;
                    if (newPri >= MAX_PRIORITY) {
                        // We have reached a rare situation.
                        newPri = shiftPriorityAndSave(wifiManager);
                    }

                    i.priority = newPri;
                    wifiManager.updateNetwork(i);
                    wifiManager.saveConfiguration();

                    state = wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    break;
                }
            }
        }
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + SSID + "\"";
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifiManager.addNetwork(conf);
        List<WifiConfiguration> more = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : more) {
            if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }

        return state;
    }

    private static void forgetWifi(WifiConfiguration i, WifiManager wifiManager) {
        try {

            wifiManager.getClass().getDeclaredMethod("forget").invoke(wifiManager, i.networkId, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    MessageHandler.d("Successfully re-forgot");
                }

                @Override
                public void onFailure(int reason) {
                    MessageHandler.d("Fail to forget");
                }
            });
        } catch (Exception e) {

        }

    }

    private static int getMaxPriority(WifiManager wifiManager) {
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int pri = 0;
        for (final WifiConfiguration config : configurations) {
            if (config.priority > pri) {
                pri = config.priority;
            }
        }
        return pri;
    }

    private static void sortByPriority(final List<WifiConfiguration> configurations) {
        Collections.sort(configurations,
                new Comparator<WifiConfiguration>() {
                    @Override
                    public int compare(WifiConfiguration object1, WifiConfiguration object2) {
                        return object1.priority - object2.priority;
                    }
                });
    }

    private static int shiftPriorityAndSave(WifiManager wifiManager) {
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        sortByPriority(configurations);
        final int size = configurations.size();
        for (int i = 0; i < size; i++) {
            final WifiConfiguration config = configurations.get(i);
            config.priority = i;
            wifiManager.updateNetwork(config);
        }
        wifiManager.saveConfiguration();
        return size;
    }

    /**
     * Add quotes to string if not already present.
     *
     * @param string
     * @return
     */
    public static String convertToQuotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }

        final int lastPos = string.length() - 1;
        if (lastPos > 0
                && (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
            return string;
        }

        return "\"" + string + "\"";
    }
}
