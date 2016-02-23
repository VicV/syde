package com.jarone.litterary.control;

import java.util.ArrayList;

/**
 * Created by Adam on 2016-01-27.
 */
public class ControlTable {

    private static String path="table_values.txt";

    private static ArrayList<TableEntry> entries;

    public static void save(){

    }

    public static void load() {

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



}
