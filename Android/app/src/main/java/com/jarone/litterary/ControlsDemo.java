package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.helpers.LocationHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Adam on 2015-11-30.
 */
public class ControlsDemo {

    LatLng startLocation;
    private ScheduledExecutorService taskScheduler;

    private float MAX_ANGLE = 500;
    private long SAMPLING_TIME = 10;

    private float P = 2;
    private float I = 5;
    private float D = 2;
    private float errorSum = 0;
    private float lastError = 0;

    public ControlsDemo() {
        taskScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startDemo() {
        startLocation = DroneState.getLatLng();
        GroundStation.setAngles(500, 0, 0);
        taskScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                controlsLoop();
            }
        },0, SAMPLING_TIME, TimeUnit.MILLISECONDS);
    }

    public void controlsLoop() {
        float distance = LocationHelper.distanceBetween(startLocation, DroneState.getLatLng());
        float error = 10 - distance;
        float action = error * P + errorSum * SAMPLING_TIME * I + (error - lastError)/SAMPLING_TIME * D;
        lastError = error;
        GroundStation.setAngles(action, 0, 0);
    }

}
