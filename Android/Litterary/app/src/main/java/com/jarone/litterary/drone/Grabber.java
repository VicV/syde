package com.jarone.litterary.drone;

import android.os.AsyncTask;

import com.jarone.litterary.handlers.MessageHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import io.socket.client.IO;
import io.socket.client.Socket;
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

    Socket socket;
    SocketTask socketTask;

    double ultrasonicReading = 0;
    double servoReading = 0;

    public Grabber() {

        //Method 1
        retrofit = new Retrofit.Builder()
                .baseUrl(arduinoIp)
                .build();

        arduino = retrofit.create(Arduino.class);


        //Method 2
        try {
            socket = IO.socket(arduinoIp);
            socket.connect();
        } catch (Exception e) {
            MessageHandler.e(e.getMessage());
        }

        //Method 3
        socketTask = new SocketTask(arduinoIp, 80);
        socketTask.execute();

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

    public void writeSocket(final String command) {
        socket.emit(command);
    }

    public void writeSocketTask(final String command) {
        socketTask.sendMessage(command);
    }

    public void messageReceived(String msg) {
        String[] fields = msg.split(" ");
        double value = Double.parseDouble(fields[1]);
        switch (fields[0]) {
            case("us"):
                ultrasonicReading = value;
                break;
            case("sv"):
                servoReading = value;
        }
    }

    public void closeSocket() {
        socket.close();
        socketTask.disconnect();
    }

    public static class Commands {
        public static final String OPEN = "open";
        public static final String CLOSE = "close";
        public static final String RAISE = "raise";
        public static final String LOWER = "lower";
        public static final String HOLD = "hold";
        public static final String HEIGHT = "height";

    }

    public class SocketTask extends AsyncTask<Void, String, Void> {

        private String address;
        private int port;
        private java.net.Socket socket;

        private OutputStream out;
        private BufferedReader in;

        int timeout = 500;

        boolean disconnect = false;

        public SocketTask(String address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        protected Void doInBackground(Void... arg) {
            socket = new java.net.Socket();
            try {
                socket.connect(new InetSocketAddress(address, port), timeout);
                out = socket.getOutputStream();
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                if (socket.isConnected()) {
                    long start = System.currentTimeMillis();
                    while(!in.ready()) {
                        long now = System.currentTimeMillis();
                        if(now - start > timeout) {
                            MessageHandler.w("Socket timed out");
                            disconnect = true;
                            break;
                        }
                    }
                    while (!disconnect) {
                        publishProgress(in.readLine());
                    }
                    socket.close();
                    in.close();
                    out.close();
                }


            } catch (IOException e) {
                MessageHandler.e(e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String msg = values[0];
            if (msg == null) return;
            messageReceived(msg);
            super.onProgressUpdate(values);
        }

        public void sendMessage(String data) {
            try {
                out.write((data + "\n").getBytes());
            } catch (Exception e) {
                MessageHandler.e(e.getMessage());
            }
        }

        public void disconnect() {
            disconnect = true;
        }
    }


}
