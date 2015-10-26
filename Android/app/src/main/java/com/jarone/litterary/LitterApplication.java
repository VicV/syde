package com.jarone.litterary;

import android.app.Application;
import android.util.Log;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef;
import dji.sdk.api.DJIError;
import dji.sdk.interfaces.DJIGeneralListener;

/**
 * Created by vic on 10/26/15.
 */
public class LitterApplication extends Application {


    /**
     * The log tag
     **/
    private static final String TAG = "Litterary";

    public static DroneState droneState = new DroneState();


    @Override
    public void onCreate() {
        super.onCreate();

        activateDJI();

        initSDK();
        DroneState.droneConnected = DJIDrone.connectToDrone();
    }


    private void initSDK() {
        DJIDrone.initWithType(this.getApplicationContext(), DJIDroneTypeDef.DJIDroneType.DJIDrone_Vision);
    }


    private void activateDJI() {
        new Thread() {
            public void run() {
                try {
                    DJIDrone.checkPermission(getApplicationContext(), new DJIGeneralListener() {
                        @Override
                        public void onGetPermissionResult(int result) {
                            if (result == 0) {
                                // show success
                                Log.e(TAG, "onGetPermissionResult =" + result);
                                Log.e(TAG,
                                        "onGetPermissionResultDescription=" + DJIError.getCheckPermissionErrorDescription(result));
                                droneState.updateDroneState();
                            } else {
                                // show errors
                                Log.e(TAG, "onGetPermissionResult =" + result);
                                Log.e(TAG,
                                        "onGetPermissionResultDescription=" + DJIError.getCheckPermissionErrorDescription(result));
                            }
                        }
                    });
                } catch (Exception e) {
                    //Something broke
                }
            }
        }.start();
    }

    public static DroneState getDroneState() {
        return droneState;
    }
}
