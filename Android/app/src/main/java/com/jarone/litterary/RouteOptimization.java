package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.NDArrayFactory;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

/**
 * Created by Adam on 2015-11-23.
 */
public class RouteOptimization {

    public INDArray array = Nd4j.create(new float[]{20, 0, 40, 0, 80, 30, 40, 90, 0, 40, 0, 0}, new int[]{2, 6});
    // height above the ground
    int height = 4;

    //x-distance between image capture points
    double distX = height * Math.tan(60)*2;

    //y-distance between image capture points
    double distY = height * Math.tan(42.5)*2;



    public static LatLng[] createOptimizedSurveyRoute(LatLng[] points, float altitude) {
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
        for (int i = 0; i < points.length; i++) {
            if (LocationHelper.distanceBetween(points[i], DroneState.getLatLng()) > GroundStation.BOUNDARY_RADIUS) {
                return false;
            }
        }
        return true;
    }


}
