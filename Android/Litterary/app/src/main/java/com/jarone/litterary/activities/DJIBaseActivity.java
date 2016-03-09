package com.jarone.litterary.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.imageproc.ImageProcessing;

import org.opencv.android.OpenCVLoader;

import dji.midware.data.manager.P3.ServiceManager;
import dji.sdk.api.DJIDrone;

public class DJIBaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DroneState.updateDroneState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DJIDrone.getDjiMC().startUpdateTimer(1000);
        DJIDrone.getDjiBattery().startUpdateTimer(2000);
        OpenCVLoader.initAsync("2.4.8", this, ImageProcessing.loaderCallback);
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
