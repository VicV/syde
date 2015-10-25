package com.jarone.litterary;

import android.util.Log;

import com.jarone.litterary.promises.Promise;
import com.jarone.litterary.promises.PromiseListener;

import java.util.concurrent.Callable;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;

/**
 * Created by Adam on 2015-10-24.
 */
public class GroundStation {

    public static final String TAG = GroundStation.class.toString();

    private static DJIGroundStationTask groundTask;

    public static float defaultAltitude;
    public static float defaultSpeed;

    public static void newTask() {
        groundTask = new DJIGroundStationTask();
    }

    public static void addPoint(double latitude, double longitude) {
        addPoint(latitude, longitude, defaultSpeed, defaultAltitude);
    }

    public static void addPoint(double latitude, double longitude, float speed, float altitude) {
        DJIGroundStationWaypoint point = new DJIGroundStationWaypoint(latitude, longitude);
        point.speed = speed;
        point.altitude = altitude;
        groundTask.addWaypoint(point);
    }

    public static void withConnection(final Callable<Void> onSuccess) {
        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {

                if (result == DJIGroundStationTypeDef.GroundStationResult.GS_Result_Success) {
                    try {
                        onSuccess.call();
                    } catch(Exception e){
                        Log.e(TAG, e.toString());
                    }
                }
            }
        });
    }
    public static void uploadAndExecuteTask() {
        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                // TODO Auto-generated method stub
                String ResultsString = "return code =" + result.toString();
                //handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));

                if (result == DJIGroundStationTypeDef.GroundStationResult.GS_Result_Success) {
                    DJIDrone.getDjiGroundStation().uploadGroundStationTask(groundTask, new DJIGroundStationExecuteCallBack() {

                        @Override
                        public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                            // TODO Auto-generated method stub
                            if (result == DJIGroundStationTypeDef.GroundStationResult.GS_Result_Success) {
                                executeTask();
                            }
                            String ResultsString = "return code =" + result.toString();
                            //handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
                        }

                    });
                }
            }
        });
    }

    public static void executeTask() {
        //NOTE: ground station must be open before this is called
        DJIDrone.getDjiGroundStation().startGroundStationTask(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                // TODO Auto-generated method stub
                String ResultsString = "return code =" + result.toString();
                //handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
            }
        });
    }

    public static void setAltitude(float altitude) {
        newTask();
        addPoint(DroneState.getLatitude(), DroneState.getLongitude(), 0, altitude);
        uploadAndExecuteTask();
    }

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

        //SOME EXAMPLE:
        Promise someConnection = new Promise();
        someConnection.add(new PromiseListener() {
            @Override
            public void succeeded() {
                super.succeeded();
                //DO STUFF
            }
        });
        DroneState.getInstance().isConnected(someConnection);
    }

    public static DJIGroundStationTask getTask() {
        return groundTask;
    }


}
