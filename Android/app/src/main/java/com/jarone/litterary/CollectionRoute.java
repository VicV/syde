package com.jarone.litterary;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.drone.GroundStation;

/**
 * Created by Adam on 2016-01-21.
 */
public class CollectionRoute extends NavigationRoute {

    public CollectionRoute(LatLng[] route, float altitude, short heading) {
        super(route, altitude, heading);
        setCallbacks();
    }

    public void setCallbacks() {
        GroundStation.taskDoneCallback = new Runnable() {
            @Override
            public void run() {
                GroundStation.getAngularController().pickupLitter(new Runnable() {
                    @Override
                    public void run() {
                        executeRouteStep();
                    }
                });
            }
        };
    }

}
