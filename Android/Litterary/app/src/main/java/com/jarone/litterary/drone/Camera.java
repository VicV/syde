package com.jarone.litterary.drone;

import com.jarone.litterary.handlers.MessageHandler;

import java.util.ArrayList;

import dji.sdk.api.Camera.DJICameraSettingsTypeDef;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.Gimbal.DJIGimbalRotation;
import dji.sdk.api.media.DJIMediaDirInfo;
import dji.sdk.api.media.DJIMediaInfo;
import dji.sdk.interfaces.DJIDownloadListener;
import dji.sdk.interfaces.DJIExecuteResultCallback;
import dji.sdk.interfaces.DJIFileDownloadCallBack;
import dji.sdk.util.DjiLocationCoordinate2D;

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
        DJIDrone.getDjiCamera().setCameraGps(
            new DjiLocationCoordinate2D(DroneState.getLatitude(), DroneState.getLongitude()),
            new DJIExecuteResultCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    DJIDrone.getDjiCamera().startTakePhoto(
                        DJICameraSettingsTypeDef.CameraCaptureMode.Camera_Single_Capture,
                        new DJIExecuteResultCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                MessageHandler.d("Take Photo: " + djiError.errorDescription);
                                downloadLatestPhoto();
                                photoCallback.run();
                                photoCallback = new Runnable() {
                                    @Override
                                    public void run() {

                                    }
                                };
                            }
                        }
                    );
                }
            }
        );
    }

    /**
     * Downloads the latest photo on the SD card and labels it with the drone's current GPS coords
     */
    public static void downloadLatestPhoto() {
        DJIDrone.getDjiCamera().fetchMediaList(new DJIDownloadListener<DJIMediaDirInfo>() {
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
            public void onSuccess(DJIMediaDirInfo djiMediaDirInfo) {
                ArrayList<DJIMediaInfo> fileList = djiMediaDirInfo.fileInfoList;
                int index = fileList.get(fileList.size()).index;

                DJIDrone.getDjiCamera().selectFileAtIndex(index, new DJIExecuteResultCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DJIDrone.getDjiCamera().downloadAllSelectedFiles(formatFileName(), new DJIFileDownloadCallBack() {
                            @Override
                            public void OnStart() {

                            }

                            @Override
                            public void OnEnd() {
                                DJIDrone.getDjiCamera().unselectAllFiles(new DJIExecuteResultCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {

                                    }
                                });
                            }

                            @Override
                            public void OnError(Exception e) {

                            }

                            @Override
                            public void OnProgressUpdate(int i) {

                            }
                        });
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });
    }

    public static String formatFileName() {
        return "survey/" + System.currentTimeMillis() + "|" + DroneState.getLatitude() + "|" + DroneState.getLongitude();
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
