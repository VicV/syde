package com.jarone.litterary.handlers;

import android.widget.Toast;

import com.jarone.litterary.LitterApplication;

/**
 * Created by Adam on 2015-11-03.
 */
public class ErrorHandler {

    public static void d (String message) {
        Toast toast = Toast.makeText(LitterApplication.getContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
