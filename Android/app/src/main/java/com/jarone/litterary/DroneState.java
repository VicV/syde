package com.jarone.litterary;

import android.util.Log;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;

/**
 * Created by Adam on 2015-10-24.
 */
public class DroneState {
    // Update the drone location based on states from MCU.

    private double latitude = 0;
    private double longitude = 0;

    private double homeLatitude;
    private double homeLongitude;

    private boolean connected = false;
    public boolean hasTask = false;

    private static final String TAG = DroneState.class.toString();

    private DJIMcuUpdateStateCallBack mMcuUpdateStateCallBack;

    public void updateDroneLocation(){
        mMcuUpdateStateCallBack = new DJIMcuUpdateStateCallBack(){

            @Override
            public void onResult(DJIMainControllerSystemState state) {
                latitude = state.droneLocationLatitude;
                longitude = state.droneLocationLongitude;
                homeLatitude = state.homeLocationLatitude;
                homeLongitude = state.homeLocationLongitude;
            }
        };
        Log.e(TAG,"setMcuUpdateState");
        DJIDrone.getDjiMC().setMcuUpdateStateCallBack(mMcuUpdateStateCallBack);
    }

    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
}
