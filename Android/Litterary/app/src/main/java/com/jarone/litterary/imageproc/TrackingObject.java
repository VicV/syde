package com.jarone.litterary.imageproc;

import android.graphics.Point;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.helpers.LocationHelper;

/**
 * Created by Adam on 2016-02-23.
 */
public class TrackingObject {

    private double previousSize;
    private Point previousPosition;
    private LatLng previousLocation;
    private double previousAltitude;

    public TrackingObject(Point position, double size, LatLng location, double altitude) {
        previousPosition = position;
        previousSize = size;
        previousLocation = location;
        previousAltitude = altitude;
    }

    public TrackingObject predictPositionAndSize(LatLng newLocation, double newAltitude) {
        double lat_dist = LocationHelper.distanceBetweenLat(newLocation, previousLocation);
        double long_dist = LocationHelper.distanceBetweenLong(newLocation, previousLocation);
        Point predictPoint = new Point(0, 0);
        double predictSize = 0;
        return new TrackingObject(predictPoint, predictSize, newLocation, newAltitude);
    }


    public LatLng getPreviousLocation() {
        return previousLocation;
    }
}
