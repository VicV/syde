package com.jarone.litterary;

import com.jarone.litterary.handlers.MessageHandler;

import dji.sdk.api.Camera.DJICameraSettingsTypeDef;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.interfaces.DJIExecuteResultCallback;

/**
 * Created by Adam on 2015-11-16.
 */
public class Camera {

    public static Runnable photoCallback = new Runnable() {
        @Override
        public void run() {

        }
    };

    public static void takePhoto() {
        DJIDrone.getDjiCamera().startTakePhoto(DJICameraSettingsTypeDef.CameraCaptureMode.Camera_Single_Capture, new DJIExecuteResultCallback() {
            @Override
            public void onResult(DJIError djiError) {
                MessageHandler.d("Take Photo: " + djiError.errorDescription);
                photoCallback.run();
            }
        });
    }
}
