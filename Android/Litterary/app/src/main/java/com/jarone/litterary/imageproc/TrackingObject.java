package com.jarone.litterary.imageproc;


import com.google.android.gms.maps.model.LatLng;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Rect;

import java.util.List;
import java.util.Vector;

/**
 * Created by Adam on 2016-02-23.
 */
public class TrackingObject {

    private double size;
    private Point position;
    private LatLng cameraLocation;
    private double cameraAltitude;

    public Mat hsv,hue,mask,prob;
    public Rect prevRect;
    public RotatedRect currBox;
    public Mat hist;
    public List<Mat> hsvArray,hueArray;


    public TrackingObject(Point position, double size, LatLng location, double altitude) {
        this.position = position;
        this.size = size;
        this.cameraLocation = location;
        this.cameraAltitude = altitude;
    }

    public TrackingObject()
    {
        this.hist=new Mat();
        this.prevRect=ImageProcessing.calculateStartingRect();
        this.currBox=new RotatedRect();
        this.hsvArray=new Vector<Mat>();
        this.hueArray=new Vector<Mat>();
    }

    public LatLng getCameraLocation() {
        return cameraLocation;
    }

    public Point getPosition() { return position; }

    public double getSize() { return size; }
}