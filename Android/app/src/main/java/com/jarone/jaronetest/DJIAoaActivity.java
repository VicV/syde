package com.jarone.jaronetest;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Objects;

import dji.midware.data.manager.P3.ServiceManager;
import dji.midware.usb.P3.DJIUsbAccessoryReceiver;
import dji.midware.usb.P3.UsbAccessoryService;

public class DJIAoaActivity extends Activity {

    private static boolean isStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_djiaoa);
        connectAoa();
    }

    //TODO: Just put in main onResume
    private void connectAoa() {

        if (!isStarted) {
            isStarted = true;
            ServiceManager.getInstance();
            UsbAccessoryService.registerAoaReceiver(this);
            Intent intent = new Intent(DJIAoaActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            //Do nothing
        }



        Intent aoaIntent = getIntent();
        if (aoaIntent != null) {
            String action = aoaIntent.getAction();
            if (action.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED) || action.equals(Intent.ACTION_MAIN)) {
                Intent attachedIntent = new Intent();
                attachedIntent.setAction(DJIUsbAccessoryReceiver.ACTION_USB_ACCESSORY_ATTACHED);
                sendBroadcast(attachedIntent);
            }
        }
        finish();
    }
}
