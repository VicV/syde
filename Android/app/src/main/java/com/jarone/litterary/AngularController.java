package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.helpers.LocationHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Adam on 2015-11-30.
 */
public class AngularController {

    LatLng startLocation;
    private ScheduledExecutorService taskScheduler;
    private ScheduledFuture controlsLoopFuture;

    private float MAX_ANGLE = 500;
    private long SAMPLING_TIME = 500;

    private float P = 50;
    private float I = 0;
    private float D = 10;
    private float errorSum = 0;
    private float lastError = 0;
    private float lastAction = 0;

    private boolean flip = false;
    boolean canFlip = true;

    public AngularController() {
        taskScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startExecutionLoop() {
        startLocation = DroneState.getLatLng();
        GroundStation.setAngles(0, 0, 0);
        controlsLoopFuture = taskScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                controlsLoop();
            }
        },0, SAMPLING_TIME, TimeUnit.MILLISECONDS);
    }

    public void controlsLoop() {
        if (!DroneState.hasValidLocation()) {
            executeAction(0);
            return;
        }
        float distance = LocationHelper.distanceBetween(startLocation, DroneState.getLatLng());
        float error;
        if (flip) {
            error = 10 - distance;
        } else {
            error = -distance;
            lastError = 0;
        }
        float action = error * P + errorSum * SAMPLING_TIME * I + (error - lastError)/SAMPLING_TIME * D;
        if (Math.abs(action) > MAX_ANGLE) {
            action = MAX_ANGLE * Math.signum(action);
        }
        if (Math.abs(error) < 1 && canFlip) {
            flip = !flip;
            canFlip = false;
        } else if (Math.abs(error) > 1) {
            canFlip = true;
        }
        lastError = error;
        executeAction(action);
    }

    private void executeAction(float action) {
        lastAction = action;
        GroundStation.setAngles(action, 0, 0);
    }

    public void stopExecutionLoop() {
        controlsLoopFuture.cancel(true);
    }

    public float getLastAction() {
        return lastAction;
    }

    public float getLastError() {
        return lastError;
    }

}
