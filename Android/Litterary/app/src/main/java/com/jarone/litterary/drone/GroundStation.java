package com.jarone.litterary.drone;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.SurveyRoute;
import com.jarone.litterary.control.AngularController;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.optimization.RouteOptimization;

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
    public static void openGroundStation(final Runnable run) {
        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                if (resultSuccess(result)) {
                    DroneState.groundStationConnected = true;
                    try {
                        run.run();
                        //handler.post(run);
                    } catch (Exception e) {
                        MessageHandler.log("Open Ground Station: " + e.toString());
                    }
                    MessageHandler.d("Open Ground Station: SUCCESS");
                } else {
                    DroneState.groundStationConnected = false;
                    MessageHandler.log("Open Ground Station: FAILURE");
                }
            }
        });
    }

    public static void closeGroundStation(final Runnable run) {
        DJIDrone.getDjiGroundStation().closeGroundStation(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                run.run();
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
        DJIDrone.getDjiGroundStation().cancelGroundStationTask(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                MessageHandler.d(groundStationResult.toString());
            }
        });

    }

    /**
     * Set a new altitude for the drone. Simply calls {@link #addPoint(double, double, float, float)} with old values and the new altitude.
     */
    public static void setAltitude(float altitude) {
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
        double latitude = DroneState.getLatitude();
        double longitude = DroneState.getLongitude();
        if (!DroneState.hasValidLocation()) {
            MessageHandler.d("Invalid GPS Coordinates");
            latitude = 43.4726657;
            longitude = -80.5403147;
        }


        DJIDrone.getDjiMainController().setAircraftHomeGpsLocation(latitude, longitude, new DJIExecuteResultCallback() {
            @Override
            public void onResult(DJIError djiError) {
                MessageHandler.d("Set Home: " + djiError.errorDescription);
                MessageHandler.d("Home Point Set To: " + String.valueOf(DroneState.getLatitude()) + " " + String.valueOf(DroneState.getLongitude()));
            }
        });

    }

    /**
     * Command the drone to return to its registered home point
     */
    public static void goHome() {

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

    /**
     * Switches from ground station GPS control to direct angular (pitch, yaw, roll) control.
     * Must pause current waypoint task before this can happen.
     * Result should be the drone holding its current position until new commands are issued
     */
    public static void engageJoystick() {
        engageJoystick(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public static void engageJoystick(final Runnable onSuccess) {
        DJIDrone.getDjiGroundStation().pauseGroundStationTask(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                DJIDrone.getDjiGroundStation().setYawControlMode(DJIGroundStationTypeDef.DJINavigationFlightControlYawControlMode.Navigation_Flight_Control_Yaw_Control_Angle);
                DJIDrone.getDjiGroundStation().setVerticalControlMode(DJIGroundStationTypeDef.DJINavigationFlightControlVerticalControlMode.Navigation_Flight_Control_Vertical_Control_Position);
                DJIDrone.getDjiGroundStation().setAircraftJoystick(0, 0, 0, 0, new DJIGroundStationExecuteCallBack() {
                    @Override
                    public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                        MessageHandler.d("Engage Joystick: " + groundStationResult.toString());
                        if (resultSuccess(groundStationResult)) {
                            DroneState.setMode(DroneState.DIRECT_MODE);
                            onSuccess.run();
                        }
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
        newTask();
        addPoint(DroneState.getLatitude(), DroneState.getLongitude());
        uploadAndExecuteTask();
    }

    /**
     * Set direct control mode angles for drone
     *
     * @param pitch
     * @param yaw
     * @param roll
     */
    public static void setAngles(final double pitch, final double yaw, final double roll) {
        setAngles(pitch, yaw, roll, 0);
    }

    public static void setAngles(final double pitch, final double yaw, final double roll, final int altitude) {
        if (DroneState.getMode() != DroneState.DIRECT_MODE) {
//             MessageHandler.log("Not in Direct Mode!");
            return;
        }
        DJIDrone.getDjiGroundStation().setAircraftJoystick((int) yaw, (int) pitch, (int) roll, altitude, new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                MessageHandler.log("Engage Joystick: " + groundStationResult.toString());
                if (resultSuccess(groundStationResult)) {
                    DroneState.setMode(DroneState.DIRECT_MODE);
                }
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
            return null;
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
