package com.jarone.litterary;

import android.app.Application;
import android.content.Context;

import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.handlers.MessageHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef;
import dji.sdk.api.DJIError;
import dji.sdk.interfaces.DJIGeneralListener;

/**
 * Created by vic on 10/26/15.
 */
public class LitterApplication extends Application {

    public static boolean devMode = true;

    private static LitterApplication instance;

    public LitterApplication() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    public static LitterApplication getInstance() {
        return instance;
    }

    private ScheduledExecutorService taskScheduler;


    public static DroneState droneState = new DroneState();

    //For android 23, the DJI library doesn't know where to look for things. So we force them to load.
    static {
        try {
            System.loadLibrary("DJICam");
            System.loadLibrary("djivideo");
            System.loadLibrary("ffmpeg-neon");
            System.loadLibrary("FlyForbid");
            System.loadLibrary("GroudStation");
            System.loadLibrary("FlyForbid-p2v");
            System.loadLibrary("GroudStation-p2v");
        } catch (Exception e) {
            MessageHandler.w("Problem loading libraries: " + e.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activateDJI();
        initSDK();
        DroneState.droneConnected = DJIDrone.connectToDrone();
        taskScheduler = Executors.newScheduledThreadPool(10);
    }

    public ScheduledExecutorService getScheduler() {
        return taskScheduler;
    }

    private void initSDK() {
        DJIDrone.initWithType(this.getApplicationContext(), DJIDroneTypeDef.DJIDroneType.DJIDrone_Vision);
    }

    private void activateDJI() {
        new Thread() {
            public void run() {
                try {
                    DJIDrone.checkPermission(getApplicationContext(), new DJIGeneralListener() {
                        @Override
                        public void onGetPermissionResult(int result) {
                            if (result == 0) {
                                // show success
                                MessageHandler.d("onGetPermissionResult =" + result);
                                MessageHandler.d(
                                        "onGetPermissionResultDescription=" + DJIError.getCheckPermissionErrorDescription(result));
                            } else {
                                // show errors
                                MessageHandler.d("onGetPermissionResult =" + result);
                                MessageHandler.d("onGetPermissionResultDescription="
                                        + DJIError.getCheckPermissionErrorDescription(result)
                                );
                            }
                        }
                    });
                } catch (Exception e) {
                    //Something broke
                }
            }
        }.start();
    }


    public static DroneState getDroneState() {
        return droneState;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

}
