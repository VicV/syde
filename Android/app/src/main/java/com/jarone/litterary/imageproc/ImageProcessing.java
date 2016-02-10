package com.jarone.litterary.imageproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by vic on 11/17/15.
 */
public class ImageProcessing {

    private static boolean connected = false;

    //The latest fully-processed result image
    private static Mat currentMat;

    //The temporary image used for intermediate processing
    private static Mat processingMat;

    //The Bitmap representation of the current result image
    private static Bitmap CVPreview = null;

    public static BaseLoaderCallback loaderCallback = new BaseLoaderCallback(ContextManager.getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    connected = true;
                    currentMat = new Mat();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public static void initializeOpenCV() {
        MessageHandler.d("INITIALIZING OPENCV");
        OpenCVLoader.initAsync("2.4.8", ContextManager.getActivity(), loaderCallback);
    }

    public static Pair calculateGPSPoints(Bitmap image, int pw, int py, Context context) {
        Pair points = new Pair<>(0, 0);
        int counter = 0, top = 0, bottom = 0, left = 0, right = 0;

        return points;
    }

    public static void setSourceImage(String source) {
        try {
            InputStream i = ContextManager.getActivity().getAssets().open(source);
            readFrame(BitmapFactory.decodeStream(i));
        } catch (IOException e) {
            MessageHandler.d("Image File not Found!");
        }
    }

    public static void setSourceFrame(byte[] videoBuffer) {
        readFrame(BitmapFactory.decodeByteArray(videoBuffer, 0, videoBuffer.length));
    }

    public static void readFrame(Bitmap image) {
        Utils.bitmapToMat(image, currentMat);
    }

    public static ArrayList<LatLng> identifyLitter(Bitmap photo) {
        return new ArrayList<>();
    }

    public static Bitmap getCVPreview() {
        return CVPreview;
    }

    /**
     * Detect blobs in an image using edge detection, closing, filling and thresholding
     */
    public static void detectBlobs() {
        processingMat = currentMat;
        Imgproc.cvtColor(processingMat, processingMat, Imgproc.COLOR_BGR2GRAY);
        double cannyThresh = determineCannyThreshold();
        Imgproc.Canny(processingMat, processingMat, cannyThresh / 2, cannyThresh);
        closeImage();
        Imgproc.threshold(processingMat, processingMat, 0, 255, Imgproc.THRESH_BINARY);
        fillImage();
        eliminateSmallBlobs(4);
        clearBorders();
        Imgproc.medianBlur(processingMat, processingMat, 31);
        currentMat = processingMat;
    }

    /**
     * Perform closing operation on the image, first downscaling to speed up processing
     */
    public static void closeImage() {
        int scaleFactor = 10;
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(100 / scaleFactor, 100 / scaleFactor));
        //Rescale to smaller size to perform closing much faster
        int width = processingMat.width();
        int height = processingMat.height();
        Imgproc.resize(processingMat, processingMat, new Size(processingMat.width() / scaleFactor, processingMat.height() / scaleFactor));
        Imgproc.morphologyEx(processingMat, processingMat, Imgproc.MORPH_CLOSE, element);
        Imgproc.resize(processingMat, processingMat, new Size(width, height));
    }

    public static void fillImage() {
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(processingMat, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        fillContours(contours, 255);
    }

    /**
     * Converts the most recently-processed Mat frame to a Bitmap and stores it in CVPreview
     *
     * @return
     */
    public static Bitmap convertLatestFrame() {
        if (CVPreview == null && currentMat != null && currentMat.width() > 0) {
            CVPreview = Bitmap.createBitmap(currentMat.width(), currentMat.height(), Bitmap.Config.ARGB_8888);
        } else if (currentMat == null || currentMat.width() <= 0) {
            return null;
        }
        Utils.matToBitmap(currentMat, CVPreview);
        return CVPreview;
    }

    /**
     * Use Otsu thresholding to determine a good threshold value for Canny edge detection
     * @return
     */
    public static double determineCannyThreshold() {
        Mat _ = new Mat();
        return Imgproc.threshold(currentMat, _, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
    }

    /**
     * Eliminate objects that are too small (noise)
     */
    public static void eliminateSmallBlobs(double threshold) {
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(processingMat, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        ArrayList<MatOfPoint> badContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            if (Imgproc.contourArea(contour) > threshold) {
                badContours.add(contour);
            }
        }
        fillContours(badContours, 0);
    }

    /**
     * Eliminate shapes which touch the border of the image
     */
    public static void clearBorders() {
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(processingMat, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        int width = processingMat.width();
        int height = processingMat.height();

        ArrayList<MatOfPoint> badContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            for (Point p : contour.toArray()) {
                if (p.x == 0 || p.x == width || p.y == 0 || p.y == height) {
                    badContours.add(contour);
                }
            }
        }
        fillContours(badContours, 0);
    }

    public static void fillContours(ArrayList<MatOfPoint> contours, int colour) {
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(processingMat, contours, i, new Scalar(colour), -1);
        }
    }
}

/**
 * function [distx, disty] = calculateGPSPoints(image, pw, ph)
 * % image is a binary image
 * [h, w, z] = size(image);
 * counter = 0;
 * top = 0;
 * bottom = 0;
 * left = 0;
 * right = 0;
 * % determine top of object
 * for i=1:h
 * counter = 0;
 * for j=1:w
 * if (image(i,j) == 1)
 * counter = counter +1;
 * if (counter > 10)
 * top = i;
 * break;
 * end
 * end
 * end
 * if (top ~= 0)
 * break;
 * end
 * end
 * % determine bottom of object
 * for i=h:-1:1
 * counter = 0;
 * for j=1:w
 * if (image(i,j) == 1)
 * counter = counter +1;
 * if (counter > 10)
 * bottom = i;
 * break;
 * end
 * end
 * end
 * if (bottom ~= 0)
 * break;
 * end
 * end
 * % determine left side of object
 * for j=1:w
 * counter = 0;
 * for i=top:bottom
 * if (image(i,j) == 1)
 * counter = counter+1;
 * if (counter > 10)
 * left = j;
 * break;
 * end
 * end
 * end
 * if (left ~= 0)
 * break;
 * end
 * end
 * % determine right side of object
 * for j=w:-1:1
 * counter = 0;
 * for i=top:bottom
 * if (image(i,j) == 1)
 * counter = counter +1;
 * if (counter > 5)
 * right = j;
 * break;
 * end
 * end
 * end
 * if (right ~= 0)
 * break;
 * end
 * end
 * % determine location of centre of detected object
 * row = round((bottom+top)/2);
 * col = round((right+left)/2);
 * % calculate distance from the centre of the image to the centre of the
 * % detected oject
 * distx = (col - w/2)/pw; %distance in the x direction from the centre
 * disty = (row - h/2)/ph; %distance in the y direction from the centre
 * end
 */

