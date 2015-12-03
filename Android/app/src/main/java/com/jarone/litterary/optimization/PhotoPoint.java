/*
* PhotoPoint.java
* Models a photoPoint
*/

package com.jarone.litterary.optimization;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.helpers.LocationHelper;

public class PhotoPoint {
    double latitude;
    double longitude;

    // Constructs a photoPoint at chosen latitude, longitude location
    public PhotoPoint(double x, double longitude) {
        this.latitude = x;
        this.longitude = longitude;
    }

    // Gets photoPoint's latitude coordinate
    public double getLatitude() {
        return this.latitude;
    }

    // Gets photoPoint's longitude coordinate
    public double getLongitude() {
        return this.longitude;
    }

    // Gets the distance to given photoPoint
    public double distanceTo(PhotoPoint photoPoint) {

        return LocationHelper.distanceBetween(new LatLng(latitude, longitude), new LatLng(photoPoint.latitude, photoPoint.longitude));
    }

    @Override
    public String toString() {
        return getLatitude() + ", " + getLongitude();
    }
}