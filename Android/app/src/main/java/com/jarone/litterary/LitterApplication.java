package com.jarone.litterary;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.jarone.litterary.drone.DroneState;

import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJISDKError;

/**
 * Created by vic on 10/26/15.
 */
public class LitterApplication extends Application {

    public static final String FLAG_CONNECTION_CHANGE = "connection_change";

    private Handler mHandler;

    private static LitterApplication instance;

    public LitterApplication() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    /**
     * The log tag
     **/
    private static final String TAG = "Litterary";

    public static DroneState droneState;


    @Override
    public void onCreate() {
        super.onCreate();
//        activateDJI();
//        initSDK();
//        DroneState.droneConnected = DJIDrone.connectToDrone();
        mHandler = new Handler(Looper.getMainLooper());

        /**
         * handles SDK Registration using the API_KEY
         */
        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);


    }

    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {

        @Override
        public void onGetRegisteredResult(DJISDKError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Registration failed",
                                Toast.LENGTH_LONG).show();
                    }
                });

            }
            Log.v(TAG, error.getDescription());
        }

        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {

            Log.v(TAG, String.format("onProductChanged oldProduct:%s, newProduct:%s", oldProduct, newProduct));
            droneState = new DroneState();
            DroneState.setmProduct(newProduct);
            if (DroneState.mProduct != null) {
                DroneState.mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();
        }

        private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {

            @Override
            public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {

                if (newComponent != null) {
                    newComponent.setDJIComponentListener(mDJIComponentListener);
                }
                Log.v(TAG, String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s", key, oldComponent, newComponent));

                notifyStatusChange();
            }

            @Override
            public void onProductConnectivityChanged(boolean isConnected) {

                Log.v(TAG, "onProductConnectivityChanged: " + isConnected);

                notifyStatusChange();
            }

        };

        private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {

            @Override
            public void onComponentConnectivityChanged(boolean isConnected) {

                Log.d("Litterary", "COMPONENT CONNECTIVITY CHANGED" + isConnected);
                notifyStatusChange();
            }

        };

        private void notifyStatusChange() {
            mHandler.removeCallbacks(updateRunnable);
            mHandler.postDelayed(updateRunnable, 500);
        }

        private Runnable updateRunnable = new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
                sendBroadcast(intent);
            }
        };
    };


    public static DroneState getDroneState() {
        return droneState;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

}
