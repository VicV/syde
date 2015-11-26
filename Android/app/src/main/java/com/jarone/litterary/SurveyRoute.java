package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.handlers.MessageHandler;

/**
 * Created by Adam on 2015-11-16.
 * Class responsible for handling the execution of survey routes
 */
public class SurveyRoute {
    private LatLng[] route;
    private int index;
    private float surveyAltitude;
    private final int SPEED = 5;
    private boolean executing;
    private boolean finished;

    public SurveyRoute(LatLng[] route, float altitude){
        this.route = route;
        this.surveyAltitude = altitude;
        index = 0;
        executing = false;
        finished = false;
    }

    /**
     * Create a one-waypoint task with the next waypoint in the survey route. Register a
     * callback to execute when the waypoint is reached. Take a photo at this point. Register a
     * callback for photo taken success that executes this method again with an incremented waypoint
     * index
     */
    public void executeRoute() {
        if (index <= route.length - 1) {
            executing = true;
            MessageHandler.d("Executing Survey Point " + index);
            GroundStation.newTask();
            GroundStation.addPoint(route[index].latitude, route[index].longitude, SPEED, surveyAltitude);
            index++;

            //set the callbacks to take a photo when the point is reached
            GroundStation.taskDoneCallback = new Runnable() {
                @Override
                public void run() {
                    Camera.photoCallback = new Runnable() {
                        @Override
                        public void run() {
                            //call this function again with the incremented index after photo taken
                            executeRoute();
                        }
                    };
                    Camera.takePhoto();
                }
            };
            GroundStation.uploadAndExecuteTask();

        } else {
            MessageHandler.d("Survey Route Complete!");
            finished = true;
            executing = false;
            stopRoute();
        }

    }

    public void stopRoute() {
        GroundStation.taskDoneCallback = new Runnable() {
            @Override
            public void run() {}
        };
        Camera.photoCallback = new Runnable() {
            @Override
            public void run() {

            }
        };
        GroundStation.stopTask();
    }

    public boolean isExecuting() { return executing; }
    public boolean isFinished() {
        return finished;
    }
}
