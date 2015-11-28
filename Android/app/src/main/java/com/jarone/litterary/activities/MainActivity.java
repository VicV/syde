package com.jarone.litterary.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.Camera;
import com.jarone.litterary.DroneState;
import com.jarone.litterary.GroundStation;
import com.jarone.litterary.R;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;

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

    private static final String TAG = MainActivity.class.toString();

    private Context mainActivity;

    private ScheduledExecutorService taskScheduler;

    //Activity is starting.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;
        setOnClickListeners();

        registerCamera();

        registerUpdateInterface();

        DroneState.registerConnectedTimer();
        GroundStation.registerMissionCallback();
        GroundStation.registerStatusCallback();
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
                GroundStation.startSurveyRoute();
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

    public View.OnClickListener getInfoButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View infoLayout = findViewById(R.id.infoLayout);
                if (infoLayout.getVisibility() == View.INVISIBLE) {
                    infoLayout.setVisibility(View.VISIBLE);
                    Camera.requestedGimbalAngle = 1000;
                } else {
                    infoLayout.setVisibility(View.INVISIBLE);
                    Camera.requestedGimbalAngle = 0;
                }
                //DroneState.updateDroneState();
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
        findViewById(R.id.button_stop).setOnClickListener(getDirectionalListener());
        findViewById(R.id.button_go_home).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.button_set_home).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.button_set_altitude).setOnClickListener(setAltitudeListener());
        findViewById(R.id.button_set_region).setOnClickListener(setRegionClickListener());
        findViewById(R.id.button_start_survey).setOnClickListener(getStartSurveyListener());
        findViewById(R.id.button_switch_mode).setOnClickListener(getSwitchModeListener());
        findViewById(R.id.button_info).setOnClickListener(getInfoButtonListener());

    }


    private Bitmap viewToBitmap(DjiGLSurfaceView view) {
        Bitmap b = Bitmap.createBitmap(view.getLayoutParams().width, view.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        view.draw(c);

        return b;
    }


    private View.OnClickListener setRegionClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(mainActivity, MapActivity.class), POINTS_REQUEST_CODE);
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

            GroundStation.initializeSurveyRoute(parcel, getAltitudeValue());
        }
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
                                GroundStation.getCurrentTarget().longitude
                        )
                );
                ((TextView) findViewById(R.id.droneConnected)).setText("" + DroneState.droneConnected);
            }
        });
    }
}
