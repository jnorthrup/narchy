package jcog.io;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Binar <-> Text Transducers
 */
public class BinTxt {

	public static final char[] symbols;

	static {
		String x = IntStream.rangeClosed(192, 255).mapToObj(i -> String.valueOf((char) i)).collect(Collectors.joining("", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_.~", ""));


        symbols = x.toCharArray();
	}

	public static final int maxBase = symbols.length;


	/**
	 * URIchars must be at least base length
	 */
	public static String toString(long aNumber, int base) {
		StringBuilder result = new StringBuilder(4);

		append(result, aNumber, base);

		return result.toString();
	}

	public static String toString(long l) {
		return toString(l, maxBase);
	}

	public static void append(StringBuilder target, long v) {
		append(target, v, maxBase);
	}

	public static void append(StringBuilder target, long v, int base) {
		if (v < 0) {
			target.append('-');
			v = -v;
		}

		_append(target, v, base);
	}

	private static void _append(StringBuilder target, long v, int base) {
		int r = (int) (v % base);

		if (v - r != 0)
			append(target, (v - r) / base, base);

		target.append(symbols[r]);
	}
}
