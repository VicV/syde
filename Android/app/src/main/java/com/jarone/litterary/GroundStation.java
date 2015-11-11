package com.jarone.litterary;

import android.os.Handler;

import com.jarone.litterary.handlers.MessageHandler;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.interfaces.DJIExecuteResultCallback;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;


/**
 * Created by Adam on 2015-10-24.
 */
public class GroundStation {

    public static final String TAG = GroundStation.class.toString();

    private static DJIGroundStationTask groundTask;

    /**
     * Default altitude of the drone. Will be used when no altitude is provided in navigation.
     **/
    public static float defaultAltitude;

    /**
     * Default speed of the drone. Will be used when no speed is provided in navigation.
     **/
    public static float defaultSpeed;

    public static void newTask() {
        groundTask = new DJIGroundStationTask();
    }

    /**
     * Add a point for the Drone to navigate to. Will use {@link #defaultSpeed} and {@link #defaultAltitude}
     */
    public static void addPoint(double latitude, double longitude) {
        addPoint(latitude, longitude, defaultSpeed, defaultAltitude);
    }

    /**
     * Add a point for the Drone to navigate to, with speed and altitude specifications.
     */
    public static void addPoint(double latitude, double longitude, float speed, float altitude) {
        DJIGroundStationWaypoint point = new DJIGroundStationWaypoint(latitude, longitude);
        point.speed = speed;
        point.altitude = altitude;
        groundTask.addWaypoint(point);
    }

    /**
     * Used to call methods that require a connection to ground station by first calling
     * openGroundStation and executing the callable in case of success
     */
    public static void withConnection(final Runnable run) {
        final Handler handler = new Handler();
        MessageHandler.d("withConnection RUNNING");
        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                if (result == DJIGroundStationTypeDef.GroundStationResult.GS_Result_Success) {
                    DroneState.groundStationConnected = true;
                    try {
                        handler.post(run);
                    } catch (Exception e) {
                        MessageHandler.d("withConnection: " + e.toString());
                    }
                    MessageHandler.d("withConnection: SUCCESS");
                } else {
                    DroneState.groundStationConnected = false;
                    MessageHandler.d("withConnection: FAILURE");
                }
            }
        });
    }

    /**
     * Gives the queued task to the Drone and then executes it.
     */
    public static void uploadAndExecuteTask() {
        DJIDrone.getDjiGroundStation().uploadGroundStationTask(groundTask, new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                if (result == DJIGroundStationTypeDef.GroundStationResult.GS_Result_Success) {
                    executeTask();
                }
                String ResultsString = "upload task =" + result.toString();
                MessageHandler.d(ResultsString);
            }
        });
    }

    /**
     * Execute the task last given to the drone.
     */
    public static void executeTask() {
        //NOTE: ground station must be open before this is called
        DJIDrone.getDjiGroundStation().startGroundStationTask(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                // TODO Auto-generated method stub
                String ResultsString = "execute task =" + result.toString();
                MessageHandler.d(ResultsString);
                //handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
            }
        });
    }

    public static void stopTask() {
        withConnection(new Runnable() {
            @Override
            public void run() {
                DJIDrone.getDjiGroundStation().cancelGroundStationTask(new DJIGroundStationExecuteCallBack() {
                    @Override
                    public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                        MessageHandler.d(groundStationResult.toString());
                    }
                });
            }
        });
    }

    /**
     * Set a new altitude for the drone. Simply calls {@link #addPoint(double, double, float, float)} with old values and the new altitude.
     */
    public static void setAltitude(float altitude) {
        if (!DroneState.hasValidLocation()) {
            MessageHandler.d("Invalid GPS Coordinates");
            return;
        }
        newTask();
        addPoint(DroneState.getLatitude(), DroneState.getLongitude(), 0, altitude);
        uploadAndExecuteTask();
    }

    public static void setHomePoint() {
        if (!DroneState.hasValidLocation()) {
            MessageHandler.d("Invalid GPS Coordinates");
            return;
        }
        withConnection(new Runnable() {
            @Override
            public void run() {
                DJIDrone.getDjiMainController().setAircraftHomeGpsLocation(DroneState.getLatitude(), DroneState.getLongitude(), new DJIExecuteResultCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        MessageHandler.d("Set Home: " + djiError.errorDescription);
                        MessageHandler.d("Home Point Set To: " + String.valueOf(DroneState.getLatitude()) + " " + String.valueOf(DroneState.getLongitude()));
                    }
                });
            }
        });
    }

    public static void goHome() {
        withConnection(new Runnable() {
            @Override
            public void run() {
                DJIDrone.getDjiGroundStation().goHome(new DJIGroundStationExecuteCallBack() {
                    @Override
                    public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                        MessageHandler.d("Go Home: " + groundStationResult.toString());
                    }
                });
            }
        });
    }
    /***
     * Switches from ground station GPS control to direct angular (pitch, yaw, roll) control.
     * Must pause current waypoint task before this can happen.
     * Result should be the drone holding its current position until new commands are issued
     */
    public static void engageJoystick() {
        DJIDrone.getDjiGroundStation().pauseGroundStationTask(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                DJIDrone.getDjiGroundStation().setAircraftJoystick(0, 0, 0, 0, new DJIGroundStationExecuteCallBack() {
                    @Override
                    public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {

                    }
                });
            }
        });
    }

    public static DJIGroundStationTask getTask() {
        return groundTask;
    }


}
