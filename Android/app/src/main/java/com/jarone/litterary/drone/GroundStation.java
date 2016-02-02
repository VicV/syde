package com.jarone.litterary.drone;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.AngularController;
import com.jarone.litterary.RouteOptimization;
import com.jarone.litterary.SurveyRoute;
import com.jarone.litterary.handlers.MessageHandler;

import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIError;


/**
 * Created by Adam on 2015-10-24.
 */
public class GroundStation {

    public static final String TAG = GroundStation.class.toString();

    /**
     * Current task to be executed by the drone
     */
    private static DJIWaypointMission djiMission;


    /**
     * Default altitude of the drone. Will be used when no altitude is provided in navigation.
     **/
    public static float defaultAltitude;

    /**
     * Default heading used for GPS waypoint nav points
     */
    public static short defaultHeading;

    /**
     * Default speed of the drone. Will be used when no speed is provided in navigation.
     **/
    public static float defaultSpeed;

    /**
     * Radius of circular boundary around drone location where ground station is allowed to operate
     * Measured in metres
     */
    public static final int BOUNDARY_RADIUS = 200;

    /**
     * Tracks the current survey route that has been initialized and/or is executing
     */
    private static SurveyRoute currentSurveyRoute;

    /**
     * Current GPS waypoint target drone is navigating towards
     */
    private static LatLng currentTarget;

    private static AngularController angularController;

    /**
     * Enum of drone status codes sent by mission controller
     */
    public enum MissionStatusCodes {
        INITIALIZING, MOVING, ROTATING, EXECUTING_ACTION, REACHED_WAYPOINT_PENDING_ACTION,
        REACHED_WAYPOINT_FINISHED_ACTION
    }

    /**
     * Callback to fire when drone reaches its target waypoint (ie. task completes)
     */
    public static Runnable taskDoneCallback = null;

    /**
     * Keeps track of whether the drone has begun moving again, thus indicating that a repeat
     * occurrence of GS_Pause_1 flight mode means it has reached its target
     */
    private static boolean canExecuteCallback = false;

    /**
     * Creates a new task, replacing the value of the djiMission variable. Points can be added
     * and then the task uploaded and executed by the drone
     */
    public static void newTask() {
        djiMission = new DJIWaypointMission();
    }

    /**
     * Add a point for the Drone to navigate to. Will use {@link #defaultSpeed} and {@link #defaultAltitude}
     */
    public static void addPoint(double latitude, double longitude) {
        addPoint(latitude, longitude, defaultSpeed, defaultAltitude);
    }

    /**
     * Add a point for the Drone to navigate to, with speed and altitude specifications.
     */
    public static void addPoint(double latitude, double longitude, float speed, float altitude) {
        addPoint(latitude, longitude, speed, altitude, defaultHeading);
    }

    public static void addPoint(double latitude, double longitude, float speed, float altitude, short heading) {
        DJIWaypoint point = new DJIWaypoint(latitude, longitude, altitude);
        point.heading = heading;
        djiMission.autoFlightSpeed = speed;
        djiMission.addWaypoint(point);
    }

    /**
     * Used to call methods that require a connection to ground station by first calling
     * openGroundStation and executing the callable in case of success
     */
    public static void withConnection(final Runnable run) {

        if (DroneState.mProduct.isConnected()) {
            DroneState.groundStationConnected = true;
            try {
                run.run();
                //handler.post(run);
            } catch (Exception e) {
                MessageHandler.d("withConnection: " + e.toString());
            }
            // MessageHandler.d("withConnection: SUCCESS");
        } else {
            DroneState.groundStationConnected = false;
            MessageHandler.d("withConnection: FAILURE");
        }
    }


    /**
     * Gives the queued task to the Drone and then executes it.
     */
    public static void uploadAndExecuteTask() {
        uploadAndExecuteTask(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    /**
     * Upload the current task, then execute it. Execute a callback when upload completes
     *
     * @param callback
     */
    public static void uploadAndExecuteTask(final Runnable callback) {

        DroneState.mProduct.getMissionManager().prepareMission(djiMission, new DJIMission.DJIMissionProgressHandler() {

            @Override
            public void onProgress(DJIMission.DJIProgressType type, float progress) {
                //
            }

        }, new DJIBaseComponent.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {

                executeTask();
                MessageHandler.d("Prepare: " + error.getDescription());

                callback.run();
            }
        });


        if (djiMission != null) {
            DroneState.mProduct.getMissionManager().setMissionExecutionFinishedCallback(new DJIBaseComponent.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    MessageHandler.d("Mission executing result: " + (error == null ? "Success" : error.getDescription()));
                }
            });

        }
    }

    /**
     * Execute the task last given to the drone.
     */
    public static void executeTask() {


        DroneState.mProduct.getMissionManager().startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {

            @Override
            public void onResult(DJIError mError) {
                if (DroneState.flightMode == DJIFlightControllerDataType.DJIFlightControllerFlightMode.Atti) {
                    MessageHandler.d("S1 switch is not in autonomous position!");
                }
                if (mError == null) {
                    DroneState.setMode(DroneState.WAYPOINT_MODE);
                    DJIWaypoint wp = djiMission.getWaypointAtIndex(0);
                    currentTarget = new LatLng(wp.latitude, wp.longitude);
                    MessageHandler.d("Mission Executed");

                } else {
                    MessageHandler.d("Start mission error: " + mError.getDescription());
                }
            }
        });
    }

    /**
     * Cancels the currently executing ground station task
     */
    public static void stopTask() {
        withConnection(new Runnable() {
            @Override
            public void run() {
                DroneState.mProduct.getMissionManager().stopMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError mError) {
                        if (mError == null) {
                            MessageHandler.d("Stopped Execution: ");
                        } else {
                            MessageHandler.d("Stop Error: " + mError.getDescription());
                        }
                    }
                });
            }
        });
    }

    /**
     * Set a new altitude for the drone. Simply calls {@link #addPoint(double, double, float, float)} with old values and the new altitude.
     */
    public static void setAltitude(float altitude) {
        DroneState.updateDroneState();
        if (!DroneState.hasValidLocation()) {
            MessageHandler.d("Invalid GPS Coordinates");
            return;
        }
        if (altitude > 100) {
            MessageHandler.d("Cannot exceed 100 m!");
            return;
        }
        newTask();
        addPoint(DroneState.getLatitude(), DroneState.getLongitude(), 0, altitude);
        uploadAndExecuteTask();
    }

    /**
     * Set the drone's home point which it will return to in emergencies or when commanded
     */
    public static void setHomePoint() {
        if (!DroneState.hasValidLocation()) {
            MessageHandler.d("Invalid GPS Coordinates");
            return;
        }
        withConnection(new Runnable() {
            @Override
            public void run() {
                DroneState.mFlightController.setHomeLocation(new DJIFlightControllerDataType.DJILocationCoordinate2D(DroneState.getLatitude(), DroneState.getLongitude()), new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        MessageHandler.d("Set Home: " + djiError.getDescription());
                        MessageHandler.d("Home Point Set To: " + String.valueOf(DroneState.getLatitude()) + " " + String.valueOf(DroneState.getLongitude()));
                    }
                });
            }
        });
    }

    /**
     * Command the drone to return to its registered home point
     */
    public static void goHome() {
        withConnection(new Runnable() {
            @Override
            public void run() {
                DroneState.mFlightController.goHome(new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DroneState.setMode(DroneState.WAYPOINT_MODE);
                        MessageHandler.d("Go Home: " + "success");
                    }
                });
            }
        });
    }

    /**
     * Switches from ground station GPS control to direct angular (pitch, yaw, roll) control.
     * Must pause current waypoint task before this can happen.
     * Result should be the drone holding its current position until new commands are issued
     */
    public static void engageJoystick() {
        withConnection(new Runnable() {
            @Override
            public void run() {
                DroneState.mProduct.getMissionManager().pauseMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError groundStationResult) {

                        DroneState.mFlightController.setYawControlMode(DJIFlightControllerDataType.DJIVirtualStickYawControlMode.Angle);
                        DroneState.mFlightController.enableVirtualStickControlMode(new DJIBaseComponent.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                DroneState.mFlightController.sendVirtualStickFlightControlData(new DJIFlightControllerDataType.DJIVirtualStickFlightControlData(0, 0, 0, 0), new DJIBaseComponent.DJICompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        MessageHandler.d("Engage Joystick: " + djiError.getDescription());
                                        DroneState.setMode(DroneState.DIRECT_MODE);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

    }

    /**
     * Send a dummy route that commands the drone to its current location in order to switch back
     * to ground station GPS nav mode
     */
    public static void engageGroundStation() {
        withConnection(new Runnable() {
            @Override
            public void run() {
                newTask();
                addPoint(DroneState.getLatitude(), DroneState.getLongitude());
                uploadAndExecuteTask();
            }
        });
    }

    /**
     * Set direct control mode angles for drone
     *
     * @param pitch
     * @param yaw
     * @param roll
     */
    public static void setAngles(final double pitch, final double yaw, final double roll) {
        if (DroneState.getMode() != DroneState.DIRECT_MODE) {
            // MessageHandler.d("Not in Direct Mode!");
            return;
        }
        withConnection(new Runnable() {
            @Override
            public void run() {


                DroneState.mFlightController.enableVirtualStickControlMode(new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DroneState.mFlightController.sendVirtualStickFlightControlData(new DJIFlightControllerDataType.DJIVirtualStickFlightControlData((int) yaw, (int) pitch, (int) roll, 0), new DJIBaseComponent.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                MessageHandler.d("Send Joystick Data: " + djiError.getDescription());
                                DroneState.setMode(DroneState.DIRECT_MODE);
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Generate the optimized survey route based on the boundary points and set it to the current
     * survey route. Does not execute the route
     *
     * @param points
     * @param altitude
     */
    public static LatLng[] initializeSurveyRoute(LatLng[] points, float altitude) {
        if (altitude < 0 || altitude > 100) {
            MessageHandler.d("Please Choose a Valid Altitude");
            return null;
        } else if (points.length < 2) {
            MessageHandler.d("Please supply valid polygon");
            return null;
        }
        GroundStation.currentSurveyRoute = new SurveyRoute(
                RouteOptimization.createOptimizedSurveyRoute(points, altitude),
                altitude,
                defaultHeading
        );
        return GroundStation.currentSurveyRoute.getRoute();
    }

    /**
     * Start the previously initialized survey route, or send a message if no route has been
     * initialized. Also, check if route is already executing so we don't start again
     */
    public static LatLng[] startSurveyRoute() {
        if (currentSurveyRoute != null && !currentSurveyRoute.isFinished() && !currentSurveyRoute.isExecuting()) {
            currentSurveyRoute.executeRoute();
        } else {
            if (currentSurveyRoute != null && currentSurveyRoute.isExecuting()) {
                MessageHandler.d("Route is already executing!");
            } else {
                MessageHandler.d("No Survey Route is Ready!");
            }
        }
        return currentSurveyRoute.getRoute();
    }

    public static SurveyRoute getCurrentSurveyRoute() {
        return currentSurveyRoute;
    }

    public static DJIMission getTask() {
        return djiMission;
    }

    public static void executeController() {
        angularController = new AngularController();
        angularController.startExecutionLoop();
    }

    public static AngularController getAngularController() {
        return angularController;
    }

    public static boolean executingController() {
        return (angularController != null);
    }

    public static void stopController() {
        if (angularController != null) {
            angularController.stopExecutionLoop();
            angularController = null;
        }
    }

//    public static void registerPhantom2Callback() {
//        DroneState.
//                DJIDrone.getDjiGroundStation().setGroundStationFlyingInfoCallBack(new DJIGroundStationFlyingInfoCallBack() {
//            @Override
//            public void onResult(DJIGroundStationFlyingInfo djiGroundStationFlyingInfo) {
//                if (taskDoneCallback != null && canExecuteCallback && djiGroundStationFlyingInfo.flightMode == DJIGroundStationTypeDef.GroundStationFlightMode.GS_Mode_Pause_1) {
//                    //Set can execute callback to false to pause the callback chain of survey
//                    //route until drone has started moving again
//                    canExecuteCallback = false;
//                    GroundStation.taskDoneCallback.run();
//                    taskDoneCallback = null;
//                } else if (djiGroundStationFlyingInfo.flightMode != DJIGroundStationTypeDef.GroundStationFlightMode.GS_Mode_Pause_1) {
//                    //if the drone has started moving, we can unpause the callback chain
//                    canExecuteCallback = true;
//                }
//                DroneState.flightMode = djiGroundStationFlyingInfo.flightMode;
//            }
//        });
//    }

    /***
     * END TEST
     */

    public static LatLng getCurrentTarget() {
        if (currentTarget != null) {
            return currentTarget;
        } else {
            return new LatLng(-1, -1);
        }
    }
}
