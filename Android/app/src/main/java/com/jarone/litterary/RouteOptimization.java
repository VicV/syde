package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;

import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.features2d.KeyPoint;

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


    private static ArrayList<LatLng> getPhotoPoints(ArrayList<LatLng> originalArray, float altitude) {
        Polygon.Builder builder = new Polygon.Builder();
        ArrayList<Point> points = new ArrayList();

        boolean longNeg;
        boolean latNeg;

        double stepSize = 0.0001;

        if (altitude == -1) {
            altitude = 4;
        }

        //x-distance between image capture points
        double distX = (altitude * Math.tan(Math.toRadians(60)) * 2) / 10000;

        //y-distance between image capture points
        double distY = (altitude * Math.tan(Math.toRadians(42.5)) * 2) / 10000;

        int m = originalArray.size();

        //array for edge of polygon initialized
        double[] xv = new double[m + 1];

        //ditto, but for y
        double[] yv = new double[m + 1];

        double xi;
        double yi;

        ArrayList<LatLng> GPS = new ArrayList<>();

        // inputting all the vertices of polygon P4 to the xv and yv array
        double maxLat = Double.NEGATIVE_INFINITY;
        double maxLong = Double.NEGATIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY;
        double minLong = Double.POSITIVE_INFINITY;
        for (int i = 0; i < m; i++) {
            xi = originalArray.get(i).latitude;

            if (xi > maxLat) {
                maxLat = xi;
            }
            if (xi < minLat) {
                minLat = xi;
            }
            xv[i] = xi;
            yi = originalArray.get(i).longitude;
            if (yi > maxLong) {
                maxLong = yi;
            }
            if (yi < minLong) {
                minLong = yi;
            }
            yv[i] = yi;
        }

        longNeg = minLong < 0;
        latNeg = minLat < 0;

        maxLat = latNeg ? maxLat + (-minLat) : maxLat;
        maxLong = longNeg ? maxLong + (-minLong) : maxLong;


        //close off xv with repeat of first point
        xv[m] = originalArray.get(0).latitude;

        //close off yv with repeat of first point
        yv[m] = originalArray.get(0).longitude;


        for (LatLng pt : originalArray) {
            Point p = new Point((latNeg ? pt.latitude + (-minLat) : pt.latitude), (longNeg ? pt.longitude + (-minLong) : pt.longitude));
            points.add(p);
            builder.addVertex(p);
        }


        Polygon polygon = builder.close().build();

        double x = points.get(0).x;

        double y = points.get(0).y;

        GPS.add(new LatLng(latNeg ? x + minLat : x, longNeg ? y + minLong : y));

        // break counter for upcoming for loop
        double y1 = 0;
        int count = 0;
        int mFlag = 0;
        while (polygon.contains(new Point(x, y))) {
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
            while (polygon.contains(new Point(x, y))) {
                if (polygon.contains(new Point(x, y))) {
                    GPS.add(new LatLng(latNeg ? x + minLat : x, longNeg ? y + minLong : y));
                }
                x = x + distX;
            }
            //the out clause for the last case
            if (mFlag == 2)
                break;

            //case where you reach the far right-most point, iterate back until
            //you are in region
            if (!polygon.contains(new Point(x, y))) {
                while (!polygon.contains(new Point(x, y))) {
                    x = x - stepSize;
                }
                GPS.add(new LatLng(latNeg ? x + minLat : x, longNeg ? y + minLong : y));
            }
            //iterate upwards to the next row of points
            x = points.get(0).x;
            y = y + distY;

            //iterate left until you find the left-most edge of the space
            if (polygon.contains(new Point(x, y))) {
                while (polygon.contains(new Point(x, y))) {
                    if (y > maxLong) {
                        y = y1;
                        mFlag = 1;
                        if (!polygon.contains(new Point(x, y))) {
                            break;
                        }
                    }
                    x = x - stepSize;
                }
            }

            //if you iterate up, and it's outside the area, iterate right until
            //you find inside the polygon
            if (!polygon.contains(new Point(x, y))) {
                while (!polygon.contains(new Point(x, y))) {
                    if (y > maxLong) {
                        y = y1;
                        mFlag = 1;
                        if (polygon.contains(new Point(x, y))) {
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


    static boolean polygonContainsPointx(double[] xv, double[] yv, double x, double y) {
        int rows = xv.length;
        int i, j;
        boolean c = false;
        for (i = 0, j = rows - 1; i < rows; j = i++) {
            if (((yv[i] > y) != (yv[j] > y)) &&
                    (x <= (xv[j] - xv[i]) * (y - yv[i]) / (yv[j] - yv[i]) + xv[i]))
                c = !c;
        }
        return c;
    }

    public static boolean polygonContainsPoint(double[] xvv, double[] yvv, double xx, double yy) {
//        Polygon polygon = Polygon.Builder().
        return true;
    }

}
