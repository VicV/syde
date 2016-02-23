package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;

/**
 * Created by Adam on 2016-01-21.
 */
public class NavigationRoute {

    public LatLng[] getRoute() {
        return route;
    }

    protected LatLng[] route;
    protected int index;
    protected final int SPEED = 5;

    protected boolean executing;
    protected boolean finished;

    protected long startTime;
    protected long endTime;

    protected float altitude;
    protected short heading;


    public NavigationRoute(LatLng[] route, float altitude, short heading) {
        this.route = route;
        this.altitude = altitude;
        this.heading = heading;
        index = 0;
        executing = false;
        finished = false;
    }

    public void executeRoute() {
        if (index == 0) {
            startTime = System.currentTimeMillis();
        }

        executeRouteStep();
    }

    /**
     * Override this to determine what happens when route is executed
     */
    public void executeRouteStep() {
        if (index <= route.length - 1) {
            executing = true;
            MessageHandler.d("Executing Survey Point " + (index + 1));
            GroundStation.newTask();
            GroundStation.addPoint(route[index].latitude, route[index].longitude, SPEED, altitude, heading);
            index++;

            GroundStation.uploadAndExecuteTask();

        } else {
            MessageHandler.d("Survey Route Complete!");
            finished = true;
            executing = false;
            stopRoute();
        }

    }

    public void stopRoute() {
        endTime = System.currentTimeMillis();
        GroundStation.taskDoneCallback = new Runnable() {
            @Override
            public void run() {
            }
        };
        GroundStation.stopTask();
    }

    public boolean isExecuting() {
        return executing;
    }

    public boolean isFinished() {
        return finished;
    }
}
