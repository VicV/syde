package com.jarone.litterary.handlers;

import android.util.Log;
import android.widget.Toast;

import com.jarone.litterary.adapters.DebugItem;
import com.jarone.litterary.helpers.ContextManager;

/**
 * Created by Adam on 2015-11-03.
 */
public class MessageHandler {

    public static void d(final String message) {
        ContextManager.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(ContextManager.getContext(), message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        Log.d("MessageHandler", message);

        ContextManager.getMainActivityInstance().updateMessageList(DebugItem.DebugLevel.DEBUG, message);
    }

    public static void e(final String message) {
        ContextManager.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(ContextManager.getContext(), message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        Log.w("MessageHandler", message);

        ContextManager.getMainActivityInstance().updateMessageList(DebugItem.DebugLevel.ERROR, message);


    }

    public static void w(final String message) {
        ContextManager.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(ContextManager.getContext(), message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        Log.w("MessageHandler", message);

        ContextManager.getMainActivityInstance().updateMessageList(DebugItem.DebugLevel.WARN, message);


    }


}
