package com.jarone.litterary.drone;

import com.jarone.litterary.handlers.MessageHandler;

import dji.sdk.api.Camera.DJICameraSettingsTypeDef;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.Gimbal.DJIGimbalRotation;
import dji.sdk.interfaces.DJIExecuteResultCallback;

/**
 * Created by Adam on 2015-11-16.
 */
public class Camera {

    public static int requestedGimbalAngle = 1000;
    /**
     * The callback which is executed when a photo is successfully taken. This will be changed
     * by calling classes
     */
    public static Runnable photoCallback = new Runnable() {
        @Override
        public void run() {

        }
    };

    /**
     * Takes a photo with the built-in drone camera, executes the callback when the action is finished
     */
    public static void takePhoto() {
        DJIDrone.getDjiCamera().startTakePhoto(DJICameraSettingsTypeDef.CameraCaptureMode.Camera_Single_Capture, new DJIExecuteResultCallback() {
            @Override
            public void onResult(DJIError djiError) {
                MessageHandler.d("Take Photo: " + djiError.errorDescription);
                photoCallback.run();
            }
        });
    }

    public static void setGimbalPitch(int angle) {
        DJIDrone.getDjiGimbal().updateGimbalAttitude(
                getGimbalRotation(angle),
                new DJIGimbalRotation(false, false, false, 0),
                new DJIGimbalRotation(false, false, false, 0)
        );
    }

    private static DJIGimbalRotation getGimbalRotation(int angle) {
        return new DJIGimbalRotation(true, false, true, angle);
    }
}
