package com.jarone.litterary.drone;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

import dji.sdk.Camera.DJICameraSettingsDef;
import dji.sdk.Camera.DJIMedia;
import dji.sdk.Camera.DJIMediaManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJICameraError;
import dji.sdk.base.DJIError;

/**
 * Created by Adam on 2015-11-16.
 */
public class Camera {

    public static int requestedGimbalAngle = 0;
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
        DroneState.mProduct.getCamera().startShootPhoto(
                DJICameraSettingsDef.CameraShootPhotoMode.Single,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        //TODO: See if this actually works.
                        downloadLatestPhoto();
                    }
                }
        );
    }

    /**
     * Downloads the latest photo on the SD card and labels it with the drone's current GPS coords
     */
    public static void downloadLatestPhoto() {
        DroneState.mProduct.getCamera().getMediaManager().fetchMediaList(new DJIMediaManager.CameraDownloadListener<ArrayList<DJIMedia>>() {
            @Override
            public void onStart() {

            }

            @Override
            public void onRateUpdate(long l, long l1, long l2) {

            }

            @Override
            public void onProgress(long l, long l1) {

            }

            @Override
            public void onSuccess(ArrayList<DJIMedia> djiMedias) {
                djiMedias.get(djiMedias.size()).fetchMediaData(new File(Environment.getExternalStorageDirectory().
                        getPath() + "/Dji_Sdk_Test/lalala.jpg"), new DJIMediaManager.CameraDownloadListener<Boolean>() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onRateUpdate(long l, long l1, long l2) {

                    }

                    @Override
                    public void onProgress(long l, long l1) {

                    }

                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        //TODO: Check whether or not the lalal.jpg actually got made?
                        //yay?
                    }

                    @Override
                    public void onFailure(DJICameraError djiCameraError) {

                    }
                });
            }

            @Override
            public void onFailure(DJICameraError djiCameraError) {

            }


        });
    }

    ;


    public static String formatFileName() {
        return "survey/" + System.currentTimeMillis() + "|" + DroneState.getLatitude() + "|" + DroneState.getLongitude();
    }

    //TODO: lalalal
//    public static void setGimbalPitch(int angle) {
//        DroneState.mProduct.getGimbal().;
//
//        rotateGimbalByAngle();
//        updateGimbalAttitude(
//                getGimbalRotation(angle),
//                new DJIGimbalRotation(false, false, false, 0),
//                new DJIGimbalRotation(false, false, false, 0)
//        );
//    }
//
//    private static DJIGimbalRotation getGimbalRotation(int angle) {
//        return new DJIGimbalRotation(true, false, true, angle);
//    }
//}
}