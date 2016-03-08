package com.jarone.litterary.helpers;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Created by V on 3/7/2016.
 */
public class WifiHelper {

    /*
 *  Max priority of networks
 */
    private static final int MAX_PRIORITY = 999999;

    /**
     * Connect to a network by SSID.
     */
    public static boolean enableNetwork(String SSID, WifiManager wifiManager) {
        boolean state = false;
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        //This is what disconnects the current wifi AND forgets it (to stop the drone from losing connection).
        //Commented out now because while it does work, it freezes the damn app for some reason.
//        forgetWifi(wifiManager.getConnectionInfo().getNetworkId(), wifiManager);


        //Walk through every single network connection and give the one we're looking for the top priority
        if (list != null && list.size() > 0) {
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals(convertToQuotedString(SSID))) {
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
        //This is a SUPER backup. Basically CREATES the connection (again, if it already exists) to
        //force a reconnect EVEN IF there is no wifi connection (DJI stuff doesn't technically have
        //one so it wont want to reconnect);
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + SSID + "\"";

        //Need this because there is no password
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

    //Forgets a wifi network, used to remove any previously known wifi networks to stop
    //From stealing connection from drone. Freezes the app though so don't use.
    private static void forgetWifi(int networkId, WifiManager wifiManager) {
        try {
            Method forgetMethod = null;
            for (Method m : wifiManager.getClass().getDeclaredMethods()) {
                if (Objects.equals(m.getName(), "forget")) {
                    forgetMethod = m;
                }
            }
            Class<?> someInterface = wifiManager.getClass().getClasses()[0];
            Object instance = Proxy.newProxyInstance(someInterface.getClassLoader(), new Class<?>[]{someInterface}, new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                    //Handle the invocations
                    if (method.getName().equals("someMethod")) {
                        return 1;
                    } else return -1;
                }
            });

            forgetMethod.invoke(wifiManager, networkId, instance);
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
     * Add quotes to string if not already present. (SSID requires this)
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
