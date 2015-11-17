package com.jarone.litterary;

import com.jarone.litterary.handlers.MessageHandler;

/**
 * Created by Adam on 2015-11-16.
 * Class responsible for handling the execution of survey routes
 */
public class SurveyRoute {
    Location[] route;
    int index;

    public SurveyRoute(Location[] route){
        this.route = route;
        index = 0;
    }

    /**
     * Create a one-waypoint task with the next waypoint in the survey route. Register a
     * callback to execute when the waypoint is reached. Take a photo at this point. Register a
     * callback for photo taken success that executes this method again with an incremented waypoint
     * index
     */
    public void executeRoute() {
        if (index <= route.length - 1) {

            GroundStation.newTask();
            GroundStation.addPoint(route[index].latitude, route[index].longitude);
            index++;

            GroundStation.taskDoneCallback = new Runnable() {
                @Override
                public void run() {
                    Camera.photoCallback = new Runnable() {
                        @Override
                        public void run() {
                            executeRoute();
                        }
                    };
                    Camera.takePhoto();
                }
            };
            GroundStation.uploadAndExecuteTask();

        } else {
            MessageHandler.d("Survey Route Complete!");
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
}
