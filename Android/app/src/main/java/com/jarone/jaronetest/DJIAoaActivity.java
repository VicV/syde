package com.jarone.jaronetest;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_djiaoa, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void connectAoa(){
        if (isStarted) {
            //Do nothing
        } else {
            isStarted = true;
            ServiceManager.getInstance();
            UsbAccessoryService.registerAoaReceiver(this);
            Intent intent = new Intent(DJIAoaActivity.this, MainActivity.class);
            startActivity(intent);
        }

        Intent aoaIntent = getIntent();
        if(aoaIntent != null) {
            String action = aoaIntent.getAction();
            if (action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED || action == Intent.ACTION_MAIN){
                Intent attachedIntent = new Intent();
                attachedIntent.setAction(DJIUsbAccessoryReceiver.ACTION_USB_ACCESSORY_ATTACHED);
                sendBroadcast(attachedIntent);
            }
        }
        finish();
    }
}
