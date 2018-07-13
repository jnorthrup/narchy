package jcog;

import com.google.common.escape.Escapers;
import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.DoubleHistogram;

import java.io.IOException;
import java.nio.CharBuffer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.BiConsumer;

/**
 * Utilities for process Text & String input/output, ex: encoding/escaping and decoding/unescaping Terms
 */
public enum Texts {
    ;


    
    
    













































































































































    /*
    public static void main(String[] args) {
    String s = "Immutable";
    String t = "Notreally";
    mutate(s, t);
    StdOut.println(t);
    
    StdOut.println("Immutable");
    }
     */

    static final ThreadLocal<Format> oneDecimal = ThreadLocal.withInitial(() -> new DecimalFormat("0.0"));


    static final ThreadLocal<Format> threeDecimal = ThreadLocal.withInitial(() -> new DecimalFormat("0.000"));
    static final ThreadLocal<Format> fourDecimal = ThreadLocal.withInitial(() -> new DecimalFormat("0.0000"));


    @Deprecated
    static final ThreadLocal<DecimalFormat> twoDecimal = ThreadLocal.withInitial(() -> new DecimalFormat("0.00"));
    static final Escapers.Builder quoteEscaper = Escapers.builder().addEscape('\"', "\\\"");

    /**
     * @author http:
     */
    public static int levenshteinDistance(CharSequence a, CharSequence b) {
        if (a.equals(b)) return 0;

        int len0 = a.length() + 1;
        int len1 = b.length() + 1;
        int[] cost = new int[len0];
        int[] newcost = new int[len0];
        for (int i = 0; i < len0; i++) {
            cost[i] = i;
        }
        for (int j = 1; j < len1; j++) {
            newcost[0] = j;
            char bj = b.charAt(j - 1);
            for (int i = 1; i < len0; i++) {
                int match = (a.charAt(i - 1) == bj) ? 0 : 1;
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newcost[i - 1] + 1;

                int c = cost_insert;
                if (cost_delete < c) c = cost_delete;
                if (cost_replace < c) c = cost_replace;

                newcost[i] = c;
            }
            int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }
        return cost[len0 - 1];
    }

    public static String n1(float x) {
        return oneDecimal.get().format(x);
    }

    public static String n3(float x) {
        return threeDecimal.get().format(x);
    }

    public static String n3(double x) {
        return threeDecimal.get().format(x);
    }

    public static String n4(float x) {
        if (x != x)
            return "NNaaNN";
        else
            return fourDecimal.get().format(x);
    }

    public static String n4(double x) {
        return fourDecimal.get().format(x);
    }

    public static long hundredths(float d) {
        return (long) ((d * 100.0f + 0.5f));
    }

    public static int tens(float d) {
        return (int) ((d * 10.0f + 0.5f));
    }

    public static String n2(float x) {
        if (x != x)
            return "NaN";

        if ((x < 0) || (x > 1.0f))
            return twoDecimal.get().format(x);

        int hundredths = (int) hundredths(x);
        switch (hundredths) {

            case 100:
                return "1.0";
            case 99:
                return ".99";
            case 90:
                return ".90";
            case 50:
                return ".50";
            case 0:
                return "0.0";
        }

        if (hundredths > 9) {
            int tens = hundredths / 10;
            return new String(new char[]{
                    '.', (char) ('0' + tens), (char) ('0' + hundredths % 10)
            });
        } else {
            return new String(new char[]{
                    '.', '0', (char) ('0' + hundredths)
            });
        }
    }

    /**
     * 1 character representing a 1 decimal of a value between 0..1.0;
     * representation; 0..9
     */
    public static char n1char(float x) {
        int i = tens(x);
        if (i >= 10)
            i = 9;
        return (char) ('0' + i);
    }

    public static int compare(CharSequence s, CharSequence t) {
        if ((s instanceof String) && (t instanceof String)) {
            return ((String) s).compareTo((String) t);
        }
        if ((s instanceof CharBuffer) && (t instanceof CharBuffer)) {
            return ((CharBuffer) s).compareTo((CharBuffer) t);
        }

        int i = 0;

        int sl = s.length();
        int tl = t.length();

        while (i < sl && i < tl) {
            char a = s.charAt(i);
            char b = t.charAt(i);

            int diff = a - b;

            if (diff != 0)
                return diff;

            i++;
        }

        return sl - tl;
    }

    public static String n2(double p) {
        return n2((float) p);
    }

    /**
     * character to a digit, or -1 if it wasnt a digit
     */
    public static int i(char c) {
        if ((c >= '0' && c <= '9'))
            return c - '0';
        return -1;
    }

    /**
     * fast parse an int under certain conditions, avoiding Integer.parse if possible
     */
    public static int i(String s) throws NumberFormatException {

        switch (s.length()) {
            case 0:
                throw new UnsupportedOperationException();
            case 1: {
                char c = s.charAt(0);
                int i = i(c);
                if (i != -1) return i;
            }
            break;
            case 2: {
                int dig1 = i(s.charAt(1));
                if (dig1 != -1) {
                    int dig10 = i(s.charAt(0));
                    if (dig10 != -1)
                        return dig10 * 10 + dig1;
                }
            }
            break;
        }

        return Integer.parseInt(s);

    }

    /**
     * fast parse an int under certain conditions, avoiding Integer.parse if possible
     */
    public static long l(String s) throws NumberFormatException {
        int sl = s.length();
        if (sl == 1) {
            char c = s.charAt(0);
            int i = i(c);
            if (i != -1) return i;
        } else if (sl == 2) {
            int dig1 = i(s.charAt(1));
            if (dig1 != -1) {
                int dig10 = i(s.charAt(0));
                if (dig10 != -1)
                    return dig10 * 10 + dig1;
            }
        }
        return Long.parseLong(s);
    }

    /**
     * fast parse a non-negative int under certain conditions, avoiding Integer.parse if possible
     * TODO parse negative values with a leading '-'
     */
    public static int i(String s, int ifMissing) {
        switch (s.length()) {
            case 0:
                return ifMissing;
            case 1:
                return i1(s, ifMissing);
            case 2:
                return i2(s, ifMissing);
            case 3:
                return i3(s, ifMissing);
            default:


                for (int i = 0; i < s.length(); i++)
                    if (i(s.charAt(i)) == -1)
                        return ifMissing;


                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return ifMissing;

                }
        }
    }

    public static String i(byte[] b, int radix) {
        assert(radix==16); //TODO other options
        byte[] c = new byte[b.length * 2];
        int i = 0;
        for(byte x : b) {
            c[i++] = (byte) ((x/radix) + '0');
            c[i++] = (byte) ((x%radix) + '0');
        }
        return new String(c);
    }

    public static int i(String s, int offset, int ifMissing) {
        int sl = s.length() - offset;
        if (sl <= 0)
            return ifMissing;

        switch (sl) {
            case 1:
                return i1(s, offset, ifMissing);
            case 2:
                return i2(s, offset, ifMissing);
            case 3:
                return i3(s, offset, ifMissing);
            default:
                try {
                    return Integer.parseInt(offset != 0 ? s.substring(offset) : s);
                } catch (NumberFormatException e) {
                    return ifMissing;
                }
        }
    }

    private static int i3(String s, int ifMissing) {
        return i3(s, 0, ifMissing);
    }

    private static int i3(String s, int offset, int ifMissing) {
        int dig100 = i(s.charAt(offset));
        if (dig100 == -1) return ifMissing;

        int dig10 = i(s.charAt(offset + 1));
        if (dig10 == -1) return ifMissing;

        int dig1 = i(s.charAt(offset + 2));
        if (dig1 == -1) return ifMissing;

        return dig100 * 100 + dig10 * 10 + dig1;
    }

    private static int i2(String s, int ifMissing) {
        return i2(s, 0, ifMissing);
    }

    private static int i2(String s, int offset, int ifMissing) {
        int dig10 = i(s.charAt(offset));
        if (dig10 == -1) return ifMissing;

        int dig1 = i(s.charAt(offset + 1));
        if (dig1 == -1) return ifMissing;

        return dig10 * 10 + dig1;
    }

    private static int i1(String s, int ifMissing) {
        return i1(s, 0, ifMissing);
    }

    private static int i1(String s, int offset, int ifMissing) {
        int dig1 = i(s.charAt(offset));
        if (dig1 == -1) return ifMissing;
        return dig1;
    }

    public static float f(String s) {
        return f(s, Float.NaN);
    }

    /**
     * fast parse for float, checking common conditions
     */
    public static float f(String s, float ifMissing) {

        switch (s) {
            case "0":
                return 0;
            case "0.00":
                return 0;
            case "1":
                return 1.0f;
            case "1.00":
                return 1.0f;
            case "0.90":
                return 0.9f;
            case "0.9":
                return 0.9f;
            case "0.5":
                return 0.5f;
            default:
                try {
                    return Float.parseFloat(s);
                } catch (NumberFormatException e) {
                    return ifMissing;
                }
        }

    }

    public static float f(String s, float min, float max) {
        float x = f(s, Float.NaN);
        if ((x < min) || x > max)
            return Float.NaN;
        return x;
    }

    public static String arrayToString(Object... signals) {
        if (signals == null) return "";
        int slen = signals.length;
        if ((signals != null) && (slen > 1))
            return Arrays.toString(signals);
        if (slen > 0)
            return signals[0].toString();
        return "";
    }

    public static CharSequence n(float v, int decimals) {

        switch (decimals) {
            case 1:
                return n1(v);
            case 2:
                return n2(v);
            case 3:
                return n3(v);
            case 4:
                return n4(v);
        }

        throw new RuntimeException("invalid decimal number");
    }

    public static int count(String s, char x) {
        int c = 0;
        for (int i = 0; i < s.length(); i++)
            if (s.charAt(i) == x)
                c++;

        return c;
    }

    public static String n2(float... v) {
        assert (v.length > 0);
        StringBuilder sb = new StringBuilder(v.length * 4 + 2 /* approx */);
        int s = v.length;
        for (int i = 0; i < s; i++) {
            sb.append(n2(v[i]));
            if (i != s - 1) sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * prints an array of numbers separated by tab, suitable for a TSV line
     */
    public static String n4(double... v) {
        StringBuilder sb = new StringBuilder(v.length * 6 + 2 /* approx */);
        int s = v.length;
        for (int i = 0; i < s; i++) {
            sb.append(n4(v[i]));
            if (i != s - 1) sb.append('\t');
        }
        return sb.toString();
    }

    /**
     * prints an array of numbers separated by tab, suitable for a TSV line
     */
    public static String n4(float... v) {
        StringBuilder sb = new StringBuilder(v.length * 6 + 2 /* approx */);
        int s = v.length;
        for (int i = 0; i < s; i++) {
            sb.append(n4(v[i]));
            if (i != s - 1) sb.append('\t');
        }
        return sb.toString();
    }

    /**
     * prints an array of numbers separated by tab, suitable for a TSV line
     */
    public static String n2(byte... v) {
        StringBuilder sb = new StringBuilder(v.length * 3);
        int s = v.length;
        for (int i = 0; i < s; i++) {
            sb.append(Integer.toHexString(Byte.toUnsignedInt(v[i]))).append(' ');
        }
        return sb.toString();
    }

    /**
     * Return formatted Date String: yyyy.MM.dd HH:mm:ss
     * Based on Unix's time() input in seconds
     *
     * @param timestamp seconds since start of Unix-time
     * @return String formatted as - yyyy.MM.dd HH:mm:ss
     * from: https:
     */
    public static String dateStr(long timestamp) {
        Date date = new Date(timestamp * 1000);
        DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        return formatter.format(date);
    }

    /**
     * string repr of an amount of nanoseconds
     * from: https:
     */
    public static String timeStr(double ns) {
        assert(Double.isFinite(ns));
        boolean neg = ns < 0;
        return (neg ? "-" : "") + _timeStr(Math.abs(ns));
    }


    private static String _timeStr(double ns) {
        if (ns < 1000) return n4(ns) + "ns";
        if (ns < 1_000_000) return n4(ns / 1_000d) + "us";
        if (ns < 1_000_000_000) return n4(ns / 1_000_000d) + "ms";

        if (ns < 1_000_000_000_000d) return n2(ns / 1_000_000_000d) + "s";
        long sec = Math.round(ns / 1_000_000_000d);
        if (sec < 5 * 60) return (sec / 60) + "m" + (sec % 60) + 's';
        long min = sec / 60;
        if (min < 60) return min + "m";
        long hour = min / 60;
        if (min < 24 * 60) return hour + "h" + (min % 60) + 'm';
        long day = hour / 24;
        return day + "d" + (day % 24) + 'h';
    }

    /**
     * from: https:
     */
    public static String byteCountString(long size) {
        if (size < 2 * (1L << 10)) return size + "b";
        if (size < 2 * (1L << 20)) return String.format("%dKb", size / (1L << 10));
        if (size < 2 * (1L << 30)) return String.format("%dMb", size / (1L << 20));
        return String.format("%dGb", size / (1L << 30));
    }

    /**
     * from: https:
     */
    public static String repeat(String s, int n) {

        if (s.length() == 1) {
            char c = s.charAt(0);
            if (c < 0xff) {
                byte[] bb = new byte[n];
                Arrays.fill(bb, (byte) c);
                return new String(bb);
            }
        }

        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < n; i++) ret.append(s);
        return ret.toString();
    }

    /**
     * pad with leading zeros
     * TODO can be made faster
     */
    public static String iPad(long v, int digits) {
        String s = String.valueOf(v);
        while (s.length() < digits)
            s = ' ' + s;
        return s;
    }

    public static String n2percent(float rate) {
        return n2(100f * rate) + '%';
    }

    public static void histogramDecode(AbstractHistogram h, String header, int linearStep, BiConsumer<String, Object> x) {
        int digits = (int) (1 + Math.log10(h.getMaxValue()));
        h.linearBucketValues(linearStep).forEach((p) -> {
            x.accept(header + " [" +
                            iPad(p.getValueIteratedFrom(), digits) + ".." + iPad(p.getValueIteratedTo(), digits) + ']',
                    p.getCountAddedInThisIterationStep());
        });
    }

    public static void histogramDecode(DoubleHistogram h, String header, double linearStep, BiConsumer<String, Object> x) {
        final char[] order = {'a'};
        h.linearBucketValues(linearStep).forEach((p) -> {
            x.accept(header + " " + (order[0]++) +
                            "[" + n4(p.getValueIteratedFrom()) + ".." + n4(p.getValueIteratedTo()) + ']',
                    p.getCountAddedInThisIterationStep());
        });
    }

    public static void histogramDecode(AbstractHistogram h, String header, BiConsumer<String, Object> x) {
        h.percentiles(1).forEach(p -> {
            x.accept(header + " [" +
                            p.getValueIteratedFrom() + ".." + p.getValueIteratedTo() + ']',
                    p.getCountAddedInThisIterationStep());
        });
    }

    public static String histogramString(AbstractHistogram h, boolean percentiles) {
        StringBuilder sb = new StringBuilder(256);
        histogramPrint(h, percentiles, sb);
        return sb.toString();
    }

    public static void histogramPrint(AbstractHistogram h, Appendable out) {
        histogramPrint(h, true, out);
    }

    public static void histogramPrint(AbstractHistogram h, boolean percentiles, Appendable out) {
        try {
            out.append("{n=" + h.getTotalCount() + " avg=" + n4(h.getMean()) + ", min=" + n4(h.getMinValue()) + ", max=" + n4(h.getMaxValue()) + ", stdev=" + n4(h.getStdDeviation()) + '}');
            if (percentiles) {
                out.append('\n');
            }
        } catch (IOException e) {
            throw new WTF(e);
        }
        if (percentiles) {
            histogramDecode(h, "", (label, value) -> {
                try {
                    out.append(label + " " + value + "\n");
                } catch (IOException e) {
                    throw new WTF(e);
                }
            });
        }
    }

    public static String quote(String s) {
        int length = s.length();

        if (length == 0)
            return "\"\"";

        if (s.charAt(0) == '\"' && s.charAt(length - 1) == '\"') {
            if (length == 1) {
                s = "\"\\\"\"";
            } else {

            }
        } else {
            s = ("\"" + quoteEscaper.build().escape(s) + '"');
        }

        return s;
    }


    public static String unquote(String x) {
        while (true) {
            int len = x.length();
            if (len > 0 && x.charAt(0) == '\"' && x.charAt(len - 1) == '\"') {
                x = x.substring(1, len - 1);
            } else {
                return x;
            }
        }
    }
}

