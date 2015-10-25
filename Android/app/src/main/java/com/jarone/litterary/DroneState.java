package com.jarone.litterary;

import android.util.Log;

import com.jarone.litterary.promises.Promise;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;

/**
 * Created by Adam on 2015-10-24.
 */
public class DroneState {

    //Persistent knowledge of connection I guess
    boolean isConnected;

    //Reasoning for this being a singleton will be made apparent later.

    private static DroneState instance;

    public static DroneState getInstance() {
        if (instance != null) {
            return instance;
        } else {
            instance = new DroneState();
        }
        return instance;
    }


    public void isConnected(Promise promise) {

        if (isConnected) {
            promise.finish();
        } else {
            // connect and then finish promise in callbacks
        }


    }

    // Update the drone location based on states from MCU.

    private static double latitude = 0;
    private static double longitude = 0;

    private static double speed = 0;
    private static double altitude = 0;

    private static double pitch = 0;
    private static double roll = 0;
    private static double yaw = 0;

    private static double velocityX = 0;
    private static double velocityY = 0;
    private static double velocityZ = 0;


    private static double homeLatitude;
    private static double homeLongitude;

    private static boolean connected = false;
    public static boolean hasTask = false;

    private static final String TAG = DroneState.class.toString();

    private static DJIMcuUpdateStateCallBack mMcuUpdateStateCallBack;

    public void updateDroneLocation() {
        mMcuUpdateStateCallBack = new DJIMcuUpdateStateCallBack() {

            @Override
            public void onResult(DJIMainControllerSystemState state) {
                latitude = state.droneLocationLatitude;
                longitude = state.droneLocationLongitude;
                homeLatitude = state.homeLocationLatitude;
                homeLongitude = state.homeLocationLongitude;

                altitude = state.altitude;
                speed = state.speed;

                velocityX = state.velocityX;
                velocityY = state.velocityY;
                velocityZ = state.velocityZ;
                pitch = state.pitch;
                roll = state.roll;
                yaw = state.yaw;
            }
        };
        Log.e(TAG, "setMcuUpdateState");
        DJIDrone.getDjiMC().setMcuUpdateStateCallBack(mMcuUpdateStateCallBack);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
