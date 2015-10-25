package com.jarone.litterary;

import android.app.Activity;
import android.os.Bundle;

import dji.midware.data.manager.P3.ServiceManager;
import dji.sdk.api.DJIDrone;

public class DJIBaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_djiconnection_manager);
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
        DJIDrone.getDjiGroundStation().stopUpdateTimer();
        ServiceManager.getInstance().pauseService(true); // Pause the service
    }
}
