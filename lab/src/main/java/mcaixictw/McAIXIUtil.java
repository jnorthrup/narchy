package mcaixictw;

public class McAIXIUtil {
	
//
//	public static String toString(List<Boolean> symlist) {
//		String s = "";
//		for (boolean b : symlist) {
//			String one = b ? "1" : "0";
//			s += one;
//		}
//		return s;
//	}

	/**
	 * Return a random integer between [0, end)
	 * 
	 * @param range
	 * @return
	 */
	public static int randRange(int end) {
		return ((int)(Math.random() * end));
	}

	public static boolean randSym() {
		return Math.random() < 0.5;
	}

	/**
	 * Decodes the value encoded on the end of a list of symbols
	 * 
	 * @param symlist
	 * @return
	 */
	public static int decode(BooleanArrayList symlist) {
		return decode(symlist, 0, symlist.size());
	}

	public static int decode(BooleanArrayList symlist, int from, int to) {
		int value = 0;
		for (int i = from; i < to; i++) {
			value = 2 * value + (symlist.get(i) ? 1 : 0);
		}
		return value;
	}

	/**
	 * Encodes a value onto the end of a symbol list using "bits" symbols. the
	 * most significant bit is at position 0 of the list.
	 * 
	 * @param value
	 * @param bits
	 * @return
	 */
	public static BooleanArrayList encode(int value, int bits) {
		BooleanArrayList symlist = new BooleanArrayList(bits);
		return encodeAppend(value, bits, symlist);
	}

	public static BooleanArrayList encodeAppend(int value, int bits, BooleanArrayList target) {
		for (int i = bits-1; i >=0; i--) {
			target.add(((1<<i) & value) != 0);
		}
		return target;
	}
	/**
	 * decrease the resolution of the given value to a number with the given
	 * number of bits. the given value should be a number between 0 and 1. for
	 * example if the number of bits is 3 then 1 would be encoded as 7 (111).
	 * 
	 * @param val
	 * @param bits
	 * @return
	 */
	public static int decreaseResolution(double val, int bits) {
		assert (val <= 1.0 && val >= 0.0);
		
		int max = (1 << bits) - 1;
		return (int) Math.round(val * max);
	}

	/**
	 * returns the number of bits needed to encode the given amount of states.
	 * 
	 * @param states
	 * @return
	 */
	public static int bitsNeeded(int states) {
		assert (states > 1);
		//return 31 - Integer.numberOfLeadingZeros(states);
		return states;
	}

//	public static boolean DebugOutput;

	public static BooleanArrayList rand(int length) {
		BooleanArrayList b = new BooleanArrayList(length);
		for (int i = 0; i < length; i++) {
			b.add(randSym());
		}
		return b;
	}
}

