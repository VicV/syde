package com.jarone.litterary.optimization;

/**
 * Point on 2D landscape
 * 
 * @author Roman Kushnarenko (sromku@gmail.com)</br>
 */
public class Point
{
	public Point(double x, double y)
	{
		this.longitude = x;
		this.latitude = y;
	}

	public double longitude;
	public double latitude;

	@Override
	public String toString()
	{
		return String.format("(%.2f,%.2f)", longitude, latitude);
	}
}