package com.jarone.litterary;

/**
 * Point on 2D landscape
 * 
 * @author Roman Kushnarenko (sromku@gmail.com)</br>
 */
public class Point
{
	public Point(double x, double y)
	{
		this.latitude = x;
		this.longitude = y;
	}

	public double latitude;
	public double longitude;

	@Override
	public String toString()
	{
		return String.format("(%.2f,%.2f)", latitude, longitude);
	}
}