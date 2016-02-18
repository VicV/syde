package com.jarone.litterary.control;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.LitterApplication;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.LocationHelper;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Adam on 2015-11-30.
 */
public class AngularController {

    LatLng startLocation;
    ScheduledExecutorService taskScheduler;
    private ScheduledFuture controlsLoopFuture;
    private ArrayList<ScheduledFuture> generateTasks = new ArrayList<>();

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
        taskScheduler = LitterApplication.getInstance().getScheduler();
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

    public void generateControlTable() {
        GroundStation.engageJoystick(new Runnable() {
            @Override
            public void run() {
                generateEntriesForAngle(0, 1);
            }
        });
    }

    /**
     * Recursively generate table entries containing distances travelled for angles up to 45 degrees
     * and times from 1 second to 10 seconds
     * @param angle
     * @param time
     */
    private void generateEntriesForAngle(final double angle, final double time) {
        //If times from 1 to 10 seconds have been generated for this angle, move to the next angle
        if (time > 10) {
            generateEntriesForAngle(angle + 5, 1);
            return;
        }
        //If all angles from 5 to 45 have been tested, exit the method because the table is done
        if (angle > 45) {
            new AlertDialog.Builder(ContextManager.getContext())
                    .setMessage("Save this Table?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ControlTable.save();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        final LatLng startLoc = DroneState.getLatLng();
        GroundStation.setAngles(angle, 0, 0);
        //Keep the drone at this angle for the given amount of time, then record an entry
        generateTasks.add(taskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                GroundStation.setAngles(0, 0, 0);
                LatLng endLoc = DroneState.getLatLng();
                double distance = LocationHelper.distanceBetween(startLoc, endLoc);
                ControlTable.addEntry(new TableEntry(angle, time, distance));
                //Put a 3-second delay between test runs to give the drone time to settle
                generateTasks.add(taskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        //Generate an entry for the current angle with one more second of time
                        generateEntriesForAngle(angle, time + 1);
                    }
                }, 3, TimeUnit.SECONDS));
            }
        }, (int)time, TimeUnit.SECONDS));
    }

    public void cancelTableGeneration() {
        for (ScheduledFuture future : generateTasks) {
            future.cancel(true);
        }
        GroundStation.setAngles(0, 0, 0);
        ControlTable.clearEntries();
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
