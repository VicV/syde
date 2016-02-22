package com.jarone.litterary.control;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.FileAccess;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Adam on 2016-01-27.
 */
public class ControlTable {

    private static final String file ="table_values";

    private static ArrayList<TableEntry> entries = new ArrayList<>();

    public static final int[] POSSIBLE_TIMES = {200, 400, 600, 800, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000};

    public static void save(){
        JSONObject[] entryList = new JSONObject[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            entryList[i] = entries.get(i).toJSON();
        }

        try {
            JSONArray entryArray = new JSONArray(entryList);
            String jsonString = entryArray.toString();
            FileAccess.saveToFile("control", file, jsonString);
            MessageHandler.d("Table Saved Successfully!");
        } catch (JSONException e) {
            MessageHandler.d("JSON Serialization Failed!");
        }
    }

    public static void load() {
        try {
            String jsonString = FileAccess.loadFromFile("control", file);
            JSONArray tableArray = new JSONArray(jsonString);
            entries = fromJSON(tableArray);
            MessageHandler.d("Table Loaded Successfully!");
        } catch (JSONException e) {
            MessageHandler.d("JSON Parsing Error");
        }
    }

    public static void displaySaveDialog() {
        ContextManager.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(ContextManager.getContext())
                        .setMessage("Save this Table?")
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ControlTable.save();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MessageHandler.d("Table not saved.");
                            }
                        })
                        .show();
            }
        });
    }

    public static void testSaveLoad() {
        entries = new ArrayList<>();
        TableEntry entry = new TableEntry(2, 6, 7);
        entries.add(entry);
        save();
        entries = new ArrayList<>();
        load();
        return;
    }

    public static void addEntry(double angle, double time, double distance) {
        entries.add(new TableEntry(angle, time, distance));
    }

    public static void addEntry(TableEntry entry) {
        entries.add(entry);
    }

    public static void clearEntries() {
        entries = new ArrayList<>();
    }

    /**
     * First find all matching table entries with a distance less than or equal to the given distance
     * Then find the ones with the largest distance, then of those find the one with the smallest time
     * @param distance
     * @return
     */
    public static TableEntry findMatchForDistance(double distance) {
        ArrayList<TableEntry> matches = new ArrayList<>();
        double maxDistance = 0;
        for (TableEntry entry : entries) {
            if (entry.distance <= distance) {
                matches.add(entry);
                if (entry.distance > maxDistance) {
                    maxDistance = entry.distance;
                }
            }
        }

        if (matches.size() < 1) {
            return null;
        }

        //Find all matches which have the largest travel distance
        ArrayList<TableEntry> bestMatches = new ArrayList<>();
        for (TableEntry match : matches) {
            if (match.distance == maxDistance) {
                bestMatches.add(match);
            }
        }

        double minTime = 9999;
        int minIndex = 0;
        int index = 0;
        for (TableEntry entry : bestMatches) {
            if (entry.time < minTime) {
                minTime = entry.time;
                minIndex = index;
            }
            index++;
        }
        return matches.get(minIndex);
    }

    public static ArrayList<TableEntry> fromJSON(JSONArray tableArray) {
        ArrayList<TableEntry> entryArray = new ArrayList<>();
        try {
            for (int i = 0; i < tableArray.length(); i++) {
                entryArray.add(new TableEntry(tableArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            MessageHandler.d("JSON Parsing Error");
        }
        return entryArray;
    }

}
