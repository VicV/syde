package com.jarone.litterary.activities;

import android.os.Bundle;
import android.util.Log;

import com.jarone.litterary.DroneState;
import com.jarone.litterary.GroundStation;
import com.jarone.litterary.R;
import com.jarone.litterary.VisionProcessor;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef;
import dji.sdk.api.DJIError;
import dji.sdk.interfaces.DJIGeneralListener;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import dji.sdk.widget.DjiGLSurfaceView;


public class MainActivity extends DJIBaseActivity {

    /**
     * The log tag
     **/
    private static final String TAG = "Litterary";

    private DjiGLSurfaceView mDjiGLSurfaceView;

    private VisionProcessor visionProcessor = new VisionProcessor();
    public DroneState droneState = new DroneState();
    public GroundStation groundStation = new GroundStation();


    //Activity is starting.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activateDJI();

        initSDK();
        DJIDrone.connectToDrone();


        registerCamera();
    }

    public void testPlan() {
        GroundStation.newTask();
        GroundStation.defaultAltitude = 20;
        GroundStation.defaultSpeed = 1;
        GroundStation.addPoint(10, 10);
        GroundStation.addPoint(10, 10);
        GroundStation.addPoint(10, 10);
        GroundStation.addPoint(10, 10);
        GroundStation.uploadAndExecuteTask();
        GroundStation.executeTask();
    }

    @Override
    protected void onDestroy() {
        if (DJIDrone.getDjiCamera() != null) {
            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
        }
        super.onDestroy();
    }

    private void initSDK() {
        DJIDrone.initWithType(this.getApplicationContext(), DJIDroneTypeDef.DJIDroneType.DJIDrone_Vision);
    }


    private void activateDJI() {
        new Thread() {
            public void run() {
                try {
                    DJIDrone.checkPermission(getApplicationContext(), new DJIGeneralListener() {
                        @Override
                        public void onGetPermissionResult(int result) {
                            if (result == 0) {
                                // show success
                                Log.e(TAG, "onGetPermissionResult =" + result);
                                Log.e(TAG,
                                        "onGetPermissionResultDescription=" + DJIError.getCheckPermissionErrorDescription(result));
                                droneState.updateDroneState();
                            } else {
                                // show errors
                                Log.e(TAG, "onGetPermissionResult =" + result);
                                Log.e(TAG,
                                        "onGetPermissionResultDescription=" + DJIError.getCheckPermissionErrorDescription(result));
                            }
                        }
                    });
                } catch (Exception e) {
                    //Something broke
                }
            }
        }.start();
    }

    private void registerCamera() {
        mDjiGLSurfaceView = (DjiGLSurfaceView) findViewById(R.id.DjiSurfaceView_02);
        mDjiGLSurfaceView.start();

        DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                visionProcessor.processFrame(videoBuffer, size);
                mDjiGLSurfaceView.setDataToDecoder(videoBuffer, size);
            }
        };
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
    }

}
