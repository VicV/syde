package com.jarone.litterary.adapters;

import com.jarone.litterary.activities.MainActivity;

import java.util.ArrayList;

/**
 * Created by V on 2/23/2016.
 */
public class DebugItemList extends ArrayList<DebugItem> {

    private MainActivity activity;



    @Override
    public boolean add(DebugItem object) {
        return super.add(object);
    }
}
