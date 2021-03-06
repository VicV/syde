package com.jarone.litterary.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jarone.litterary.R;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by vic on 11/9/15.
 */


public class MapActivity extends FragmentActivity implements OnMapReadyCallback {


    /**
     * The actual instance of the map
     **/
    private GoogleMap droneMap;

    /**
     * The polygon area
     **/
    private Polygon currentPolygon;

    private Polyline routeLine;

    /**
     * A list of all the lat long points that are the verticies of the polygon
     **/
    private ArrayList<LatLng> polyPoints = new ArrayList<>();

    /**
     * A list of all the markers which lie on the verticies of the polygon
     **/
    private ArrayList<Marker> markers = new ArrayList();

    private ArrayList<Marker> photoMarkers = new ArrayList();

    private ArrayList<LatLng> photoPoints = new ArrayList();

    /**
     * The center location of the circular boundary inside which points can be placed
     */
    private LatLng boundaryCenter;

    private boolean returnWithResults = true;

    private int BOUNDARY_COLOUR = Color.argb(120, 10, 192, 192);

    private boolean resetMode = false;

    private boolean onlyPath = false;

    private Context c;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity_layout);
        c = this;
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        //Gets the map loaded.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Bundle bundle = getIntent().getExtras();

        if (bundle.get("polygon") != null) {
            Parcelable[] points = (Parcelable[]) bundle.get("polygon");
            polyPoints = new ArrayList(Arrays.asList(Arrays.copyOf(points, points.length, LatLng[].class)));
            if (bundle.get("picturePoints") != null) {
                photoPoints = (ArrayList<LatLng>) bundle.get("picturePoints");
                if (photoPoints != null && photoPoints.size() > 1) {
                    findViewById(R.id.undo_button).setVisibility(View.GONE);
                    findViewById(R.id.altitude_button).setVisibility(View.GONE);
                    findViewById(R.id.set_button).setVisibility(View.GONE);
                    findViewById(R.id.path_button).setVisibility(View.VISIBLE);
                    resetMode = true;
                }
            }
        }

        // Undo -- removes the last point and marker.
        // TODO: Undo drags

        findViewById(R.id.undo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (polyPoints.size() > 0) {
                    polyPoints.remove(polyPoints.size() - 1);
                    //Remove the marker from the map and then from our array.
                    markers.get(markers.size() - 1).remove();
                    markers.remove(markers.size() - 1);
                    currentPolygon.setPoints(polyPoints);
                }
            }
        });

        final EditText altitudeText = (EditText) findViewById(R.id.altitude_input);
        altitudeText.getBackground().mutate().setColorFilter(getResources().getColor(R.color.textcolor), PorterDuff.Mode.SRC_ATOP);

        altitudeText.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
    /* When focus is lost check that the text field
    * has valid values.
    */
                if (!hasFocus && !altitudeText.getText().toString().endsWith("m")) {
                    altitudeText.setCursorVisible(false);
                    altitudeText.setText(altitudeText.getText().toString() + "m");
                }
            }
        });

        altitudeText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (event != null && !event.isShiftPressed()) {
                                // the user is done typing.
                                if (!altitudeText.getText().toString().endsWith("m")) {
                                    altitudeText.setCursorVisible(false);
                                    altitudeText.setText(altitudeText.getText().toString() + "m");
                                }
                                return true; // consume.
                            }
                        }
                        return false; // pass on to other listeners.
                    }
                });

        //The set button finishes the activity.
        findViewById(R.id.set_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (polyPoints.size() < 3) {
                    Toast.makeText(c, "Please create a polygon with at least three points", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });

        findViewById(R.id.button_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                droneMap.clear();

                droneMap.addCircle(new CircleOptions()
                        .center(boundaryCenter)
                        .radius(GroundStation.BOUNDARY_RADIUS)
                        .fillColor(BOUNDARY_COLOUR)
                        .strokeWidth(2)
                );

                droneMap.addMarker(new MarkerOptions()
                        .position(boundaryCenter)
                        .icon(BitmapDescriptorFactory.fromAsset("drone.png"))
                        .anchor(0.5f, 0.5f)
                );

                if (routeLine != null) {
                    routeLine.remove();
                }

                currentPolygon.remove();


                if (markers != null) {
                    markers.clear();
                }
                for (Marker m : markers) {
                    m.remove();
                }
                if (photoMarkers != null) {
                    for (Marker m : photoMarkers) {
                        m.remove();
                    }
                    photoMarkers.clear();
                }

                if (photoPoints != null) {
                    photoPoints.clear();
                }

                if (polyPoints != null) {
                    polyPoints.clear();
                }

                findViewById(R.id.undo_button).setVisibility(View.VISIBLE);
                findViewById(R.id.set_button).setVisibility(View.VISIBLE);
                findViewById(R.id.altitude_button).setVisibility(View.VISIBLE);
                findViewById(R.id.path_button).setVisibility(View.GONE);

                resetMode = false;
            }
        });

        findViewById(R.id.path_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!onlyPath) {
                    currentPolygon.remove();
                    for (Marker m : markers) {
                        m.remove();
                    }
                    for (Marker m : photoMarkers) {
                        m.remove();
                    }
                    ((TextView) findViewById(R.id.path_text)).setText("all markers");
                    ((ImageView) findViewById(R.id.path_icon)).setImageDrawable(getDrawable(R.drawable.map_pin_small));
                    onlyPath = true;
                } else {
                    if (polyPoints != null && polyPoints.size() > 0) {
                        currentPolygon = droneMap.addPolygon(new PolygonOptions().strokeWidth(2).fillColor(Color.parseColor("#50FF0000")).addAll(polyPoints));
                    }

                    if (polyPoints != null) {
                        for (LatLng latLng : polyPoints) {
                            markers.add(droneMap.addMarker(new MarkerOptions().position(latLng).draggable(true)));
                        }
                    }

                    if (photoPoints != null && photoPoints.size() > 1) {
                        for (LatLng latLng : photoPoints) {
                            photoMarkers.add(droneMap.addMarker(new MarkerOptions().position(latLng).draggable(false).icon(BitmapDescriptorFactory.fromAsset("red-star.png")).anchor(0.1f, 0.1f)));
                            routeLine = droneMap.addPolyline(new PolylineOptions().addAll(photoPoints)
                                    .width(2)
                                    .color(Color.RED));
                        }
                    }
                    onlyPath = false;
                    ((TextView) findViewById(R.id.path_text)).setText("path only");
                    ((ImageView) findViewById(R.id.path_icon)).setImageDrawable(getDrawable(R.drawable.path_icon_small));
                }
            }
        });
    }

    //This is when the map loads
    @Override
    public void onMapReady(final GoogleMap map) {
        this.droneMap = map;

        //Default map location is to be E5
        LatLng defaultLocation = new LatLng(43.472, -80.54);

        droneMap.setMyLocationEnabled(true);

        if (DroneState.hasValidLocation()) {
            droneMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DroneState.getLatLng(), 16));
        } else {
            droneMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 16));
        }

        if (DroneState.getLatitude() != 0) {
            boundaryCenter = DroneState.getLatLng();
        } else {
            boundaryCenter = defaultLocation;
        }

        droneMap.addCircle(new CircleOptions()
                .center(boundaryCenter)
                .radius(GroundStation.BOUNDARY_RADIUS)
                .fillColor(BOUNDARY_COLOUR)
                .strokeWidth(2)
        );

        droneMap.addMarker(new MarkerOptions()
                .position(boundaryCenter)
                .icon(BitmapDescriptorFactory.fromAsset("drone.png"))
                .anchor(0.5f, 0.5f)
        );

        if (polyPoints != null && polyPoints.size() > 0) {
            currentPolygon = map.addPolygon(new PolygonOptions().strokeWidth(2).fillColor(Color.parseColor("#50FF0000")).addAll(polyPoints));
        }

        if (polyPoints != null) {
            for (LatLng latLng : polyPoints) {
                markers.add(droneMap.addMarker(new MarkerOptions().position(latLng).draggable(true)));
            }
        }

        if (photoPoints != null && photoPoints.size() > 1) {
            for (LatLng latLng : photoPoints) {
                photoMarkers.add(droneMap.addMarker(new MarkerOptions().position(latLng).draggable(false).icon(BitmapDescriptorFactory.fromAsset("red-star.png")).anchor(0.1f, 0.1f)));
                routeLine = map.addPolyline(new PolylineOptions().addAll(photoPoints)
                        .width(2)
                        .color(Color.RED));
            }
        }

        //This is the pin placing mechanic.
        droneMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (!resetMode) {
                    //Check if the requested point is inside the boundary
                    if (LocationHelper.distanceBetween(latLng, boundaryCenter) > GroundStation.BOUNDARY_RADIUS) {
                        MessageHandler.d("Choose a point inside the boundary circle");
                        return;
                    }

                    //Erase the polygon because its about to be redrawn.
                    if (currentPolygon != null) {
                        currentPolygon.remove();
                    }

                    //Add a point at the location of the finger.
                    polyPoints.add(latLng);
                    //Redraw the map
                    currentPolygon = map.addPolygon(new PolygonOptions().strokeWidth(2).fillColor(Color.parseColor("#50FF0000")).addAll(polyPoints));
                    //Put a draggable marker at this vertex
                    Marker marker = droneMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                    //Store our marker.
                    markers.add(marker);
                }

            }
        });

        //Dragging mechanic of the polygons.
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            //Tracks if its the FIRST pass of the drag loop.
            boolean first = true;

            LatLng lastPosition = null;

            @Override
            public void onMarkerDragStart(Marker marker) {

                LatLng thisPoint = null;
                float lowest = -1;

                //There's no nice mechanic to finding what point is being dragged.
                //This iterates through all of our known points until it finds the closest one to the marker.
                //Then, it deletes that point, and then the related marker.
                for (Object latLng : polyPoints) {
                    LatLng point = (LatLng) latLng;
                    float distance = LocationHelper.distanceBetween(marker.getPosition(), point);
                    if (lowest == -1 || distance < lowest) {
                        lowest = distance;
                        thisPoint = point;
                    }

                }
                if (LocationHelper.distanceBetween(marker.getPosition(), boundaryCenter) < GroundStation.BOUNDARY_RADIUS) {
                    polyPoints.remove(thisPoint);
                    markers.remove(marker);
                }

            }

            @Override
            public void onMarkerDrag(Marker marker) {
                if (LocationHelper.distanceBetween(marker.getPosition(), boundaryCenter) < GroundStation.BOUNDARY_RADIUS) {
                    lastPosition = marker.getPosition();
                    //Erase the polygon because its about to be redrawn.
                    currentPolygon.remove();
                    //If its not the first pass, remove the last added point.
                    if (!first) {
                        polyPoints.remove(polyPoints.size() - 1);
                    } else {
                        first = false;
                    }

                    //Add our new point!
                    polyPoints.add(marker.getPosition());
                    currentPolygon = map.addPolygon(new PolygonOptions().strokeWidth(2).fillColor(Color.parseColor("#50FF0000")).addAll(polyPoints));
                } else {
                    if (lastPosition != null) {
                        marker.setPosition(lastPosition);
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (LocationHelper.distanceBetween(marker.getPosition(), boundaryCenter) < GroundStation.BOUNDARY_RADIUS) {
                    markers.add(marker);
                    first = true;
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        returnWithResults = false;
        super.onBackPressed();
    }

    @Override
    public void finish() {
        //LatLng is parcelable. So just put all the points into an array and send it back to the calling activity.
        //TODO: Store these.
        Intent data = new Intent();
        LatLng[] p = new LatLng[markers.size()];

        for (int i = 0; i < markers.size(); i++) {
            Marker marker = markers.get(i);
            p[i] = marker.getPosition();
        }
        data.putExtra("points", p);

        if (returnWithResults) {
            if (getParent() == null) {
                setResult(MainActivity.POINTS_RESULT_CODE, data);
            } else {
                getParent().setResult(MainActivity.POINTS_RESULT_CODE, data);
            }
        }

        super.finish();
    }

}
