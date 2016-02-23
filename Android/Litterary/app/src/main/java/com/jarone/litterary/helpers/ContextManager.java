package com.jarone.litterary.helpers;

import android.app.Activity;
import android.content.Context;

/**
 * Created by Adam on 2015-11-30.
 */
public class ContextManager {
    private static Context context;

    public static void setContext(Context context) {
        ContextManager.context = context;
    }
    public static Context getContext() {
        return context;
    }

    public static Activity getActivity() {
        return (Activity) context;
    }
}
