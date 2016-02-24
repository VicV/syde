package com.jarone.litterary.helpers;

import android.app.Activity;
import android.content.Context;

import com.jarone.litterary.activities.MainActivity;

/**
 * Created by Adam on 2015-11-30.
 */
public class ContextManager {
    private static Context context;

    public static MainActivity getMainActivityInstance() {
        return mainActivityInstance;
    }

    private static MainActivity mainActivityInstance;

    public static void setContext(Context context) {
        if (context instanceof MainActivity) {
            mainActivityInstance = (MainActivity) context;
        }
        ContextManager.context = context;
    }

    public static Context getContext() {
        return context;
    }

    public static Activity getActivity() {
        return (Activity) context;
    }

}
