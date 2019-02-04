package jcog.io;

/**
 * Binar <-> Text Transducers
 */
public class BinTxt {

    static final char[] URIchars;
    static {
        StringBuilder x = new StringBuilder("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_.~");

        
        for (int i = 192; i <= 255; i++) {
            x.append((char)i);
        }

        URIchars = x.toString().toCharArray();
    }
    public static final int maxBase = URIchars.length;

    


































































































































































    /** URIchars must be at least base length */
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

        target.append(URIchars[r]);
    }
}
