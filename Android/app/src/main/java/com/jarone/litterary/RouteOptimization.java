package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;
import com.jarone.litterary.optimization.GA;
import com.jarone.litterary.optimization.PhotoPoint;
import com.jarone.litterary.optimization.Point;
import com.jarone.litterary.optimization.Polygon;
import com.jarone.litterary.optimization.Population;
import com.jarone.litterary.optimization.Route;
import com.jarone.litterary.optimization.RouteManager;

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


//            INDArray picturePoints = getPhotoPoints(array);
          //  INDArray orderedPoints = optimizePhotoRoute(picturePoints);

            ArrayList<LatLng> picturePoints = getPhotoPoints(latLngs, altitude);

            ArrayList<LatLng> orderedPoints = optimizePhotoRoute(picturePoints);

            Object[] route = orderedPoints.toArray();

            return (Arrays.copyOf(route, route.length, LatLng[].class));
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
//        ArrayList<LatLng> points = new ArrayList();
        ArrayList<Point> polyPoints = new ArrayList();


        //Whether or not the latitude/longitude are negative
        boolean longNeg;
        boolean latNeg;

        //Current step size. At 0.0001 we'll get around 15 points. Right now we get about 250.
        double stepSize = .000005;

        if (altitude == -1) {
            altitude = 4;
        }

        //x-distance between image capture points
        double distX = (altitude * Math.tan(Math.toRadians(60)) * 2) / 200000;

        //y-distance between image capture points
        double distY = (altitude * Math.tan(Math.toRadians(42.5)) * 2) / 200000;

        //Number of rows.
        int m = originalArray.size();

        double xi;
        double yi;

        ArrayList<LatLng> GPS = new ArrayList<>();

        // inputting all the vertices of polygon P4 to the xv and yv array
        double maxLat = Double.NEGATIVE_INFINITY;
        double maxLong = Double.NEGATIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY;
        double minLong = Double.POSITIVE_INFINITY;

        int lowestLngIndex = -1;

        for (int i = 0; i < m; i++) {
            xi = originalArray.get(i).latitude;
            yi = originalArray.get(i).longitude;

            if (xi > maxLat) {
                maxLat = xi;
            }
            if (xi < minLat) {
                minLat = xi;

            }
            if (yi > maxLong) {
                maxLong = yi;
            }
            if (yi < minLong) {
                minLong = yi;
                lowestLngIndex = i;
            }
        }

        //Set if these are negative.
        longNeg = minLong < 0;
        latNeg = minLat < 0;

        maxLat = latNeg ? maxLat + (-minLat) : maxLat;
        maxLong = longNeg ? maxLong + (-minLong) : maxLong;

        //Copy original array into a new array of Points. If the latitude or longitude were negative, add the inverse of
        // the min of them to each point so we start at 0.0;
        for (LatLng pt : originalArray) {
            Point p = new Point((latNeg ? pt.latitude + (-minLat) : pt.latitude), (longNeg ? pt.longitude + (-minLong) : pt.longitude));
            polyPoints.add(p);
            builder.addVertex(new Point(p.latitude, p.longitude));
        }

        //Create our polygon.
        Polygon polygon = builder.close().build();

        //Start at the first point.
        double x = polyPoints.get(lowestLngIndex).latitude;
        double y = polyPoints.get(lowestLngIndex).longitude;

        //Add a new point to our final array. If it WAS negative, we now subtract that minimum value because
        // We would have added it previously.
        GPS.add(new LatLng(latNeg ? x + minLat : x, longNeg ? y + minLong : y));

        // break counter for upcoming for loop
        double y1 = 0;
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
                    if (x < minLat) {
                        x = polyPoints.get(lowestLngIndex).latitude;
                        break;
                    }
                }
                GPS.add(new LatLng(latNeg ? x + minLat : x, longNeg ? y + minLong : y));
            }

            //iterate upwards to the next row of points
            x = polyPoints.get(lowestLngIndex).latitude;
            y = y + distY;

            //iterate left until you find the left-most edge of the space
            if (polygon.contains(new Point(x, y))) {
                while (polygon.contains(new Point(x, y))) {
                    if (y > maxLong) {
                        y = y1;
                        mFlag = 1;
                        if (x < minLat) {
                            x = polyPoints.get(lowestLngIndex).latitude;
                            break;
                        }
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
                    if (x > maxLat) {
                        x = polyPoints.get(lowestLngIndex).latitude;
                        break;
                    }
                    x = x + stepSize;

                }
            }
        }

        return GPS;
    }

    private static ArrayList<LatLng> optimizePhotoRoute(ArrayList<LatLng> picturePoints) {

        ArrayList<LatLng> bestRoute = new ArrayList();

        RouteManager.addAllPhotoPoints(picturePoints);
        // Initialize population
        Population pop = new Population(50, true);

        MessageHandler.d("Initial distance: " + pop.getFittest().getDistance());

        // Evolve population for 1000 generations
        pop = GA.evolvePopulation(pop);
        for (int i = 0; i < 1000; i++) {
            pop = GA.evolvePopulation(pop);
        }

        Route route = pop.getFittest();

        MessageHandler.d("Initial distance: " + pop.getFittest().getDistance());


        for (PhotoPoint pt : route.getRoute()) {
            bestRoute.add(new LatLng(pt.getLatitude(), pt.getLongitude()));
        }

        return bestRoute;


    }
    
}
