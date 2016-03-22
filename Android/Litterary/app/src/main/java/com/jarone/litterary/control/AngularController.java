package com.jarone.litterary.control;

import android.widget.EditText;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.LitterApplication;
import com.jarone.litterary.R;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.LocationHelper;
import com.jarone.litterary.imageproc.ImageProcessing;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Adam on 2015-11-30.
 */
public class AngularController {

    ScheduledExecutorService taskScheduler;
    private ScheduledFuture controlsLoopFuture;
    private ArrayList<ScheduledFuture> generateTasks = new ArrayList<>();

    double MAX_ANGLE = 400;
    private long SAMPLING_TIME = 50;

    double P = 14;
    double I = 0;
    double D = 10;

    int loopIterations = 0;
    int descendIterations = 0;
    double pitchErrorSum = 0;
    double rollErrorSum = 0;
    private double lastPitchError = 0;
    private double lastRollError = 0;
    private double lastAction = 0;
    private boolean descend = false;
    private int descendTimer = 0;

    private boolean flip = false;
    boolean canFlip = true;

    private boolean isRetrieving = false;

    private boolean doDescend = false;


    private boolean generatorFlip = false;

    //Determine if the current control action is directing pitch or roll
    public enum ActiveAngle { PITCH, ROLL}

    private ActiveAngle activeAngle = ActiveAngle.PITCH;

    private EditText inputField;

    public AngularController() {

        taskScheduler = LitterApplication.getInstance().getScheduler();
        inputField = (EditText) ContextManager.getMainActivityInstance().findViewById(R.id.data_input);
    }

    /**
     * Initiates the PID control loop using the SAMPLING_TIME
     */
    public void startExecutionLoop() {
        GroundStation.engageJoystick(new Runnable() {
            @Override
            public void run() {
                GroundStation.setAngles(0, 0, 0);
                controlsLoopFuture = taskScheduler.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        controlsLoop();
                    }
                },0, SAMPLING_TIME, TimeUnit.MILLISECONDS);
            }
        });
    }

    public float getInputParameter() {
        return Float.parseFloat(inputField.getText().toString());
    }
    /**
     * The control loop governing the PID control strategy. Loop is executed on a timer with
     * a rate of SAMPLING_TIME
     */
    public void controlsLoop() {

        double pitchError = ImageProcessing.distanceFromTarget(ActiveAngle.PITCH, DroneState.getAltitude());
        double rollError = ImageProcessing.distanceFromTarget(ActiveAngle.ROLL, DroneState.getAltitude());
        //P = getInputParameter();
        double action;
        double error;
        if (activeAngle == ActiveAngle.PITCH) {
            action = PID(pitchError, lastPitchError, pitchErrorSum);
            error = pitchError;
            lastPitchError = pitchError;
            pitchErrorSum += pitchError;
        } else {
            action = PID(rollError, lastRollError, rollErrorSum);
            error = rollError;
            lastRollError = rollError;
            rollErrorSum += rollError;
        }

        if (Math.abs(action) > MAX_ANGLE) {
            action = MAX_ANGLE * Math.signum(action);
        }

        lastAction = action;
        if (descend) {
            GroundStation.setAngles(0, 0, 0, 2);
            descendTimer++;
        } else {
            if (activeAngle == ActiveAngle.PITCH) {
                GroundStation.setAngles(action, 0, 0);
            } else {
                GroundStation.setAngles(0, 0, action);
            }
            loopIterations++;
        }
        //GroundStation.setAngles(0,0,0,0);

        MessageHandler.log(action + " " + error + " " + activeAngle);

        //Switch active controlled angle every second
        //TODO switch more often if error is increasing past some threshold
        if (loopIterations > 500 / SAMPLING_TIME || descendTimer > 200 / SAMPLING_TIME) {
//            if (activeAngle == ActiveAngle.ROLL  && !descend) {
//                if (doDescend && ImageProcessing.distanceFromTarget(ActiveAngle.PITCH, DroneState.getAltitude()) < 0.2 && ImageProcessing.distanceFromTarget(ActiveAngle.PITCH, DroneState.getAltitude()) < 0.2) {
//                    descend = true;
//                    MessageHandler.log("Descending");
//                } else {
//                    activeAngle = ActiveAngle.PITCH;
//                }
//            } else if (activeAngle == ActiveAngle.PITCH) {
//                activeAngle = ActiveAngle.ROLL;
//                MessageHandler.log("Switching angle to " + activeAngle);
//            } else if (descend) {
//                descend = false;
//                activeAngle = ActiveAngle.PITCH;
//                MessageHandler.log("Switching angle to " + activeAngle);
//            }
            if (pitchError < 0.1 && rollError < 0.1 && doDescend) {
                descend = true;
            } else if (Math.abs(pitchError * 1.2) > Math.abs(rollError)) {
                activeAngle = ActiveAngle.PITCH;
            } else if (Math.abs(pitchError * 1.2) <= Math.abs(rollError)) {
                activeAngle = ActiveAngle.ROLL;
            }

            loopIterations = 0;
            descendTimer = 0;
        }

    }

    public void stopExecutionLoop() {
        if (controlsLoopFuture != null) {
            controlsLoopFuture.cancel(true);
        }
    }


    /**
     * Based on the given distance, determine the next action for the drone to undertake and perform it
     * @param distance
     */
    public void performNextAction(double distance, Runnable callback) {
        TableEntry action = ControlTable.findMatchForDistance(distance);
        flyAtAngleForTime(action.angle, action.time,  activeAngle == ActiveAngle.PITCH, callback);
        toggleActiveAngle();
    }

    public void toggleActiveAngle() {
        if (activeAngle == ActiveAngle.PITCH) {
            activeAngle = ActiveAngle.ROLL;
        } else {
            activeAngle = ActiveAngle.PITCH;
        }
    }

    /**
     *  Command the drone to fly at the specified angle for the given amount of time
     * @param angle Angle to fly at
     * @param time Time to fly at given angle
     * @param isPitch True if controlling pitch angle, false if controlling roll
     */
    public void flyAtAngleForTime(final double angle, final double time, final boolean isPitch, final Runnable callback) {
        if (DroneState.getAltitude() < 0.2) {
            //Drone is at a height to pick up the litter, run the callback
            isRetrieving = false;
            callback.run();
            ImageProcessing.stopTrackingObject();
            return;
        }
        GroundStation.engageJoystick(new Runnable() {
            @Override
            public void run() {
                int newAltitude = (int) DroneState.getAltitude() / 2;
                if (isPitch) {
                    GroundStation.setAngles(angle, 0, 0, newAltitude);
                } else {
                    GroundStation.setAngles(0, 0, angle, newAltitude);
                }
                taskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        GroundStation.setAngles(0, 0, 0);
                        GroundStation.taskDoneCallback = new Runnable() {
                            @Override
                            public void run() {
                                if (isRetrieving) {
                                    performNextAction(ImageProcessing.distanceFromTarget(activeAngle, DroneState.getAltitude()), callback);
                                }
                            }
                        };
                    }
                }, (int)time, TimeUnit.MILLISECONDS);
            }
        });
    }

    public void pickupLitter(Runnable callback) {
        //TODO Implement this
        if (!ImageProcessing.isTracking()) {
            ImageProcessing.startTrackingObject();
        }
        isRetrieving = true;
        double dist = ImageProcessing.distanceFromTarget(activeAngle, DroneState.getAltitude());
        performNextAction(dist, callback);
        //determine tracking target
        //calulate error from target
        //execute control step
    }

    public void generateControlTable() {
        GroundStation.engageJoystick(new Runnable() {
            @Override
            public void run() {
                generateEntriesForAngle(200, 0);
            }
        });
    }

    /**
     * Recursively generate table entries containing distances travelled for angles up to 45 degrees
     * and times from 1 second to 10 seconds
     * @param angle
     * @param timeIndex
     */
    private void generateEntriesForAngle(final double angle, final int timeIndex) {
        //If times from 1 to 10 seconds have been generated for this angle, move to the next angle
        if (timeIndex >= ControlTable.POSSIBLE_TIMES.length) {
            generateEntriesForAngle(angle + 100, 0);
            return;
        }
        //If all angles from 5 to 45 have been tested, exit the method because the table is done
        if (angle > 700) {
            ControlTable.displaySaveDialog();
            return;
        }

        //Alternate direction of testing angles so drone doesn't fly in one long direction
        final double testAngle;
        if (generatorFlip) {
            testAngle = -angle;
        } else {
            testAngle = angle;
        }
        generatorFlip = !generatorFlip;

        final double time = ControlTable.POSSIBLE_TIMES[timeIndex];
        MessageHandler.d("Proccessing Entry for " + angle + " " + time);
        final LatLng startLoc = DroneState.getLatLng();
        GroundStation.setAngles(testAngle, 0, 0);

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
                        generateEntriesForAngle(angle, timeIndex + 1);
                    }
                }, 3, TimeUnit.SECONDS));
            }
        }, (int)time, TimeUnit.MILLISECONDS));
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
    private double PID(double error, double lastError, double errorSum) {
        return error * P + errorSum * SAMPLING_TIME * I + (error - lastError)/SAMPLING_TIME * D;
    }


}