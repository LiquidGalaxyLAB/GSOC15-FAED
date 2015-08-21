package is.erle.mavlink;

/**
 * Stores a minimum-maximum pair.
 * 
 * @author Abhay Kumar
 * @version %I%, %G%
 * @since 1.0.0
 * @param <T>
 *            Type of minimum maximum pair like double, int etc.
 */
public class MinMaxPair<T>
{
	/**
	 * Stores the minimum value of the pair.
	 */
	private T min;
	
	/**
	 * Stores the maximum value of the pair.
	 */
	private T max;

	/**
	 * Constructor to initialize this object.
	 * 
	 * @param min
	 *            Minimum value of the pair.
	 * @param max
	 *            Maximum value of the pair.
	 */
	public MinMaxPair(T min, T max)
	{
		this.min = min;
		this.max = max;
	}
	
	/**
	 * Gets minimum value of the pair.
	 * 
	 * @return Minimum value of the pair.
	 */
	public T getMin()
	{
		return min;
	}
	
	/**
	 * Gets maximum value of the pair.
	 * 
	 * @return Maximum value of the pair.
	 */
	public T getMax()
	{
		return max;
	}
	
	/**
	 * Updates the current value of minimum-maximum pair in this object.
	 * 
	 * @param min
	 *            Minimum value of the pair.
	 * @param max
	 *            Maximum value of the pair.
	 */
	public void update(T min, T max)
	{
		this.min = min;
		this.max = max;
	}
}
