package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;

/**
 * Created by Adam on 2015-10-24.
 * <p/>
 * Static class containing information about the current state of the Drone.
 */
public class DroneState {

    public static final int WAYPOINT_MODE = 0;
    public static final int DIRECT_MODE = 1;

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

    private static double battery = 0;
    private static int mode;

    /**
     * Latitude of the home station
     */
    private static double homeLatitude;

    /**
     * Longitude of the home station
     */
    private static double homeLongitude;

    public static boolean droneConnected = false;
    public static boolean groundStationConnected = false;
    public static boolean hasTask = false;

    private static DJIMainControllerSystemState state;
    private static final String TAG = DroneState.class.toString();

    private static DJIMcuUpdateStateCallBack mMcuUpdateStateCallBack;

    public static SurveyRoute currentSurveyRoute;

    /**
     * Update the current state of the drone.
     */
    public static void updateDroneState() {
        mMcuUpdateStateCallBack = new DJIMcuUpdateStateCallBack() {

            @Override
            public void onResult(DJIMainControllerSystemState state) {
                latitude = state.droneLocationLatitude;
                longitude = state.droneLocationLongitude;
                homeLatitude = state.homeLocationLatitude;
                homeLongitude = state.homeLocationLongitude;
                altitude = state.altitude;
                speed = state.speed;
                battery = state.powerLevel;
                velocityX = state.velocityX;
                velocityY = state.velocityY;
                velocityZ = state.velocityZ;
                pitch = state.pitch;
                roll = state.roll;
                yaw = state.yaw;
                DroneState.state = state;
            }
        };
        DJIDrone.getDjiMC().setMcuUpdateStateCallBack(mMcuUpdateStateCallBack);
    }

    public static double getLatitude() {
        return latitude;
    }

    public static double getLongitude() {
        return longitude;
    }

    public static LatLng getLatLng() {return new LatLng(latitude, longitude);}

    public static boolean hasValidLocation() {
        return (getLongitude() != 0.0 && getLatitude() != 0.0);
    }

    public static void setMode(int newMode) {
        mode = newMode;
    }

    public static int getMode() {
        return mode;
    }

    public static double getPitch() {
        return pitch;
    }

    public static double getYaw() {
        return yaw;
    }

    public static double getRoll() {
        return roll;
    }
}
