package com.jarone.litterary.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.imageproc.ImageProcessing;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import dji.midware.data.manager.P3.ServiceManager;
import dji.sdk.api.DJIDrone;

public class DJIBaseActivity extends FragmentActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    protected void onResume() {
        super.onResume();
        DroneState.updateDroneState();
        DJIDrone.getDjiMC().startUpdateTimer(1000);
        DJIDrone.getDjiBattery().startUpdateTimer(2000);
        if (!OpenCVLoader.initDebug()) {
            MessageHandler.d("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, ImageProcessing.loaderCallback);
        } else {
            MessageHandler.d("OpenCV library found inside package. Using it!");
            ImageProcessing.loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        DJIDrone.getDjiGroundStation().startUpdateTimer(1000);
        ServiceManager.getInstance().pauseService(false); // Resume the service
    }

    @Override
    protected void onPause() {
        super.onPause();
        DJIDrone.getDjiMC().stopUpdateTimer();
        DJIDrone.getDjiBattery().stopUpdateTimer();

        ServiceManager.getInstance().pauseService(true); // Pause the service
    }

    @Override
    protected void onDestroy() {
        if (DJIDrone.getDjiCamera() != null) {
            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
        }
        super.onDestroy();
    }
}
