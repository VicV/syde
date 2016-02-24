package com.jarone.litterary.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.opengl.GLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.R;
import com.jarone.litterary.control.AngularController;
import com.jarone.litterary.drone.Camera;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.LocationHelper;
import com.jarone.litterary.imageproc.ImageProcessing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import dji.sdk.api.DJIDrone;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import dji.sdk.widget.DjiGLSurfaceView;


public class MainActivity extends DJIBaseActivity {

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    boolean buttonPress = false;
    public static final int POINTS_REQUEST_CODE = 130;
    public static final int POINTS_RESULT_CODE = 230;

    private DjiGLSurfaceView mDjiGLSurfaceView;

    private static final String TAG = MainActivity.class.toString();
    int count = 0;

    private ImageView CPreview;

    private Context mainActivity;

    private ScheduledExecutorService taskScheduler;

    private LatLng[] currentPolygon = null;
    private ArrayList<LatLng> currentPhotoPoints = null;


    //Activity is starting.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;
        verifyStoragePermissions(this);
        setOnClickListeners();

        registerCamera();

        registerUpdateInterface();

        ContextManager.setContext(this);


        DroneState.registerConnectedTimer();
        GroundStation.registerPhantom2Callback();

        CPreview = ((ImageView) findViewById(R.id.CVPreview));

    }

    private void registerUpdateInterface() {
        taskScheduler = Executors.newSingleThreadScheduledExecutor();
        taskScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateInterface();
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }


    public View.OnClickListener getDirectionalListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button_up:
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                            GroundStation.setAngles(DroneState.getPitch() + 500, DroneState.getYaw(), DroneState.getRoll());
                        }
                        break;
                    case R.id.button_down:
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                            GroundStation.setAngles(DroneState.getPitch() - 500, DroneState.getYaw(), DroneState.getRoll());
                        }
                        break;
                    case R.id.button_left:
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                            GroundStation.setAngles(DroneState.getPitch(), DroneState.getYaw(), DroneState.getRoll() - 500);
                        }
                        break;
                    case R.id.button_right:
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                            GroundStation.setAngles(DroneState.getPitch(), DroneState.getYaw(), DroneState.getRoll() + 500);
                        }
                        break;
                    case R.id.button_stop:
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                            GroundStation.setAngles(0, 0, 0);
                        } else {
                            GroundStation.stopTask();
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
                float altitude = getAltitudeValue();
                if (altitude == -1) {
                    MessageHandler.d("Please enter a valid number");
                } else if (altitude < 100) {
                    GroundStation.setAltitude(altitude);
                } else {
                    MessageHandler.d("Please choose valid altitude <100 m");
                }
            }
        };
    }

    public View.OnClickListener getStartSurveyListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng[] points = GroundStation.startSurveyRoute();
                if (points != null && points.length > 2) {
                    currentPhotoPoints = new ArrayList<>(Arrays.asList(points));
                    currentPhotoPoints.size();
                }
            }
        };
    }

    private interface BitmapCreatedCallback {
        void onBitmapCreated(Bitmap bitmap);
    }

    // Create a bitmap from the current surface frame.
    private void createBitmapFromFrame(final BitmapCreatedCallback bitmapCreatedCallback) {

        mDjiGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                EGL10 egl = (EGL10) EGLContext.getEGL();
                //Get GL10 object from EGL context.
                GL10 gl = (GL10) egl.eglGetCurrentContext().getGL();
                Bitmap frame = smallerTest(0, 0, mDjiGLSurfaceView.getWidth(), mDjiGLSurfaceView.getHeight(), gl);

                bitmapCreatedCallback.onBitmapCreated(frame);
            }
        });

    }

    private Bitmap smallerTest(int x, int y, int w, int h, GL10 gl) {

        if (gl != null && w != 0 && h != 0) {
            int screenshotSize = w * h;
            ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
            bb.order(ByteOrder.nativeOrder());
            gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
            int pixelsBuffer[] = new int[screenshotSize];
            bb.asIntBuffer().get(pixelsBuffer);
            Bitmap bitmap = Bitmap.createBitmap(w, h,
                    Bitmap.Config.RGB_565);

            bitmap.setPixels(pixelsBuffer, screenshotSize - w, -w, 0,
                    0, w, h);

            short sBuffer[] = new short[screenshotSize];
            ShortBuffer sb = ShortBuffer.wrap(sBuffer);
            bitmap.copyPixelsToBuffer(sb);

            // Making created bitmap (from OpenGL points) compatible with
            // Android bitmap
            for (int i = 0; i < screenshotSize; ++i) {
                short v = sBuffer[i];
                sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
            }
            sb.rewind();
            bitmap.copyPixelsFromBuffer(sb);
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
        return null;
    }

    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL10 gl) {

        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            //Pull the pixels from the surface
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    //Go through each pixel and offset it the appropriate amount to be understood as a bitmap by android
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    //Set that pixel in the bitmap as the pixel created above.
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            Log.e(TAG, "createBitmapFromGLSurface: " + e.getMessage(), e);
            return null;
        }

        //Actually make the bitmap from the pixel array.
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    public View.OnClickListener getFindLitterListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!GroundStation.getCurrentSurveyRoute().isFinished()) {
                    MessageHandler.d("Survey Still Executing!");
                } else {
                    GroundStation.getCurrentSurveyRoute().analyzeSurveyPhotos();
                }
            }
        };
    }

    public View.OnClickListener getSwitchModeListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToggleButton button = (ToggleButton) v;
                if (button.isChecked()) {
                    GroundStation.engageJoystick();
                } else {
                    GroundStation.engageGroundStation();
                }
                /**
                 * Reset the button to its previous state because it can't actually change until
                 * the drone itself changes modes. This will be taken care of in the updateInterface
                 * calls
                 */
                button.setChecked(!button.isChecked());
            }
        };
    }

    public View.OnClickListener getStatusButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View infoLayout = findViewById(R.id.infoLayout);
                if (infoLayout.getVisibility() == View.INVISIBLE) {
                    infoLayout.setVisibility(View.VISIBLE);
                    //Camera.requestedGimbalAngle = 1000;
                } else {
                    infoLayout.setVisibility(View.INVISIBLE);
                    //Camera.requestedGimbalAngle = 0;
                }
            }
        };
    }

    public View.OnClickListener getPIDButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GroundStation.executingController()) {
                    GroundStation.stopController();
                } else {
                    GroundStation.executeController();
                }
            }
        };
    }

    public View.OnClickListener getSpecialButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.button_special1:
                        Camera.takePhoto();
                        break;
                    case R.id.button_special2:
                        AngularController ctrl = new AngularController();
                        ctrl.generateControlTable();
                        //ControlTable.testSaveLoad();
//                        long start = System.currentTimeMillis();
//                        int blobCount = 10;
//                        for (int i = 0; i < blobCount; i++) {
//                            ImageProcessing.setSourceImage("charger.jpg");
//                            ImageProcessing.detectBlobs();
//                        }
//                        long end = System.currentTimeMillis();
//                        MessageHandler.d("Average: " + ((end - start) / blobCount));

                        break;
                    case R.id.button_special3:
//                        buttonPress = true;
//                        count = 0;
                        // Camera.takePhoto();
                        //ControlTable.testSaveLoad();
                        Camera.downloadLatestPhoto();
                        break;
                }
            }
        };
    }

    public View.OnClickListener getCameraViewListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: REMOVE
//                switch (v.getId()) {
//                    case R.id.DjiSurfaceView_02:
//                        v.setVisibility(View.INVISIBLE);
//                        findViewById(R.id.CVPreview).setVisibility(View.VISIBLE);
//                        break;
//                    case R.id.CVPreview:
//                        v.setVisibility(View.INVISIBLE);
//                        findViewById(R.id.DjiSurfaceView_02).setVisibility(View.VISIBLE);
//                        break;
//                }
            }
        };
    }

    private boolean processing = false;

    private void registerCamera() {
        mDjiGLSurfaceView = (DjiGLSurfaceView) findViewById(R.id.DjiSurfaceView_02);
        mDjiGLSurfaceView.start();

        DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                mDjiGLSurfaceView.setDataToDecoder(videoBuffer, size);

                if (buttonPress && !processing && count < 20) {
                    processing = true;
                    new ImageAsyncTask().execute();
                } else if (count >= 20 && processing) {
                    processing = false;
                    buttonPress = false;
                }
            }
        };
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
    }

    private class ImageAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            createBitmapFromFrame(new BitmapCreatedCallback() {
                @Override
                public void onBitmapCreated(final Bitmap bitmap) {
                    count++;
                    processing = false;
                    CPreview.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (bitmap != null) {
                                CPreview.setImageBitmap(ImageProcessing.processImage(bitmap));
                            }
                        }
                    }, 200);
                    // MessageHandler.d("Bitmap: " + count);
                }

            });
            return null;
        }
    }

    private View.OnClickListener setRegionClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = (new Intent(mainActivity, MapActivity.class));
                intent.putExtra("polygon", currentPolygon);
                intent.putExtra("picturePoints", currentPhotoPoints);
                startActivityForResult(intent, POINTS_REQUEST_CODE);
            }
        };
    }

    public float getAltitudeValue() {
        EditText text = (EditText) findViewById(R.id.editText);
        try {
            return Float.parseFloat(text.getText().toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == POINTS_REQUEST_CODE && resultCode == POINTS_RESULT_CODE) {
            Parcelable[] parcelArray = data.getParcelableArrayExtra("points");
            //can't cast parcelable to latlng array, need to copy it
            LatLng[] parcel = Arrays.copyOf(parcelArray, parcelArray.length, LatLng[].class);
            if (parcel.length > 2) {
                currentPolygon = parcel;
            }


            new DoRouteTask().execute();
        }
    }

    public class DoRouteTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            findViewById(R.id.linearLayout).setVisibility(View.INVISIBLE);
            findViewById(R.id.loadingLayout).setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {


            LatLng[] points = GroundStation.initializeSurveyRoute(currentPolygon, getAltitudeValue());

            if (points != null) {
                currentPhotoPoints = new ArrayList<>(Arrays.asList(points));
                currentPhotoPoints.size();
            }

            routeComplete();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            final TextView loadingText = ((TextView) findViewById(R.id.loading));
            loadingText.setText("DONE!");
            loadingText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingText.setText("Creating Optimized Photo Route");
                    findViewById(R.id.linearLayout).setVisibility(View.VISIBLE);
                    findViewById(R.id.loadingLayout).setVisibility(View.INVISIBLE);
                }
            }, 500);
            super.onPostExecute(aVoid);
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     * <p/>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void routeComplete() {

    }

    private void updateInterface() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToggleButton modeButton = (ToggleButton) findViewById(R.id.button_switch_mode);
                if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                    modeButton.setChecked(true);
                    ((TextView) findViewById(R.id.currentMode)).setText("DIRECT");
                } else if (DroneState.getMode() == DroneState.WAYPOINT_MODE) {
                    modeButton.setChecked(false);
                    ((TextView) findViewById(R.id.currentMode)).setText(DroneState.flightMode.name());
                }
                ((TextView) findViewById(R.id.currentLocation)).setText(
                        LocationHelper.formatForDisplay(DroneState.getLatitude(), DroneState.getLongitude()));

                ((TextView) findViewById(R.id.targetLocation)).setText(
                        LocationHelper.formatForDisplay(
                                GroundStation.getCurrentTarget().latitude,
                                GroundStation.getCurrentTarget().longitude)

                );
                ((TextView) findViewById(R.id.droneConnected)).setText("" + DroneState.droneConnected);
                if (GroundStation.executingController()) {
                    ((TextView) findViewById(R.id.pid_angle)).setText("" + GroundStation.getAngularController().getLastAction());
                    ((TextView) findViewById(R.id.pid_error)).setText("" + GroundStation.getAngularController().getLastError());
                }

                ImageProcessing.convertLatestFrame();
                ((ImageView) findViewById(R.id.CVPreview)).setImageBitmap(ImageProcessing.getCVPreview());
            }
        });
    }

    @Override
    public void onBackPressed() {
        View infoLayout = findViewById(R.id.infoLayout);
        if (infoLayout.getVisibility() == View.INVISIBLE) {
            super.onBackPressed();
        } else {
            infoLayout.setVisibility(View.INVISIBLE);
        }
    }


    private void setOnClickListeners() {
        findViewById(R.id.button_down).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_left).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_right).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_up).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_stop).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_go_home).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.button_set_home).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.button_set_altitude).setOnClickListener(setAltitudeListener());
        findViewById(R.id.button_set_region).setOnClickListener(setRegionClickListener());
        findViewById(R.id.button_start_survey).setOnClickListener(getStartSurveyListener());
        findViewById(R.id.button_switch_mode).setOnClickListener(getSwitchModeListener());
        findViewById(R.id.button_status).setOnClickListener(getStatusButtonListener());
        findViewById(R.id.button_pid).setOnClickListener(getPIDButtonListener());
        findViewById(R.id.button_special1).setOnClickListener(getSpecialButtonListener());
        findViewById(R.id.button_special2).setOnClickListener(getSpecialButtonListener());
        findViewById(R.id.button_special3).setOnClickListener(getSpecialButtonListener());
        findViewById(R.id.DjiSurfaceView_02).setOnClickListener(getCameraViewListener());
        findViewById(R.id.CVPreview).setOnClickListener(getCameraViewListener());
    }
}