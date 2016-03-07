package com.jarone.litterary.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.LitterApplication;
import com.jarone.litterary.R;
import com.jarone.litterary.Receivers.WifiChangeReceiver;
import com.jarone.litterary.Receivers.WifiScanReceiver;
import com.jarone.litterary.adapters.DebugMessageRecyclerAdapter;
import com.jarone.litterary.adapters.ViewPagerAdapter;
import com.jarone.litterary.control.AngularController;
import com.jarone.litterary.datatypes.DebugItem;
import com.jarone.litterary.drone.Camera;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.Grabber;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.ImageHelper;
import com.jarone.litterary.helpers.LocationHelper;
import com.jarone.litterary.helpers.WifiHelper;
import com.jarone.litterary.imageproc.ImageProcessing;
import com.jarone.litterary.views.AndroidCameraSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    public static final int POINTS_REQUEST_CODE = 130;
    public static final int POINTS_RESULT_CODE = 230;
    public static ArrayList<DebugItem> messageList;

    private DjiGLSurfaceView mDjiGLSurfaceView;
    private AndroidCameraSurfaceView mAndroidCameraSurfaceView;

    private ImageView CPreview;
    private WifiManager wifiManager;
    private RecyclerView debugMessageRecyclerView;
    private Context mainActivity;
    private ScheduledExecutorService taskScheduler;
    private ScheduledFuture trackFuture;

    private LatLng[] currentPolygon = null;
    private ArrayList<LatLng> currentPhotoPoints = null;
    private ViewPager viewPager;

    Grabber grabber;

    //Activity is starting.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;

        setupViewPager();

        verifyStoragePermissions(this);
        registerCamera();
        ContextManager.setContext(this);
        DroneState.registerConnectedTimer();
        GroundStation.registerPhantom2Callback();
        setupWifiReceivers();


        taskScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    private void setupWifiReceivers() {

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        registerReceiver(new WifiScanReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        IntentFilter wifiChangeIntentFilter = new IntentFilter();
        wifiChangeIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        registerReceiver(new WifiChangeReceiver(wifiManager), wifiChangeIntentFilter);
    }


    private void registerUpdateInterface() {
        taskScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateInterface();
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
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

    public View.OnClickListener getDevButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.button_imgproc_1:
                        Camera.takePhoto();
                        break;
                    case R.id.button_imgproc_2:
                        AngularController ctrl = new AngularController();
                        ctrl.generateControlTable();
                        // TODO should be run once at startup
                        new ImageProcessing.CalibrateTask().execute();

                        break;
                    case R.id.button_imgproc_3:
                        if (grabber == null) {
                            grabber = new Grabber();
                        }
                        grabber.sendCommand(Grabber.Commands.OPEN);
                        break;
                    case R.id.button_special_1:
                        break;
                    case R.id.button_special_2:
                        break;
                    case R.id.button_special_3:
                        break;
                    case R.id.button_special_camera:
                        if (mDjiGLSurfaceView.getVisibility() != View.GONE) {
                            mDjiGLSurfaceView.setVisibility(View.GONE);
                            mDjiGLSurfaceView.pause();
                            mDjiGLSurfaceView.destroy();
                            AndroidCameraSurfaceView androidCamera = (AndroidCameraSurfaceView) findViewById(R.id.android_camera_surfaceview);
                            androidCamera.setVisibility(View.VISIBLE);
                            androidCamera.setupSurfaceView();
                        }
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
        mAndroidCameraSurfaceView = (AndroidCameraSurfaceView) findViewById(R.id.android_camera_surfaceview);
        mDjiGLSurfaceView = (DjiGLSurfaceView) findViewById(R.id.DJI_camera_surfaceview);
        mDjiGLSurfaceView.start();

        DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                mDjiGLSurfaceView.setDataToDecoder(videoBuffer, size);

//                if (!processing) {
//                    processing = true;
//                }
            }
        };
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
    }

    public void processFrame() {
        new ImageAsyncTask().execute(mDjiGLSurfaceView.getVisibility() == View.GONE ? mAndroidCameraSurfaceView : mDjiGLSurfaceView);
    }

    public class ImageAsyncTask extends AsyncTask<GLSurfaceView, Void, Void> {


        @Override
        protected Void doInBackground(GLSurfaceView... params) {
            ImageHelper.createBitmapFromFrame(new ImageHelper.BitmapCreatedCallback() {
                @Override
                public void onBitmapCreated(final Bitmap bitmap) {
                    processing = false;
                    if (CPreview != null) {
                        CPreview.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (bitmap != null) {
                                    CPreview.setImageBitmap(ImageProcessing.processImage(bitmap));
                                }
                            }
                        }, 200);
                    }
                    // MessageHandler.d("Bitmap: " + count);
                }

            }, params[0]);
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
//        EditText text = (EditText) findViewById(R.id.editText);
        try {
//            return Float.parseFloat(text.getText().toString());
        } catch (NumberFormatException e) {
            return -1;
        }
        return 20;
    }

    /**
     * Update the debug message list.
     * Note that there are two states;
     * if the recyclerView is null, that means the adapter hasn't been initialized.
     * <p/>
     * If this is true, just hand it to the arraylist that will be used to initialize the view.
     *
     * @param item
     */
    public void updateMessageList(DebugItem item) {
        if (debugMessageRecyclerView == null) {
            messageList.add(item);
        } else {
            ((DebugMessageRecyclerAdapter) debugMessageRecyclerView.getAdapter()).getDebugItemList().add(item);
            debugMessageRecyclerView.getAdapter().notifyDataSetChanged();
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
            findViewById(R.id.loading_text).setVisibility(View.VISIBLE);
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
            final TextView loadingText = ((TextView) findViewById(R.id.loading_text));
            loadingText.setText("DONE!");
            loadingText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingText.setText("creating optimized photo route...");
                    findViewById(R.id.loading_text).setVisibility(View.INVISIBLE);
                    ((TextView) findViewById(R.id.map_text)).setText("view route");
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
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                if (wifiManager.getConnectionInfo().getSSID().contains("Phantom")) {
                    ((ImageView) findViewById(R.id.connect_icon)).setImageDrawable(getDrawable(R.drawable.wifi_connected_small));
                    ((TextView) findViewById(R.id.connect_text)).setText("connected");
                } else {
                    ((TextView) findViewById(R.id.connect_text)).setText("connect");
                    ((ImageView) findViewById(R.id.connect_icon)).setImageDrawable(getDrawable(R.drawable.wifi_not_connected_small));
                }
                ImageView modeButton = (ImageView) findViewById(R.id.switch_mode_icon);
                if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                    modeButton.setImageDrawable(getDrawable(R.drawable.direct_small));
                    ((TextView) findViewById(R.id.switch_mode_text)).setText("direct");
                    ((TextView) findViewById(R.id.currentMode)).setText("DIRECT");


                } else if (DroneState.getMode() == DroneState.WAYPOINT_MODE) {
                    modeButton.setImageDrawable(getDrawable(R.drawable.map_pin_small));
                    ((TextView) findViewById(R.id.switch_mode_text)).setText("waypoint");
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
//                if (GroundStation.executingController()) {
//                    ((TextView) findViewById(R.id.pid_angle)).setText("" + GroundStation.getAngularController().getLastAction());
//                    ((TextView) findViewById(R.id.pid_error)).setText("" + GroundStation.getAngularController().getLastError());
//                }

                ImageProcessing.convertLatestFrame();
                if (LitterApplication.devMode) {
                    ((ImageView) findViewById(R.id.CVPreview)).setImageBitmap(ImageProcessing.getCVPreview());
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 1) {
            super.onBackPressed();
        } else {
            viewPager.setCurrentItem(1);
        }
    }


    private void setupViewPager() {
        messageList = new ArrayList<>();
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        //Forces all views to be loaded.
        viewPager.setOffscreenPageLimit(10);
        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.setCurrentItem(1);
        viewPager.post(new Runnable() {
            @Override
            public void run() {
                //This is done as a post on the viewpager because we must wait for it to
                //Be initialized before interacting with the views.
                setOnClickListeners();
                registerUpdateInterface();
                if (LitterApplication.devMode) {
                    CPreview = ((ImageView) findViewById(R.id.CVPreview));
                }

                //Set up our debug message lest
                debugMessageRecyclerView = (RecyclerView) findViewById(R.id.message_list_view);
                debugMessageRecyclerView.setAdapter(new DebugMessageRecyclerAdapter(mainActivity, messageList));
                debugMessageRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));

                //Forces scroll to bottom on every update.
                debugMessageRecyclerView.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        debugMessageRecyclerView.scrollToPosition(debugMessageRecyclerView.getAdapter().getItemCount() - 1);
                    }
                });

            }
        });

        //Tabs for viewpager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        //This automatically sets up the tabs and everything for us.
        tabLayout.setupWithViewPager(viewPager);
    }


    public View.OnClickListener getHomeButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.go_home_button:
                        GroundStation.goHome();
                        break;
                    case R.id.home_button:
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


    public void connectWithSSID(String SSID) {
        MessageHandler.d("Attempting to connect");
        WifiHelper.enableNetwork(SSID, wifiManager);
    }

    public void setupWifi() {
        if (wifiManager.getConnectionInfo().getSSID().startsWith("Phantom")) {
            MessageHandler.d("Drone already connected");
        } else {
            MessageHandler.d("Performing wifi scan...");
            registerReceiver(new WifiScanReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            wifiManager.startScan();
        }
    }

    public View.OnClickListener getWifiClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupWifi();
            }
        };

    }

    public View.OnClickListener getTrackListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case (R.id.button_special_1):
                        ImageProcessing.startTrackingObject();
                        trackFuture = taskScheduler.scheduleAtFixedRate(new Runnable() {
                            @Override
                            public void run() {
                                ImageProcessing.trackObject();
                            }
                        }, 0, 300, TimeUnit.MILLISECONDS);
                        break;
                    case (R.id.button_special_2):
                        ImageProcessing.stopTrackingObject();
                        if (trackFuture != null) {
                            trackFuture.cancel(true);
                            trackFuture = null;
                        }
                        break;
                }
            }
        };
    }


    private void setOnClickListeners() {
        //Main page
        findViewById(R.id.go_home_button).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.home_button).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.map_button).setOnClickListener(setRegionClickListener());
        findViewById(R.id.start_button).setOnClickListener(getStartSurveyListener());
//        findViewById(R.id.button_switch_mode).setOnClickListener(getSwitchModeListener());
        findViewById(R.id.DJI_camera_surfaceview).setOnClickListener(getCameraViewListener());
        findViewById(R.id.connect_button).setOnClickListener(getWifiClickListener());

        //Dev stuff
//        findViewById(R.id.button_track).setOnClickListener(getTrackListener());
//        findViewById(R.id.button_stop_track).setOnClickListener(getTrackListener());

        //Dev toggle
        if (LitterApplication.devMode) {
            findViewById(R.id.CVPreview).setOnClickListener(getCameraViewListener());
            findViewById(R.id.button_special_camera).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_special_1).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_special_2).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_special_3).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_imgproc_1).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_imgproc_2).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_imgproc_3).setOnClickListener(getDevButtonListener());
        }
    }
}