package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;

/**
 * Created by Adam on 2015-11-23.
 * <p/>
 * Optimizin routes.
 */
public class RouteOptimization {

    public static INDArray array = Nd4j.create(new double[]{20, 0, 40, 0, 80, 30, 40, 90, 0, 40, 0, 0}, new int[]{2, 6});


    public static LatLng[] createOptimizedSurveyRoute(LatLng[] points, float altitude) {
        if (validateBoundary(points)) {
            //TODO: put Jordan's code here

            INDArray picturePoints = getPhotoPoints(array);
            INDArray orderedPoints = optimizePhotoRoute(picturePoints);

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


    private static INDArray getPhotoPoints(INDArray array) {

        // height above the ground
        int height = 4;

        //x-distance between image capture points
        double distX = height * Math.tan(60) * 2;

        //y-distance between image capture points
        double distY = height * Math.tan(42.5) * 2;

        int m = array.rows();

        //array for edge of polygon initialized
        INDArray xv = Nd4j.zeros(1, m + 1);

        //ditto, but for y
        INDArray yv = Nd4j.zeros(1, m + 1);

        double xi;
        double yi;

        INDArray GPS = Nd4j.emptyLike(Nd4j.create(new double[]{1, 2}));

        // inputting all the vertices of polygon P4 to the xv and yv array
        for (int i = 1; i < m; i++) {
            xi = array.getDouble(i, 1);
            xv.put(1, i, xi);
            yi = array.getDouble(i, 2);
            yv.put(1, i, yi);
        }

        //close off xv with repeat of first point
        xv.put(1, m + 1, array.getDouble(1, 1));
        //close off yv with repeat of first point
        yv.put(1, m + 1, array.getDouble(1, 2));
        double x = array.getDouble(1, 1);
        double y = array.getDouble(1, 2);
        GPS = Nd4j.vstack(GPS, Nd4j.create(new double[]{x, y}));

        // break counter for upcoming for loop
        double y1 = 0;
        int mFlag = 0;
        while (polygonContainsPoint(array, x, y)) {
            //last loop break clause
            if (mFlag == 1)
                mFlag = 2;

            //case where next y point is greater than the top of the polygon
            if ((y + distY) > yv.maxNumber().doubleValue()) {
                y1 = y + distY;
                while (y1 > yv.maxNumber().doubleValue()) {
                    //iterate down until you are in the polygon
                    y = y1 - 0.1;
                }
            }
            //general case where the x and y test value is in the polygon
            while (polygonContainsPoint(array, x, y)) {
                if (polygonContainsPoint(array, x, y)) {
                    GPS = Nd4j.vstack(GPS, Nd4j.create(new double[]{x, y}));
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
                GPS = Nd4j.vstack(GPS, Nd4j.create(new double[]{x, y}));
            }
            //iterate upwards to the next row of points
            x = array.getDouble(1, 1);
            y = y + distY;

            //iterate left until you find the left-most edge of the space
            if (polygonContainsPoint(array, x, y)) {
                while (polygonContainsPoint(array, x, y)) {
                    if (y > yv.maxNumber().doubleValue()) {
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
                    if (y > yv.maxNumber().doubleValue()) {
                        y = y1;
                        mFlag = 1;
                    }
                    x = x + 0.1;
                }
            }
        }

        return GPS;
    }

    private static INDArray optimizePhotoRoute(INDArray picturePoints) {
        ArrayList<Double> longitudes = new ArrayList<>(), latitudes = new ArrayList<>();
        int rows = picturePoints.rows();
        for (int i = 1; i < rows-1; i++) {
            longitudes.add(picturePoints.getDouble(i, 1));
            latitudes.add(picturePoints.getDouble(i, 2));
        }

        int nStops = rows-1;


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
    public static boolean polygonContainsPoint(INDArray points, double x, double y) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.columns() - 1; i < points.rows(); j = i++) {
            if ((points.getDouble(i, 2) > y) != (points.getDouble(j, 2) > y) &&
                    (x < (points.getDouble(j, 1) - points.getDouble(i, 1)) * (y - points.getDouble(i, 2)) / (points.getDouble(j, 2) - points.getDouble(i, 2)) + points.getDouble(i, 1))) {
                result = !result;
            }
        }
        return result;
    }
}
