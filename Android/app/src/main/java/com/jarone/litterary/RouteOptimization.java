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
 * <p>
 * Optimizin routes.
 */
public class RouteOptimization {

    public static LatLng[] createOptimizedSurveyRoute(LatLng[] points, float altitude) {

        ArrayList<LatLng> coordinates = new ArrayList<>();

        if (points != null && points.length > 0) {
            coordinates = new ArrayList<>(Arrays.asList(points));
        }

        if (validateBoundary(points)) {

            ArrayList<LatLng> picturePoints = getPhotoPoints(coordinates, altitude);

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


    /**
     * Get an ArrayList of coordinates which are the points to take photos.
     *
     * @param vertices Vertex coordinates of defined boundary to take photos within.
     * @param altitude Height at which the drone will be flown.
     * @return ArrayList of {@link LatLng}
     */
    private static ArrayList<LatLng> getPhotoPoints(ArrayList<LatLng> vertices, float altitude) {
        Polygon.Builder builder = new Polygon.Builder();
        ArrayList<Point> polyPoints = new ArrayList<>();

        //Whether or not the longitude/latitude are negative
        boolean haveNegativeLong;
        boolean haveNegativeLat;

        //Current step size. This is how much we move around when testing whether or not a point overlaps.
        //At 0.0001 we'll get around 15 points. Right now we get about 250.
        //TODO: Test for this number
        double stepSize = .000005;

        //If for some reason we didn't type anything, assume we're taking it from 5m
        if (altitude == -1) {
            altitude = 5;
        }

        //x-distance between image capture points.
        double xDistanceBetweenCapturePoints = (altitude * Math.tan(Math.toRadians(60)) * 2) / 200000;

        //y-distance between image capture points
        double yDistanceBetweenCapturePoints = (altitude * Math.tan(Math.toRadians(42.5)) * 2) / 200000;

        //Number of rows.
        int m = vertices.size();


        ArrayList<LatLng> GPS = new ArrayList<>();

        // inputting all the vertices of surveyArea P4 to the xv and yv array
        double maxLat = Double.NEGATIVE_INFINITY;
        double maxLong = Double.NEGATIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY;
        double minLong = Double.POSITIVE_INFINITY;

        int lowestLatIndex = -1;
        double currentLat, currentLong;

        //Find the max and mins of our longitutes and latitudes
        for (int i = 0; i < m; i++) {
            currentLat = vertices.get(i).latitude;
            currentLong = vertices.get(i).longitude;

            if (currentLat > maxLat) {
                maxLat = currentLat;
            }
            if (currentLat < minLat) {
                minLat = currentLat;
                lowestLatIndex = i;
            }
            if (currentLong > maxLong) {
                maxLong = currentLong;
            }
            if (currentLong < minLong) {
                minLong = currentLong;
            }
        }

        //Set if these are negative.
        haveNegativeLong = minLong < 0;
        haveNegativeLat = minLat < 0;

        maxLat = haveNegativeLat ? maxLat + (-minLat) : maxLat;
        maxLong = haveNegativeLong ? maxLong + (-minLong) : maxLong;

        //Copy original array into a new array of Points. If the longitude or latitude were negative, add the inverse of
        // the min of them to each point so we start at 0.0;
        for (LatLng pt : vertices) {
            Point p = new Point((haveNegativeLong ? pt.longitude + (-minLong) : pt.longitude), (haveNegativeLat ? pt.latitude + (-minLat) : pt.latitude));
            polyPoints.add(p);
            builder.addVertex(new Point(p.longitude, p.latitude));
        }

        //Create our surveyArea.
        Polygon surveyArea = builder.close().build();

        //Start at the first point.
        double currentY = polyPoints.get(lowestLatIndex).longitude;
        double currentX = polyPoints.get(lowestLatIndex).latitude;

        //Add a new point to our final array. If it WAS negative, we now subtract that minimum value because
        // We would have added it previously.
        GPS.add(new LatLng(haveNegativeLong ? currentX + minLong : currentX, haveNegativeLat ? currentY + minLat : currentY));

        // break counter for upcoming for loop
        double nextY = 0;
        int mFlag = 0;
        int count = 0;

        while (surveyArea.contains(new Point(currentX, currentY))) {

            while (surveyArea.contains(new Point(currentX, currentY))) {
                currentX = currentX - xDistanceBetweenCapturePoints;
            }

            while (!surveyArea.contains(new Point(currentX, currentY))) {
                currentX = currentX + stepSize;
            }

            while (surveyArea.contains(new Point(currentX, currentY))) {
                GPS.add(new LatLng(haveNegativeLong ? currentX + minLong : currentX, haveNegativeLat ? currentY + minLat : currentY));
                currentX = currentX + xDistanceBetweenCapturePoints;
            }

            while (!surveyArea.contains(new Point(currentX, currentY))) {
                currentX = currentX - stepSize;
            }
            GPS.add(new LatLng(haveNegativeLong ? currentX + minLong : currentX, haveNegativeLat ? currentY + minLat : currentY));

            currentX = polyPoints.get(lowestLatIndex).latitude;
            currentY = currentY + yDistanceBetweenCapturePoints;
        }


//        //MAIN BULK OF ALGORITHM
//        while (surveyArea.contains(new Point(currentX, currentY))) {
//
//            //last loop break clause
//            if (mFlag == 1)
//                mFlag = 2;
//
//            //case where next y point is greater than the top of the surveyArea
//            if ((currentY + yDistanceBetweenCapturePoints) > maxLat) {
//                nextY = currentY + yDistanceBetweenCapturePoints;
//                while (nextY > maxLat) {
//                    //iterate down until you are in the surveyArea
//                    nextY = nextY - stepSize;
//                }
//            }
//
//            //general case where the x and y test value is in the surveyArea
//            while (surveyArea.contains(new Point(currentX, currentY))) {
//                //TODO: Make more understandable
//                GPS.add(new LatLng(haveNegativeLong ? currentX + minLong : currentX, haveNegativeLat ? currentY + minLat : currentY));
//                currentX = currentX + xDistanceBetweenCapturePoints;
//            }
//
//            //the out clause for the last case
//            if (mFlag == 2)
//                break;
//
//            //case where you reach the far right-most point, iterate back until you are in region
//            while (!surveyArea.contains(new Point(currentX, currentY))) {
//                currentX = currentX - stepSize;
//                if (currentX < minLong) {
//                    currentX = polyPoints.get(lowestLatIndex).latitude;
//                    break;
//                }
//            }
//            GPS.add(new LatLng(haveNegativeLong ? currentX + minLong : currentX, haveNegativeLat ? currentY + minLat : currentY));
//
//            //iterate upwards to the next row of points
//            currentX = polyPoints.get(lowestLatIndex).latitude;
//            currentY = currentY + yDistanceBetweenCapturePoints;
//
//            //iterate left until you find the left-most edge of the space
//            while (surveyArea.contains(new Point(currentX, currentY))) {
//                if (currentY > maxLat) {
//                    currentY = nextY;
//                    mFlag = 1;
//                    if (currentX < minLat) {
//                        currentX = polyPoints.get(lowestLatIndex).latitude;
//                        break;
//                    }
//                    if (!surveyArea.contains(new Point(currentX, currentY))) {
//                        break;
//                    }
//
//                }
//                currentX = currentX - stepSize;
//            }
//
//            //if you iterate up, and it's outside the area, iterate right until
//            //you find inside the surveyArea
//            while (!surveyArea.contains(new Point(currentX, currentY))) {
//                if (currentX > maxLong) {
//                    currentX = nextY;
//                    mFlag = 1;
//                    if (surveyArea.contains(new Point(currentX, currentY))) {
//                        break;
//                    }
//                }
//                if (currentY > maxLat) {
//                    currentY = polyPoints.get(lowestLatIndex).longitude;
//                    break;
//                }
//                currentY = currentY + stepSize;
//            }
//        }

        return GPS;
    }

    private static ArrayList<LatLng> optimizePhotoRoute(ArrayList<LatLng> picturePoints) {

        ArrayList<LatLng> bestRoute = new ArrayList();

        RouteManager.removeAllPoints();

        RouteManager.addAllPhotoPoints(picturePoints);

        // Initialize population
        Population pop = new Population(50, true);

        MessageHandler.d("Initial distance: " + pop.getFittest().getDistance());
        long startTime = System.currentTimeMillis();

        // Evolve population for 300 generations
        pop = GA.evolvePopulation(pop);

        int iterations = 100;//300;
        MessageHandler.d("Iterations: " + iterations);

        for (int i = 0; i < iterations; i++) {
            pop = GA.evolvePopulation(pop);
        }

        long endTime = System.currentTimeMillis();

        Route route = pop.getFittest();

        MessageHandler.d("Final distance: " + pop.getFittest().getDistance() + ". Time: " + (endTime - startTime));


        for (PhotoPoint pt : route.getRoute()) {
            bestRoute.add(new LatLng(pt.getLatitude(), pt.getLongitude()));
        }

        return bestRoute;


    }

}
