package com.jarone.litterary.imageproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.jarone.litterary.control.AngularController;
import com.jarone.litterary.drone.DroneState;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vic on 11/17/15.
 */
public class ImageProcessing {

    private static boolean connected = false;

    //The latest fully-processed result image
    private static Mat currentMat;

    //The temporary image used for intermediate processing
    private static Mat processingMat;

    //Stores the list of blobs detected from the current Mat
    private static ArrayList<MatOfPoint> currentBlobs;

    //The Bitmap representation of the current result image
    private static Bitmap CVPreview = null;

    private static TrackingObject trackingObject;

    private static boolean isTracking;

    //measured result 114.8 degrees
    private static final double CAMERA_FOVX = 110;

    private static final double imageX = 4384;
    private static final double imageY = 2466;

    // the saved chessboard image
    private static Mat savedImage;
    // the calibrated camera frame
    private static Image undistortedImage;

    // various variables needed for the calibration
    private static List<Mat> imagePoints;
    private static List<Mat> objectPoints;
    private static MatOfPoint3f obj;
    private static MatOfPoint2f imageCorners;
    private static int boardsNumber;
    private static int numCornersHor;
    private static int numCornersVer;
    private static Mat intrinsic;
    private static Mat distCoeffs;
    private static boolean isCalibrated;

    /**
     * Init all the (global) variables needed in the controller
     */
    public static void init()
    {
        numCornersHor = 9;
        numCornersVer = 6;
        obj = new MatOfPoint3f();
        imageCorners = new MatOfPoint2f();
        savedImage = new Mat();
        undistortedImage = null;
        imagePoints = new ArrayList<>();
        objectPoints = new ArrayList<>();
        intrinsic = new Mat(3, 3, CvType.CV_32FC1);
        distCoeffs = new Mat();
        boardsNumber = 2;
        isCalibrated = false;
        int numSquares = numCornersHor*numCornersVer;
        for (int j = 0; j < numSquares; j++)
            obj.push_back(new MatOfPoint3f(new Point3(j / numCornersHor, j % numCornersVer, 0.0f)));
    }

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

    public static Bitmap processImage(Bitmap image) {
        identifyLitter(image, DroneState.getLatLng());
        convertLatestFrame();
        return CVPreview;
    }

    public static void readFrame(Bitmap image) {
        Utils.bitmapToMat(image, currentMat);
    }

    public static ArrayList<LatLng> identifyLitter(Bitmap photo, LatLng origin) {
        readFrame(photo);
        correctDistortion();
        ArrayList<Point> points = detectBlobs();
        return calculateGPSCoords(points, origin);
    }

    public static Bitmap getCVPreview() {
        return CVPreview;
    }

    /**
     * Detect blobs in an image using edge detection, closing, filling and thresholding
     * Returns a list of blob centres in terms of points on the image
     */
    public static ArrayList<Point> detectBlobs() {
        processingMat = currentMat;
        Imgproc.cvtColor(processingMat, processingMat, Imgproc.COLOR_BGR2GRAY);
        double cannyThresh = determineCannyThreshold();
        Imgproc.Canny(processingMat, processingMat, cannyThresh, cannyThresh*2);
        closeImage();
        Imgproc.threshold(processingMat, processingMat, 0, 255, Imgproc.THRESH_BINARY);
        fillImage();
        //TODO determine below threshold parameter from the drone's altitude and FOV
        eliminateSmallBlobs(600);
        clearBorders();
        Imgproc.medianBlur(processingMat, processingMat, 31);
        ArrayList<Point> centres = findBlobCentres();
        currentMat = processingMat;
        return centres;
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
        Mat temp = processingMat.clone();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        fillContours(contours, 255);
    }

    /**VCR1honey
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
        return Imgproc.threshold(currentMat, _, 0, 255, Imgproc.THRESH_OTSU);
    }

    /**
     * Eliminate objects that are too small (noise)
     */
    public static ArrayList<MatOfPoint> eliminateSmallBlobs(double threshold) {
        Mat temp = processingMat.clone();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        ArrayList<Double> areas = new ArrayList<>();
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        ArrayList<MatOfPoint> badContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            areas.add(Imgproc.contourArea(contour));
            if (Imgproc.contourArea(contour) < threshold) {
                badContours.add(contour);
            }
        }
        fillContours(badContours, 0);
        contours.removeAll(badContours);
        return contours;
    }

    /**
     * Eliminate shapes which touch the border of the image
     */
    public static void clearBorders() {
        Mat temp = processingMat.clone();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        int width = processingMat.width();
        int height = processingMat.height();

        ArrayList<MatOfPoint> badContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            for (Point p : contour.toArray()) {
                if (p.x <= 0 || p.x >= width || p.y <= 0 || p.y >= height) {
                    badContours.add(contour);
                }
            }
        }
        fillContours(badContours, 0);
    }

    /**
     * Determine the centroid of each detected blob
     * @return
     */
    public static ArrayList<Point> findBlobCentres() {
        Mat temp = processingMat.clone();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        currentBlobs = contours;
        ArrayList<Point> centres = new ArrayList<>();
        for (MatOfPoint contour : currentBlobs) {
            centres.add(contourCentroid(contour));
        }
        return centres;
    }

    /**
     * Uses images moments to determine the centroid of the given contour
     * @param contour
     * @return
     */
    public static Point contourCentroid(MatOfPoint contour) {
        Moments moment = Imgproc.moments(contour);
        int x = (int)moment.get_m10() / (int)moment.get_m00();
        int y = (int)moment.get_m01() / (int)moment.get_m00();
        return new Point(x, y);
    }

    public static double contourSize(MatOfPoint contour) {
        return Imgproc.contourArea(contour);
    }

    public static void fillContours(ArrayList<MatOfPoint> contours, int colour) {
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(processingMat, contours, i, new Scalar(colour), -1);
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
        for (Point p : points) {
            double distance = pixelDistance(p, new Point(currentMat.width() / 2, currentMat.height() / 2));
            if (distance < minDistance) {
                minDistance = distance;
                minPoint = p;
            }
        }
        return minPoint;
    }

    /**
     * TODO Implement this
     * Return the current distance of the drone from the target being tracked
     * @return
     */
    public static double distanceFromTarget(AngularController.ActiveAngle angle) {
        return 10;
    }

    public static double pixelDistance(Point p1, Point p2) {
       return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    /**
     * Begin tracking the object closest to the centre of the camera
     */
    public static void startTrackingObject() {
        isTracking = true;
        ArrayList<Point> centres = detectBlobs();
        Point object = closestToCentre(centres);
        int index = centres.indexOf(object);
        trackingObject = new TrackingObject(object, contourSize(currentBlobs.get(index)), DroneState.getLatLng(), DroneState.getAltitude());
    }

    /**
     * Perform one "tracking interation" where we predict where the object should be in the current
     * frame based on where it was and how far we've moved. If we find a blob near that location,
     * it must be the tracked object. Update the track object to this new location and return the
     * point identified
     * @return
     */
    public static Point trackObject() {
        if (!isTracking) {
            MessageHandler.w("Not tracking object!");
            return null;
        }
        TrackingObject tmp = trackingObject.predictPositionAndSize(DroneState.getLatLng(), DroneState.getAltitude());
        ArrayList<Point> centres = detectBlobs();
        Point trackerObj = null;
        int index = 0;
        for (Point centre: centres) {
            //If we are within 50 pixels of the assumed new location of the blob
            if (pixelDistance(centre, tmp.getPosition()) < 50 && Math.abs(contourSize(currentBlobs.get(index)) - tmp.getSize()) < 10) {
                trackerObj = centre;
                break;
            }
            index++;
        }
        if (trackerObj != null) {
            trackingObject = tmp;
            return trackerObj;
        } else {
            MessageHandler.w("Lost track of object!");
            return null;
        }
    }

    public static void stopTrackingObject() {
        isTracking = false;
        trackingObject = null;
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static double metresToPixels(double metres, double altitude){
        double degrees = Math.atan(metres/altitude);
        return degrees/CAMERA_FOVX * imageX;
    }

        /**
         * Correct distorted input stream using the chessboard pattern
         */
        public static void correctDistortion()
        {
            if (isCalibrated)
            {
                processingMat = currentMat;
                Mat correctedMat = new Mat();
                Imgproc.undistort(processingMat, correctedMat, intrinsic, distCoeffs);
                currentMat = correctedMat;
                //For testing
                /*
                Bitmap preview = Bitmap.createBitmap(processingMat.width(), processingMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(correctedMat, preview);
                CVPreview = preview;*/
            }
        }

        private static void loadCalibrationImages()
        {
            init();
            try {
                //TODO take new checkerboard images that do not fail and change boardsNumber appropriately
                for (int j = 1; j < boardsNumber+1; j++)
                {
                    InputStream i = ContextManager.getActivity().getAssets().open("calibration" + j + ".jpg");
                    Bitmap decoded = BitmapFactory.decodeStream(i);
                    int nh = (int) ( decoded.getHeight() * (512.0 / decoded.getWidth()) );
                    Bitmap scaled = Bitmap.createScaledBitmap(decoded, 512, nh, true);
                    readFrame(scaled);
                    savedImage = currentMat;
                    findAndDrawPoints();
                }
            } catch (IOException e) {
                MessageHandler.d("Error Loading Calibration Images: " + e);
            }
            // reach the correct number of images needed for the calibration
            calibrateCamera();
        }
        /**
         * Find and draws the points needed for the calibration on the chessboard
         *
         */
        private static void findAndDrawPoints()
        {
            // init
            Mat grayImage = new Mat();
            // I would perform this operation only before starting the calibration
            // process
            // convert the frame in gray scale
            Imgproc.cvtColor(savedImage, grayImage, Imgproc.COLOR_BGR2GRAY);
            // the size of the chessboard
            Size boardSize = new Size(numCornersHor, numCornersVer);

            // look for the inner chessboard corners
            boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners,
                    Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
            if (found)
            {
                TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
                Imgproc.cornerSubPix(grayImage, imageCorners, new Size(11, 11), new Size(-1, -1), term);
                // save the current frame for further elaborations
                grayImage.copyTo(savedImage);
                // show the chessboard inner corners on screen
                Calib3d.drawChessboardCorners(savedImage, boardSize, imageCorners, found);
                imagePoints.add(imageCorners);
                objectPoints.add(obj);
            }
        }

    private static void calibrateCamera()
    {
        // init needed variables according to OpenCV docs
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        intrinsic.put(0, 0, 1);
        intrinsic.put(1, 1, 1);

        // calibrate!
        Calib3d.calibrateCamera(objectPoints, imagePoints, savedImage.size(), intrinsic, distCoeffs, rvecs, tvecs);
        Bitmap preview = Bitmap.createBitmap(currentMat.width(), currentMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(currentMat, preview);
        CVPreview = preview;
        isCalibrated = true;
    }

    public static class CalibrateTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            loadCalibrationImages();
            return true;
        }
    }
}

