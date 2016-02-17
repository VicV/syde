package com.jarone.litterary.control;

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

    float MAX_ANGLE = 500;
    private long SAMPLING_TIME = 200;
    double CONVERGENCE_THRESHOLD = 0.1;

    float P = 50;
    float I = 0;
    float D = 10;
    float errorSum = 0;
    private float lastError = 0;
    private float lastAction = 0;

    private boolean flip = false;
    boolean canFlip = true;



    public AngularController() {
        taskScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Initiates the PID control loop using the SAMPLING_TIME
     */
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

    /**
     * The control loop governing the PID control strategy. Loop is executed on a timer with
     * a rate of SAMPLING_TIME
     */
    public void controlsLoop() {
        if (!DroneState.hasValidLocation()) {
            executeAction(0);
            return;
        }
        float distance = LocationHelper.distanceBetween(startLocation, DroneState.getLatLng());
        float error;

        //Set error based on status of flip variable to trigger movement towards or away from start
        if (flip) {
            error = 10 - distance;
        } else {
            error = -distance;
        }

        float action = PID(error);

        if (Math.abs(action) > MAX_ANGLE) {
            action = MAX_ANGLE * Math.signum(action);
        }

        if (Math.abs(error) < CONVERGENCE_THRESHOLD && canFlip) {
            flip = !flip;
            canFlip = false;
            lastError = 0;
            errorSum = 0;
        } else if (Math.abs(error) > CONVERGENCE_THRESHOLD) {
            canFlip = true;
        }

        lastError = error;
        errorSum += error;
        executeAction(action);
    }

    /**
     * PID Control Equation, using constant parameters defined in class
     * @param error The error term used for control
     * @return The calculated action based on PID control math
     */
    private float PID(float error) {
        return error * P + errorSum * SAMPLING_TIME * I + (error - lastError)/SAMPLING_TIME * D;
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
