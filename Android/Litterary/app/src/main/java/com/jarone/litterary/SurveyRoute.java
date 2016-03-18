package com.jarone.litterary;

import android.graphics.BitmapFactory;
import android.os.Environment;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.drone.Camera;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.FileAccess;
import com.jarone.litterary.helpers.LocationHelper;
import com.jarone.litterary.imageproc.ImageProcessing;
import com.jarone.litterary.optimization.RouteOptimization;

import org.opencv.core.Point;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Adam on 2015-11-16.
 * Class responsible for handling the execution of survey routes
 */
public class SurveyRoute extends NavigationRoute{

    private float altitude;
    private short heading;


    private ArrayList<LatLng> litterPoints;

    public SurveyRoute(LatLng[] route, float altitude, short heading) {
        super(route, altitude, heading);
        setCallbacks();
    }

    /**
     * Create a one-waypoint task with the next waypoint in the route. Register a
     * callback to execute when the waypoint is reached. Take a photo at this point. Register a
     * callback for photo taken success that executes this method again with an incremented waypoint
     * index
     */
    @Override
    protected void setCallbacks() {
        //set the callbacks to take a photo when the point is reached
        GroundStation.taskDoneCallback = new Runnable() {
            @Override
            public void run() {
                //MessageHandler.d("Point Reached, Taking Photo");
                Camera.photoCallback = new Runnable() {
                    @Override
                    public void run() {
                        //call this function again with the incremented index after photo taken
                        executeRouteStep();
                    }
                };
                Camera.takePhoto();
            }
        };
    }

    @Override
    public void stopRoute() {
        Camera.photoCallback = new Runnable() {
            @Override
            public void run() {

            }
        };
        super.stopRoute();
    }

    /**
     * Downloads all the photos taken since the survey started, then analyzes them
     */
    public void downloadAndAnalyzeSurveyPhotos() {
        Camera.downloadPhotosSince(startTime, new Runnable() {
            @Override
            public void run() {
                analyzeSurveyPhotos();
            }
        });

    }

    /**
     * Loads all the photos taken since the survey started from the file system,
     * and runs them through blob detection to identify the GPS points of all the litter
     *
     */
    public void analyzeSurveyPhotos() {
        //Load photos from external storage directory
        String path = Environment.getExternalStorageDirectory().toString()+"/Litterary/survey";
        File f = new File(path);
        File files[] = f.listFiles();

        if (files == null) {
            return;
        }

        ArrayList<File> surveyPhotos = new ArrayList<>();

        //We only care about photos with timestamps that fall within the survey time
        for (File file : files) {
            long timestamp = Long.parseLong(file.getName().split(".jpg")[0]);
            if (timestamp > startTime && timestamp < endTime) {
                surveyPhotos.add(file);
            }
        }

        //Uses previously loaded photos to identify litter
        ArrayList<LatLng> litter = new ArrayList<>();
        for (File photo : surveyPhotos) {
            ArrayList<Point> points = ImageProcessing.identifyLitter(BitmapFactory.decodeFile(photo.getAbsolutePath()));
            ContextManager.getMainActivityInstance().setCPreview();
            litter.addAll(
                    ImageProcessing.calculateGPSCoords(
                            points,
                            FileAccess.coordsFromPhoto(photo)
                    )
            );
        }
        //Remove points very close to each other, then initialize a new collection route
        //using the optimized route provided by route optimizer
        litterPoints = LocationHelper.removeDuplicates(litter);
        LatLng[] points = RouteOptimization.createOptimizedCollectionRoute(litterPoints);
        CollectionRoute pickup = new CollectionRoute(points, altitude, heading);
        pickup.save();
    }

}
