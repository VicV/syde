/*
* TourManager.java
* Holds the cities of a tour
*/

package com.jarone.litterary.optimization;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class RouteManager {

    // Holds our photoPoints
    private static ArrayList destinationPoints = new ArrayList<PhotoPoint>();

    // Adds a destination photoPoint
    public static void addPhotoPoint(PhotoPoint photoPoint) {
        destinationPoints.add(photoPoint);
    }

    // Adds a destination photoPoint
    public static void addAllPhotoPoints(ArrayList<LatLng> latlngs) {
        for (LatLng pt : latlngs) {
            destinationPoints.add(new PhotoPoint(pt.latitude, pt.longitude));
        }
    }

    // Get a photoPoint
    public static PhotoPoint getPhotoPoint(int index) {
        return (PhotoPoint) destinationPoints.get(index);
    }

    // Get the number of destination photoPoints
    public static int numberOfPhotoPoints() {
        return destinationPoints.size();
    }


}