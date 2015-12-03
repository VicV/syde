/*
* Route.java
* Stores a candidate route
*/

package com.jarone.litterary.optimization;

import java.util.ArrayList;
import java.util.Collections;

public class Route {

    public ArrayList<PhotoPoint> getRoute() {
        return route;
    }

    // Holds our route of cities
    private ArrayList<PhotoPoint> route = new ArrayList<>();
    // Cache
    private double fitness = 0;
    private double distance = 0;

    // Constructs a blank route
    public Route() {
        for (int i = 0; i < RouteManager.numberOfPhotoPoints(); i++) {
            route.add(null);
        }
    }

    public Route(ArrayList<PhotoPoint> route) {
        this.route = route;
    }

    // Creates a random individual
    public void generateIndividual() {
        // Loop through all our destination cities and add them to our route
        for (int photoPointIndex = 0; photoPointIndex < RouteManager.numberOfPhotoPoints(); photoPointIndex++) {
            setPhotoPoint(photoPointIndex, RouteManager.getPhotoPoint(photoPointIndex));
        }
        // Randomly reorder the route
        Collections.shuffle(route);
    }

    // Gets a photoPoint from the route
    public PhotoPoint getPhotoPoint(int routePosition) {
        return (PhotoPoint) route.get(routePosition);
    }

    // Sets a photoPoint in a certain position within a route
    public void setPhotoPoint(int routePosition, PhotoPoint photoPoint) {
        route.set(routePosition, photoPoint);
        // If the routes been altered we need to reset the fitness and distance
        fitness = 0;
        distance = 0;
    }

    // Gets the routes fitness
    public double getFitness() {
        if (fitness == 0) {
            fitness = 1 / (double) getDistance();
        }
        return fitness;
    }

    // Gets the total distance of the route
    public double getDistance() {
        if (distance == 0) {
            int routeDistance = 0;
            // Loop through our route's cities
            for (int photoPointIndex = 0; photoPointIndex < routeSize(); photoPointIndex++) {
                // Get photoPoint we're travelling from
                PhotoPoint fromPhotoPoint = getPhotoPoint(photoPointIndex);
                // PhotoPoint we're travelling to
                PhotoPoint destinationPhotoPoint;
                // Check we're not on our route's last photoPoint, if we are set our 
                // route's final destination photoPoint to our starting photoPoint
                if (photoPointIndex + 1 < routeSize()) {
                    destinationPhotoPoint = getPhotoPoint(photoPointIndex + 1);
                } else {
                    destinationPhotoPoint = getPhotoPoint(0);
                }
                // Get the distance between the two cities
                routeDistance += fromPhotoPoint.distanceTo(destinationPhotoPoint);
            }
            distance = routeDistance;
        }
        return distance;
    }

    // Get number of photoPoints on our route
    public int routeSize() {
        return route.size();
    }

    // Check if the route contains a photoPoint
    public boolean containsPhotoPoint(PhotoPoint photoPoint) {
        return route.contains(photoPoint);
    }

    @Override
    public String toString() {
        String geneString = "|";
        for (int i = 0; i < routeSize(); i++) {
            geneString += getPhotoPoint(i) + "|";
        }
        return geneString;
    }
}
