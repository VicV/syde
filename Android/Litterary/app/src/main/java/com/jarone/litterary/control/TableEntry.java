package com.jarone.litterary.control;

import com.jarone.litterary.handlers.MessageHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Adam on 2016-02-17.
 */
public class TableEntry {

    double angle;
    double time;
    double distance;

    public TableEntry(double angle, double time, double distance) {
        this.angle = angle;
        this.time = time;
        this.distance = distance;
    }

    public TableEntry(JSONObject json) {
        try {
            this.angle = json.getDouble("angle");
            this.time = json.getDouble("time");
            this.distance = json.getDouble("distance");
        } catch (JSONException e) {
            MessageHandler.d("JSON Parsing Error");
        }
    }

    public JSONObject toJSON() {
        HashMap<String, Double> mapEntry = new HashMap<>();
        mapEntry.put("distance", distance);
        mapEntry.put("time", time);
        mapEntry.put("angle", angle);
        return new JSONObject(mapEntry);
    }
}