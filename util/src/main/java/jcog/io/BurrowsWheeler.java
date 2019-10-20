package jcog.io;

import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ByteObjectHashMap;

import java.util.Arrays;

/** modified from https://github.com/fujiawu/burrows-wheeler-compression/blob/master/BurrowsWheeler.java */
public class BurrowsWheeler {


	private static class CircularSuffixArray {

		private final byte[] input;
		private final int[] index;

		public CircularSuffixArray(byte[] input)  {
			this.input = input;

			var n = input.length;

			index = new int[n];
			for (var i = 0; i < n; i++)
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
			var input = this.input;
			var n = input.length;
			for (var i = 0; i < n; i++) {
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

		var suffixes = new CircularSuffixArray(input);

		var n = suffixes.input.length;
		var key = -1;

		for (var i = 0; i < n; i++) {
			if (suffixes.index[i] == 0)
				key = i;

			var position = (suffixes.index[i] + n - 1) % n;
			if (position < 0)
				position += n;

			output[i] = input[position];
		}

		return key;
	}

	public static byte[] decode(byte[] in, int key, byte[] out) {

		// map list of positions for each characters 
		var n = in.length;
		var positions = new ByteObjectHashMap<IntArrayList>(n);
		for (var i = 0; i < n; i++ )
			positions.getIfAbsentPut(in[i], IntArrayList::new).add(i);

		Arrays.sort(in); // sort last word

		var next = new int[10];
		var count = 0;
		for (var b : in) {
			var removeAtIndex = positions.get(b).removeAtIndex(0);
			if (next.length == count) next = Arrays.copyOf(next, count * 2);
			next[count++] = removeAtIndex;
		}
		next = Arrays.copyOfRange(next, 0, count);

		var cur = key;
		for (var i = 0; i < n; i++) {
			out[i] = in[cur];
			cur = next[cur];
		}

		return out;
	}


	public static void main(String[] args) {
		var i = "sdkfjklsdfklsdfj";
		var ib = i.getBytes();
		var eb = new byte[ib.length];
		var key = encode(ib, eb);
		var ob = new byte[eb.length];
		decode(eb, key, ob);
		var o = new String(ob);
		System.out.println(i + " " + new String(eb) + " " + o + "\t" + i.equals(o));
	}

}