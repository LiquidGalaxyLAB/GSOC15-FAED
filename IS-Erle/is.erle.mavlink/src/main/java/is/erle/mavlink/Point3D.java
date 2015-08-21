package is.erle.mavlink;

/**
 * Represents a 3D point in space. It has all the functions to operate on a
 * point like normalize, cross product, dot product , distance etc.
 * 
 * @author Abhay Kumar
 * @version %I%, %G%
 * @since 1.0.0
 * 
 */
public class Point3D
{
	/**
	 * Represents X coordinate/longitude.
	 */
	private double x;
	
	/**
	 * Represents Y coordinate/latitude.
	 */
	private double y;
	
	/**
	 * Represents Z coordinate/altitude.
	 */
	private double z;
	
	/**
	 * Represents the hash value of the class.
	 */
	private int hash = 0;

	/**
	 * Get X value/longitude.
	 * 
	 * @return Returns {@link #x}
	 */
	public final double getX()
	{
		return x;
	}

	/**
	 * Get Y value/latitude.
	 * 
	 * @return Returns {@link #y}
	 */
	public final double getY()
	{
		return y;
	}

	/**
	 * Get Z value/altitude.
	 * 
	 * @return Returns {@link #z}
	 */
	public final double getZ()
	{
		return z;
	}

	/**
	 * Constructor to initialize the X, Y and Z values.
	 * 
	 * @param x
	 *            {@link #x} value/longitude.
	 * @param y
	 *            {@link #y} value/latitude.
	 * @param z
	 *            {@link #z} value/altitude
	 */
	public Point3D(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Adds this Point3D object with another Point3D object.
	 * 
	 * @param point3D
	 *            Point3D object to be added to this object.
	 * @return Added values of this object.
	 */
	public Point3D add(Point3D point3D)
	{
		return add(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	/**
	 * Adds this Point3D object with another Point.
	 * 
	 * @param x
	 *            X value/longitude.
	 * @param y
	 *            Y value/latitude.
	 * @param z
	 *            Z value/altitude
	 * @return Added values of this object.
	 */
	public Point3D add(double x, double y, double z)
	{
		return new Point3D(getX() + x, getY() + y, getZ() + z);
	}

	/**
	 * Distance between this Point3D object with another point.
	 * 
	 * @param x1
	 *            X value/longitude.
	 * @param y1
	 *            Y value/latitude.
	 * @param z1
	 *            Z value/altitude
	 * @return Distance of the point from this object.
	 */
	public double distance(double x1, double y1, double z1)
	{
		double a = getX() - x1;
		double b = getY() - y1;
		double c = getZ() - z1;
		return Math.sqrt(a * a + b * b + c * c);
	}

	/**
	 * Distance between this Point3D object with another Point3D object.
	 * 
	 * @param point3D Another Point3D object.
	 * @return Distance of the point from this object.
	 */
	public double distance(Point3D point3D)
	{
		return distance(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	/**
	 * Subtracts this Point3D object with another Point.
	 * 
	 * @param x
	 *            X value/longitude.
	 * @param y
	 *            Y value/latitude.
	 * @param z
	 *            Z value/altitude
	 * @return Subtracted values of this object.
	 */
	public Point3D subtract(double x, double y, double z)
	{
		return new Point3D(getX() - x, getY() - y, getZ() - z);
	}

	/**
	 * Subtracts this Point3D object with another Point3D object.
	 * 
	 * @param point3D
	 *            Point3D object to be subtracted to this object.
	 * @return Subtracted values of this object.
	 */
	public Point3D subtract(Point3D point3D)
	{
		return subtract(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	/**
	 * Multiply this Point3D object with a constant.
	 * 
	 * @param constant Constant value to be multiplied to this object.
	 * @return Multiplied values of this object.
	 */
	public Point3D multiply(double constant)
	{
		return new Point3D(getX() * constant, getY() * constant, getZ() * constant);
	}

	/**
	 * Normalizes this Point3D object and returns a new normalized one without
	 * affecting this object.
	 * 
	 * @return Normalized values of this object.
	 */
	public Point3D normalize()
	{
		final double mag = magnitude();

		if (mag == 0.0)
		{
			return new Point3D(0.0, 0.0, 0.0);
		}

		return new Point3D(getX() / mag, getY() / mag, getZ() / mag);
	}

	/**
	 * Midpoint of this Point3D object and another point.
	 * 
	 * @param x
	 *            X value/longitude.
	 * @param y
	 *            Y value/latitude.
	 * @param z
	 *            Z value/altitude
	 * @return Midpoint value between 2 points.
	 */
	public Point3D midpoint(double x, double y, double z)
	{
		return new Point3D(x + (getX() - x) / 2.0, y + (getY() - y) / 2.0, z
				+ (getZ() - z) / 2.0);
	}

	/**
	 * Midpoint of this Point3D object and another Point3D object.
	 * 
	 * @param point3D
	 *            Another Point3D object.
	 * @return Midpoint value between 2 Point3D objects.
	 */
	public Point3D midpoint(Point3D point3D)
	{
		return midpoint(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	/**
	 * Angle between this Point3D object treated as a vector and another point
	 * vector.
	 * 
	 * @param x
	 *            X value/longitude.
	 * @param y
	 *            Y value/latitude.
	 * @param z
	 *            Z value/altitude
	 * @return Angle value between 2 point vectors.
	 */
	public double angle(double x, double y, double z)
	{
		final double ax = getX();
		final double ay = getY();
		final double az = getZ();

		final double delta = (ax * x + ay * y + az * z)
				/ Math.sqrt((ax * ax + ay * ay + az * az)
						* (x * x + y * y + z * z));

		if (delta > 1.0)
		{
			return 0.0;
		}
		if (delta < -1.0)
		{
			return 180.0;
		}

		return Math.toDegrees(Math.acos(delta));
	}

	/**
	 * Angle between this Point3D object treated as a vector and Point3D object
	 * treated as a vector.
	 * 
	 * @param point3D
	 *            Another Point3D object.
	 * @return Angle value between 2 point vectors.
	 */
	public double angle(Point3D point3D)
	{
		return angle(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	/**
	 * Angle between two Point3D object treated as a vector.
	 * 
	 * @param point3D1
	 *            A Point3D object.
	 * @param point3D2
	 *            Another Point3D object.
	 * @return Angle value between 2 point vectors.
	 */
	public double angle(Point3D point3D1, Point3D point3D2)
	{
		final double x = getX();
		final double y = getY();
		final double z = getZ();

		final double ax = point3D1.getX() - x;
		final double ay = point3D1.getY() - y;
		final double az = point3D1.getZ() - z;
		final double bx = point3D2.getX() - x;
		final double by = point3D2.getY() - y;
		final double bz = point3D2.getZ() - z;

		final double delta = (ax * bx + ay * by + az * bz)
				/ Math.sqrt((ax * ax + ay * ay + az * az)
						* (bx * bx + by * by + bz * bz));

		if (delta > 1.0)
		{
			return 0.0;
		}
		if (delta < -1.0)
		{
			return 180.0;
		}

		return Math.toDegrees(Math.acos(delta));
	}

	/**
	 * Calculates magnitude of this Point3D object.
	 * 
	 * @return Magnitude of this Point3D object.
	 */
	public double magnitude()
	{
		final double x = getX();
		final double y = getY();
		final double z = getZ();

		return Math.sqrt(x * x + y * y + z * z);
	}

	/**
	 * Dot product of this Point3D object treated as a vector and another point
	 * vector.
	 * 
	 * @param x
	 *            X value/longitude.
	 * @param y
	 *            Y value/latitude.
	 * @param z
	 *            Z value/altitude
	 * @return Dot product of 2 point vectors.
	 */
	public double dotProduct(double x, double y, double z)
	{
		return getX() * x + getY() * y + getZ() * z;
	}

	/**
	 * Dot product of this Point3D object treated as a vector and Point3D object
	 * treated as a vector.
	 * 
	 * @param point3D
	 *            Another Point3D object.
	 * @return Dot product value of 2 point vectors.
	 */
	public double dotProduct(Point3D point3D)
	{
		return dotProduct(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	/**
	 * Cross product of this Point3D object treated as a vector and another point
	 * vector.
	 * 
	 * @param x
	 *            X value/longitude.
	 * @param y
	 *            Y value/latitude.
	 * @param z
	 *            Z value/altitude
	 * @return Cross product of 2 point vectors.
	 */
	public Point3D crossProduct(double x, double y, double z)
	{
		final double ax = getX();
		final double ay = getY();
		final double az = getZ();

		return new Point3D(ay * z - az * y, az * x - ax * z, ax * y - ay * x);
	}

	/**
	 * Cross product of this Point3D object treated as a vector and Point3D object
	 * treated as a vector.
	 * 
	 * @param point3D
	 *            Another Point3D object.
	 * @return Cross product value of 2 point vectors.
	 */
	public Point3D crossProduct(Point3D point3D)
	{
		return crossProduct(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	/**
	 * Compares 2 Point3D objects for equality.
	 * 
	 * @param obj
	 *            Another Point3D object to be compared.
	 * @return <code>true</code> if both Point3D objects are equal,
	 *         <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;
		if (obj instanceof Point3D)
		{
			Point3D other = (Point3D) obj;
			return getX() == other.getX() && getY() == other.getY()
					&& getZ() == other.getZ();
		}
		else
			return false;
	}

	/**
	 * Generates and return hash values of this Point3D object.
	 * @return Hash value of this object.
	 */
	@Override
	public int hashCode()
	{
		if (hash == 0)
		{
			long bits = 7L;
			bits = 31L * bits + Double.doubleToLongBits(getX());
			bits = 31L * bits + Double.doubleToLongBits(getY());
			bits = 31L * bits + Double.doubleToLongBits(getZ());
			hash = (int) (bits ^ (bits >> 32));
		}
		return hash;
	}

	/**
	 * Gives string representation of this object.
	 * 
	 * @return String representation of this object
	 */
	@Override
	public String toString()
	{
		return "Point3D [x = " + getX() + ", y = " + getY() + ", z = " + getZ()
				+ "]";
	}

}
