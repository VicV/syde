package com.jarone.litterary.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
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
    PolygonOptions rectOptions = new PolygonOptions().strokeWidth(2).fillColor(Color.parseColor("#80FF0000"));


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
                polyPoints.remove(polyPoints.size() - 1);
                currentPolygon.setPoints(polyPoints);
            }
        });


    }

    @Override
    public void onMapReady(final GoogleMap map) {
        this.droneMap = map;
        LatLng sydney = new LatLng(-33.867, 151.206);

        droneMap.setMyLocationEnabled(true);
        droneMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 13));

        droneMap.addMarker(new MarkerOptions()
                .title("Sydney")
                .snippet("The most populous city in Australia.")
                .position(sydney));


        droneMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (currentPolygon == null) {
                    rectOptions.add(latLng);
                    map.addPolygon(rectOptions);
                } else {
                    polyPoints.add(latLng);
                    rectOptions.addAll(polyPoints);
                    currentPolygon.remove();
                    currentPolygon = map.addPolygon(rectOptions);
                }
            }
        });
    }
}
