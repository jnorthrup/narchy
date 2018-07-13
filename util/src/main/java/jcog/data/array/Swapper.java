package jcog.data.array;

/**
 * Created by me on 6/8/15.
 */
@FunctionalInterface public interface Swapper {
	/**
	 * Swaps the data at the given positions.
	 * 
	 * @param a
	 *            the first position to swap.
	 * @param b
	 *            the second position to swap.
	 */
	void swap(int a, int b);
}
