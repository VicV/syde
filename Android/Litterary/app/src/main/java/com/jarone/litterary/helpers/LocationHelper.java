package com.jarone.litterary.helpers;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.drone.DroneState;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by Adam on 2015-11-25.
 */
public class LocationHelper {
    public static float distanceBetween(LatLng location1, LatLng location2) {
        float[] results = new float[1];
        Location.distanceBetween(location1.latitude, location1.longitude, location2.latitude, location2.longitude, results);
        return results[0];
    }

    public static float distanceBetweenLat(LatLng location1, LatLng location2) {
        float[] results = new float[1];
        Location.distanceBetween(location1.latitude, DroneState.getLongitude(), location2.latitude, DroneState.getLongitude(), results);
        return results[0];
    }

    public static float distanceBetweenLong(LatLng location1, LatLng location2) {
        float[] results = new float[1];
        Location.distanceBetween(DroneState.getLatitude(), location1.longitude, DroneState.getLatitude(), location2.longitude, results);
        return results[0];
    }

    public static String formatForDisplay(LatLng location) {
        DecimalFormat df = new DecimalFormat("#.########");
        return df.format(location.latitude) + ", " + df.format(location.longitude);
    }

    public static String formatForDisplay(double latitude, double longitude) {
        return formatForDisplay(new LatLng(latitude, longitude));
    }

    /**
     * VERY BAD n^2 removal of duplicate LatLngs. Rewrite if there's time
     * @param initial
     * @return
     */
    public static ArrayList<LatLng> removeDuplicates(ArrayList<LatLng> initial) {
        int threshold = 2;
        ArrayList<LatLng> deduped = new ArrayList<>();
        for (int i = 0; i < initial.size(); i++) {
            boolean duplicate = false;
            for (int a = 0; a < initial.size(); a++) {
                if (i != a && distanceBetween(initial.get(i), initial.get(a)) < threshold) {
                    duplicate = true;
                }
            }
            if (!duplicate) {
                deduped.add(initial.get(i));
            }
        }
        return deduped;
    }
}

