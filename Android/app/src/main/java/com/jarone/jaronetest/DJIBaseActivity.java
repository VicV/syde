package com.jarone.jaronetest;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import dji.midware.data.manager.P3.ServiceManager;

public class DJIBaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_djiconnection_manager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ServiceManager.getInstance().pauseService(false); // Resume the service
    }

    @Override
    protected void onPause() {
        super.onPause();
        ServiceManager.getInstance().pauseService(true); // Pause the service
    }
}
