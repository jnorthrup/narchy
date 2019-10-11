package jcog.sort;

import jcog.data.array.IntComparator;
import jcog.util.ArrayUtil;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;

public class QuickSort {
	public static final int SMALL = 3;
	private static final int MEDIUM = 40;

	/**
	 * Sorts the specified range of elements using the specified swapper and according to the order induced by the specified
	 * comparator using quicksort.
	 * <p>
	 * <p>The sorting algorithm is a tuned quicksort adapted from Jon L. Bentley and M. Douglas
	 * McIlroy, &ldquo;Engineering a Sort Function&rdquo;, <i>Software: Practice and Experience</i>, 23(11), pages
	 * 1249&minus;1265, 1993.
	 *
	 * @param from    the index of the first element (inclusive) to be sorted.
	 * @param to      the index of the last element (exclusive) to be sorted.
	 * @param comp    the comparator to determine the order of the generic data.
	 * @param swapper an object that knows how to swap the elements at any two positions.
	 */
	public static void quickSort(int from, int to, IntComparator comp, IntIntProcedure swapper) {
		int len;
		while ((len = to - from) > 1) {

			if (len < SMALL) {
				//bubble sort
				for (int i = from; i < to; i++)
					for (int j = i; j > from && (comp.compare(j - 1, j) > 0); j--) {
						swapper.value(j, j - 1);
					}
				return;
			}


			int m = from + len / 2;
			if (len > SMALL) {
				int l = from;
				int n = to - 1;
				if (len > MEDIUM) {
					int s = len / 8;
					l = med3(l, l + s, l + 2 * s, comp);
					m = med3(m - s, m, m + s, comp);
					n = med3(n - 2 * s, n - s, n, comp);
				}
				m = med3(l, m, n, comp);
			}


			int a = from;
			int b = a;
			int c = to - 1;

			int d = c;
			while (true) {
				int comparison;
				while (b <= c && ((comparison = comp.compare(b, m)) <= 0)) {
					if (comparison == 0) {
						if (a == m) m = b;
						else if (b == m) m = a;
						swapper.value(a++, b);
					}
					b++;
				}
				while (c >= b && ((comparison = comp.compare(c, m)) >= 0)) {
					if (comparison == 0) {
						if (c == m) m = d;
						else if (d == m) m = c;
						swapper.value(c, d--);
					}
					c--;
				}
				if (b > c) break;
				if (b == m) m = d;
				swapper.value(b++, c--);
			}


			int s = Math.min(a - from, b - a);
			vecSwap(swapper, from, b - s, s);

			s = Math.min(d - c, to - d - 1);
			vecSwap(swapper, b, to - s, s);


			if ((s = b - a) > 1) {
				//TODO push
				quickSort(from, from + s, comp, swapper); //TODO non-recursive
				//TODO pop
			}

			if ((s = d - c) > 1) {
				//quickSort(to - s, to, comp, swapper);
				from = to-s;
			} else {
				//TODO pop , else done
				break; //done
			}
		}
	}

	/**
	 * Returns the index of the median of the three indexed chars.
	 */
	private static int med3(int a, int b, int c, IntComparator comp) {
		int ab = comp.compare(a, b);
		int ac = comp.compare(a, c);
		int bc = comp.compare(b, c);
		return (ab < 0 ?
			(bc < 0 ? b : ac < 0 ? c : a) :
			(bc > 0 ? b : ac > 0 ? c : a));
	}
	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecSwap(IntIntProcedure swapper, int from, int l, int s) {
		for (int i = 0; i < s; i++, from++, l++)
			swapper.value(from, l);
	}

	/**
	 * sorts descending
	 */
	public static void sort(int[] a, IntToFloatFunction v) {
		sort(a, 0, a.length, v);
	}

	public static void sort(int[] x, int left, int right /* inclusive */, IntToFloatFunction v) {
		quickSort(left, right, (a, b)->a==b ? 0 : Float.compare(v.valueOf(a), v.valueOf(b)), (a, b)-> ArrayUtil.swapInt(x, a, b));
	}

	public static <X> void sort(X[] x, int left, int right /* inclusive */, IntToDoubleFunction v) {
		quickSort(left, right, (a, b)->a==b ? 0 : Double.compare(v.valueOf(a), v.valueOf(b)), (a, b)-> ArrayUtil.swapObj(x, a, b));
	}

	/**
	 * sorts descending, left and right BOTH inclusive
	 */
	public static <X> void sort(X[] x, int left, int right /* inclusive */, FloatFunction<X> v) {
		quickSort(left, right, (a, b)->a==b ? 0 : Float.compare(v.floatValueOf(x[a]), v.floatValueOf(x[b])), (a, b)-> ArrayUtil.swapObj(x, a, b));
	}

	/** modifies order of input array */
	public static <X> X[] sort(X[] x,  FloatFunction<X> v) {
		sort(x, 0, x.length, v);
		return x;
	}
}
