package com.jarone.litterary.activities;

import android.graphics.Color;
import android.os.Bundle;
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

/**
 * Created by vic on 11/9/15.
 */


public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap droneMap;
    private Polygon currentPolygon;
    private ArrayList polyPoints = new ArrayList();
    private ArrayList markers = new ArrayList();
    PolygonOptions rectOptions = new PolygonOptions().strokeWidth(15).fillColor(Color.parseColor("#90FF0000")).strokeColor(Color.parseColor("#90FF0000"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity_layout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        findViewById(R.id.button_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (polyPoints.size() > 0) {
                    polyPoints.remove(polyPoints.size() - 1);
                    ((Marker) markers.get(markers.size() - 1)).remove();
                    markers.remove(markers.size() - 1);
                }
                currentPolygon.setPoints(polyPoints);
            }
        });
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        this.droneMap = map;
        LatLng defaultLocation = new LatLng(43.472, -80.54);

        droneMap.setMyLocationEnabled(true);
        droneMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 13));

        droneMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (currentPolygon != null) {
                    currentPolygon.remove();
                }
                polyPoints.add(latLng);
                currentPolygon = map.addPolygon(new PolygonOptions().strokeWidth(2).fillColor(Color.parseColor("#50FF0000")).addAll(polyPoints));
                Marker marker = droneMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                markers.add(marker);

            }
        });

        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

                for (Object latLng : polyPoints) {
                    LatLng point = (LatLng) latLng;

                    if (marker.getPosition().equals(point)) {
                        polyPoints.remove(point);
                        markers.remove(marker);
                        return;
                    }

                }
                markers.remove(marker);
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                currentPolygon.remove();
                polyPoints.add(marker.getPosition());
                currentPolygon = map.addPolygon(new PolygonOptions().strokeWidth(2).fillColor(Color.parseColor("#50FF0000")).addAll(polyPoints));
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                markers.add(marker);

            }
        });
    }
}
