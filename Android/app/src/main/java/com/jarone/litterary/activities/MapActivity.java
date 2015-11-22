package com.jarone.litterary.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.jarone.litterary.R;

import java.util.ArrayList;
import java.util.HashMap;

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

    /**
     * A list of all the lat long points that are the verticies of the polygon
     **/
    private ArrayList polyPoints = new ArrayList<>();

    /**
     * A list of all the markers which lie on the verticies of the polygon
     **/
    private ArrayList markers = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity_layout);

        //Gets the map loaded.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Undo -- removes the last point and marker.
        // TODO: Undo drags

        findViewById(R.id.button_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (polyPoints.size() > 0) {
                    polyPoints.remove(polyPoints.size() - 1);
                    //Remove the marker from the map and then from our array.
                    ((Marker) markers.get(markers.size() - 1)).remove();
                    markers.remove(markers.size() - 1);
                    currentPolygon.setPoints(polyPoints);
                }
            }
        });

        //The set button finishes the activity.
        findViewById(R.id.button_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
        droneMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14));

        //This is the pin placing mechanic.
        droneMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
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
        });

        //Dragging mechanic of the polygons.
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            //Tracks if its the FIRST pass of the drag loop.
            boolean first = true;

            @Override
            public void onMarkerDragStart(Marker marker) {

                LatLng thisPoint = null;
                float lowest = -1;

                //There's no nice mechanic to finding what point is being dragged.
                //This iterates through all of our known points until it finds the closest one to the marker.
                //Then, it deletes that point, and then the related marker.
                for (Object latLng : polyPoints) {
                    LatLng point = (LatLng) latLng;
                    float[] results = new float[1];
                    Location.distanceBetween(marker.getPosition().latitude, marker.getPosition().longitude, point.latitude, point.longitude, results);
                    if (lowest == -1 || results[0] < lowest) {
                        lowest = results[0];
                        thisPoint = point;
                    }

                }
                polyPoints.remove(thisPoint);
                markers.remove(marker);
            }

            @Override
            public void onMarkerDrag(Marker marker) {
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
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                markers.add(marker);
                first = true;
            }
        });
    }

    @Override
    public void finish() {
        //LatLng is parcelable. So just put all the points into an array and send it back to the calling activity.
        //TODO: Store these.
        Intent data = new Intent();
        LatLng[] p = new LatLng[markers.size()];

        for (int i = 0; i < markers.size(); i++) {
            Marker marker = ((Marker) markers.get(i));
            p[i] = marker.getPosition();
        }
        data.putExtra("points", p);
        if (getParent() == null) {
            setResult(MainActivity.POINTS_RESULT_CODE, data);
        } else {
            getParent().setResult(MainActivity.POINTS_RESULT_CODE, data);
        }
        super.finish();
    }

}
