package com.jarone.litterary.handlers;

import android.util.Log;
import android.widget.Toast;

import com.jarone.litterary.helpers.ContextManager;

/**
 * Created by Adam on 2015-11-03.
 */
public class MessageHandler {

    public static void d (final String message) {
        ContextManager.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(ContextManager.getContext(), message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        Log.d("MessageHandler", message);
    }

}
