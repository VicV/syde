package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;

/**
 * Created by Adam on 2015-11-23.
 */
public class RouteOptimization {
    public static LatLng[] createOptimizedSurveyRoute(LatLng[] points, float altitude) {
        if (validateBoundary(points)) {
            //TODO: put Jordan's code here
            LatLng[] route = new LatLng[20];
            return points;
        } else {
            MessageHandler.d("Boundary Points Are Too Far From Drone!");
            return new LatLng[0];
        }
    }

    private static boolean validateBoundary(LatLng[] points) {
        for (int i = 0; i < points.length; i++) {
            if (LocationHelper.distanceBetween(points[i], DroneState.getLatLng()) > GroundStation.BOUNDARY_RADIUS) {
                return false;
            }
        }
        return true;
    }
}
