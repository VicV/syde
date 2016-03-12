package com.jarone.litterary.drone;

import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.FileAccess;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dji.sdk.api.Camera.DJICameraSettingsTypeDef;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.Gimbal.DJIGimbalRotation;
import dji.sdk.api.media.DJIMedia;
import dji.sdk.interfaces.DJIExecuteResultCallback;
import dji.sdk.interfaces.DJIMediaFetchCallBack;
import dji.sdk.interfaces.DJIReceivedFileDataCallBack;
import dji.sdk.util.DjiLocationCoordinate2D;

/**
 * Created by Adam on 2015-11-16.
 */
public class Camera {

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd kk:mm:ss", Locale.ENGLISH);
    public static int requestedGimbalAngle = DJIDrone.getDjiGimbal().getGimbalPitchMaxAngle();
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
                                if (djiError.errorCode != DJIError.RESULT_OK) {
                                    engageCameraMode();
                                    takePhoto();
                                    return;
                                }
                                MessageHandler.d("Take Photo: " + djiError.errorDescription);
                                //downloadLatestPhoto();
                                photoCallback.run();
//                                photoCallback = new Runnable() {
//                                    @Override
//                                    public void run() {
//
//                                    }
//                                };
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
        DJIDrone.getDjiCamera().fetchMediaList(new DJIMediaFetchCallBack() {
            @Override
            public void onResult(List<DJIMedia> list, DJIError djiError) {
                if (djiError.errorCode == 0) {
                    downloadPhotoData(list.get(0), new Runnable() {
                        @Override
                        public void run() {
                            engageCameraMode();
                        }
                    });
                } else {
                    engageCameraMode();
                }
            }
        });
    }

    public static void downloadPhotosSince(final long timestamp, final Runnable callback) {
        DJIDrone.getDjiCamera().fetchMediaList(new DJIMediaFetchCallBack() {
            @Override
            public void onResult(List<DJIMedia> list, DJIError djiError) {
                if (djiError.errorCode == 0) {
                    if (list.size() < 1) {
                        return;
                    }
                    int index = 0;
                    Date date = parseDate(list.get(index).createTime);
                    ArrayList<DJIMedia> validPhotos = new ArrayList<DJIMedia>();
                    while (index < list.size() && date != null && date.after(new Date(timestamp))){
                        if (list.get(index).durationSeconds == 0) {
                            validPhotos.add(list.get(index));
                        }
                        date = parseDate(list.get(index).createTime);
                        index++;
                    }
                    downloadPhotoList(validPhotos, 0, callback);
                } else {
                    engageCameraMode();
                }
            }
        });
    }

    public static void downloadPhotoList(final ArrayList<DJIMedia> list, final int index, final Runnable callback) {
        Date date = parseDate(list.get(index).createTime);
        if (date != null) {
            MessageHandler.d("Downloading photo from " + date.toString());
            downloadPhotoData(list.get(index), new Runnable() {
                @Override
                public void run() {
                    if (index + 1 < list.size()) {
                        downloadPhotoList(list, index + 1, callback);
                    } else {
                        callback.run();
                        engageCameraMode();
                    }
                }
            });
        }
    }


    public static void downloadPhotoData(DJIMedia file, final Runnable callback) {
        try {
            FileOutputStream out = new FileOutputStream(formatFileName(parseDate(file.createTime).getTime()));
            final OutputStream outFile = new BufferedOutputStream(out);
            final int[] curProgress = {0};
            DJIDrone.getDjiCamera().fetchMediaData(file, new DJIReceivedFileDataCallBack() {
                @Override
                public void onResult(byte[] bytes, int size, int progress, DJIError djiError) {
                    if (djiError.errorCode == DJIError.RESULT_OK) {
                        try {
                            outFile.write(bytes, 0, size);
                            if (progress % 10 == 0 && progress > curProgress[0]) {
                                MessageHandler.d(size + " " + progress + " " + djiError.errorDescription);
                                curProgress[0] = progress;
                            }

                            if (progress == 100) {
                                outFile.close();
                                //engageCameraMode();
                                callback.run();
                            }
                        } catch (IOException e) {
                            engageCameraMode();
                        }
                    } else {
                        engageCameraMode();
                    }
                }
            });
        } catch (IOException e) {
            MessageHandler.e(e.getMessage());
        }
    }

    public static Date parseDate(String date) {
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
    public static void engageCameraMode() {
        engageCameraMode(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    /**
     * Switch camera to camera mode instead of file transfer mode)
     * @param callback
     */
    public static void engageCameraMode(final Runnable callback) {
        DJIDrone.getDjiCamera().setCameraMode(DJICameraSettingsTypeDef.CameraMode.Camera_Camera_Mode, new DJIExecuteResultCallback() {
            @Override
            public void onResult(DJIError djiError) {
                callback.run();
            }
        });
    }

    public static String formatFileName() {
        return FileAccess.formatFileName("survey", System.currentTimeMillis() + ".jpg").toString();
    }

    public static String formatFileName(long time) {
        return FileAccess.formatFileName("survey", time + ".jpg").toString();
    }

    /**
     * Set gimbal angle to a value between 1000 (straight down) and 0 (horizontal)
     * @param angle
     */
    public static void setGimbalPitch(int angle) {
        DJIDrone.getDjiGimbal().updateGimbalAttitude(
                getGimbalRotation(angle),
                new DJIGimbalRotation(false, false, false, 0),
                new DJIGimbalRotation(false, false, false, 0)
        );
    }
    public static void setGimbalDown() {
        DJIDrone.getDjiGimbal().updateGimbalAttitude(
                getGimbalRotation(DJIDrone.getDjiGimbal().getGimbalPitchMaxAngle()),
                new DJIGimbalRotation(false, false, false, 0),
                new DJIGimbalRotation(false, false, false, 0)
        );
    }

    private static DJIGimbalRotation getGimbalRotation(int angle) {
        return new DJIGimbalRotation(true, false, true, angle);
    }
}
