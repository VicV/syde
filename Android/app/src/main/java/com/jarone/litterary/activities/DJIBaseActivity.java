package com.jarone.litterary.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.jarone.litterary.DroneState;

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

        DJIDrone.getDjiGroundStation().startUpdateTimer(1000);
        ServiceManager.getInstance().pauseService(false); // Resume the service
    }

    @Override
    protected void onPause() {
        super.onPause();
        DJIDrone.getDjiMC().stopUpdateTimer();

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
