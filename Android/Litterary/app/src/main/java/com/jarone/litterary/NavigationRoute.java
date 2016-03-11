package com.jarone.litterary;

import android.os.Environment;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.drone.GroundStation;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.FileAccess;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;

/**
 * Created by Adam on 2016-01-21.
 */
public class NavigationRoute {

    public LatLng[] getRoute() {
        return route;
    }

    protected LatLng[] route;
    protected int index;
    protected final int SPEED = 5;

    protected boolean executing;
    protected boolean finished;

    protected long startTime;
    protected long endTime;

    protected float altitude;
    protected short heading;


    public NavigationRoute(LatLng[] route, float altitude, short heading) {
        this.route = route;
        this.altitude = altitude;
        this.heading = heading;
        index = 0;
        executing = false;
        finished = false;
    }

    public NavigationRoute(String timestamp) {
        load(timestamp);
    }

    public void executeRoute() {
        if (index == 0) {
            startTime = System.currentTimeMillis();
        }

        executeRouteStep();
    }

    /**
     * Override this to determine what happens when route is executed
     */
    public void executeRouteStep() {
        if (index <= route.length - 1) {
            executing = true;
            MessageHandler.d("Executing Survey Point " + (index + 1));
            GroundStation.newTask();
            GroundStation.addPoint(route[index].latitude, route[index].longitude, SPEED, altitude, heading);
            index++;
            setCallbacks();
            GroundStation.uploadAndExecuteTask();

        } else {
            MessageHandler.d("Survey Route Complete!");
            finished = true;
            executing = false;
            stopRoute();
        }

    }

    public void stopRoute() {
        endTime = System.currentTimeMillis();
        GroundStation.taskDoneCallback = new Runnable() {
            @Override
            public void run() {
            }
        };
        GroundStation.stopTask();
    }

    protected void setCallbacks() {
        GroundStation.taskDoneCallback = new Runnable() {
            @Override
            public void run() {
               executeRouteStep();
            }
        };
    }

    public boolean isExecuting() {
        return executing;
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * Save list of GPS points in the navigation route
     */
    public void save() {
        JSONArray list = new JSONArray();
        JSONArray saveObj = new JSONArray();
        for (LatLng point : route) {
            list.put(point.latitude + "," + point.longitude);
        }
        try {
            saveObj.put(list);
            saveObj.put(altitude);
            saveObj.put(heading);

            FileAccess.saveToFile("survey", System.currentTimeMillis() + "-survey", saveObj.toString());
        } catch (JSONException e) {
            MessageHandler.e(e.getMessage());

        }
    }

    public void load(String timestamp) {
        try {
            String jsonString = FileAccess.loadFromFile("survey", timestamp + "-survey");
            JSONArray array = new JSONArray(jsonString);
            altitude = (float) array.getDouble(1);
            heading = (short) array.getLong(2);
            JSONArray points = array.getJSONArray(0);
            route = new LatLng[points.length()];
            for (int i = 0; i < points.length(); i++) {
                String value = points.getString(i);
                String[] coords = value.split(",");
                route[i] = new LatLng(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
            }

        } catch (Exception e) {
            MessageHandler.e(e.getMessage());
        }
    }

    public File formatFilename(String timestamp) {
        File file = new File(Environment.getExternalStorageDirectory() + "/Litterary/survey/");
        file.mkdirs();
        return new File(file, timestamp + "-survey");
    }

}