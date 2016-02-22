package com.jarone.litterary.helpers;

import android.os.Environment;

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

    public static File formatFileName(String directory, String name) {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Litterary/" + directory);
        dir.mkdirs();
        return new File(dir, name);
    }

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
        } catch (IOException e) {
        }
        return null;
    }

    public static boolean saveToFile(String directory, String name, String data) {
        try {
            FileOutputStream out = new FileOutputStream(formatFileName(directory, name));
            out.write(data.getBytes());
            out.close();
            return true;
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
        return false;
    }
}
