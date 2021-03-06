package com.jarone.litterary.optimization;

/**
 * Line is defined by starting point and ending point on 2D dimension.<br>
 * 
 * @author Roman Kushnarenko (sromku@gmail.com)
 */
public class Line
{
	private final Point _start;
	private final Point _end;
	private double _a = Double.NaN;
	private double _b = Double.NaN;
	private boolean _vertical = false;

	public Line(Point start, Point end)
	{
		_start = start;
		_end = end;

		if (_end.latitude - _start.latitude != 0)
		{
			_a = ((_end.longitude - _start.longitude) / (_end.latitude - _start.latitude));
			_b = _start.longitude - _a * _start.latitude;
		}

		else
		{
			_vertical = true;
		}
	}

	/**
	 * Indicate whereas the point lays on the line.
	 * 
	 * @param point
	 *            - The point to check
	 * @return <code>True</code> if the point lays on the line, otherwise return <code>False</code>
	 */
	public boolean isInside(Point point)
	{
		double maxX = _start.latitude > _end.latitude ? _start.latitude : _end.latitude;
		double minX = _start.latitude < _end.latitude ? _start.latitude : _end.latitude;
		double maxY = _start.longitude > _end.longitude ? _start.longitude : _end.longitude;
		double minY = _start.longitude < _end.longitude ? _start.longitude : _end.longitude;

		if ((point.latitude >= minX && point.latitude <= maxX) && (point.longitude >= minY && point.longitude <= maxY))
		{
			return true;
		}
		return false;
	}

	/**
	 * Indicate whereas the line is vertical. <br>
	 * For example, line like latitude=1 is vertical, in other words parallel to axis Y. <br>
	 * In this case the A is (+/-)infinite.
	 * 
	 * @return <code>True</code> if the line is vertical, otherwise return <code>False</code>
	 */
	public boolean isVertical()
	{
		return _vertical;
	}

	/**
	 * longitude = <b>A</b>latitude + B
	 * 
	 * @return The <b>A</b>
	 */
	public double getA()
	{
		return _a;
	}

	/**
	 * longitude = Ax + <b>B</b>
	 * 
	 * @return The <b>B</b>
	 */
	public double getB()
	{
		return _b;
	}

	/**
	 * Get start point
	 * 
	 * @return The start point
	 */
	public Point getStart()
	{
		return _start;
	}

	/**
	 * Get end point
	 * 
	 * @return The end point
	 */
	public Point getEnd()
	{
		return _end;
	}

	@Override
	public String toString()
	{
		return String.format("%s-%s", _start.toString(), _end.toString());
	}
}
