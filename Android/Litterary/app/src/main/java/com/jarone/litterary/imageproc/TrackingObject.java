package com.jarone.litterary.imageproc;


import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.helpers.LocationHelper;

import org.opencv.core.Point;

/**
 * Created by Adam on 2016-02-23.
 */
public class TrackingObject {

    private double size;
    private Point position;
    private LatLng cameraLocation;
    private double cameraAltitude;

    public TrackingObject(Point position, double size, LatLng location, double altitude) {
        this.position = position;
        this.size = size;
        this.cameraLocation = location;
        this.cameraAltitude = altitude;
    }

    public TrackingObject predictPositionAndSize(LatLng newLocation, double newAltitude) {
        double lat_dist = LocationHelper.distanceBetweenLat(newLocation, cameraLocation);
        double long_dist = LocationHelper.distanceBetweenLong(newLocation, cameraLocation);
        lat_dist = ImageProcessing.metresToPixels(lat_dist, newAltitude);
        long_dist = ImageProcessing.metresToPixels(long_dist, newAltitude);
        Point predictPoint = new Point(position.x + long_dist, position.y + lat_dist);
        double predictSize = cameraAltitude / newAltitude * size;
        return new TrackingObject(predictPoint, predictSize, newLocation, newAltitude);
    }


    public LatLng getCameraLocation() {
        return cameraLocation;
    }

    public Point getPosition() { return position; }

    public double getSize() { return size; }
}
