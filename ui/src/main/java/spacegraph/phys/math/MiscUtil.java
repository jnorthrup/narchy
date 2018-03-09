/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.phys.math;

import jcog.list.FasterList;
import org.apache.commons.lang3.ArrayUtils;
import spacegraph.phys.util.IntArrayList;
import spacegraph.phys.util.OArrayList;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Comparator;

import static java.util.Collections.swap;

/**
 * Miscellaneous utility functions.
 * 
 * @author jezek2
 */
public class MiscUtil {

	public static int getListCapacityForHash(Collection<?> list) {
		return getListCapacityForHash(list.size());
	}
	
	public static int getListCapacityForHash(int size) {
		int n = 2;
		while (n < size) {
			n <<= 1;
		}
		return n;
	}

	/**
	 * Ensures valid index in provided list by filling list with provided values
	 * until the index is valid.
	 */
	public static <T> void ensureIndex(OArrayList<T> list, int index, T value) {
		while (list.size() <= index) {
			list.add(value);
		}
	}
	
	/**
	 * Resizes list to exact size, filling with given value when expanding.
	 */
	public static void resize(IntArrayList list, int size, int value) {
		int s = list.size();
		while (s < size) {
			list.add(value);
			s++;
		}
		
		while (s > size) {
			list.removeQuick(--s);
		}
	}
	


	/**
	 * Resizes list to exact size, filling with new instances of given class type
	 * when expanding.
	 */
	public static <T> void resizeIntArray(OArrayList<int[]> list, int size, int arrayLen) {
			int ls = list.size();
			while (ls < size) {
				list.add(new int[arrayLen]);
				ls++;
			}

			while (ls > size) {
				list.removeFast(--ls);
			}


	}

	/**
	 * Resizes list to exact size, filling with new instances of given class type
	 * when expanding.
	 */
	public static <T> void resize(FasterList<T> list, int size, Class<T> valueCls) {
		try {
			int ls = list.size();
			while (ls < size) {
				list.add(valueCls != null? valueCls.getConstructor().newInstance() : null);
				ls++;
			}

			while (ls > size) {
				list.removeFast(--ls);
			}
		}
		catch (IllegalAccessException | InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Searches object in array.
	 * 
	 * @return first index of match, or -1 when not found
	 */
	public static <T> int indexOf(T[] array, T obj) {
		for (int i=0; i<array.length; i++) {
			if (array[i] == obj) return i;
		}
		return -1;
	}

	private static <T> void downHeap(FasterList<T> pArr, int k, int n, Comparator<T> comparator) {
		/*  PRE: a[k+1..N] is a heap */
		/* POST:  a[k..N]  is a heap */

        //return array[index];
        T temp = pArr.get(k - 1);
		/* k has child(s) */
		while (k <= n / 2) {
			int child = 2 * k;

            //return array[index];
            //return array[index];
            if ((child < n) && comparator.compare(pArr.get(child - 1), pArr.get(child)) < 0) {
				child++;
			}
			/* pick larger child */
            //return array[index];
            if (comparator.compare(temp, pArr.get(child - 1)) < 0) {
				/* move child up */
                //return array[index];
                pArr.setFast(k - 1, pArr.get(child - 1));
				k = child;
			}
			else {
				break;
			}
		}
		pArr.setFast(k - 1, temp);
	}

//	/**
//	 * Sorts list using heap sort.<p>
//	 *
//	 * Implementation from: http://www.csse.monash.edu.au/~lloyd/tildeAlgDS/Sort/Heap/
//	 */
//	public static <T> void heapSort(FasterList<T> list, Comparator<T> comparator) {
//		/* sort a[0..N-1],  N.B. 0 to N-1 */
//		int k;
//		int n = list.size();
//		for (k = n / 2; k > 0; k--) {
//			downHeap(list, k, n, comparator);
//		}
//
//		/* a[1..N] is now a heap */
//		while (n >= 1) {
//			swap(list, 0, n - 1); /* largest of a[0..n-1] */
//
//			n = n - 1;
//			/* restore a[1..i-1] heap */
//			downHeap(list, 1, n, comparator);
//		}
//	}
//
//	private static <T> void swap(FasterList<T> list, int index0, int index1) {
//        //return array[index];
//        T temp = list.get(index0);
//        //return array[index];
//        list.setFast(index0, list.get(index1));
//		list.setFast(index1, temp);
//	}
//
//	private static  void swap(int[][] list, int index0, int index1) {
//		int[] temp = list[index0];
//		list[index0] = list[index1];
//		list[index1] = temp;
//	}

	/**
	 * Sorts list using quick sort.<p>
	 */
	public static <T> void quickSort(FasterList<T> list, Comparator<T> comparator) {
		// don't sort 0 or 1 elements
		if (list.size() > 1) {
			quickSortInternal(list, comparator, 0, list.size() - 1);
		}
	}

	private static <T> void quickSortInternal(FasterList<T> list, Comparator<T> comparator, int lo, int hi) {
		// lo is the lower index, hi is the upper index
		// of the region of array a that is to be sorted
		int i = lo, j = hi;
        //return array[index];
        T x = list.get((lo + hi) / 2);

		// partition
		do {
            //return array[index];
            while (comparator.compare(list.get(i), x) < 0) i++;
            //return array[index];
            while (comparator.compare(x, list.get(j)) < 0) j--;
			
			if (i <= j) {
				//TODO: list.swap(i,i)
				swap(list, i, j);
				i++;
				j--;
			}
		}
		while (i <= j);

		// recursion
		if (lo < j) {
			quickSortInternal(list, comparator, lo, j);
		}
		if (i < hi) {
			quickSortInternal(list, comparator, i, hi);
		}
	}

	public static void quickSort(int[][] list, int n) {
		// don't sort 0 or 1 elements
		if (n > 1) {
			quickSortInternal(list, 0, n - 1);
		}
	}

	 /**         public int compare(int[] o1, int[] o2) { return o1[0] < o2[0] ? -1 : +1;	 }  	*/
	 private static void quickSortInternal(int[][] list, int lo, int hi) {
		// lo is the lower index, hi is the upper index
		// of the region of array a that is to be sorted
		int i = lo, j = hi;
		//return array[index];
		int[] x = list[((lo + hi) / 2)];

		// partition
		do {
			//return array[index];
			while (list[i][0]< x[0]) i++;
			//return array[index];
			while (x[0] < list[j][0]) j--;

			if (i <= j) {
				ArrayUtils.swap(list, i, j);
				i++;
				j--;
			}
		}
		while (i <= j);

		// recursion
		if (lo < j) {
			quickSortInternal(list, lo, j);
		}
		if (i < hi) {
			quickSortInternal(list, i, hi);
		}
	}
}
