package com.jarone.litterary.drone;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.control.AngularController;
import com.jarone.litterary.RouteOptimization;
import com.jarone.litterary.SurveyRoute;
import com.jarone.litterary.handlers.MessageHandler;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.GroundStation.DJIGroundStationFlyingInfo;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.interfaces.DJIExecuteResultCallback;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIGroundStationFlyingInfoCallBack;


/**
 * Created by Adam on 2015-10-24.
 */
public class GroundStation {

    public static final String TAG = GroundStation.class.toString();

    /**
     * Current task to be executed by the drone
     */
    private static DJIGroundStationTask groundTask;

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
     * Creates a new task, replacing the value of the groundTask variable. Points can be added
     * and then the task uploaded and executed by the drone
     */
    public static void newTask() {
        groundTask = new DJIGroundStationTask();
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
        DJIGroundStationWaypoint point = new DJIGroundStationWaypoint(latitude, longitude);
        point.speed = speed;
        point.altitude = altitude;
        point.heading = heading;
        groundTask.addWaypoint(point);
    }

    /**
     * Used to call methods that require a connection to ground station by first calling
     * openGroundStation and executing the callable in case of success
     */
    public static void withConnection(final Runnable run) {
        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                if (resultSuccess(result)) {
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
        });
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
        DJIDrone.getDjiGroundStation().uploadGroundStationTask(groundTask, new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                if (resultSuccess(result)) {
                    executeTask();
                }
                String ResultsString = "upload task =" + result.toString();
                MessageHandler.d(ResultsString);
                callback.run();
            }
        });
    }

    /**
     * Execute the task last given to the drone.
     */
    public static void executeTask() {
        //NOTE: ground station must be open before this is called
        DJIDrone.getDjiGroundStation().startGroundStationTask(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                if (DroneState.flightMode == DJIGroundStationTypeDef.GroundStationFlightMode.GS_Mode_Atti) {
                    MessageHandler.d("S1 switch is not in autonomous position!");
                }
                if (resultSuccess(result)) {
                    DroneState.setMode(DroneState.WAYPOINT_MODE);
                    DJIGroundStationWaypoint wp = groundTask.getWaypointAtIndex(groundTask.getStartWaypointIndex());
                    currentTarget = new LatLng(wp.latitude, wp.longitude);
                }
                String ResultsString = "execute task =" + result.toString();
                MessageHandler.d(ResultsString);
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
                DJIDrone.getDjiGroundStation().cancelGroundStationTask(new DJIGroundStationExecuteCallBack() {
                    @Override
                    public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                        MessageHandler.d(groundStationResult.toString());
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
                DJIDrone.getDjiMainController().setAircraftHomeGpsLocation(DroneState.getLatitude(), DroneState.getLongitude(), new DJIExecuteResultCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        MessageHandler.d("Set Home: " + djiError.errorDescription);
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
                DJIDrone.getDjiGroundStation().goHome(new DJIGroundStationExecuteCallBack() {
                    @Override
                    public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                        if (resultSuccess(groundStationResult)) {
                            DroneState.setMode(DroneState.WAYPOINT_MODE);
                        }
                        MessageHandler.d("Go Home: " + groundStationResult.toString());
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
                DJIDrone.getDjiGroundStation().pauseGroundStationTask(new DJIGroundStationExecuteCallBack() {
                    @Override
                    public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                        DJIDrone.getDjiGroundStation().setYawControlMode(DJIGroundStationTypeDef.DJINavigationFlightControlYawControlMode.Navigation_Flight_Control_Yaw_Control_Angle);
                        DJIDrone.getDjiGroundStation().setAircraftJoystick(0, 0, 0, 0, new DJIGroundStationExecuteCallBack() {
                            @Override
                            public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                                MessageHandler.d("Engage Joystick: " + groundStationResult.toString());
                                if (resultSuccess(groundStationResult)) {
                                    DroneState.setMode(DroneState.DIRECT_MODE);
                                }
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
                DJIDrone.getDjiGroundStation().setAircraftJoystick((int) yaw, (int) pitch, (int) roll, 0, new DJIGroundStationExecuteCallBack() {
                    @Override
                    public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                        //MessageHandler.d("Engage Joystick: " + groundStationResult.toString());
                        if (resultSuccess(groundStationResult)) {
                            DroneState.setMode(DroneState.DIRECT_MODE);
                        }
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

    public static DJIGroundStationTask getTask() {
        return groundTask;
    }

    private static boolean resultSuccess(DJIGroundStationTypeDef.GroundStationResult result) {
        return result == DJIGroundStationTypeDef.GroundStationResult.GS_Result_Success;
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

    /**
     * Register the callback for the drone mission controller's status updates. Calls the configured
     * callback when the drone has reached the target waypoint. This method is called one time
     * to set up callback path, but taskDoneCallback can be changed elsewhere to modify behaviour
     */
//    public static void registerMissionCallback() {
//        DJIDrone.getDjiGroundStation().setGroundStationMissionPushInfoCallBack(new DJIGroundStationMissionPushInfoCallBack() {
//            @Override
//            public void onResult(DJIGroundStationMissionPushInfo djiGroundStationMissionPushInfo) {
//                //MessageHandler.d("Mission Code" + djiGroundStationMissionPushInfo.currState);
//                if (djiGroundStationMissionPushInfo.currState == MissionStatusCodes.REACHED_WAYPOINT_FINISHED_ACTION.ordinal()) {
//                    GroundStation.taskDoneCallback.run();
//                }
////                DJIGroundStationWaypoint wp = groundTask.getWaypointAtIndex(djiGroundStationMissionPushInfo.targetWayPointIndex + 1);
////                currentTarget = new LatLng(wp.latitude, wp.longitude);
//            }
//        });
//    }
//
//    /*** TEST DIFFERENT CALLBACKS TO FIND THE ONE THAT WORKS */
//    public static void registerStatusCallback() {
//        DJIDrone.getDjiGroundStation().setGroundStationExecutionPushInfoCallBack(new DJIGroundStationExecutionPushInfoCallBack() {
//            @Override
//            public void onResult(DJIGroundStationExecutionPushInfo djiGroundStationExecutionPushInfo) {
//                //MessageHandler.d("Ground Station Update");
//                Log.d("EXECUTE GROUND STATION", djiGroundStationExecutionPushInfo.eventType.name());
//                if (djiGroundStationExecutionPushInfo.eventType == DJIGroundStationTypeDef.GroundStationExecutionPushType.Navi_Mission_Finish) {
//                    GroundStation.taskDoneCallback.run();
//                    currentTarget = new LatLng(-1, -1);
//                }
//            }
//        });
//    }
    public static void registerPhantom2Callback() {
        DJIDrone.getDjiGroundStation().setGroundStationFlyingInfoCallBack(new DJIGroundStationFlyingInfoCallBack() {
            @Override
            public void onResult(DJIGroundStationFlyingInfo djiGroundStationFlyingInfo) {
                if (taskDoneCallback != null && canExecuteCallback && djiGroundStationFlyingInfo.flightMode == DJIGroundStationTypeDef.GroundStationFlightMode.GS_Mode_Pause_1) {
                    //Set can execute callback to false to pause the callback chain of survey
                    //route until drone has started moving again
                    canExecuteCallback = false;
                    GroundStation.taskDoneCallback.run();
                    taskDoneCallback = null;
                } else if (djiGroundStationFlyingInfo.flightMode != DJIGroundStationTypeDef.GroundStationFlightMode.GS_Mode_Pause_1) {
                    //if the drone has started moving, we can unpause the callback chain
                    canExecuteCallback = true;
                }
                DroneState.flightMode = djiGroundStationFlyingInfo.flightMode;
            }
        });
    }

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
