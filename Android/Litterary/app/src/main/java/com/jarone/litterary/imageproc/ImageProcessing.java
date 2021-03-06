package com.jarone.litterary.imageproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.control.AngularController;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.video.Video;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vic on 11/17/15.
 */
public class ImageProcessing {

    private static boolean connected = false;

    //The latest fully-processed result image
    private static Mat currentMat;

    //For the tracking algorithm (keep track of coloured image)
    private static Mat originalMat;

    private static Mat processingMat;

    private static Mat r;
    private static Mat g;
    private static Mat b;

    private static Mat temp;

    //Stores the list of blobs detected from the current Mat
    private static ArrayList<MatOfPoint> currentBlobs;

    //Thresholding values for Canny edge detector in blob detector
    private static double highThreshold;
    private static double lowThreshold;

    //counter and threshold value for determining whether to retry object tracking
    private static int retryCounter = 0;
    private static int retryThreshold = 10;


    //The Bitmap representation of the current result image
    private static Bitmap CVPreview = null;
    private static TrackingObject trackingObject;
    private static boolean isTracking;
    private static TrackingObject trackedObject;
    private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255);

    //measured result 114.8 degrees
    private static final double CAMERA_FOVX = 110;
    private static final double CAMERA_FOVY = 0.75 * CAMERA_FOVX;

    //pixel density: metres per pixel
    private static double px; //horizontal pixel density
    private static double py; //vertical pixel density

    private static double imageX = 0;
    private static double imageY = 0;

    //keeps track of submat error
    private static boolean createdTrackingObject = false;

    private static ArrayList<Point> blobCentres;

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

    public static void setSourceImage(String source) {
        try {
            InputStream i = ContextManager.getActivity().getAssets().open(source);
            readFrame(BitmapFactory.decodeStream(i));
        } catch (IOException e) {
            MessageHandler.d("Image File not Found!");
        }
    }

    /**
     * Identifies litter in an image and returns the processed image as a bitmap
     *
     * @param image
     * @return
     */
    public static Bitmap processImage(Bitmap image) {
        Mat mat = identifyLitterMat(image);
        convertFrame(mat);
        return CVPreview;
    }

    /**
     * Identifies litter in a mat and returns the processed image as a bitmap
     *
     * @return
     */
    public static Bitmap processImage(Mat mat) {
        identifyLitterMatFromMat(mat);
        convertFrame(mat);
        mat.release();
        return CVPreview;
    }

    /**
     * Set the "original" unmodified image as a Mat
     *
     * @param frame
     */
    public static void setOriginalImage(Bitmap frame) {
        if (originalMat == null) {
            originalMat = new Mat();
        }
        Utils.bitmapToMat(frame, originalMat);
    }

    public static void setOriginalImage(Mat mat) {
        originalMat = mat;
    }

    /**
     * Reads a bitmap image and stores it on currentMat
     *
     * @param image
     */
    public static void readFrame(Bitmap image) {
        Utils.bitmapToMat(image, currentMat);
    }


    private static Mat bitmapToMat;

    /***
     * Identify blobs on the given Mat and return the identified litter points
     *
     * @param photo
     * @return
     */
    public static ArrayList<Point> identifyLitter(Bitmap photo) {
        if (bitmapToMat == null) {
            bitmapToMat = new Mat();
        }
        Utils.bitmapToMat(photo, bitmapToMat);
        //correctDistortion();
        ArrayList<Point> points = detectBlobs(bitmapToMat);
        return points;
    }

    public static Mat identifyLitterMat(Bitmap photo) {
        if (bitmapToMat == null) {
            bitmapToMat = new Mat();
        }
        Utils.bitmapToMat(photo, bitmapToMat);
        ArrayList<Point> points = detectBlobs(bitmapToMat);
        return bitmapToMat;
    }

    public static Mat identifyLitterMatFromMat(Mat mat) {
        //ArrayList<Point> points = detectBlobs(mat);
        detectBlobs(mat);
        return mat;
    }

    /**
     * Returns the bitmap representing the latest processed image
     *
     * @return
     */
    public static Bitmap getCVPreview() {
        return CVPreview;
    }

    //    public static ArrayList<Point> detectBlobsVic(Mat mat) {
    //        Mat out = new Mat();
    //
    //        FeatureDetector blobDetector;
    //
    //        blobDetector = FeatureDetector.create(FeatureDetector.SIFT);
    //
    //        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
    //
    //        Imgproc.cvtColor(mat, out, Imgproc.COLOR_RGBA2GRAY);
    //        blobDetector.detect(mat, keypoints1);
    //
    //        org.opencv.core.Scalar cores = new org.opencv.core.Scalar(0, 0, 255);
    //
    //        org.opencv.features2d.Features2d.drawKeypoints(out, keypoints1, out, cores, 2);
    //
    //    }


    /**
     * Detect blobs in an image using edge detection, closing, filling and thresholding
     * Returns a list of blob centres in terms of points on the image
     */
    public static ArrayList<Point> detectBlobs(Mat mat) {
        if (mat.empty()) {
            return null;
        }

        if (processingMat == null) {
            processingMat = new Mat();
        }

        if (r == null) {
            r = new Mat();
        }
        if (g == null) {
            g = new Mat();
        }
        if (b == null) {
            b = new Mat();
        }

        if (imageX == 0) {
            imageX = processingMat.width();
        }
        if (imageY == 0) {
            imageY = processingMat.height();
        }

        mat.copyTo(processingMat);

        ArrayList<Mat> channels = new ArrayList<>();
        Core.split(processingMat, channels);
        if (channels.size() < 3) {
            return blobCentres;
        }

        MatOfDouble means = new MatOfDouble();
        MatOfDouble stds = new MatOfDouble();
        Core.meanStdDev(processingMat, means, stds);

        Imgproc.threshold(channels.get(0), r, means.get(0, 0)[0] + 50, 255, Imgproc.THRESH_BINARY);
        Imgproc.threshold(channels.get(1), b, means.get(1, 0)[0] + 50, 255, Imgproc.THRESH_BINARY);
        Imgproc.threshold(channels.get(2), g, means.get(2, 0)[0] + 50, 255, Imgproc.THRESH_BINARY);

        ArrayList<Double> cMeans = new ArrayList<>();
        cMeans.add(Core.mean(r).val[0]);
        cMeans.add(Core.mean(g).val[0]);
        cMeans.add(Core.mean(b).val[0]);

        morphImage(r, Imgproc.MORPH_OPEN, 30);
        morphImage(g, Imgproc.MORPH_OPEN, 30);
        morphImage(b, Imgproc.MORPH_OPEN, 30);

        clearBorders(r);
        clearBorders(g);
        clearBorders(b);

        double minMean = 0;
        int mindex = 0;
        for (int i = 0; i < 3; i++) {
            if (means.get(i, 0)[0] < minMean) {
                minMean = means.get(i, 0)[0];
                mindex = i;
            }
        }

        if (mindex == 0) {
            r.copyTo(processingMat);
        } else if (mindex == 1) {
            g.copyTo(processingMat);
        } else if (mindex == 2) {
            b.copyTo(processingMat);
        }

        fillImage(processingMat);

        //eliminateSmallBlobs(processingMat, Math.pow(metresToPixels(0.3, DroneState.getAltitude()), 2));

        blobCentres = findBlobCentres(processingMat);

        for (Point p : blobCentres) {
            Imgproc.ellipse(processingMat, p, new Size(50, 50), 0, 0, 360, new Scalar(127, 56, 255));
        }

        processingMat.copyTo(mat);

        r.release();
        g.release();
        b.release();
        //can't release processingMat otherwise other areas of the code will fail (like find closest to centre)
        //processingMat.release();
        return blobCentres;
    }

    /**
     * Perform closing operation on the image, first downscaling to speed up processing
     */
    public static void morphImage(Mat mat, int operation, double size) {
        int scaleFactor = 5;
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(size / scaleFactor, size / scaleFactor));
        // Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));

        //Rescale to smaller size to perform closing much faster
        int width = mat.width();
        int height = mat.height();
        Imgproc.resize(mat, mat, new Size(width / scaleFactor, height / scaleFactor));
        //Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_DILATE, element);
        Imgproc.morphologyEx(mat, mat, operation, element);

        Imgproc.resize(mat, mat, new Size(width, height));
        Imgproc.threshold(mat, mat, 0, 255, Imgproc.THRESH_BINARY);
    }

    public static void fillImage(Mat mat) {
        Mat temp = mat.clone();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        temp.release();
        fillContours(mat, contours, 255);
    }

    /**
     * Converts the most recently-processed Mat frame to a Bitmap and stores it in CVPreview
     *
     * @return
     */
    public static void convertFrame(Mat mat) {
        if (CVPreview == null && mat != null && mat.width() > 0) {
            CVPreview = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
        } else if (mat == null || mat.width() <= 0) {
            return;
        }
        Utils.matToBitmap(mat, CVPreview);
    }

    /**
     * Use Otsu thresholding to determine a good threshold value for Canny edge detection
     *
     * @return
     */

    public static void determineCannyThreshold(Mat mat) {
        Mat _ = new Mat();
        highThreshold = Imgproc.threshold(mat, _, 127, 255, Imgproc.THRESH_OTSU);
        lowThreshold = highThreshold * 0.333;
    }

    /**
     * Eliminate objects that are too small (noise)
     */
    public static ArrayList<MatOfPoint> eliminateSmallBlobs(Mat mat, double threshold) {
        Mat temp = mat.clone();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        ArrayList<Double> areas = new ArrayList<>();
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        temp.release();
        ArrayList<MatOfPoint> badContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            double size = Imgproc.contourArea(contour);
            areas.add(size);
            if (size < threshold) {
                badContours.add(contour);
            }
        }
        fillContours(mat, badContours, 0);
        contours.removeAll(badContours);
        return contours;
    }

    /**
     * Eliminate shapes which touch the border of the image
     */
    public static void clearBorders(Mat mat) {
        Mat temp = mat.clone();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        int width = temp.width();
        int height = temp.height();
        temp.release();
        ArrayList<MatOfPoint> badContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            for (Point p : contour.toArray()) {
                if (p.x <= 10 || p.x >= width - 10 || p.y <= 10 || p.y >= height - 10) {
                    badContours.add(contour);
                }
            }
        }
        fillContours(mat, badContours, 0);
    }

    /**
     * Determine the centroid of each detected blob
     *
     * @return
     */
    public static ArrayList<Point> findBlobCentres(Mat mat) {
        Mat temp = mat.clone();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(temp, contours, temp, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        temp.release();
        currentBlobs = contours;
        ArrayList<Point> centres = new ArrayList<>();
        for (MatOfPoint contour : currentBlobs) {
            Point centre = contourCentroid(contour);
            if (centre != null) {
                centres.add(centre);
            }
        }
        return centres;
    }

    /**
     * Uses images moments to determine the centroid of the given contour
     *
     * @param contour
     * @return
     */
    public static Point contourCentroid(MatOfPoint contour) {
        Moments moment = Imgproc.moments(contour);
        try {
            int x = (int) moment.get_m10() / (int) moment.get_m00();
            int y = (int) moment.get_m01() / (int) moment.get_m00();
            return new Point(x, y);
        } catch (ArithmeticException e) {
            //divided by zero, image moment is invalid for this contour
            return null;
        }
    }

    public static void fillContours(Mat mat, ArrayList<MatOfPoint> contours, int colour) {
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(mat, contours, i, new Scalar(colour), -1);
        }
    }

    public static ArrayList<LatLng> calculateGPSCoords(ArrayList<Point> points, LatLng origin) {
        //TODO Implement this proper
        ArrayList<LatLng> coords = new ArrayList<>();
        for (Point point : points) {
            coords.add(new LatLng(point.x, point.y));
        }
        return coords;
    }

    public static Point closestToCentre(ArrayList<Point> points) {
        double minDistance = 99999;
        Point minPoint = null;
        if (points == null) {
            return null;
        }
        for (Point p : points) {
//            if (p.x < 5 || p.y < 5 || p.x > processingMat.width() - 5 || p.y > processingMat.height() - 5) {
//                continue;
//            }
            double distance = pixelDistance(p, new Point(processingMat.width() / 2, processingMat.height() / 2));
            if (distance < minDistance) {
                minDistance = distance;
                minPoint = p;
            }
        }
        return minPoint;
    }

    public static Rect calculateStartingRect() {
        Point object = closestToCentre(blobCentres);
        if (object != null) {
            int x = (int) object.x;
            int y = (int) object.y;
            return new Rect(x, y, 20, 20);
        }
        return null;
    }

    /**
     * TODO Implement this
     * Return the current distance of the drone from the target being tracked
     *
     * @return
     */
    public static double distanceFromTarget(AngularController.ActiveAngle angle, double altitude) {
        calculatePixelDensity(altitude);
        if (angle == AngularController.ActiveAngle.PITCH) {
            //Forward is positive, backward is negative
            return (originalMat.height() / 2 + originalMat.height() * 0.1 - trackedObject.getPosition().y ) * py;
        } else {
            //To the left is negative, right is positive
            return (trackedObject.getPosition().x - originalMat.width() / 2) * py;
        }
    }

    public static double pixelDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    public static boolean isTracking() {
        return isTracking;
    }

    //Calculates the density of each pixel in the vertical, py, and horizontal, px, directions in m per pixel
    public static void calculatePixelDensity(double altitude) {
        double W = 2 * altitude * Math.tan(Math.toRadians(CAMERA_FOVX)); //width of image area in m
        double H = 2 * altitude * Math.tan(Math.toRadians(CAMERA_FOVY)); //height of image area in m
        px = W / processingMat.width(); //m per pixel
        py = H / processingMat.height(); //m per pixel
    }

    public static double metresToPixels(double metres, double altitude) {
        double degrees = Math.atan(metres / altitude);
        return degrees / CAMERA_FOVX * processingMat.width();
    }

    public static double pixelsToMetres(double pixels, double altitude) {
        double degrees = CAMERA_FOVX * pixels / processingMat.width();
        return Math.tan(degrees) * altitude;
    }

    public static ArrayList<Point> getBlobCentres() {
        return blobCentres;
    }

    public static void createTrackedObject(Mat mRgba, Rect region) {
        if (!isTracking)
            return;
        trackedObject.hsv = new Mat(mRgba.size(), CvType.CV_8UC3);
        trackedObject.mask = new Mat(mRgba.size(), CvType.CV_8UC1);
        trackedObject.hue = new Mat(mRgba.size(), CvType.CV_8UC1);
        trackedObject.prob = new Mat(mRgba.size(), CvType.CV_8UC1);
        updateHueImage();

        try {
            //create a histogram representation of the object
            Mat tempmask = trackedObject.mask.submat(region);

            MatOfFloat ranges = new MatOfFloat(0f, 256f);
            MatOfInt histSize = new MatOfInt(25);

            List<Mat> images = Arrays.asList(trackedObject.hueArray.get(0).submat(region));
            Imgproc.calcHist(images, new MatOfInt(0), tempmask, trackedObject.hist, histSize, ranges);

            Core.normalize(trackedObject.hist, trackedObject.hist);
            trackedObject.prevRect = region;
            createdTrackingObject = true;
        } catch (CvException e) {
            createdTrackingObject = false;
        }
    }

    public static void updateHueImage() {

        if (!isTracking)
            return;

        int vmin = 65, vmax = 256, smin = 55;

        //Mat is already a bgr image: convert to HSV color model
        Imgproc.cvtColor(originalMat, trackedObject.hsv, Imgproc.COLOR_BGR2HSV);

        //mask out-of-range values
        Core.inRange(trackedObject.hsv, new Scalar(0, smin, Math.min(vmin, vmax)), new Scalar(180, 256, Math.max(vmin, vmax)), trackedObject.mask);

        trackedObject.hsvArray.clear();
        trackedObject.hueArray.clear();
        trackedObject.hsvArray.add(trackedObject.hsv);
        trackedObject.hueArray.add(trackedObject.hue);
        MatOfInt from_to = new MatOfInt(0, 0);

        //extract the hue channel, split: src, dest channels
        Core.mixChannels(trackedObject.hsvArray, trackedObject.hueArray, from_to);
    }

    public static void trackObject() {

        if (!isTracking) {
            MessageHandler.w("Not tracking object!");
            return;
        }
        MatOfFloat ranges = new MatOfFloat(0f, 256f);

        updateHueImage();

        Imgproc.calcBackProject(trackedObject.hueArray, new MatOfInt(0), trackedObject.hist, trackedObject.prob, ranges, 255);
        Core.bitwise_and(trackedObject.prob, trackedObject.mask, trackedObject.prob, new Mat());

        trackedObject.currBox = Video.CamShift(trackedObject.prob, trackedObject.prevRect, new TermCriteria(TermCriteria.EPS, 10, 1));
        trackedObject.prevRect = trackedObject.currBox.boundingRect();
        trackedObject.currBox.angle = -trackedObject.currBox.angle;
        Imgproc.ellipse(originalMat, trackedObject.currBox, RECT_COLOR, 6);
        Utils.matToBitmap(originalMat, CVPreview);

        if (trackedObject.currBox.size.height == 0) {
            retryCounter++;
            if (retryCounter > retryThreshold) {
                retryCounter = 0;
                retryTracking();
            }
        }
    }

    //reattempts tracking if something goes wrong
    public static void retryTracking() {
        Mat temp = originalMat.clone();
        isTracking = false;
        detectBlobs(temp);
        startTrackingObject();
        temp.release();
    }

    /**
     * Begin tracking the object closest to the centre of the camera
     */
    public static void startTrackingObject() {
        if (!isTracking) {
            //Only want to create a new Tracked Object if are not currently tracking (throws exception otherwise)
            MessageHandler.d("Started Tracking...");
            //detectBlobs();
            trackedObject = new TrackingObject();
            if (trackedObject.prevRect != null) {
                isTracking = true;
                createTrackedObject(originalMat, trackedObject.prevRect);
                if (createdTrackingObject) {
                    trackObject();
                } else {
                    MessageHandler.d("Failed to create tracking object!");
                    isTracking = false;
                }
            } else {
                MessageHandler.d("Couldn't find object to track!");
            }
        } else {
            MessageHandler.d("Already Tracking...");
        }
    }

    /**
     * Stop tracking object
     */
    public static void stopTrackingObject() {
        MessageHandler.d("Stopped Tracking...");
        isTracking = false;
//        trackedObject = null;
    }
}