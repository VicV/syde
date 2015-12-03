package com.jarone.litterary.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.R;
import com.jarone.litterary.RouteOptimization;
import com.jarone.litterary.drone.Camera;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.LocationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dji.sdk.api.DJIDrone;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import dji.sdk.widget.DjiGLSurfaceView;


public class MainActivity extends DJIBaseActivity {

    public static final int POINTS_REQUEST_CODE = 130;
    public static final int POINTS_RESULT_CODE = 230;

    private DjiGLSurfaceView mDjiGLSurfaceView;
    private ImageView cameraView;

    private static final String TAG = MainActivity.class.toString();

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
        setOnClickListeners();

        registerCamera();

        registerUpdateInterface();

        ContextManager.setContext(this);

        //      cameraView = (ImageView) findViewById(R.id.camera_texture);

        DroneState.registerConnectedTimer();
        GroundStation.registerPhantom2Callback();
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
                        //TODO: Up Code
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                            GroundStation.setAngles(DroneState.getPitch() + 500, DroneState.getYaw(), DroneState.getRoll());
                        }
                        break;
                    case R.id.button_down:
                        //TODO: Down Code
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                            GroundStation.setAngles(DroneState.getPitch() - 500, DroneState.getYaw(), DroneState.getRoll());
                        }
                        break;
                    case R.id.button_left:
                        //TODO: Left Code
                        if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                            GroundStation.setAngles(DroneState.getPitch(), DroneState.getYaw(), DroneState.getRoll() - 500);
                        }
                        break;
                    case R.id.button_right:
                        //TODO: Right Code
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

                findViewById(R.id.button_special1).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }

                });
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
                        break;
                }
            }
        };
    }

    private void registerCamera() {
        mDjiGLSurfaceView = (DjiGLSurfaceView) findViewById(R.id.DjiSurfaceView_02);
        mDjiGLSurfaceView.start();
        //mDjiGLSurfaceView.getHolder().addCallback(this);

        DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                mDjiGLSurfaceView.setDataToDecoder(videoBuffer, size);
                //viewToBitmap(mDjiGLSurfaceView.getHolder(),mDjiGLSurfaceView.getWidth(), mDjiGLSurfaceView.getHeight());

            }
        };
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
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
    }


    private Bitmap viewToBitmap(SurfaceHolder holder, int width, int height) {
        if (height <= 0 || width <= 0) {
            Log.d("viewToBitmap", "Wrong size");
            return null;
        }
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = holder.lockCanvas();
        c.setBitmap(b);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setARGB(255, 40, 40, 40);
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), backgroundPaint);
        mDjiGLSurfaceView.draw(c);
        final Bitmap b2 = Bitmap.createBitmap(b, 0, 0, width, height);
        c.setBitmap(null);
        holder.unlockCanvasAndPost(c);


//        Paint backgroundPaint = new Paint();
//        backgroundPaint.setAlpha(255);
//        //c.drawRect(0, 0, c.getWidth() - 10, c.getHeight() - 10, backgroundPaint);
//        //view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
//        //c.drawBitmap(view.getDrawingCache(),0, 0, backgroundPaint);
//        ByteBuffer buf = ByteBuffer.allocateDirect(view.getWidth() * view.getHeight() * 4);
//        buf.order(ByteOrder.LITTLE_ENDIAN);
//        GLES20.glReadPixels(0, 0, view.getWidth(), view.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
//        buf.rewind();
//        b.copyPixelsFromBuffer(buf);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraView.setImageBitmap(b2);
            }
        });
        return b;
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


}
