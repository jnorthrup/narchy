package jcog.grammar.parse.examples.combinatorics;

import java.util.Iterator;
import java.util.stream.IntStream;

/*
 * Copyright (c) 2000 Steven J. Metsker. All Rights Reserved.
 * 
 * Steve Metsker makes no representations or warranties about
 * the fitness of this software for any particular purpose, 
 * including the implied warranty of merchantability.
 */

/**
 * The Permutations class provides an enumeration of all permutations of an
 * array of objects. Each 'permutation' is an ordered list of the group.
 * <p>
 * For example, to see all of the ways we can select a school representative and
 * an alternate from a list of 4 children, begin with an array of names::
 * <blockquote>
 * 
 * <pre>
 * Object[] children = { &quot;Leonardo&quot;, &quot;Monica&quot;, &quot;Nathan&quot;, &quot;Olivia&quot; };
 * </pre>
 * 
 * </blockquote> To see all 2-permutations of these 4 names, create and use a
 * Permutations enumeration: <blockquote>
 * 
 * <pre>
 * Permutations c = new Permutations(children, 2);
 * while (c.hasMoreElements()) {
 * 	Object[] perm = (Object[]) c.nextElement();
 * 	for (int i = 0; i &lt; perm.length; i++) {
 * 		System.out.print(perm[i] + &quot; &quot;);
 * 	}
 * 	System.out.println();
 * }
 * </pre>
 * 
 * </blockquote> This will print out: <blockquote>
 * 
 * <pre>
 * 	Leonardo Monica 
 * 	Leonardo Nathan 
 * 	Leonardo Olivia 
 * 	Monica Leonardo 
 * 	Monica Nathan 
 * 	Monica Olivia 
 * 	Nathan Leonardo 
 * 	Nathan Monica 
 * 	Nathan Olivia 
 * 	Olivia Leonardo 
 * 	Olivia Monica 
 * 	Olivia Nathan
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0
 */

public class Permutations implements Iterator<Object> {
	private Object[] inArray;
	private int n;
    private int m;
	private int[] index;
	private boolean hasMore = true;

	/**
	 * Create a Permutation to enumerate through all possible lineups of the
	 * supplied array of Objects.
	 * 
	 * @param Object
	 *            [] inArray the group to line up
	 * 
	 * @exception CombinatoricException
	 *                Should never happen with this interface
	 * 
	 */
	public Permutations(Object[] inArray) throws CombinatoricException {

		this(inArray, inArray.length);
	}

	/**
	 * Create a Permutation to enumerate through all possible lineups of the
	 * supplied array of Objects.
	 * 
	 * @param Object
	 *            [] inArray the group to line up
	 * 
	 * @param inArray
	 *            java.lang.Object[], the group to line up
	 * 
	 * @param m
	 *            int, the number of objects to use
	 * 
	 * @exception CombinatoricException
	 *                if m is greater than the length of inArray, or less than
	 *                0.
	 */
    private Permutations(Object[] inArray, int m) throws CombinatoricException {

		this.inArray = inArray;
		this.n = inArray.length;
		this.m = m;

		
		Combinatoric.check(n, m);

		/**
		 * 'index' is an array of ints that keep track of the next permutation
		 * to return. For example, an index on a permutation of 3 things might
		 * contain {1 2 0}. This index will be followed by {2 0 1} and {2 1 0}.
		 * Initially, the index is {0 ... n - 1}.
		 */

		index = new int[n];
		for (int i = 0; i < n; i++) {
			index[i] = i;
		}

		/**
		 * The elements from m to n are always kept ascending right to left.
		 * This keeps the 'dip' in the interesting region.
		 */
		reverseAfter(m - 1);
	}

	/**
	 * @return true, unless we have already returned the last permutation.
	 */
	public boolean hasNext() {
		return hasMore;
	}

	/**
	 * Move the index forward a notch. The algorithm first finds the rightmost
	 * index that is less than its neighbor to the right. This is the 'dip'
	 * point. The algorithm next finds the least element to the right of the dip
	 * that is greater than the dip. That element is switched with the dip.
	 * Finally, the list of elements to the right of the dip is reversed.
	 * <p>
	 * For example, in a permuation of 5 items, the index may be {1, 2, 4, 3,
	 * 0}. The 'dip' is 2 -- the rightmost element less than its neighbor on its
	 * right. The least element to the right of 2 that is greater than 2 is 3.
	 * These elements are swapped, yielding {1, 3, 4, 2, 0}, and the list right
	 * of the dip point is reversed, yielding {1, 3, 0, 2, 4}.
	 * <p>
	 * The algorithm is from "Applied Combinatorics", by Alan Tucker.
	 * 
	 */
    private void moveIndex() {

		
		int i = rightmostDip();
		if (i < 0) {
			hasMore = false;
			return;
		}

		
		int leastToRightIndex = i + 1;
		for (int j = i + 2; j < n; j++) {
			if (index[j] < index[leastToRightIndex] && index[j] > index[i]) {
				leastToRightIndex = j;
			}
		}

		
		
		int t = index[i];
		index[i] = index[leastToRightIndex];
		index[leastToRightIndex] = t;

		if (m - 1 > i) {
			
			reverseAfter(i);
			
			reverseAfter(m - 1);
		}

	}

	/**
	 * @return Object, the next permutation of the original Object array.
	 *         <p>
	 *         Actually, an array of Objects is returned. The declaration must
	 *         say just "Object", since the Permutations class implements
	 *         Enumeration, which declares that the "nextElement()" returns a
	 *         plain Object. Users must cast the returned object to (Object[]).
	 */
	public Object next() {
		if (!hasMore) {
			return null;
		}

		Object[] out = IntStream.range(0, m).mapToObj(i -> inArray[index[i]]).toArray();

        moveIndex();
		return out;
	}

	/**
	 * Reverse the index elements to the right of the specified index.
	 */
    private void reverseAfter(int i) {

		int start = i + 1;
		int end = n - 1;
		while (start < end) {
			int t = index[start];
			index[start] = index[end];
			index[end] = t;
			start++;
			end--;
		}

	}

	/**
	 * @return int the index of the first element from the right that is less
	 *         than its neighbor on the right.
	 */
    private int rightmostDip() {
        return IntStream.iterate(n - 2, i -> i >= 0, i -> i - 1).filter(i -> index[i] < index[i + 1]).findFirst().orElse(-1);

    }

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
