package jcog.math;

import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.NoSuchElementException;

/** from http://stackoverflow.com/questions/2920315/permutation-of-array */
public class Permutations {

	int size;

	/** total possible */
	int num;

	/** current iteration */
	int count;

	
	protected int[] ind;

	

	public Permutations() {
	}

	public Permutations restart(int size) {

		
		this.size = size;

        int[] ind = this.ind;
		if (ind == null || ind.length < size) {
			this.ind = ind = new int[size];
		}

		for (int i = 0; i < size; i++) {
			ind[i] = i;
		}
		count = -1;
		num = (int) CombinatoricsUtils.factorial(size);

		return this;
	}

	public final boolean hasNext() {
		return count < num - 1;
	}
	public final boolean hasNextThenNext() {
		if (hasNext()) {
			next();
			return true;
		}
		return false;
	}

	/**
	 * Computes next permutations. Same array instance is returned every time!
	 * 
	 * @return
	 */
	public final int[] next() {
        int size = this.size;

        int count = (++this.count);

		if (count == (num))
			throw new NoSuchElementException();

        int[] ind = this.ind;

		if (count == 0) {
			
			return ind;
		}

		for (int tail = size - 1; tail > 0; tail--) {

            int tailMin1 = tail - 1;

            int itm = ind[tailMin1];

			if (itm < ind[tail]) {


                int s = size - 1;
				while (itm >= ind[s])
					s--;

				swap(ind, tailMin1, s);

				
				for (int i = tail, j = size - 1; i < j; i++, j--) {
					swap(ind, i, j);
				}
				break;
			}

		}

		return ind;
	}

	public int permute(int index) {
		return ind[index];
	}

	private static void swap(int[] arr, int i, int j) {
        int t = arr[i];
		arr[i] = arr[j];
		arr[j] = t;
	}

	public int total() {
		return num;
	}
}
