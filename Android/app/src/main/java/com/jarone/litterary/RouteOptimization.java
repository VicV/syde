package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Adam on 2015-11-23.
 * <p/>
 * Optimizin routes.
 */
public class RouteOptimization {

    public static LatLng[] createOptimizedSurveyRoute(LatLng[] points, float altitude) {

        ArrayList<LatLng> latLngs = new ArrayList<>();

        if (points != null && points.length > 0) {
            latLngs = new ArrayList<>(Arrays.asList(points));
        }


        if (validateBoundary(points)) {

            ArrayList<LatLng> picturePoints = getPhotoPoints(latLngs, altitude);
            ArrayList<LatLng> orderedPoints = optimizePhotoRoute(picturePoints);

            LatLng[] route = new LatLng[20];
            return points;
        } else {
            MessageHandler.d("Boundary Points Are Too Far From Drone!");
            return new LatLng[0];
        }
    }


    private static boolean validateBoundary(LatLng[] points) {
        for (LatLng point : points) {
            if (LocationHelper.distanceBetween(point, DroneState.getLatLng()) > GroundStation.BOUNDARY_RADIUS) {
                return false;
            }
        }
        return true;
    }


    private static ArrayList<LatLng> getPhotoPoints(ArrayList<LatLng> array, float altitude) {

        double stepSize = 0.00001;

        if (altitude == -1) {
            altitude = 4;
        }

        //x-distance between image capture points
        double distX = (altitude * Math.tan(Math.toRadians(60)) * 2) / 100000;

        //y-distance between image capture points
        double distY = (altitude * Math.tan(Math.toRadians(42.5)) * 2) / 100000;

        int m = array.size();

        //array for edge of polygon initialized
        double[] xv = new double[m + 1];

        //ditto, but for y
        double[] yv = new double[m + 1];

        double xi;
        double yi;

        ArrayList<LatLng> GPS = new ArrayList<>();

        // inputting all the vertices of polygon P4 to the xv and yv array
        double maxLat = 0;
        double maxLong = 0;
        for (int i = 0; i < m; i++) {
            xi = array.get(i).latitude;
            if (Math.abs(xi) > Math.abs(maxLat)) {
                maxLat = xi;
            }
            xv[i] = xi;
            yi = array.get(i).longitude;
            if (Math.abs(yi) > Math.abs(maxLong)) {
                maxLong = yi;
            }
            yv[i] = yi;
        }

        //close off xv with repeat of first point
        xv[m] = array.get(0).latitude;

        //close off yv with repeat of first point
        yv[m] = array.get(0).longitude;

        double x = array.get(0).latitude;

        double y = array.get(0).longitude;

        GPS.add(new LatLng(x, y));

        // break counter for upcoming for loop
        double y1 = 0;
        int mFlag = 0;
        while (polygonContainsPoint(xv, yv, x, y)) {
            //last loop break clause
            if (mFlag == 1)
                mFlag = 2;

            //case where next y point is greater than the top of the polygon
            if ((y + distY) > maxLong) {
                y1 = y + distY;
                while (y1 > maxLong) {
                    //iterate down until you are in the polygon
                    y1 = y1 - stepSize;
                }
            }
            //general case where the x and y test value is in the polygon
            while (polygonContainsPoint(xv, yv, x, y)) {
                if (polygonContainsPoint(xv, yv, x, y)) {
                    GPS.add(new LatLng(x, y));
                }
                x = x + distX;
            }
            //the out clause for the last case
            if (mFlag == 2)
                break;

            //case where you reach the far right-most point, iterate back until
            //you are in region
            if (!polygonContainsPoint(xv, yv, x, y)) {
                while (!polygonContainsPoint(xv, yv, x, y)) {
                    x = x - stepSize;
                }
                GPS.add(new LatLng(x, y));
            }
            //iterate upwards to the next row of points
            x = array.get(1).latitude;
            y = y + distY;

            //iterate left until you find the left-most edge of the space
            if (polygonContainsPoint(xv, yv, x, y)) {
                while (polygonContainsPoint(xv, yv, x, y)) {
                    if (y > maxLong) {
                        y = y1;
                        mFlag = 1;
                        if (!polygonContainsPoint(xv, yv, x, y)) {
                            break;
                        }
                    }
                    x = x - stepSize;
                }
            }

            //if you iterate up, and it's outside the area, iterate right until
            //you find inside the polygon
            if (!polygonContainsPoint(xv, yv, x, y)) {
                while (!polygonContainsPoint(xv, yv, x, y)) {
                    if (y > maxLong) {
                        y = y1;
                        mFlag = 1;
                        if (polygonContainsPoint(xv, yv, x, y)) {
                            break;
                        }
                    }
                    x = x + stepSize;
                }
            }
        }

        return GPS;
    }

    private static ArrayList optimizePhotoRoute(ArrayList picturePoints) {
//        ArrayList<Double> longitudes = new ArrayList<>(), latitudes = new ArrayList<>();
//        int rows = picturePoints.rows();
//        for (int i = 1; i < rows-1; i++) {
//            longitudes.add(picturePoints.getDouble(i, 1));
//            latitudes.add(picturepoints.get(i).latitute );
//        }
//
//        int nStops = rows-1;

//        SalesmanSolver.solve(picturePoints);


        return picturePoints;
    }


    static boolean polygonContainsPoint(double[] xv, double[] yv, double x, double y) {
        int rows = xv.length;
        int i, j;
        boolean c = false;
        for (i = 0, j = rows - 1; i < rows; j = i++) {
            if (((yv[i] > y) != (yv[j] > y)) &&
                    (x < (xv[j] - xv[i]) * (y - yv[i]) / (yv[j] - yv[i]) + xv[i]))
                c = !c;
        }
        return c;
    }
}
