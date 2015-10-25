package com.jarone.litterary;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;

/**
 * Created by Adam on 2015-10-24.
 */
public class GroundStation {

    private DJIGroundStationTask groundTask;

    public float defaultAltitude;
    public float defaultSpeed;

    public void newTask() {
        groundTask = new DJIGroundStationTask();
    }

    public void addPoint(double latitude, double longitude) {
        addPoint(latitude, longitude, defaultSpeed, defaultAltitude);
    }

    public void addPoint(double latitude, double longitude, float speed, float altitude) {
        DJIGroundStationWaypoint point = new DJIGroundStationWaypoint(latitude, longitude);
        point.speed = speed;
        point.altitude = altitude;
        groundTask.addWaypoint(point);
    }

    public void uploadTask() {
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
                            String ResultsString = "return code =" + result.toString();
                            //handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
                        }

                    });
                }
            }
        });
    }

    public void executeTask() {
        DJIDrone.getDjiGroundStation().startGroundStationTask(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                // TODO Auto-generated method stub
                String ResultsString = "return code =" + result.toString();
                //handler.sendMessage(handler.obtainMessage(SHOWTOAST, ResultsString));
            }
        });
    }

    public DJIGroundStationTask getTask() {
        return groundTask;
    }


}
