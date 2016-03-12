package com.jarone.litterary.handlers;

import android.util.Log;
import android.widget.Toast;

import com.jarone.litterary.datatypes.DebugItem;
import com.jarone.litterary.helpers.ContextManager;

/**
 * Created by Adam on 2015-11-03.
 */
public class MessageHandler {

    public static void d(final String message) {
        Log.d("MessageHandler", message);
        runOnUIThread(DebugItem.DebugLevel.DEBUG, message, true);
    }

    public static void e(final String message) {
        Log.e("MessageHandler", message);
        runOnUIThread(DebugItem.DebugLevel.ERROR, message, true);
    }

    public static void w(final String message) {
        Log.w("MessageHandler", message);
        runOnUIThread(DebugItem.DebugLevel.WARN, message, true);
    }

    public static void log(final String message) {
        Log.d("MessageHandler", message);
        runOnUIThread(DebugItem.DebugLevel.DEBUG, message, false);
    }

    public static void runOnUIThread(final DebugItem.DebugLevel level, final String message, final boolean makeToast) {
        ContextManager.getMainActivityInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (makeToast) {
                    Toast toast = Toast.makeText(ContextManager.getContext(), message, Toast.LENGTH_SHORT);
                    toast.show();
                }
                ContextManager.getMainActivityInstance().updateMessageList(new DebugItem(level, message, System.currentTimeMillis()));
            }
        });
    }


}