package jcog.io;

import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import org.eclipse.collections.impl.map.mutable.primitive.ByteObjectHashMap;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

public class BurrowsWheeler {


	private static class CircularSuffixArray {

		private final byte[] input;
		private final int[] index;

		public CircularSuffixArray(byte[] input)  {
			this.input = input;

			final int n = input.length;

			index = new int[n];
			for (int i = 0; i < n; i++)
				index[i] = i;

			QuickSort.quickSort(0, n, this::compare, this::swap);
		}

		private void swap(int a, int b) {
			ArrayUtil.swapInt(index, a, b);
		}

		private int compare(int a, int b) {
			if (a==b)
				return 0;

			int s1 = index[a], s2 = index[b];
			int t1 = s1, t2 = s2;
			byte[] input = this.input;
			int n = input.length;
			for (int i = 0; i < n; i++) {
				byte c1 = input[t1], c2 = input[t2];
				if (c1 < c2)
					return -1;
				else if (c1 > c2)
					return 1;

				if (++t1 == n) t1 = 0;
				if (++t2 == n) t2 = 0;
			}

			return (int) Math.signum(s2 - s1); // the longest byte[] is the most
		}

	}

	public static int encode(byte[] input, byte[] output) {

		CircularSuffixArray suffixes = new CircularSuffixArray(input);

		int n = suffixes.input.length;
		int key = -1;

		for (int i = 0; i < n; i++) {
			if (suffixes.index[i] == 0)
				key = i;

			int position = (suffixes.index[i] + n - 1) % n;
			if (position < 0)
				position += n;

			output[i] = input[position];
		}

		return key;
	}

	/**
	 * apply Burrows-Wheeler decoding, reading from standard input and writing to standard output
	 */
	public static byte[] decode(byte[] in, int key, byte[] out) {

		// map list of positions for each characters 
		int n = in.length;
		ByteObjectHashMap<Deque<Integer>> positions = new ByteObjectHashMap<>(n);
		for (int i = 0; i < n; i++ )
			positions.getIfAbsentPut(in[i], LinkedList::new).add(i);

		Arrays.sort(in); // sort last word

		int[] next = new int[n];
		for (int i = 0; i < n; i++)
			next[i] = positions.get(in[i]).removeFirst();

		int cur = key;
		for (int i = 0; i < n; i++) {
			out[i] = in[cur];
			cur = next[cur];
		}

		return out;
	}


	public static void main(String[] args) {
		String i = "sdkfjklsdfklsdfj";
		byte[] ib = i.getBytes();
		byte[] eb = new byte[ib.length];
		int key = encode(ib, eb);
		byte[] ob = new byte[eb.length];
		decode(eb, key, ob);
		String o = new String(ob);
		System.out.println(i + " " + o + "\t" + i.equals(o));
	}

}