package com.jarone.litterary.imageproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Base64;

import com.google.android.gms.maps.model.LatLng;
import com.google.myjson.Gson;
import com.google.myjson.JsonObject;
import com.google.myjson.JsonParser;
import com.jarone.litterary.control.AngularController;
import com.jarone.litterary.handlers.MessageHandler;
import com.jarone.litterary.helpers.ContextManager;
import com.jarone.litterary.helpers.FileAccess;
import com.jarone.litterary.helpers.SerializationUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
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

    //The temporary image used for intermediate processing
    private static Mat processingMat;

    //Stores the list of blobs detected from the current Mat
    private static ArrayList<MatOfPoint> currentBlobs;

    //Thresholding values for Canny edge detector in blob detector
    private static double highThreshold;
    private static double lowThreshold;

    //The Bitmap representation of the current result image
    private static Bitmap CVPreview = null;

    private static TrackingObject trackingObject;

    private static boolean isTracking;

    private static TrackingObject trackedObject;
    private static Mat bgr;
    private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255);

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
    public static Mat intrinsic;
    public static Mat distCoeffs;
    private static boolean isCalibrated;

    private static ArrayList<Point> blobCentres;

    /**
     * Init all the (global) variables needed in the controller
     */
    public static void init() {
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
        boardsNumber = 9;
        isCalibrated = false;
        int numSquares = numCornersHor * numCornersVer;
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

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, ContextManager.getActivity(), loaderCallback);
    }

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
     * Set the "original" unmodified image as a Mat
     *
     * @param frame
     */
    public static void setOriginalImage(Bitmap frame) {
        originalMat = new Mat();
        Utils.bitmapToMat(frame, originalMat);
    }

    /**
     * Reads a bitmap image and stores it on currentMat
     *
     * @param image
     */
    public static void readFrame(Bitmap image) {
        Utils.bitmapToMat(image, currentMat);
    }

    /***
     * Identify blobs on the given Mat and return the identified litter points
     * @param photo
     * @return
     */
    public static ArrayList<Point> identifyLitter(Bitmap photo) {
        Mat mat = new Mat();
        Utils.bitmapToMat(photo, mat);
        //correctDistortion();
        ArrayList<Point> points = detectBlobs(mat);
        ContextManager.getMainActivityInstance().setProcessing(false);
        return points;
    }

    public static Mat identifyLitterMat(Bitmap photo) {
        Mat mat = new Mat();
        Utils.bitmapToMat(photo, mat);
        ArrayList<Point> points = detectBlobs(mat);
        ContextManager.getMainActivityInstance().setProcessing(false);
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

    /**
     * Detect blobs in an image using edge detection, closing, filling and thresholding
     * Returns a list of blob centres in terms of points on the image
     */
    public static ArrayList<Point> detectBlobs(Mat mat) {
        if (mat.empty()) {
            return null;
        }
        Mat processing = new Mat();
        mat.copyTo(processing);
        Imgproc.cvtColor(processing, processing, Imgproc.COLOR_BGR2GRAY);
        determineCannyThreshold(processing);
        Imgproc.Canny(processing, processing, lowThreshold, highThreshold);
        closeImage(processing);
        Imgproc.threshold(processing, processing, 0, 255, Imgproc.THRESH_BINARY);
        fillImage(processing);

        //TODO determine below threshold parameter from the drone's altitude and FOV
        //eliminateSmallBlobs(processing, Math.pow(metresToPixels(0.3, DroneState.getAltitude()), 2));
        //clearBorders();
        blobCentres = findBlobCentres(processing);
        processing.copyTo(mat);

        //MEDIANBLUR NOT NECESSARY AND MAKES THINGS VERY SLOW --vic&adam
//        Imgproc.medianBlur(processing, processing, 31);


        return blobCentres;
    }

    /**
     * Perform closing operation on the image, first downscaling to speed up processing
     */
    public static void closeImage(Mat mat) {
        int scaleFactor = 10;
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(150 / scaleFactor, 150 / scaleFactor));
        //Rescale to smaller size to perform closing much faster
        int width = mat.width();
        int height = mat.height();
        Imgproc.resize(mat, mat, new Size(mat.width() / scaleFactor, mat.height() / scaleFactor));
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, element);
        Imgproc.resize(mat, mat, new Size(width, height));
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
    public static Bitmap convertFrame(Mat mat) {
        if (CVPreview == null && mat != null && mat.width() > 0) {
            CVPreview = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        } else if (mat == null || mat.width() <= 0) {
            return null;
        }
        Mat tmp = new Mat();
        mat.copyTo(tmp);
        Utils.matToBitmap(tmp, CVPreview);
        return CVPreview;
    }

    /**
     * Use Otsu thresholding to determine a good threshold value for Canny edge detection
     *
     * @return
     */

    public static void determineCannyThreshold(Mat mat) {
        Mat _ = new Mat();
        highThreshold = Imgproc.threshold(mat, _, 127, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);
        lowThreshold = highThreshold * 0.3;
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
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        int width = temp.width();
        int height = temp.height();
        temp.release();
        ArrayList<MatOfPoint> badContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            for (Point p : contour.toArray()) {
                if (p.x <= 0 || p.x >= width || p.y <= 0 || p.y >= height) {
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
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
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
        }catch (ArithmeticException e) {
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
        for (Point p : points) {
            double distance = pixelDistance(p, new Point(currentMat.width() / 2, currentMat.height() / 2));
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
    public static double distanceFromTarget(AngularController.ActiveAngle angle) {
        if (angle == AngularController.ActiveAngle.PITCH) {
            return originalMat.height() / 2 - trackedObject.getPosition().y;
        } else {
            return originalMat.width() / 2 - trackedObject.getPosition().x;
        }
    }

    public static double pixelDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    /**
     * Perform one "tracking interation" where we predict where the object should be in the current
     * frame based on where it was and how far we've moved. If we find a blob near that location,
     * it must be the tracked object. Update the track object to this new location and return the
     * point identified
     *
     * @return
     */
//    public static Point trackObject() {
//        if (!isTracking) {
//            MessageHandler.w("Not tracking object!");
//            return null;
//        }
//        TrackingObject tmp = trackingObject.predictPositionAndSize(DroneState.getLatLng(), DroneState.getAltitude());
//        Point trackerObj = null;
//        double trackerSize = 0;
//
//        int index = 0;
//        for (Point centre: blobCentres) {
//            //If we are within 50 pixels of the assumed new location of the blob
//            double size = contourSize(currentBlobs.get(index));
//            if (pixelDistance(centre, tmp.getPosition()) < 100000 && (Math.abs(size) - tmp.getSize()) < 100000) {
//                trackerObj = centre;
//                trackerSize = size;
//                break;
//            }
//            index++;
//        }
//
//        if (trackerObj != null) {
//            trackingObject = new TrackingObject(trackerObj, trackerSize, DroneState.getLatLng(), DroneState.getAltitude());
//            return trackerObj;
//        } else {
//            MessageHandler.w("Lost track of object!");
//            return null;
//        }
//    }
    public static boolean isTracking() {
        return isTracking;
    }

    public static double metresToPixels(double metres, double altitude) {
        double degrees = Math.atan(metres / altitude);
        return degrees / CAMERA_FOVX * imageX;
    }

    /**
     * Correct distorted input stream using the chessboard pattern
     */
    public static void correctDistortion(Mat mat) {
        if (isCalibrated) {
            Mat correctedMat = new Mat();
            Imgproc.undistort(mat, correctedMat, intrinsic, distCoeffs);
            correctedMat.copyTo(mat);
            //For testing
            /*
            Bitmap preview = Bitmap.createBitmap(processingMat.width(), processingMat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(correctedMat, preview);
            CVPreview = preview;*/
        }
    }

    private static void loadCalibrationImages() {
        init();
        try {
            //TODO take new checkerboard images that do not fail and change boardsNumber appropriately
            for (int j = 1; j < boardsNumber + 1; j++) {
                InputStream i = ContextManager.getActivity().getAssets().open("c" + j + ".jpg");
                Bitmap decoded = BitmapFactory.decodeStream(i);
                int nh = (int) (decoded.getHeight() * (2000.0 / decoded.getWidth()));
                Bitmap scaled = Bitmap.createScaledBitmap(decoded, 2000, nh, true);
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
     */
    private static void findAndDrawPoints() {
        // init
        MessageHandler.d("Finding points");
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
        MessageHandler.d("Done finding");
        if (found) {
            MessageHandler.d("Find successful");
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

    private static void calibrateCamera() {
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
        saveCalibration();
        isCalibrated = true;
    }

    public static void saveCalibration() {
        FileAccess.saveToFile("calibration", "dist", matToJson(distCoeffs));
        FileAccess.saveToFile("calibration", "intrinsic", matToJson(intrinsic));
    }

    public static void loadCalibration() {
        String dist = FileAccess.loadFromFile("calibration", "dist");
        String intrins = FileAccess.loadFromFile("calibration", "intrinsic");

        distCoeffs = matFromJson(dist);
        intrinsic = matFromJson(intrins);
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

    public static String matToJson(Mat mat) {
        JsonObject obj = new JsonObject();

        if (mat.isContinuous()) {
            int cols = mat.cols();
            int rows = mat.rows();
            int elemSize = (int) mat.elemSize();
            int type = mat.type();

            obj.addProperty("rows", rows);
            obj.addProperty("cols", cols);
            obj.addProperty("type", type);

            // We cannot set binary data to a json object, so:
            // Encoding data byte array to Base64.
            String dataString;

            if (type == CvType.CV_32S || type == CvType.CV_32SC2 || type == CvType.CV_32SC3 || type == CvType.CV_16S) {
                int[] data = new int[cols * rows * elemSize];
                mat.get(0, 0, data);
                dataString = new String(Base64.encode(SerializationUtils.toByteArray(data), Base64.DEFAULT));
            } else if (type == CvType.CV_32F || type == CvType.CV_32FC2) {
                float[] data = new float[cols * rows * elemSize];
                mat.get(0, 0, data);
                dataString = new String(Base64.encode(SerializationUtils.toByteArray(data), Base64.DEFAULT));
            } else if (type == CvType.CV_64F || type == CvType.CV_64FC2) {
                double[] data = new double[cols * rows * elemSize];
                mat.get(0, 0, data);
                dataString = new String(Base64.encode(SerializationUtils.toByteArray(data), Base64.DEFAULT));
            } else if (type == CvType.CV_8U) {
                byte[] data = new byte[cols * rows * elemSize];
                mat.get(0, 0, data);
                dataString = new String(Base64.encode(data, Base64.DEFAULT));
            } else {

                throw new UnsupportedOperationException("unknown type");
            }
            obj.addProperty("data", dataString);

            Gson gson = new Gson();
            String json = gson.toJson(obj);

            return json;
        } else {
            System.out.println("Mat not continuous.");
        }
        return "{}";
    }

    public static Mat matFromJson(String json) {


        JsonParser parser = new JsonParser();
        JsonObject JsonObject = parser.parse(json).getAsJsonObject();

        int rows = JsonObject.get("rows").getAsInt();
        int cols = JsonObject.get("cols").getAsInt();
        int type = JsonObject.get("type").getAsInt();

        Mat mat = new Mat(rows, cols, type);

        String dataString = JsonObject.get("data").getAsString();
        if (type == CvType.CV_32S || type == CvType.CV_32SC2 || type == CvType.CV_32SC3 || type == CvType.CV_16S) {
            int[] data = SerializationUtils.toIntArray(Base64.decode(dataString.getBytes(), Base64.DEFAULT));
            mat.put(0, 0, data);
        } else if (type == CvType.CV_32F || type == CvType.CV_32FC2) {
            float[] data = SerializationUtils.toFloatArray(Base64.decode(dataString.getBytes(), Base64.DEFAULT));
            mat.put(0, 0, data);
        } else if (type == CvType.CV_64F || type == CvType.CV_64FC2) {
            double[] data = SerializationUtils.toDoubleArray(Base64.decode(dataString.getBytes(), Base64.DEFAULT));
            mat.put(0, 0, data);
        } else if (type == CvType.CV_8U) {
            byte[] data = Base64.decode(dataString.getBytes(), Base64.DEFAULT);
            mat.put(0, 0, data);
        } else {

            throw new UnsupportedOperationException("unknown type");
        }
        return mat;
    }

    public static ArrayList<Point> getBlobCentres() {
        return blobCentres;
    }

    public static void createTrackedObject(Mat mRgba, Rect region) {
        trackedObject.hsv = new Mat(mRgba.size(), CvType.CV_8UC3);
        trackedObject.mask = new Mat(mRgba.size(), CvType.CV_8UC1);
        trackedObject.hue = new Mat(mRgba.size(), CvType.CV_8UC1);
        trackedObject.prob = new Mat(mRgba.size(), CvType.CV_8UC1);
        updateHueImage();

        //create a histogram representation of the object
        Mat tempmask = trackedObject.mask.submat(region);

        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        MatOfInt histSize = new MatOfInt(25);

        List<Mat> images = Arrays.asList(trackedObject.hueArray.get(0).submat(region));
        Imgproc.calcHist(images, new MatOfInt(0), tempmask, trackedObject.hist, histSize, ranges);

        Core.normalize(trackedObject.hist, trackedObject.hist);
        trackedObject.prevRect = region;
    }

    public static void updateHueImage() {

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
                trackObject();
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
        trackedObject = null;
    }
}