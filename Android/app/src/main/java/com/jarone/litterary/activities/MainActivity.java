package com.jarone.litterary.activities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.jarone.litterary.DroneState;
import com.jarone.litterary.GroundStation;
import com.jarone.litterary.R;
import com.jarone.litterary.handlers.MessageHandler;

import dji.sdk.api.DJIDrone;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import dji.sdk.widget.DjiGLSurfaceView;


public class MainActivity extends DJIBaseActivity {

    private DjiGLSurfaceView mDjiGLSurfaceView;

    private static final String TAG = MainActivity.class.toString();

    //Activity is starting.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setOnClickListeners();

        registerCamera();

    }


    public View.OnClickListener getDirectionalListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button_up:
                        //TODO: Up Code
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {

                        }
                        break;
                    case R.id.button_down:
                        //TODO: Down Code
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {

                        }
                        break;
                    case R.id.button_left:
                        //TODO: Left Code
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {

                        }
                        break;
                    case R.id.button_right:
                        //TODO: Right Code
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {

                        }
                        break;
                }
            }
        };
    }

    public View.OnClickListener getHomeButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button_go_home:
                        GroundStation.goHome();
                        break;
                    case R.id.button_set_home:
                        GroundStation.setHomePoint();
                        break;
                }
            }
        };
    }

    public View.OnClickListener setAltitudeListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText text = (EditText)findViewById(R.id.editText);
                try {
                    float altitude = Float.parseFloat(text.getText().toString());
                    if (altitude < 100) {
                        GroundStation.setAltitude(altitude);
                    } else {
                        MessageHandler.d("Please choose altitude <100 m");
                    }
                } catch (NumberFormatException e) {
                    return;
                }
            }
        };
    }

    private void registerCamera() {
        mDjiGLSurfaceView = (DjiGLSurfaceView) findViewById(R.id.DjiSurfaceView_02);
        mDjiGLSurfaceView.start();

        DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                mDjiGLSurfaceView.setDataToDecoder(videoBuffer, size);

            }
        };
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
    }


    private void setOnClickListeners() {
        findViewById(R.id.button_down).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_left).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_right).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_up).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_go_home).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.button_set_home).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.button_set_altitude).setOnClickListener(setAltitudeListener());

    }


    private Bitmap viewToBitmap(DjiGLSurfaceView view) {
        Bitmap b = Bitmap.createBitmap(view.getLayoutParams().width, view.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        view.draw(c);
        return b;
    }


//    public void testPlan() {
//        GroundStation.newTask();
//        GroundStation.defaultAltitude = 20;
//        GroundStation.defaultSpeed = 1;
//        GroundStation.addPoint(10, 10);
//        GroundStation.addPoint(10, 10);
//        GroundStation.addPoint(10, 10);
//        GroundStation.addPoint(10, 10);
//
//        GroundStation.withConnection(new Runnable() {
//            @Override
//            public void run() {
//                GroundStation.uploadAndExecuteTask();
//            }
//        });
//    }


}
