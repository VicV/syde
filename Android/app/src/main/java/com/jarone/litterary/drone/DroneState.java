package com.jarone.litterary.drone;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.SurveyRoute;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.Products.DJIHandHeld;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseProduct;

/**
 * Created by Adam on 2015-10-24.
 * <p>
 * Static class containing information about the current state of the Drone.
 */
public class DroneState {

    public static void setmProduct(DJIBaseProduct mProduct) {
        DroneState.mProduct = mProduct;
    }

    public static DJIBaseProduct mProduct;


    public DroneState() {
        mFlightController = ((DJIAircraft) mProduct).getFlightController();
        mProduct = DJISDKManager.getInstance().getDJIProduct();
    }

    public static boolean isAircraftConnected() {
        return mProduct != null && mProduct instanceof DJIAircraft;
    }

    public static boolean isHandHeldConnected() {
        return mProduct != null && mProduct instanceof DJIHandHeld;
    }

    public static synchronized DJIAircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (DJIAircraft) mProduct;
    }

    public static synchronized DJIHandHeld getHandHeldInstance() {
        if (!isHandHeldConnected()) return null;
        return (DJIHandHeld) mProduct;
    }

    public static DJIFlightController mFlightController;


    public static final int WAYPOINT_MODE = 0;
    public static final int DIRECT_MODE = 1;

    private static double latitude = 43.472;
    private static double longitude = -80.54;

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

    public static DJIFlightControllerDataType.DJIFlightControllerFlightMode flightMode = DJIFlightControllerDataType.DJIFlightControllerFlightMode.AssistedTakeOff;

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

    private static DJIFlightControllerDataType.DJIFlightControllerCurrentState state;
    private static final String TAG = DroneState.class.toString();

    private static DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback mMcuUpdateStateCallBack;

    public static SurveyRoute currentSurveyRoute;

    private static ScheduledExecutorService taskScheduler = Executors.newSingleThreadScheduledExecutor();

    private static ScheduledFuture connectedTimer;

    public static void registerConnectedTimer() {
        connectedTimer = taskScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                droneConnected = false;
                updateDroneState();
                GroundStation.registerPhantom2Callback();
            }
        }, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    /**
     * Update the current state of the drone.
     */
    public static void updateDroneState() {
        mMcuUpdateStateCallBack = new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {

            @Override
            public void onResult(DJIFlightControllerDataType.DJIFlightControllerCurrentState state) {
                latitude = state.getAircraftLocation().getLatitude();
                longitude = state.getAircraftLocation().getLongitude();
                homeLatitude = state.getHomeLocation().getLatitude();
                homeLongitude = state.getHomeLocation().getLongitude();
                altitude = state.getAircraftLocation().getAltitude();
                battery = state.getRemainingBattery().value();
                velocityX = state.getVelocityX();
                velocityY = state.getVelocityY();
                velocityZ = state.getVelocityZ();
                pitch = state.getAttitude().pitch;
                roll = state.getAttitude().roll;
                yaw = state.getAttitude().yaw;
                DroneState.state = state;

                //TODO: REIMPLEMENT THIS
//                Camera.setGimbalPitch(Camera.requestedGimbalAngle);

                droneConnected = true;
                if (connectedTimer != null) {
                    connectedTimer.cancel(true);
                    registerConnectedTimer();
                }
            }
        };

        mFlightController.setUpdateSystemStateCallback(mMcuUpdateStateCallBack);
    }

    public static double getLatitude() {
        return latitude;
    }

    public static double getLongitude() {
        return longitude;
    }

    public static LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

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
