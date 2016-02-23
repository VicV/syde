package com.jarone.litterary.helpers;

import android.media.ExifInterface;
import android.os.Environment;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.handlers.MessageHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Adam on 2016-02-22.
 * Handles saving and loading string objects in the app's folder on external storage
 */
public class FileAccess {

    /**
     * Return a file object representing the given file in the given directory
     * Creates all parent directories if they don't exist
     * @param directory
     * @param name
     * @return
     */
    public static File formatFileName(String directory, String name) {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Litterary/" + directory);
        dir.mkdirs();
        return new File(dir, name);
    }

    /**
     * Load the contents of the given file and return them as a string
     * @param directory
     * @param name
     * @return String contents of file
     */
    public static String loadFromFile(String directory, String name) {
        try {
            FileInputStream in = new FileInputStream(formatFileName(directory, name));
            StringBuilder sb = new StringBuilder();
            int readByte = in.read();
            while (readByte != -1) {
                sb.append((char)readByte);
                readByte = in.read();
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            MessageHandler.d("Couldn't find file");
        } catch (IOException e) {
            MessageHandler.d("Couldn't read from file");
        }
        return null;
    }

    /**
     * Save the contents of the given string to file
     * @param directory
     * @param name
     * @param data
     * @return
     */
    public static boolean saveToFile(String directory, String name, String data) {
        try {
            FileOutputStream out = new FileOutputStream(formatFileName(directory, name));
            out.write(data.getBytes());
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            MessageHandler.d("Couldn't create file");
        } catch (IOException e) {
            MessageHandler.d("Couldn't write to file");
        }
        return false;
    }

    /**
     * Return LatLng of GPS coordinates from photo's exif data
     * @param directory
     * @param name
     * @return
     */
    public static LatLng coordsFromPhoto(String directory, String name) {
        try {
            ExifInterface exif = new ExifInterface(formatFileName(directory, name).toString());
            float[] latlng = new float[2];
            exif.getLatLong(latlng);
            return new LatLng(latlng[0], latlng[1]);
        } catch (IOException e) {

        }
        return null;
    }
}
