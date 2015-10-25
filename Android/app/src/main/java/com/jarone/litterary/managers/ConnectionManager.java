package com.jarone.litterary.managers;

import com.jarone.litterary.promises.Promise;

/**
 * Created by vic on 10/24/15.
 */
public class ConnectionManager {


    //Reasoning for this being a singleton will be made apparent later.

    private static ConnectionManager instance;

    public static ConnectionManager getInstance() {
        if (instance != null) {
            return instance;
        } else {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public void isConnected(Promise promise) {

        if (someConnectionCheck()) {
            promise.finish();
        }


    }

    private boolean someConnectionCheck() {
        return true;
    }
}
