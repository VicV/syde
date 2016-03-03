package com.jarone.litterary.drone;

import com.jarone.litterary.handlers.MessageHandler;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
/**
 * Created by Adam on 2016-03-02.
 */
public class Grabber {


    public static final String arduinoIp = "http://192.168.1.66";

    Retrofit retrofit;
    Arduino arduino;

    public Grabber() {
        retrofit = new Retrofit.Builder()
                .baseUrl(arduinoIp)
                .build();

        arduino = retrofit.create(Arduino.class);
    }


    public void sendCommand(final String command) {
        arduino.commandRequest(command).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                MessageHandler.d("Sent command " + command);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                MessageHandler.e("Failed to send command " + command);
            }
        });
    }

    public static class Commands {
        public static final String OPEN = "open";
        public static final String CLOSE = "close";
        public static final String RAISE = "raise";
        public static final String LOWER = "lower";
        public static final String HOLD = "hold";
        public static final String HEIGHT = "height";

    }
    
}