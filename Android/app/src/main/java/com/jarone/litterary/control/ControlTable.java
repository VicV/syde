package com.jarone.litterary.control;

import android.content.Context;

import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Adam on 2016-01-27.
 */
public class ControlTable {

    private static String path="table_values.txt";

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
            FileOutputStream out = ContextManager.getContext().openFileOutput(path, Context.MODE_PRIVATE);
            out.write(jsonString.getBytes());
            out.close();
        } catch (JSONException e) {
            MessageHandler.d("JSON Serialization Failed!");
        } catch (FileNotFoundException e) {
            MessageHandler.d("Failed to Open Output File");
        } catch (IOException e) {
            MessageHandler.d("Failed to write to output file");
        }
    }

    public static void load() {
        try {
            BufferedInputStream in = new BufferedInputStream(ContextManager.getContext().openFileInput(path));
            StringBuilder sb = new StringBuilder();
            int readByte = in.read();
            while (readByte != -1) {
                sb.append(readByte);
                readByte = in.read();
            }
            String jsonString = sb.toString();
            JSONArray tableArray = new JSONArray(jsonString);
            entries = fromJSON(tableArray);
        } catch (FileNotFoundException e) {
            MessageHandler.d("No table has been saved!");
        } catch (IOException e) {
            MessageHandler.d("Failed to read table input stream");
        } catch (JSONException e) {
            MessageHandler.d("JSON Parsing Error");
        }
    }

    public static void testSaveLoad() {
        entries = new ArrayList<>();
        TableEntry entry = new TableEntry(2, 6, 7);
        entries.add(entry);
        save();
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

    public static TableEntry findMatchForDistance(double distance) {
        ArrayList<TableEntry> matches = new ArrayList<>();
        for (TableEntry entry : entries) {
            if (entry.distance <= distance) {
                matches.add(entry);
            }
        }

        if (matches.size() < 1) {
            return null;
        }

        double minTime = 9999;
        int minIndex = 0;
        int index = 0;
        for (TableEntry entry : matches) {
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
