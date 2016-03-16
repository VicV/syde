package com.jarone.litterary.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.LitterApplication;
import com.jarone.litterary.R;
import com.jarone.litterary.adapters.DebugMessageRecyclerAdapter;
import com.jarone.litterary.adapters.ViewPagerAdapter;
import com.jarone.litterary.control.AngularController;
import com.jarone.litterary.datatypes.DebugItem;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.Grabber;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.ImageHelper;
import com.jarone.litterary.helpers.LocationHelper;
import com.jarone.litterary.helpers.WifiHelper;
import com.jarone.litterary.imageproc.ImageProcessing;

import org.opencv.android.CameraGLSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
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
    private CameraGLSurfaceView mAndroidCameraSurfaceView;

    private ImageView CPreview;
    private WifiManager wifiManager;
    private RecyclerView debugMessageRecyclerView;
    private Context mainActivity;
    private ScheduledFuture trackFuture;
    private AngularController angularController;

    private LatLng[] currentPolygon = null;
    private ArrayList<LatLng> currentPhotoPoints = null;
    private ViewPager viewPager;
    private boolean canStartProcessing = false;

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

//        mDjiGLSurfaceView.setZOrderMediaOverlay(true);
//        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        addContentView(getLayoutInflater().inflate(R.layout.surface_overlay_layout, null), lp);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    private void registerUpdateInterface() {
        LitterApplication.getInstance().getScheduler().scheduleAtFixedRate(new Runnable() {
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
                if (DroneState.getMode() == DroneState.DIRECT_MODE) {
                    GroundStation.engageGroundStation();
                } else {
                    GroundStation.engageJoystick();
                }
                /**
                 * Reset the button to its previous state because it can't actually change until
                 * the drone itself changes modes. This will be taken care of in the updateInterface
                 * calls
                 */
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
//                        Camera.downloadPhotosSince(Camera.parseDate("2016-Mar-01 12:00:00").getTime(), new Runnable() {
//                            @Override
//                            public void run() {
//                                MessageHandler.d("DONE DOWNLOADING!!");
//                            }
//                        });
                        GroundStation.setAngles(0, 0, 0, 1);
                        break;
                    case R.id.button_imgproc_2:
                        if (grabber == null) {
                            grabber = new Grabber();
                        }
                        grabber.sendCommand(Grabber.Commands.LOWER);
                        break;
                    case R.id.button_imgproc_3:
                        if (ImageProcessing.isTracking()) {
                            ImageProcessing.stopTrackingObject();
                            if (trackFuture != null) {
                                trackFuture.cancel(true);
                                trackFuture = null;
                            }

                        } else {
                            ImageProcessing.startTrackingObject();
                            trackFuture = LitterApplication.getInstance().getScheduler().scheduleAtFixedRate(new Runnable() {
                                @Override
                                public void run() {
                                    ImageProcessing.trackObject();
                                }
                            }, 0, 300, TimeUnit.MILLISECONDS);
                        }

                        break;
                    case R.id.button_special_1:
                        if (grabber == null) {
                            grabber = new Grabber();
                        }
                        grabber.sendCommand(Grabber.Commands.OPEN);
                        break;

                    case R.id.button_special_2:
                        if (grabber == null) {
                            grabber = new Grabber();
                        }
                        grabber.sendCommand(Grabber.Commands.CLOSE);

                        break;
                    case R.id.button_special_3:
                        canStartProcessing = true;
//                        ImageProcessing.loadCalibration();
//                        try {
//                            InputStream i = ContextManager.getActivity().getAssets().open("c2.jpg");
//                            Bitmap decoded = BitmapFactory.decodeStream(i);
//                            int nh = (int) (decoded.getHeight() * (2000.0 / decoded.getWidth()));
//                            Bitmap scaled = Bitmap.createScaledBitmap(decoded, 2000, nh, true);
//                            ImageProcessing.readFrame(scaled);
//                            ImageProcessing.correctDistortion();
//                        } catch (IOException e) {
//                        }
                        break;
                    case R.id.button_special_camera:
                        if (mDjiGLSurfaceView.getVisibility() != View.GONE) {
                            mDjiGLSurfaceView.setVisibility(View.GONE);
                            mDjiGLSurfaceView.pause();
                            mDjiGLSurfaceView.destroy();
                            mAndroidCameraSurfaceView.setVisibility(View.VISIBLE);
                            mAndroidCameraSurfaceView.setCameraTextureListener(new CameraGLSurfaceView.CameraTextureListener() {
                                @Override
                                public void onCameraViewStarted(int i, int i1) {

                                }

                                @Override
                                public void onCameraViewStopped() {

                                }

                                @Override
                                public boolean onCameraTexture(int i, int i1, int i2, int i3) {
                                    processFrame();
                                    return false;
                                }
                            });
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
                //TODO: DO SOMETHING IF NECESSARY
            }
        };
    }

    private boolean processing = false;

    private void registerCamera() {
        mAndroidCameraSurfaceView = (CameraGLSurfaceView) findViewById(R.id.android_camera_surfaceview);
        mDjiGLSurfaceView = (DjiGLSurfaceView) findViewById(R.id.DJI_camera_surfaceview);
        mDjiGLSurfaceView.start();

        DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                mDjiGLSurfaceView.setDataToDecoder(videoBuffer, size);
                if (canStartProcessing) {
                    processFrame();
                }
            }
        };
        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);
    }

    public ArrayBlockingQueue<ImageAsyncTask> runningTasks = new ArrayBlockingQueue<>(5);

    public void processFrame() {
        ImageAsyncTask newTask = new ImageAsyncTask();
        if (runningTasks.offer(newTask)) {
            newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mDjiGLSurfaceView.getVisibility() == View.GONE ? mAndroidCameraSurfaceView : mDjiGLSurfaceView);
        }
    }

    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

    public class ImageAsyncTask extends AsyncTask<GLSurfaceView, Void, Void> {

        private ImageAsyncTask context = this;

        @Override
        protected Void doInBackground(GLSurfaceView... params) {
            ImageHelper.createBitmapFromFrame(new ImageHelper.BitmapCreatedCallback() {
                @Override
                public void onBitmapCreated(final Bitmap bitmap) {
                    if (CPreview != null) {
                        CPreview.post(new Runnable() {
                            @Override
                            public void run() {
                                if (bitmap != null) {
                                    ImageProcessing.setOriginalImage(bitmap);
                                    if (ImageProcessing.isTracking()) {
                                        ImageProcessing.trackObject();
                                    } else {
                                        ImageProcessing.processImage(bitmap);
                                    }
                                    CPreview.setImageBitmap(ImageProcessing.getCVPreview());
                                    runningTasks.remove(context);
                                }
                            }
                        });
                    }

                    // MessageHandler.d("Bitmap: " + count);
                }

            }, params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
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

    private boolean interfaceSetup = false;

    private ImageView connectIcon;
    private TextView connectText;
    private TextView switchModeText;
    private TextView currentModeText;
    private TextView currentLocation;
    private TextView targetLocation;
    private TextView droneConnectedText;
    private ImageView modeButton;
    private String lastWifi = "";
    private int lastMode = -6;
    private double lastLat = -99999;
    private double lastLong = -99999;
    private double lastTargetLat = -9999999;
    private double lastTargetLong = -999999;
    private boolean lastConnected = false;

    private void setupInterfaceUpdate() {
        connectIcon = ((ImageView) findViewById(R.id.connect_icon));
        connectText = ((TextView) findViewById(R.id.connect_text));
        switchModeText = ((TextView) findViewById(R.id.switch_mode_text));
        currentModeText = ((TextView) findViewById(R.id.currentMode));
        currentLocation = ((TextView) findViewById(R.id.currentLocation));
        targetLocation = ((TextView) findViewById(R.id.targetLocation));
        droneConnectedText = ((TextView) findViewById(R.id.droneConnected));
        modeButton = (ImageView) findViewById(R.id.switch_mode_icon);
    }

    private void updateInterface() {
        if (!interfaceSetup) {
            setupInterfaceUpdate();
        }
        runOnUiThread(new Runnable() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                String currentSSID = wifiManager.getConnectionInfo().getSSID();
                if (!currentSSID.equals(lastWifi) && wifiManager.getConnectionInfo().getSSID().contains("Phantom")) {
                    lastWifi = wifiManager.getConnectionInfo().getSSID();
                    connectIcon.setImageDrawable(getDrawable(R.drawable.wifi_connected_small));
                    connectText.setText("connected");
                } else if (!wifiManager.getConnectionInfo().getSSID().contains("Phantom")) {
                    connectText.setText("connect");
                    connectIcon.setImageDrawable(getDrawable(R.drawable.wifi_not_connected_small));
                }
                int mode = DroneState.getMode();
                if (mode != lastMode && mode == DroneState.DIRECT_MODE) {
                    modeButton.setImageDrawable(getDrawable(R.drawable.direct_small));
                    switchModeText.setText("direct");
                    currentModeText.setText("DIRECT");
                    lastMode = mode;
                } else if (mode != lastMode && mode == DroneState.WAYPOINT_MODE) {
                    modeButton.setImageDrawable(getDrawable(R.drawable.map_pin_small));
                    switchModeText.setText("waypoint");
                    currentModeText.setText(DroneState.flightMode.name());
                    lastMode = mode;
                }

                double currentLat = DroneState.getLatitude();
                double currentLong = DroneState.getLongitude();

                if (currentLat != lastLat || currentLong != lastLong) {
                    lastLat = currentLat;
                    lastLong = currentLong;
                    currentLocation.setText(LocationHelper.formatForDisplay(currentLat, currentLong));

                }
                double currentTargetLat = GroundStation.getCurrentTarget().latitude;
                double currentTargetLong = GroundStation.getCurrentTarget().longitude;
                if (currentTargetLat != lastTargetLat || currentTargetLong != lastTargetLong) {
                    lastTargetLat = currentTargetLat;
                    lastTargetLong = currentTargetLong;
                    targetLocation.setText(
                            LocationHelper.formatForDisplay(
                                    currentTargetLat,
                                    currentTargetLong));
                }

                boolean droneConnected = DroneState.droneConnected;
                if (lastConnected != droneConnected) {
                    lastConnected = droneConnected;
                    droneConnectedText.setText(String.valueOf(droneConnected));
                }
//                if (GroundStation.executingController()) {
//                    ((TextView) findViewById(R.id.pid_angle)).setText("" + GroundStation.getAngularController().getLastAction());
//                    ((TextView) findViewById(R.id.pid_error)).setText("" + GroundStation.getAngularController().getLastError());
//                }

//                if (LitterApplication.devMode) {
//                    CPreview.setImageBitmap(ImageProcessing.getCVPreview());
//                }

//                if (currentPhotoPoints != null && currentPhotoPoints.size() > 0) {
//                    findViewById(R.id.remaining_items).setVisibility(View.VISIBLE);
//                    //TODO: UPDATE ON EACH PICTURE TAKEN
//                } else {
//                    findViewById(R.id.remaining_items).setVisibility(View.GONE);
//                }

                //TODO: UPDATE BATTERY. NEED TO KNOW WTF REMAINPOWER IS FROM ADAM
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

    public void setWantResults(boolean wantResults) {
        this.wantResults = wantResults;
    }

    public boolean isWantResults() {
        return wantResults;
    }

    private boolean wantResults = false;

    public void setupWifi() {
        if (wifiManager.getConnectionInfo().getSSID().startsWith("Phantom")) {
            MessageHandler.d("Drone already connected");
        } else {
            wantResults = true;
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

    public View.OnClickListener getStartTrackListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ImageProcessing.isTracking()) {
                    angularController = new AngularController();
                    angularController.startExecutionLoop();
                } else {
                    ImageProcessing.startTrackingObject();
                }
            }
        };
    }

    public View.OnClickListener getStopTrackListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (angularController != null) {
                    angularController.stopExecutionLoop();
                }
                ImageProcessing.stopTrackingObject();
            }
        };
    }


    private void setOnClickListeners() {
        //Main page
        findViewById(R.id.go_home_button).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.home_button).setOnClickListener(getHomeButtonListener());
        findViewById(R.id.map_button).setOnClickListener(setRegionClickListener());
        findViewById(R.id.start_button).setOnClickListener(getStartSurveyListener());
        findViewById(R.id.switch_mode_button).setOnClickListener(getSwitchModeListener());
        findViewById(R.id.DJI_camera_surfaceview).setOnClickListener(getCameraViewListener());
        findViewById(R.id.connect_button).setOnClickListener(getWifiClickListener());

        //Dev toggle
        if (LitterApplication.devMode) {
            findViewById(R.id.CVPreview).setOnClickListener(getCameraViewListener());
            findViewById(R.id.button_special_camera).setOnClickListener(getDevButtonListener());

            //Dev stuff
            findViewById(R.id.button_special_1).setOnClickListener(getStartTrackListener());
            findViewById(R.id.button_special_2).setOnClickListener(getStopTrackListener());
            findViewById(R.id.button_special_3).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_imgproc_1).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_imgproc_2).setOnClickListener(getDevButtonListener());
            findViewById(R.id.button_imgproc_3).setOnClickListener(getDevButtonListener());
        }
    }
}