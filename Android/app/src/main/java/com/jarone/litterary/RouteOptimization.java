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

//    public static double[][] array = null;


    public static LatLng[] createOptimizedSurveyRoute(LatLng[] points, float altitude) {

//        try {
        final String propertyName = "Property";
        String oldProperty = System.getProperty("java.version");
        System.setProperty(propertyName, "1.7.0_79");
        String newProperty = System.getProperty("java.version");
        ArrayList<LatLng> latLngs = new ArrayList<>();
        if (points != null && points.length > 0) {
            latLngs = (ArrayList<LatLng>) Arrays.asList(points);
        }

//            Nd4jBackend.load();
//        } catch (Nd4jBackend.NoAvailableBackendException e) {
//            //
//        }
        latLngs.add(new LatLng(20, 0));
        latLngs.add(new LatLng(40, 0));
        latLngs.add(new LatLng(80, 30));
        latLngs.add(new LatLng(40, 90));
        latLngs.add(new LatLng(0, 40));
        latLngs.add(new LatLng(0, 20));

//        array = new double[][]{{20, 40, 80, 40, 0, 0}, {0, 0, 30, 90, 40, 0}};
        ArrayList<LatLng> picturePoints = getPhotoPoints(latLngs);
        ArrayList<LatLng> orderedPoints = optimizePhotoRoute(picturePoints);

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
//        for (LatLng point : points) {
//            if (LocationHelper.distanceBetween(point, DroneState.getLatLng()) > GroundStation.BOUNDARY_RADIUS) {
//                return false;
//            }
//        }
        return true;
    }


    private static ArrayList<LatLng> getPhotoPoints(ArrayList<LatLng> array) {

        // height above the ground
        int height = 4;

        //x-distance between image capture points
        double distX = height * Math.tan(60) * 2;

        //y-distance between image capture points
        double distY = height * Math.tan(42.5) * 2;

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
            if (Math.abs(xi) > maxLat) {
                maxLat = xi;
            }
            xv[i] = xi;
            yi = array.get(i).longitude;
            if (Math.abs(yi) > maxLong) {
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
        while (polygonContainsPoint(array, x, y)) {
            //last loop break clause
            if (mFlag == 1)
                mFlag = 2;

            //case where next y point is greater than the top of the polygon
            if ((y + distY) > maxLong) {
                y1 = y + distY;
                while (y1 > maxLong) {
                    //iterate down until you are in the polygon
                    y = y1 - 0.1;
                }
            }
            //general case where the x and y test value is in the polygon
            while (polygonContainsPoint(array, x, y)) {
                if (polygonContainsPoint(array, x, y)) {
                    GPS.add(new LatLng(x, y));
                }
                x = x + distX;
            }
            //the out clause for the last case
            if (mFlag == 2)
                break;

            //case where you reach the far right-most point, iterate back until
            //you are in region
            if (!polygonContainsPoint(array, x, y)) {
                while (!polygonContainsPoint(array, x, y)) {
                    x = x - 0.1;
                }
                GPS.add(new LatLng(x, y));
            }
            //iterate upwards to the next row of points
            x = array.get(1).latitude;
            y = y + distY;

            //iterate left until you find the left-most edge of the space
            if (polygonContainsPoint(array, x, y)) {
                while (polygonContainsPoint(array, x, y)) {
                    if (y > maxLong) {
                        y = y1;
                        mFlag = 1;
                    }
                    x = x - 0.1;
                }
            }

            //if you iterate up, and it's outside the area, iterate right until
            //you find inside the polygon
            if (!polygonContainsPoint(array, x, y)) {
                while (polygonContainsPoint(array, x, y)) {
                    if (y > maxLong) {
                        y = y1;
                        mFlag = 1;
                    }
                    x = x + 0.1;
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


    /**
     * Determine if our polygon contains the point at hand.
     *
     * @param points Vertices of our polygon
     * @param x      x value
     * @param y      y value
     * @return boolean whether or not in polygon
     */
    public static boolean polygonContainsPoint(ArrayList<LatLng> points, double x, double y) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            if ((points.get(i).latitude > y) != (points.get(j).latitude > y) &&
                    (x < (points.get(j).longitude - points.get(i).longitude) * (y - points.get(i).latitude) / (points.get(j).latitude - points.get(i).latitude) + points.get(i).longitude)) {
                result = !result;
            }
        }
        return result;
    }

    static boolean pnpoly(double[] vertx, double[] verty, double testx, double testy) {
        int nvert = vertx.length;
        int i, j;
        boolean c = false;
        for (i = 0, j = nvert - 1; i < nvert; j = i++) {
            if (((verty[i] > testy) != (verty[j] > testy)) &&
                    (testx < (vertx[j] - vertx[i]) * (testy - verty[i]) / (verty[j] - verty[i]) + vertx[i]))
                c = !c;
        }
        return c;
    }
}
