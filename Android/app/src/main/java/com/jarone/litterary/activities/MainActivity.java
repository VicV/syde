package com.jarone.litterary.activities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;

import com.jarone.litterary.GroundStation;
import com.jarone.litterary.R;
import com.jarone.litterary.VisionProcessor;

import dji.sdk.api.DJIDrone;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import dji.sdk.widget.DjiGLSurfaceView;


public class MainActivity extends DJIBaseActivity {

    private DjiGLSurfaceView mDjiGLSurfaceView;

    VisionProcessor visionProcessor = new VisionProcessor();

    //Activity is starting.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        GroundStation.withConnection(new Runnable() {
            @Override
            public void run() {
                GroundStation.uploadAndExecuteTask();
            }
        });
    }


    private Bitmap viewToBitmap(DjiGLSurfaceView view) {


        Bitmap b = Bitmap.createBitmap(view.getLayoutParams().width, view.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        view.draw(c);
        return b;

    }


    private void registerCamera() {
        mDjiGLSurfaceView = (DjiGLSurfaceView) findViewById(R.id.DjiSurfaceView_02);
        mDjiGLSurfaceView.start();

        DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                mDjiGLSurfaceView.setDataToDecoder(videoBuffer, size);
                visionProcessor.processFrame(viewToBitmap(mDjiGLSurfaceView));
            }
        };
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
    }


    @Override
    protected void onDestroy() {
        if (DJIDrone.getDjiCamera() != null) {
            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
        }
        super.onDestroy();
    }

}
