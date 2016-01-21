package com.jarone.litterary;

import android.graphics.BitmapFactory;
import android.os.Environment;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.drone.Camera;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.LocationHelper;
import com.jarone.litterary.imageproc.ImageProcessing;

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
    }

    /**
     * Create a one-waypoint task with the next waypoint in the survey route. Register a
     * callback to execute when the waypoint is reached. Take a photo at this point. Register a
     * callback for photo taken success that executes this method again with an incremented waypoint
     * index
     */
    @Override
    public void executeRouteStep() {
        if (index <= route.length - 1) {
            executing = true;
            MessageHandler.d("Executing Survey Point " + (index + 1));
            GroundStation.newTask();
            GroundStation.addPoint(route[index].latitude, route[index].longitude, SPEED, altitude, heading);
            index++;

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
            GroundStation.uploadAndExecuteTask();

        } else {
            MessageHandler.d("Survey Route Complete!");
            finished = true;
            executing = false;
            stopRoute();
        }

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

    public ArrayList<File> findSurveyPhotos() {
        String path = Environment.getExternalStorageDirectory().toString()+"/survey";
        File f = new File(path);
        File files[] = f.listFiles();

        ArrayList<File> surveyPhotos = new ArrayList<>();

        for (File file : files) {
            long timestamp = Long.parseLong(file.getName().split("|")[0]);
            if (timestamp > startTime && timestamp < endTime) {
                surveyPhotos.add(file);
            }
        }
        return surveyPhotos;
    }

    public void analyzeSurveyPhotos() {
        ArrayList<File> photos = findSurveyPhotos();
        ArrayList<LatLng> litter = new ArrayList<>();
        for (File photo : photos) {
            litter.addAll(
                    ImageProcessing.identifyLitter(
                            BitmapFactory.decodeFile(
                                    photo.getAbsolutePath()
                            )
                    )
            );
        }
        litterPoints = LocationHelper.removeDuplicates(litter);
    }

}
