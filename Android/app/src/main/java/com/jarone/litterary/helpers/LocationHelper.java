package com.jarone.litterary.helpers;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

/**
 * Created by Adam on 2015-11-25.
 */
public class LocationHelper {
    public static float distanceBetween(LatLng location1, LatLng location2) {
        float[] results = new float[1];
        Location.distanceBetween(location1.latitude, location1.longitude, location2.latitude, location2.longitude, results);
        return results[0];
    }

    public static String formatForDisplay(LatLng location) {
        DecimalFormat df = new DecimalFormat("#.########");
        return df.format(location.latitude) + ", " + df.format(location.longitude);
    }

    public static String formatForDisplay(double latitude, double longitude) {
        return formatForDisplay(new LatLng(latitude, longitude));
    }
}

