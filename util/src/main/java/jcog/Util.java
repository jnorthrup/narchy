package jcog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.primitives.*;
import jcog.data.list.FasterList;
import jcog.io.BinTxt;
import jcog.math.FloatSupplier;
import jcog.math.NumberException;
import jcog.pri.ScalarValue;
import jcog.util.ArrayUtil;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.stat.Frequency;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.EmptyIterator;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.lang.Math.pow;
import static java.lang.Thread.onSpinWait;
import static java.util.stream.Collectors.toList;

/**
 *
 */
public class Util {
    public static final Unsafe unsafe;

    public static final Iterator emptyIterator = EmptyIterator.getInstance();
    public static final Iterable emptyIterable = new Iterable() {
        @NotNull
        @Override
        public Iterator iterator() {
            return emptyIterator;
        }
    };
    public static final double PHI = 1.6180339887498948482;
    public static final float PHIf = (float) PHI;
    public static final double PHI_min_1 = PHI - 1.0;
    public static final float PHI_min_1f = (float) PHI_min_1;
    public static final int PRIME3 = 524287;
    public static final int PRIME2 = 92821;
    public static final int PRIME1 = 31;

    public static final ObjectMapper jsonMapper =
            new ObjectMapper()
                    .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                    .enable(SerializationFeature.WRAP_EXCEPTIONS)
                    .enable(SerializationFeature.WRITE_NULL_MAP_VALUES)
                    .enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
                    .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                    .enable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)
                    .enable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                    .enable(MapperFeature.AUTO_DETECT_FIELDS)
                    .enable(MapperFeature.AUTO_DETECT_GETTERS)
                    .enable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    public static final ObjectMapper cborMapper =
            new ObjectMapper(new CBORFactory())
                    .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    public static final double log2 = Math.log(2.0);
    private static final int BIG_ENOUGH_INT = 16 * 1024;
    private static final double BIG_ENOUGH_FLOOR = (double) BIG_ENOUGH_INT;
    public static float sqrtMIN_NORMAL = (float) Math.sqrt((double) Float.MIN_NORMAL);

    static {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            unsafe = (Unsafe) singleoneInstanceField.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * It is basically the same as a lookup table with 2048 entries and linear interpolation between the entries, but all this with IEEE floating point tricks.
     * http:
     */
    public static double expFast(double val) {
        long tmp = (long) (1512775.0 * val + (double) (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }

    public static String UUIDbase64() {
        long low = UUID.randomUUID().getLeastSignificantBits();
        long high = UUID.randomUUID().getMostSignificantBits();
        return new String(Base64.getEncoder().encode(
                Bytes.concat(
                        Longs.toByteArray(low),
                        Longs.toByteArray(high)
                )
        ));
    }


    public static int hash(byte[] x, int from, int to) {

        int len = to - from;
        switch (len) {
            case 0:
                return 1;
            case 1:
                return (int) x[from];
            case 2:
                return (int) Shorts.fromBytes(x[from], x[from + 1]);
            case 3:
                return Ints.fromBytes(x[from], x[from + 1], x[from + 2], (byte) 0);
            case 4:
                return Ints.fromBytes(x[from], x[from + 1], x[from + 2], x[from + 3]);
            default:
                return

                        hashFNV(x, from, to);
        }


    }

    public static int hashJava(byte[] bytes, int len) {
        int result = 1;

        for (int i = 0; i < len; ++i) {
            result = 31 * result + (int) bytes[i];
        }

        return result;
    }

    public static int hash(byte[] bytes) {
        return hash(bytes, 0, bytes.length);
    }

    public static <X> Predicate<X> limit(Predicate<X> x, int max) {
        if (max <= 0)
            throw new WTF();

        if (max == 1) {
            return new Predicate<X>() {
                @Override
                public boolean test(X z) {
                    x.test(z);
                    return false;
                }
            };
        } else {
            int[] remain = {max};
            return new Predicate<X>() {
                @Override
                public boolean test(X z) {
                    boolean next = (--remain[0] > 0);
                    return x.test(z) && next;
                }
            };
        }
    }


    public static int hashFNV(byte[] bytes, int from, int to) {
        int h = 0x811c9dc5;
        for (int i = from; i < to; i++)
            h = (h * 16777619) ^ (int) bytes[i];
        return h;
    }


    public static void assertNotNull(Object test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
    }

    public static void assertNotEmpty(Object[] test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.length == 0) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }

    public static void assertNotEmpty(CharSequence test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.length() == 0) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }

    public static void assertNotBlank(CharSequence test, String varName) {
        if (test != null) {
            test = test.toString().trim();
        }
        assertNotEmpty(test, varName);
    }


    public static String globToRegEx(String line) {

        line = line.trim();
        int strLen = line.length();
        StringBuilder sb = new StringBuilder(strLen);

        if (strLen > 0 && (int) line.charAt(0) == (int) '*') {
            line = line.substring(1);
            strLen--;
        }
        if (strLen > 0 && (int) line.charAt(strLen - 1) == (int) '*') {
            line = line.substring(0, strLen - 1);
            strLen--;
        }
        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : line.toCharArray()) {
            switch (currentChar) {
                case '*':
                    if (escaping)
                        sb.append("\\*");
                    else
                        sb.append(".*");
                    escaping = false;
                    break;
                case '?':
                    if (escaping)
                        sb.append("\\?");
                    else
                        sb.append('.');
                    escaping = false;
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    sb.append('\\');
                    sb.append(currentChar);
                    escaping = false;
                    break;
                case '\\':
                    if (escaping) {
                        sb.append("\\\\");
                        escaping = false;
                    } else
                        escaping = true;
                    break;
                case '{':
                    if (escaping) {
                        sb.append("\\{");
                    } else {
                        sb.append('(');
                        inCurlies++;
                    }
                    escaping = false;
                    break;
                case '}':
                    if (inCurlies > 0 && !escaping) {
                        sb.append(')');
                        inCurlies--;
                    } else if (escaping)
                        sb.append("\\}");
                    else
                        sb.append('}');
                    escaping = false;
                    break;
                case ',':
                    if (inCurlies > 0 && !escaping) {
                        sb.append('|');
                    } else if (escaping)
                        sb.append("\\,");
                    else
                        sb.append(',');
                    break;
                default:
                    escaping = false;
                    sb.append(currentChar);
            }
        }
        return sb.toString();
    }

    public static long hashPJW(String str) {
        long BitsInUnsignedInt = (long) (4 * 8);
        long ThreeQuarters = (BitsInUnsignedInt * 3L) / 4L;
        long OneEighth = BitsInUnsignedInt / 8L;
        long HighBits = (0xFFFFFFFFL) << (BitsInUnsignedInt - OneEighth);
        long hash = 0L;
        long test = 0L;

        for (int i = 0; i < str.length(); i++) {
            hash = (hash << OneEighth) + (long) str.charAt(i);

            if ((test = hash & HighBits) != 0L) {
                hash = ((hash ^ (test >> ThreeQuarters)) & (~HighBits));
            }
        }

        return hash;
    }

    public static long hashELF(String str) {
        long hash = 0L;
        long x = 0L;

        int l = str.length();
        for (int i = 0; i < l; i++) {
            hash = (hash << 4) + (long) str.charAt(i);

            if ((x = hash & 0xF0000000L) != 0L) {
                hash ^= (x >> 24);
            }
            hash &= ~x;
        }

        return hash;
    }

    /**
     * from: ConcurrentReferenceHashMap.java found in Hazelcast
     */
    public static int hashWangJenkins(int h) {


        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }

    public static int hashJava(int a, int b) {
        return a * 31 + b;
    }

    public static int hashJavaX(int a, int b) {
        return a * Util.PRIME2 + b;
    }

    /**
     * from clojure.Util - not tested
     * also appears in https:
     */
    public static int hashCombine(int a, int b) {
        return a ^ (b + 0x9e3779b9 + (a << 6) + (a >> 2));
    }

    public static int hashCombine(int a, long b) {

        return Util.hashCombine(a, Long.hashCode(b));
    }

    public static int hashCombine(int i, long x, long y) {

        int ix = hashCombine(i, x);
        return x == y ? ix : hashCombine(ix, Long.hashCode(y));
    }

    public static int hashCombine(int a, long[] b) {
        int x = Util.hashCombine(a, b[0]);
        for (int i = 1; i < b.length; i++) {
            x = Util.hashCombine(x, b[i]);
        }
        return x;
    }

    public static int hashCombine(int a, Object b) {
        return hashCombine(a, b.hashCode());
    }

    public static int hashCombine(Object a, Object b) {
        if (a != b) {
            return hashCombine(a.hashCode(), b.hashCode());
        } else {
            int ah = a.hashCode();
            return hashCombine(ah, ah);
        }
    }

    /**
     * hashCombine(1, b)
     */
    public static int hashCombine1(Object bb) {
        return hashCombine(1, bb.hashCode());
    }

    public static int hashCombine(int a, int b, int c) {

        return hashCombine(hashCombine(a, b), c);


    }


    /**
     * returns the next index
     */
    public static int longToBytes(long l, byte[] target, int offset) {
        for (int i = offset + 7; i >= offset; i--) {
            target[i] = (byte) (l & 0xFFL);
            l >>= 8L;
        }
        return offset + 8;
    }

    /**
     * returns the next index
     */
    public static int intToBytes(int l, byte[] target, int offset) {
        for (int i = offset + 3; i >= offset; i--) {
            target[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return offset + 4;
    }


    /**
     * returns the next index
     */
    public static int short2Bytes(int l, byte[] target, int offset) {
        target[offset++] = (byte) ((l >> 8) & 0xff);
        target[offset++] = (byte) ((l) & 0xff);
        return offset;
    }

    /**
     * http:
     */
    public static int floorInt(float x) {
        return (int) ((double) x + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
    }

    /**
     * linear interpolate between target & current, factor is between 0 and 1.0
     * targetFactor=1:   full target
     * targetfactor=0.5: average
     * targetFactor=0:   full current
     */
    public static float lerp(float x, float min, float max) {
        return lerpSafe(unitize(x), min, max);
    }


    public static float lerpSafe(float x, float min, float max) {
        return min + x * (max - min);
    }

    public static float unlerp(float y, float min, float max) {
        return (y - min) / (max - min);
    }

    /**
     * no checking of x
     */


    public static double lerp(double x, double min, double max) {
        return min + (max - min) * unitize(x);
    }

    public static long lerpLong(float x, long min, long max) {
        if (min == max) return min;
        return Math.round((double) min + (double) (max - min) * unitize((double) x));
    }

    public static int lerpInt(float x, int min, int max) {
        if (min == max) return min;
        return Math.round((float) min + (float) (max - min) * unitize(x));
    }


    public static float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    public static float mean(float a, float b) {
        return (a + b) / 2.0F;
    }

    public static long mean(long a, long b) {
        return (a + b) / 2L;
    }

    public static float mean(float a, float b, float c) {
        return (a + b + c) / 3.0F;
    }


    public static double mean(double a, double b) {
        return (a + b) / 2.0;
    }

    public static double mean(double... d) {
        double result = 0.0;
        for (double v : d) {
            result += v;
        }

        return result / (double) d.length;
    }

    /**
     * Generic utility method for running a list of tasks in current thread
     */
    public static void run(Deque<Runnable> tasks) {
        run(tasks, tasks.size(), Runnable::run);
    }

    public static void run(Deque<Runnable> tasks, int maxTasksToRun, Consumer<Runnable> runner) {
        while (!tasks.isEmpty() && maxTasksToRun-- > 0) {
            runner.accept(tasks.removeFirst());
        }
    }

    /**
     * clamps a value to 0..1 range
     */
    public static double unitize(double x) {
        assertFinite(x);
        return unitizeSafe(x);
    }

    /**
     * clamps a value to 0..1 range
     */
    public static float unitize(float x) {
        assertFinite(x);
        return unitizeSafe(x);
    }

    public static float unitizeSafe(float x) {
        return Util.clampSafe(x, (float) 0, 1f);
    }

    public static double unitizeSafe(double x) {
        return Util.clampSafe(x, (double) 0, 1);
    }


    public static float assertFinite(float x) throws NumberException {
        if (!Float.isFinite(x))
            throw new NumberException("non-finite", x);
        return x;
    }

    public static double assertFinite(double x) throws NumberException {
        if (!Double.isFinite(x))
            throw new NumberException("non-finite", x);
        return x;
    }

    public static float notNaN(float x) throws NumberException {
        if (x != x)
            throw new NumberException("NaN", x);
        return x;
    }

    public static double notNaN(double x) throws NumberException {
        if (x != x)
            throw new NumberException("NaN", x);
        return x;
    }


    /**
     * discretizes values to nearest finite resolution real number determined by epsilon spacing
     */
    public static float round(float value, float epsilon) {
        assertFinite(epsilon);
        assertFinite(value);
        return roundSafe(value, epsilon);
    }

    public static float roundSafe(float value, float epsilon) {
        if (epsilon <= Float.MIN_NORMAL) return value;
        else return (float) Math.round(value / epsilon) * epsilon;
    }

    public static double round(double value, double epsilon) {
        assertFinite(epsilon);
        assertFinite(value);
        return roundSafe(value, epsilon);
    }

    public static double roundSafe(double value, double epsilon) {
        if (epsilon <= Double.MIN_NORMAL) return value;
        return (double) Math.round(value / epsilon) * epsilon;
    }

    /**
     * rounds x to the nearest multiple of the dither parameter
     */
    public static int round(int x, int dither) {


        return (int) round((long) x, dither);
    }

    public static long round(long x, int dither) {
        return (long) dither * Math.round(((double) x) / (double) dither);
    }


    public static int toInt(float f, int discretness) {
        return Math.round(f * (float) discretness);
    }

    public static long toInt(double f, int discretness) {
        return Math.round(f * (double) discretness);
    }

    public static float toFloat(int i, int discretness) {
        return ((float) i) / (float) discretness;
    }


    public static boolean equals(float a, float b) {
        return equals(a, b, Float.MIN_NORMAL * 2.0F);
    }

    public static boolean equals(long a, long b, int tolerance) {
        assert (tolerance > 0);
        return Math.abs(a - b) < (long) tolerance;
    }

    /**
     * tests equivalence (according to epsilon precision)
     */
    public static boolean equals(float a, float b, float epsilon) {
        if (a == b)
            return true;

        return Math.abs(a - b) < epsilon;


    }

    public static boolean equals(double a, double b) {
        return equals(a, b, Double.MIN_NORMAL * 2.0);
    }

    /**
     * tests equivalence (according to epsilon precision)
     */
    public static boolean equals(double a, double b, double epsilon) {
        if (a == b)
            return true;

        return Math.abs(a - b) < epsilon;


    }


    public static boolean equals(float[] a, float[] b, float epsilon) {
        if (Arrays.equals(a, b)) return true;
        int l = a.length;
        for (int i = 0; i < l; i++) {
            if (!equals(a[i], b[i], epsilon)) {
                return false;
            }
        }
        return true;
    }

    /**
     * applies a quick, non-lexicographic ordering compare
     * by first testing their lengths
     */
    public static int compare(long[] x, long[] y) {
        if (Arrays.equals(x, y)) return 0;

        int xlen = x.length;

        int yLen = y.length;
        if (xlen != yLen) {
            return Integer.compare(xlen, yLen);
        } else {

            for (int i = 0; i < xlen; i++) {
                int c = Long.compare(x[i], y[i]);
                if (c != 0) {
                    return c;
                }
            }
            return 0;

        }
    }

    public static byte[] intAsByteArray(int index) {

        if (index < 36) {
            byte x = base36(index);
            return new byte[]{x};
        } else if (index < (36 * 36)) {
            byte x1 = base36(index % 36);
            byte x2 = base36(index / 36);
            return new byte[]{x2, x1};
        } else {
            throw new RuntimeException("variable index out of range for this method");
        }


    }

    public static int bin(FloatSupplier x, int bins) {
        return bin(x.asFloat(), bins);
    }

    public static int bin(float x, int bins) {


        return Util.clampSafe((int) (x * (float) bins), 0, bins - 1);


    }


    /**
     * finds the mean value of a given bin
     */
    public static float unbinCenter(int b, int bins) {
        return ((float) b) / (float) bins;
    }


    public static MethodHandle mhRef(Class<?> type, String name) {
        try {
            for (Method m : type.getMethods()) {
                if (m.getName().equals(name)) {
                    return MethodHandles
                            .lookup()

                            .unreflect(Optional.of(m).get());
                }
            }
            return MethodHandles
                    .lookup()

                    .unreflect(Optional.<Method>empty().get());
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public static <F> MethodHandle mh(String name, F fun) {
        return mh(name, fun.getClass(), fun);
    }

    public static <F> MethodHandle mh(String name, Class<? extends F> type, F fun) {
        return mhRef(type, name).bindTo(fun);
    }

    @SafeVarargs
    public static <F> MethodHandle mh(String name, F... fun) {
        F fun0 = fun[0];
        MethodHandle m = mh(name, fun0.getClass(), fun0);
        for (int i = 1; i < fun.length; i++) {
            m = m.bindTo(fun[i]);
        }
        return m;
    }

    public static byte base36(int index) {
        if (index < 10)
            return (byte) ((int) '0' + index);
        else if (index < (10 + 26))
            return (byte) ((index - 10) + (int) 'a');
        else
            throw new RuntimeException("out of bounds");
    }

    /**
     * clamps output to 0..+1.  y=0.5 at x=0
     */
    public static float sigmoid(float v) {
        return (float) (1.0 / (1.0 + Math.exp((double) -v)));
    }

    public static double sigmoid(double v) {
        return (1.0 / (1.0 + Math.exp(-v)));
    }

    public static float sigmoidDiff(float a, float b) {
        float sum = a + b;
        float delta = a - b;
        float deltaNorm = delta / sum;
        return sigmoid(deltaNorm);
    }

    public static float sigmoidDiffAbs(float a, float b) {
        float sum = a + b;
        float delta = Math.abs(a - b);
        float deltaNorm = delta / sum;
        return sigmoid(deltaNorm);
    }

    public static List<String> inputToStrings(InputStream is) throws IOException {
        List<String> x = CharStreams.readLines(new InputStreamReader(is, Charsets.UTF_8));
        Closeables.closeQuietly(is);
        return x;
    }

    public static String inputToString(InputStream is) throws IOException {
        String s = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        Closeables.closeQuietly(is);
        return s;
    }

    public static int[] reverse(IntArrayList l) {
        switch (l.size()) {
            case 0:
                throw new UnsupportedOperationException();
            case 1:
                return new int[]{l.get(0)};
            case 2:
                return new int[]{l.get(1), l.get(0)};
            case 3:
                return new int[]{l.get(2), l.get(1), l.get(0)};
            default:


                return l.asReversed().toArray();
        }
    }

    public static byte[] reverse(ByteArrayList l) {
        int s = l.size();
        switch (s) {
            case 0:
                return ArrayUtil.EMPTY_BYTE_ARRAY;
            case 1:
                return new byte[]{l.get(0)};
            case 2:
                return new byte[]{l.get(1), l.get(0)};
            default:
                byte[] b = new byte[s];
                for (int i = 0; i < s; i++)
                    b[i] = l.get(--s);
                return b;
        }
    }

    public static String s(String s, int maxLen) {
        if (s.length() < maxLen) return s;
        return s.substring(0, maxLen - 2) + "..";
    }

    public static void writeBits(int x, int numBits, float[] y, int offset) {

        for (int i = 0, j = offset; i < numBits; i++, j++) {
            int mask = 1 << i;
            y[j] = ((x & mask) == 1) ? 1f : 0f;
        }

    }

    /**
     * a and b must be instances of input, and output must be of size input.length-2
     */
    public static <X> X[] except(X[] input, X a, X b, X[] output) {
        int targetLen = input.length - 2;
        if (output.length != targetLen) {
            throw new RuntimeException("wrong size");
        }
        int j = 0;
        for (X x : input) {
            if ((x != a) && (x != b))
                output[j++] = x;
        }

        return output;
    }


    public static float[] normalize(float[] x) {
        float[] minmax = minmax(x);
        return normalize(x, minmax[0], minmax[1]);
    }

    public static double[] normalize(double[] x) {
        double[] minmax = minmax(x);
        return normalize(x, minmax[0], minmax[1]);
    }

    public static float[] normalizeCartesian(float[] x) {
        double magSq = (double) 0;
        for (int i = 0; i < x.length; i++) {
            float xi = x[i];
            magSq = magSq + (double) xi * xi;
        }

        if (magSq < Math.sqrt((double) Float.MIN_NORMAL))
            return x;

        float mag = (float) Math.sqrt(magSq);
        for (int i = 0; i < x.length; i++)
            x[i] /= mag;

        return x;
    }

    public static double[] normalizeCartesian(double[] x) {
        double magSq = 0.0;
        for (double xi : x) {
            double v = xi * xi;
            magSq += v;
        }
        if (magSq < Math.sqrt(Double.MIN_NORMAL))
            return x;
        double mag = Math.sqrt(magSq);
        for (int i = 0; i < x.length; i++) {
            x[i] /= mag;
        }
        return x;
    }

    public static float[] normalizeMargin(float lowerPct, float upperPct, float[] x) {
        float[] minmax = minmax(x);
        float range = minmax[1] - minmax[0];
        return normalize(x, minmax[0] - lowerPct * range, minmax[1] + upperPct * range);
    }

    public static float[] normalize(float[] x, float min, float max) {
        return normalize(x, 0, x.length, min, max);
    }

    public static float[] normalizeSafe(float[] x, float min, float max) {
        return normalizeSafe(x, 0, x.length, min, max);
    }

    public static double[] normalize(double[] x, double min, double max) {
        return normalize(x, 0, x.length, min, max);
    }

    public static float[] normalize(float[] x, int s, int e) {
        return normalize(x, s, e, Util.min(s, e, x), Util.max(s, e, x));
    }

    public static float[] normalize(float[] x, int s, int e, float min, float max) {
        for (int i = s; i < e; i++)
            x[i] = normalize(x[i], min, max);
        return x;
    }

    public static float[] normalizeSafe(float[] x, int s, int e, float min, float max) {
        for (int i = s; i < e; i++)
            x[i] = normalizeSafe(x[i], min, max);
        return x;
    }

    public static double[] normalize(double[] x, int s, int e, double min, double max) {
        for (int i = s; i < e; i++)
            x[i] = normalize(x[i], min, max);
        return x;
    }

    public static double normalize(double x, double min, double max) {
        if (x != x)
            return (double) Float.NaN;
        assertFinite(min);
        assertFinite(max);
        if (max - min <= Double.MIN_NORMAL)
            return 0.5;
        else {
            assert (max >= min);
            return (x - min) / (max - min);
        }
    }

    public static float normalizeSafe(float x, float min, float max) {
        return ((max - min) <= Float.MIN_NORMAL) ? 0.5f : ((x - min) / (max - min));
    }

    public static float normalize(float x, float min, float max) {
        if (x != x)
            return Float.NaN;

        assertFinite(min);
        assertFinite(max);
        assert (max >= min);
        return normalizeSafe(x, min, max);
    }

    public static float variance(float[] population) {
        float average = 0.0f;
        for (float p : population) {
            average += p;
        }
        int n = population.length;
        average = average / (float) n;

        float variance = 0.0f;
        for (float p : population) {
            float d = p - average;
            variance += d * d;
        }
        return variance / (float) n;
    }

    public static double[] avgvar(double[] population) {
        double average = 0.0;
        for (double v : population) {
            average += v;
        }
        int n = population.length;
        average = average / (double) n;

        double variance = 0.0;
        for (double p : population) {
            double d = p - average;
            variance += d * d;
        }
        variance = variance / (double) n;

        return new double[]{average, variance};
    }

    public static double[] variance(DoubleStream s) {
        DoubleArrayList dd = new DoubleArrayList();
        s.forEach(dd::add);
        if (dd.isEmpty())
            return null;

        double avg = dd.average();

        int n = dd.size();
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double p = dd.get(i);
            double d = p - avg;
            double v = d * d;
            sum += v;
        }
        double variance = sum;
        variance = variance / (double) n;

        return new double[]{avg, variance};
    }

    public static String className(Object p) {
        Class<?> pClass = p.getClass();
        String s = pClass.getSimpleName();
        if (s.isEmpty())
            return pClass.toString().replace("class ", "");
        return s;
    }

    public static float[] toFloat(double[] d) {
        int l = d.length;
        float[] f = new float[l];
        for (int i = 0; i < l; i++)
            f[i] = (float) d[i];
        return f;
    }

    public static double[] toDouble(float[] d) {
        int l = d.length;
        double[] f = new double[10];
        int count = 0;
        for (int i = 0; i < l; i++) {
            double v = (double) d[i];
            if (f.length == count) f = Arrays.copyOf(f, count * 2);
            f[count++] = v;
        }
        f = Arrays.copyOfRange(f, 0, count);
        return f;
    }

    public static long[] minmax(IntToLongFunction f, int from, int to) {

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int i = from; i < to; i++) {
            long y = f.applyAsLong(i);
            if (y < min) min = y;
            if (y > max) max = y;
        }
        return new long[]{min, max};

    }

    public static float[] minmax(float[] x) {

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float y : x) {

            if (y < min) min = y;
            if (y > max) max = y;
        }
        return new float[]{min, max/*, sum */};
    }

    public static float[] minmaxsum(float[] x) {
        float sum = (float) 0;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float y : x) {
            sum += y;
            if (y < min) min = y;
            if (y > max) max = y;
        }
        return new float[]{min, max, sum};
    }

    public static double[] minmax(double[] x) {

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double y : x) {

            if (y < min) min = y;
            if (y > max) max = y;
        }
        return new double[]{min, max/*, sum */};
    }

    public static void time(Logger logger, String procName, Runnable procedure) {
        if (!logger.isInfoEnabled()) {
            procedure.run();
        } else {
            long dtNS = timeNS(procedure);
            logger.info("{} {}", procName, Texts.INSTANCE.timeStr((double) dtNS));
        }
    }

    public static long timeNS(Runnable procedure) {
        long start = System.nanoTime();

        procedure.run();

        long end = System.nanoTime();
        return end - start;
    }

    public static String tempDir() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * TODO make a version of this which can return the input array if no modifications occurr either by .equals() or identity
     */
    @SafeVarargs
    public static <X, Y> Y[] map(Function<X, Y> f, Y[] target, X... src) {
        return map(f, target, Math.min(target.length, src.length), src);
    }

    @SafeVarargs
    public static <X, Y> Y[] map(Function<X, Y> f, Y[] target, int size, X... src) {
        return map(f, target, 0, src, 0, size);
    }

    public static <X, Y> Y[] map(Function<X, Y> f, Y[] target, int targetOffset, X[] src, int srcFrom, int srcTo) {
        for (int i = srcFrom; i < srcTo; i++) {
            target[targetOffset++] = f.apply(src[i]);
        }
        return target;
    }

    /**
     * TODO make a version of this which can return the input array if no modifications occurr either by .equals() or identity
     */
    @SafeVarargs
    public static <X, Y> Y[] map(Function<X, Y> f, IntFunction<Y[]> targetBuilder, X... src) {
        int i = 0;
        Y[] target = targetBuilder.apply(src.length);
        for (X x : src) {
            Y y = f.apply(x);
            target[i++] = y;
        }
        return target;
    }

    @SafeVarargs
    public static <X> X[] mapIfChanged(UnaryOperator<X> f, X... src) {
        X[] target = null;
        for (int i = 0, srcLength = src.length; i < srcLength; i++) {
            X x = src[i];
            X y = f.apply(x);
            if (y != x) {
                if (target == null)
                    target = src.clone();

                target[i] = y;
            }
        }
        if (target == null)
            return src;
        else
            return target;
    }

    @SafeVarargs
    public static <X> float sum(FloatFunction<X> value, X... xx) {
        float y = (float) 0;
        for (X x : xx)
            y += value.floatValueOf(x);
        return y;
    }

    @SafeVarargs
    public static <X> double sumDouble(FloatFunction<X> value, X... xx) {
        double y = 0.0;
        for (X x : xx) {
            double floatValueOf = (double) value.floatValueOf(x);
            y += floatValueOf;
        }
        return y;
    }

    @SafeVarargs
    public static <X> double sum(ToDoubleFunction<X> value, X... xx) {
        double y = 0.0;
        for (X x : xx) {
            double v = value.applyAsDouble(x);
            y += v;
        }
        return y;
    }

    public static <X> int sum(ToIntFunction<X> value, Iterable<X> xx) {
        int y = 0;
        for (X x : xx)
            y += value.applyAsInt(x);
        return y;
    }

    public static <X> float sum(FloatFunction<X> value, Iterable<X> xx) {
        float y = (float) 0;
        for (X x : xx)
            y += value.floatValueOf(x);
        return y;
    }

    public static <X> float avg(FloatFunction<X> value, Iterable<X> xx) {
        float y = (float) 0;
        int count = 0;
        for (X x : xx) {
            y += value.floatValueOf(x);
            count++;
        }
        return y / (float) count;
    }

    @SafeVarargs
    public static <X> int sum(ToIntFunction<X> value, X... xx) {
        return sum(value, 0, xx.length, xx);
    }

    @SafeVarargs
    public static <X> int sum(ToIntFunction<X> value, int from, int to, X... xx) {
        int len = to - from;
        int y = 0;
        for (int i = from; i < len; i++) {
            int applyAsInt = value.applyAsInt(xx[i]);
            y += applyAsInt;
        }
        return y;
    }

    @SafeVarargs
    public static <X> long sum(ToLongFunction<X> value, X... xx) {
        long y = 0L;
        for (X x : xx) {
            long l = value.applyAsLong(x);
            y += l;
        }
        return y;
    }

    @SafeVarargs
    public static <X> long min(ToLongFunction<X> value, X... xx) {
        long y = Long.MAX_VALUE;
        for (X x : xx)
            y = Math.min(y, value.applyAsLong(x));
        return y;
    }

    @SafeVarargs
    public static <X> long max(ToLongFunction<X> value, X... xx) {
        long y = Long.MIN_VALUE;
        for (X x : xx)
            y = Math.max(y, value.applyAsLong(x));
        return y;
    }

    @SafeVarargs
    public static <X> int max(ToIntFunction<X> value, X... xx) {
        int y = Integer.MIN_VALUE;
        for (X x : xx)
            y = Math.max(y, value.applyAsInt(x));
        return y;
    }

    public static <X> long max(ToLongFunction<X> value, Iterable<X> xx) {
        long y = Long.MIN_VALUE;
        for (X x : xx)
            y = Math.max(y, value.applyAsLong(x));
        return y;
    }

    public static <X> long min(ToLongFunction<X> value, Iterable<X> xx) {
        long y = Long.MAX_VALUE;
        for (X x : xx)
            y = Math.min(y, value.applyAsLong(x));
        return y;
    }

    @SafeVarargs
    public static <X> boolean sumBetween(ToIntFunction<X> value, int min, int max, X... xx) {
        int y = 0;
        for (X x : xx) {
            if ((y += value.applyAsInt(x)) > max)
                return false;
        }
        return (y >= min);
    }

    @SafeVarargs
    public static <X> boolean sumExceeds(ToIntFunction<X> value, int max, X... xx) {
        int y = 0;
        for (X x : xx) {
            if ((y += value.applyAsInt(x)) > max)
                return true;
        }
        return false;
    }

    /**
     * warning: if values are the same then biases towards the first
     * TODO make a random one for cases where equivalents exist
     */
    public static int maxIndex(float... xx) {
        float y = Float.NEGATIVE_INFINITY;
        int best = -1;
        for (int i = 0; i < xx.length; i++) {
            float x = xx[i];
            if (x > y) {
                y = x;
                best = i;
            }
        }
        return best;
    }

    @SafeVarargs
    public static <X> float mean(FloatFunction<X> value, X... xx) {
        float y = (float) 0;
        for (X x : xx)
            y += value.floatValueOf(x);
        return y / (float) xx.length;
    }

    @SafeVarargs
    public static <X> float max(FloatFunction<X> value, X... xx) {
        float y = Float.NEGATIVE_INFINITY;
        for (X x : xx)
            y = Math.max(y, value.floatValueOf(x));
        return y;
    }

    public static <X> float max(FloatFunction<X> value, Iterable<X> xx) {
        float y = Float.NEGATIVE_INFINITY;
        for (X x : xx)
            y = Math.max(y, value.floatValueOf(x));
        return y;
    }

    @SafeVarargs
    public static <X> float min(FloatFunction<X> value, X... xx) {
        float y = Float.POSITIVE_INFINITY;
        for (X x : xx)
            y = Math.min(y, value.floatValueOf(x));
        return y;
    }

    public static int sum(int... x) {
        int y = 0;
        for (int i : x) {
            y += i;
        }
        return y;
    }

    public static int sum(int[] x, int from, int to) {
        int y = 0;
        for (int j = from; j < to; j++) {
            int i = x[j];
            y += i;
        }
        return y;
    }

    public static double max(double... x) {
        boolean seen = false;
        double best = (double) 0;
        for (double f : x) {
            if (f >= Double.NEGATIVE_INFINITY) {
                if (!seen || Double.compare(f, best) > 0) {
                    seen = true;
                    best = f;
                }
            }
        }
        double y = seen ? best : Double.NEGATIVE_INFINITY;
        return y;
    }

    public static byte max(byte... x) {
        byte y = Byte.MIN_VALUE;
        for (byte f : x) {
            if ((int) f > (int) y)
                y = f;
        }
        return y;
    }

    public static float max(float... x) {
        float y = Float.NEGATIVE_INFINITY;
        for (float f : x) {
            if (f > y)
                y = f;
        }
        return y;
    }

    public static double min(double... x) {
        boolean seen = false;
        double best = (double) 0;
        for (double f : x) {
            if (f <= Double.POSITIVE_INFINITY) {
                if (!seen || Double.compare(f, best) < 0) {
                    seen = true;
                    best = f;
                }
            }
        }
        double y = seen ? best : Double.POSITIVE_INFINITY;
        return y;
    }

    public static float min(float... x) {
        float y = Float.POSITIVE_INFINITY;
        for (float f : x) {
            if (f < y)
                y = f;
        }
        return y;
    }

    public static float min(int s, int e, float... x) {
        float y = Float.POSITIVE_INFINITY;
        for (int i = s; i < e; i++) {
            float f = x[i];
            if (f < y)
                y = f;
        }
        return y;
    }

    public static float max(int s, int e, float... x) {
        float y = Float.NEGATIVE_INFINITY;
        for (int i = s; i < e; i++) {
            float f = x[i];
            if (f > y)
                y = f;
        }
        return y;
    }

    public static float sum(float... x) {
        float y = (float) 0;
        for (float f : x)
            y += f;
        return y;
    }

    public static float sumAbs(float... x) {
        float y = (float) 0;
        for (float f : x) {
            y += Math.abs(f);
        }
        return y;
    }

    /**
     * TODO fair random selection when exist equal values
     */
    public static int argmax(double... vec) {
        int result = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0, l = vec.length; i < l; i++) {
            double v = vec[i];
            if (v > max) {
                max = v;
                result = i;
            }
        }
        return result;
    }

    /**
     * TODO fair random selection when exist equal values
     */
    public static int argmax(float... vec) {
        int result = -1;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0, l = vec.length; i < l; i++) {
            float v = vec[i];
            if (v > max) {
                max = v;
                result = i;
            }
        }
        return result;
    }

    public static void shuffle(Object[] ar, Random rnd) {
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);

            Object a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    public static int argmax(Random random, float... vec) {
        int result = -1;
        float max = Float.NEGATIVE_INFINITY;

        int l = vec.length;
        int start = random.nextInt(l);
        for (int i = 0; i < l; i++) {
            int ii = (i + start) % l;
            float v = vec[ii];
            if (v > max) {
                max = v;
                result = ii;
            }
        }
        return result;
    }


    /**
     * min is inclusive, max is exclusive: [min, max)
     */
    public static int unitize(int x, int min, int max) {
        if (x < min) x = min;
        else if (x > max) x = max;
        return x;
    }

    public static float sum(int count, IntToFloatFunction values) {
        float weightSum = (float) 0;
        for (int i = 0; i < count; i++) {
            float w = values.valueOf(i);
            assert (w == w && w >= (float) 0);
            weightSum += w;
        }
        return weightSum;
    }

    public static float sumIfPositive(int count, IntToFloatFunction values) {
        float weightSum = (float) 0;
        for (int i = 0; i < count; i++) {
            float w = values.valueOf(i);

            if (w == w && w > Float.MIN_NORMAL)
                weightSum += w;
        }
        return weightSum;
    }

    public static boolean equals(double[] a, double[] b, double epsilon) {
        if (Arrays.equals(a, b)) return true;
        int l = a.length;
        for (int i = 0; i < l; i++) {
            if (!equals(a[i], b[i], epsilon)) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(long[] a, long[] b, int firstN) {
        if (Arrays.equals(a, b)) return true;
        for (int i = 0; i < firstN; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(long[] a, long[] b) {
        if (Arrays.equals(a, b)) return true;
        int l = a.length;
        if (b.length != l)
            return false;
        for (int i = 0; i < l; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(short[] a, short[] b) {
        if (Arrays.equals(a, b)) return true;
        int l = a.length;
        if (b.length != l)
            return false;
        for (int i = 0; i < l; i++) {
            if ((int) a[i] != (int) b[i]) {
                return false;
            }
        }
        return true;
    }

    public static int short2Int(short high, short low) {
        return (int) high << 16 | (int) low;
    }

    public static short short2Int(int x, boolean high) {
        return high ? (short) (x >> 16) : (short) (x & 0xffff);
    }

    public static float clamp(float f, float min, float max) {
        assertFinite(f);
        assertFinite(min);
        assertFinite(max);
        assert (min <= max);
        return clampSafe(f, min, max);
    }

    public static float clampSafe(float f, float min, float max) {
        if (f > max) f = max;
        if (f < min) f = min;
        return f;

    }

    public static double clamp(double f, double min, double max) {
        assertFinite(f);
        assertFinite(min);
        assertFinite(max);
        assert (min <= max);
        return clampSafe(f, min, max);
    }

    public static double clampSafe(double f, double min, double max) {
        return Math.max(Math.min(f, max), min);
    }

    public static int clamp(int i, int min, int max) {
        assert (min <= max);
        return clampSafe(i, min, max);
    }

    public static int clampSafe(int i, int min, int max) {
        if (i < min) i = min;
        if (i > max) i = max;
        return i;
    }

    public static long clamp(long i, long min, long max) {
        assert (min <= max);
        return clampSafe(i, min, max);
    }

    public static long clampSafe(long i, long min, long max) {
        if (i < min) i = min;
        if (i > max) i = max;
        return i;
    }

    /**
     * range [a, b)
     */
    public static int[] intArray(int a, int b) {
        int ba = b - a;
        int[] x = new int[10];
        int count = 0;
        for (int i = 0; i < ba; i++) {
            int i1 = a + i;
            if (x.length == count) x = Arrays.copyOf(x, count * 2);
            x[count++] = i1;
        }
        x = Arrays.copyOfRange(x, 0, count);
        return x;
    }

    public static double sqr(long x) {
        return (double) (x * x);
    }

    public static int sqr(int x) {
        return x * x;
    }

    public static int cube(int x) {
        return x * x * x;
    }

    public static float sqr(float x) {
        return x * x;
    }

    public static float sqrt(float v) {
        return (float) Math.sqrt((double) v);
    }

    public static float cube(float x) {
        return x * x * x;
    }

    public static double cube(double x) {
        return x * x * x;
    }

    public static double sqr(double x) {
        return x * x;
    }

    public static String uuid128() {
        UUID u = UUID.randomUUID();
        long a = u.getLeastSignificantBits();
        StringBuilder sb = new StringBuilder(6);
        BinTxt.append(sb, a);
        long b = u.getMostSignificantBits();
        BinTxt.append(sb, b);
        return sb.toString();
    }


    public static String uuid64() {


        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long c = rng.nextLong();
        return BinTxt.toString(c);
    }

    /**
     * adaptive spinlock behavior
     * see: https:
     * TODO tune
     * TODO randomize?
     */
    public static void pauseSpin(int previousContiguousPauses) {
        if (previousContiguousPauses < 1) {
            return;
        } else if (previousContiguousPauses < 512) {
            onSpinWait();
        } else if (previousContiguousPauses < 1024) {


            if ((previousContiguousPauses & 31) == 0) {
                Thread.yield();
            } else {
                onSpinWait();
            }

        } else if (previousContiguousPauses < 2048) {

            if ((previousContiguousPauses & 7) == 0) {
                Thread.yield();
            } else {
                onSpinWait();
            }
        } else {

            Thread.yield();
        }
    }

    /*
        static final long PARK_TIMEOUT = 50L;
    static final int MAX_PROG_YIELD = 2000;
            if(n > 500) {
            if(n<1000) {
                
                if((n & 0x7) == 0) {
                    LockSupport.parkNanos(PARK_TIMEOUT);
                } else {
                    onSpinWait();
                }
            } else if(n<MAX_PROG_YIELD) {
                
                if((n & 0x3) == 0) {
                    Thread.yield();
                } else {
                    onSpinWait();
                }
            } else {
                Thread.yield();
                return n;
            }
        } else {
            onSpinWait();
        }
        return n+1;

     */


    public static void sleepMS(long periodMS) {
        sleepNS(periodMS * 1_000_000L);
    }


    public static void sleep(long sleepFor, TimeUnit unit) {
        sleepNS(unit.toNanos(sleepFor));
    }

    public static void sleepNS(long remainingNanos) {

        sleepNS(remainingNanos, (long) (50 * 1000) /* 50uSec is the default linux kernel resolution result */);
    }

    /**
     * https:
     * expect ~50uSec resolution on linux
     */
    public static void sleepNS(long nanos, long thresholdNS) {

        if (nanos <= 0L) return;

        long now = System.nanoTime();
        long end = now + nanos;
        while (nanos > 0L) {

            if (nanos >= thresholdNS) {
                LockSupport.parkNanos(nanos);
            } else {
                Thread.onSpinWait();
            }

            now = System.nanoTime();
            nanos = end - now;

        }


    }


    public static void sleepNSwhile(long periodNS, long napTimeNS, BooleanSupplier keepSleeping) {
        if (!keepSleeping.getAsBoolean())
            return;

        if (periodNS <= napTimeNS) {
            sleepNS(periodNS);
        } else {
            long now = System.nanoTime();
            long end = now + periodNS;
            do {
                sleepNS(Math.min(napTimeNS, end - now));
            } while (((now = System.nanoTime()) < end) && keepSleeping.getAsBoolean());
        }
    }

    public static int largestPowerOf2NoGreaterThan(int i) {
        if (isPowerOf2(i))
            return i;
        else {
            int curr = i - 1;
            while (curr > 0) {
                if (isPowerOf2(curr)) {
                    return curr;
                } else {
                    --curr;
                }
            }
            return 0;
        }
    }

    public static boolean isPowerOf2(int n) {
        if (n < 1) {
            return false;
        } else {
            double p_of_2 = (Math.log((double) n) / log2);
            return Math.abs(p_of_2 - (double) Math.round((float) (int) p_of_2)) == (double) 0;
        }
    }

    /**
     * http:
     * calculate height on a uniform grid, by splitting a quad into two triangles:
     */
    public static float lerp2d(float x, float z, float nw, float ne, float se, float sw) {

        x = x - (float) (int) x;
        z = z - (float) (int) z;


        if (x > z)
            sw = nw + se - ne;
        else
            ne = se + nw - sw;


        float n = lerp(x, ne, nw);
        float s = lerp(x, se, sw);
        return lerp(z, s, n);
    }

    public static String secondStr(double s) {
        int decimals;
        if (s >= 0.01) decimals = 0;
        else if (s >= 0.00001) decimals = 3;
        else decimals = 6;

        return secondStr(s, decimals);
    }

    public static String secondStr(double s, int decimals) {
        if (decimals < 0)
            return secondStr(s);
        else {
            switch (decimals) {
                case 0:
                    return Texts.INSTANCE.n2(s) + 's';
                case 3:
                    return Texts.INSTANCE.n2(s * 1000.0) + "ms";
                case 6:
                    return Texts.INSTANCE.n2(s * 1.0E6) + "us";
                default:
                    throw new UnsupportedOperationException("TODO");
            }
        }
    }

    /**
     * A function where the output is disjunctively determined by the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The output that is no smaller than each input
     */

    public static <X> X[] sortUniquely(X[] arg) {
        int len = arg.length;
        Arrays.sort(arg);
        for (int i = 0; i < len - 1; i++) {
            int dups = 0;
            while (arg[i].equals(arg[i + 1])) {
                dups++;
                if (++i == len - 1)
                    break;
            }
            if (dups > 0) {
                System.arraycopy(arg, i, arg, i - dups, len - i);
                len -= dups;
            }
        }

        return len != arg.length ? Arrays.copyOfRange(arg, 0, len) : arg;
    }

    public static boolean calledBySomethingContaining(String s) {
        return Joiner.on(' ').join(Thread.currentThread().getStackTrace()).contains(s);
    }

    /**
     * a and b should be in 0..1.0 unit domain; output will also
     */
    public static float or(float a, float b) {
        return 1.0f - ((1.0f - a) * (1.0f - b));
    }

    public static double or(double a, double b) {
        return 1.0 - ((1.0 - a) * (1.0 - b));
    }


    public static <X> int count(Predicate<X> p, X[] xx) {
        long count = 0L;
        for (X x : xx) {
            if (p.test(x)) {
                count++;
            }
        }
        int i = (int) count;
        return i;
    }

    public static <X> boolean and(Predicate<X> p, int from, int to, X[] xx) {
        for (int i = from; i < to; i++) {
            if (!p.test(xx[i])) {
                return false;
            }
        }
        return true;
    }

    public static <X> boolean or(Predicate<X> p, int from, int to, X[] xx) {
        for (int i = from; i < to; i++) {
            if (p.test(xx[i])) {
                return true;
            }
        }
        return false;
    }

    public static <X> boolean and(Predicate<X> p, X[] xx) {
        return and(p, 0, xx.length, xx);
    }

    public static <X> boolean or(Predicate<X> p, X[] xx) {
        return or(p, 0, xx.length, xx);
    }

    public static <X> boolean and(X x, Iterable<Predicate<? super X>> p) {
        for (Predicate pp : p) {
            if (!pp.test(x))
                return false;
        }
        return true;
    }

    public static <X> boolean and(Predicate<? super X> p, Iterable<X> xx) {
        for (X x : xx) {
            if (!p.test(x))
                return false;
        }
        return true;
    }

    public static <X> boolean or(Predicate<? super X> p, Iterable<X> xx) {
        for (X x : xx) {
            if (p.test(x))
                return true;
        }
        return false;
    }

    /**
     * a and b should be in 0..1.0 unit domain; output will also
     */
    public static float and(float a, float b) {
        return a * b;
    }

    public static float and(float a, float b, float c) {
        return a * b * c;
    }

    public static float or(float a, float b, float c) {
        return 1.0f - ((1.0f - a) * (1.0f - b) * (1.0f - c));
    }

    /**
     * json/msgpack serialization
     */
    public static byte[] toBytes(Object x) throws JsonProcessingException {
        return cborMapper.writeValueAsBytes(x);
    }

    public static byte[] toBytes(Object x, Class cl) throws JsonProcessingException {
        return cborMapper.writerFor(cl).writeValueAsBytes(x);
    }


    /**
     * msgpack deserialization
     */
    public static <X> X fromBytes(byte[] msgPacked, Class<? extends X> type) throws IOException {
        return cborMapper/*.reader(type)*/.readValue(msgPacked, type);
    }

    public static <X> X fromBytes(byte[] msgPacked, int len, Class<? extends X> type) throws IOException {
        return cborMapper/*.reader(type)*/.readValue(msgPacked, 0, len, type);
    }


    public static JsonNode jsonNode(Object x) {
        if (x instanceof String) {
            try {
                return jsonMapper.readTree(x.toString());
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        return cborMapper.valueToTree(x);
    }

    /**
     * x in -1..+1, y in -1..+1.   typical value for sharpen will be ~ >5
     * http:
     */
    public static float sigmoidBipolar(float x, float sharpen) {
        return (float) ((1.0 / (1.0 + Math.exp((double) (-sharpen * x))) - 0.5) * 2.0);
    }


    public static float[] toFloat(double[] a, int from, int to, DoubleToFloatFunction df) {
        float[] result = new float[to - from];
        for (int j = 0, i = from; i < to; i++, j++) {
            result[j] = df.valueOf(a[i]);
        }
        return result;
    }

    public static float[] toFloat(double[] a, int from, int to) {
        float[] result = new float[to - from];
        for (int j = 0, i = from; i < to; i++, j++) {
            result[j] = (float) a[i];
        }
        return result;
    }

    public static void mul(float scale, float[] f) {
        for (int i = 0; i < f.length; i++)
            f[i] *= scale;
    }

    public static void mul(double scale, double[] f) {
        for (int i = 0; i < f.length; i++)
            f[i] *= scale;
    }

    public static <X> X[] map(int n, IntFunction<X[]> arrayizer, IntFunction<X> build) {
        return map(0, n, arrayizer, build);
    }

    public static <X> X[] map(int from, int to, IntFunction<X[]> arrayizer, IntFunction<X> build) {
        assert (to >= from);
        return map(from, to, build, arrayizer.apply(to - from));
    }

    private static <X> X[] map(int from, int to, IntFunction<X> build, X[] x) {
        for (int i = from, j = 0; i < to; )
            x[j++] = build.apply(i++);
        return x;
    }

    public static float[] map(int num, IntToFloatFunction build) {
        return map(num, build, null);
    }


    public static float softmax(float x, float temp) {
        float f = (float) Math.exp((double) (x / temp));
        if (!Float.isFinite(f))
            throw new RuntimeException("softmax(" + f + ',' + temp + ") is non-finite");
        return f;
    }


    public static float[] map(IntToFloatFunction build, @Nullable float[] target) {
        return map(target.length, build, target);
    }

    public static double[] map(IntToDoubleFunction build, @Nullable double[] target) {
        return map(target.length, build, target);
    }

    public static float[] map(int num, IntToFloatFunction build, @Nullable float[] reuse) {
        @Nullable float[] f = (reuse != null && reuse.length == num) ? reuse : new float[num];
        for (int i = 0; i < num; i++)
            f[i] = build.valueOf(i);
        return f;
    }

    public static double[] map(int num, IntToDoubleFunction build, @Nullable double[] reuse) {
        @Nullable double[] f = (reuse != null && reuse.length == num) ? reuse : new double[num];
        for (int i = 0; i < num; i++)
            f[i] = build.applyAsDouble(i);
        return f;
    }

    public static <X> float[] map(X[] what, FloatFunction<X> value) {
        int num = what.length;
        float[] f = new float[num];
        for (int i = 0; i < num; i++) {
            f[i] = value.floatValueOf(what[i]);
        }
        return f;
    }

    /**
     * returns amount of memory used as a value between 0 and 100% (1.0)
     */
    public static float memoryUsed() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();
        long usedMemory = total - free;
        long availableMemory = max - usedMemory;
        float ratio = 1f - ((float) availableMemory) / (float) max;

        return ratio;
    }

    /**
     * reverse a subarray in place
     * indices are inclusive, so be careful the 'j' param may need -1
     */
    public static void reverse(Object[] array, int i, int j) {
        while (j > i) {
            Object tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    public static void toMap(Frequency f, String header, BiConsumer<String, Object> x) {
        toMap(f.entrySetIterator(), header, x);
    }

    public static void toMap(HashBag<?> f, String header, BiConsumer<String, Object> x) {
        f.forEachWithIndex((e, n) -> x.accept(header + ' ' + e, n));
    }

    public static void toMap(Iterator<? extends Map.Entry<?, ?>> f, String header, BiConsumer<String, Object> x) {
        f.forEachRemaining((e) -> x.accept(header + ' ' + e.getKey(), e.getValue()));
    }


    /**
     * pretty close
     * http:
     * https:
     * http:
     */
    public static float tanhFast(float x) {
        if (x <= -3.0F) return -1f;
        if (x >= 3f) return +1f;
        return x * (27.0F + x * x) / (27.0F + 9.0F * x * x);
    }

    public static Object toString(Object x) {
        return x.getClass() + "@" + System.identityHashCode(x);
    }

    public static <X> Supplier<Stream<X>> buffer(Stream<X> x) {
        List<X> buffered = x.collect(toList());
        return buffered::stream;
    }

    /**
     * creates an immutable sublist from a ByteList, since this isnt implemented yet in Eclipse collections
     */
    public static ImmutableByteList subList(ByteList x, int a, int b) {
        int size = b - a;
        if (a == 0 && b == x.size())
            return x.toImmutable();

        switch (size) {
            case 0:
                return ByteLists.immutable.empty();
            case 1:
                return ByteLists.immutable.of(x.get(a));
            case 2:
                return ByteLists.immutable.of(x.get(a++), x.get(a));
            case 3:
                return ByteLists.immutable.of(x.get(a++), x.get(a++), x.get(a));
            case 4:
                return ByteLists.immutable.of(x.get(a++), x.get(a++), x.get(a++), x.get(a));
            case 5:
                return ByteLists.immutable.of(x.get(a++), x.get(a++), x.get(a++), x.get(a++), x.get(a));
            case 6:
                return ByteLists.immutable.of(x.get(a++), x.get(a++), x.get(a++), x.get(a++), x.get(a++), x.get(a));
            default:
                return ByteLists.immutable.of(ArrayUtil.subarray(x.toArray(), a, b));
        }
    }

    public static byte branchOr(byte key, ByteByteHashMap count, byte branch) {
        byte branchBit = (byte) (1 << (int) branch);
        return count.updateValue(key, branchBit, new ByteToByteFunction() {
            @Override
            public byte valueOf(byte x) {
                return (byte) ((int) x | (int) branchBit);
            }
        });
    }

    public static <X> X first(X[] x) {
        return x[0];
    }

    public static <X> X last(X[] x) {
        return x[x.length - 1];
    }

    /*
    curved-sawtooth function:
        the left-half of this wave is convex, like capacitor charging.
        the right-half of the wave is concave, like capacitor discharging

        for efficiency, dont use the exponential function which circuit models
        use but instead use the circle which can be computed with polynomial

            left    0<=x<=0.5:     0.5 * (1-(x-0.5)*(x-0.5)*4)^0.5
            right:  0.5<x<=1.0:   -0.5 * (1-(x-1.0)*(x-1.0)*4)^0.5+0.5
                                     a * sqrt(1-(x+tx)*(x+tx)*4) + b

        hypothesis: the discontinuity at the midpoint represents the threshold point
        where metastable behavior can be attracted

        this value is then used to LERP between the min and max priority limits, mapping
        that range to the 0..1.0 unit range of this sawtooth.

        see doc/CurvedSawtooth_Function.svg
     */
    public static float sawtoothCurved(float x) {
        float tx, a, b;
        if (x < 0.5f) {
            a = +0.5f;
            b = 0f;
            tx = -0.5f;
        } else {
            a = -0.5f;
            b = +0.5f;
            tx = -1f;
        }
        float x0 = (x + tx);
        return (float) ((double) a * Math.sqrt((double) (1f - x0 * x0 * 4.0F)) + (double) b);
    }


    /**
     * Get the location from which the supplied object's class was loaded.
     *
     * @param object the object for whose class the location should be retrieved
     * @return an {@code Optional} containing the URL of the class' location; never
     * {@code null} but potentially empty
     */
    public static @Nullable URL locate(ClassLoader loader, String className) {


        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
            while (loader != null && loader.getParent() != null) {
                loader = loader.getParent();
            }
        }

        if (loader != null) {


            try {
                return (loader.getResource(className));
            } catch (Throwable ignore) {
                /* ignore */
            }
        }


        return null;
    }

    public static FloatSupplier compose(FloatSupplier f, FloatToFloatFunction g) {
        return new FloatSupplier() {
            @Override
            public float asFloat() {
                float fx = f.asFloat();
                return g.valueOf(fx);
            }
        };
    }

    public static FloatToFloatFunction compose(FloatToFloatFunction f, FloatToFloatFunction g) {
        return new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                float fx = f.valueOf(x);
                return g.valueOf(fx);
            }
        };
    }

    public static int concurrency() {
        return concurrencyExcept(0);
    }

    public static int concurrencyExcept(int reserve) {

        String specifiedThreads = System.getenv("threads");
        int threads;
        if (specifiedThreads != null)
            threads = Texts.INSTANCE.i(specifiedThreads);
        else
            threads = Runtime.getRuntime().availableProcessors() - reserve;

        int maxThreads = Integer.MAX_VALUE;
        int minThreads = 2;
        return Util.clamp(
                threads, minThreads, maxThreads);
    }

    /**
     * modifies the input; instance compare, not .equals
     */
    public static <X> X[] replaceDirect(X[] xx, X from, X to) {
        for (int i = 0, xxLength = xx.length; i < xxLength; i++) {
            X x = xx[i];
            if (x == from)
                xx[i] = to;
        }
        return xx;
    }

    public static <X> X[] replaceDirect(X[] xx, UnaryOperator<X> f) {
        return replaceDirect(xx, 0, xx.length, f);
    }

    public static <X> X[] replaceDirect(X[] xx, int start, int end, UnaryOperator<X> f) {
        for (int i = start; i < end; i++) {
            X x = xx[i];
            xx[i] = f.apply(x);
        }
        return xx;
    }

    public static <X> FloatFunction<X> softmaxFunc(FloatFunction<X> f, float temperature) {
        return new FloatFunction<X>() {
            @Override
            public float floatValueOf(X x) {
                return softmax(f.floatValueOf(x), temperature);
            }
        };
    }

    public static float and(float a, float b, float c, float d) {
        return a * b * c * d;
    }

    /**
     * A function where the output is the arithmetic average the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The arithmetic average the inputs
     */
    public static float aveAri(float... arr) {
        float sum = (float) 0;
        for (float f : arr) {
            sum += f;
        }
        return sum / (float) arr.length;
    }

    /**
     * more efficient version
     */
    public static float aveAri(float a, float b) {
        return (a + b) / 2.0f;
    }

    /**
     * A function where the output is the geometric average the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The geometric average the inputs
     */
    public static float aveGeo(float... arr) {
        float product = 1.0F;
        for (float f : arr) {
            if (f == (float) 0) return (float) 0;
            product *= f;
        }
        return (float) pow((double) product, 1.00 / (double) arr.length);
    }

    public static float aveGeo(float a, float b) {
        return (float) Math.sqrt((double) (a * b));
    }

    public static void assertUnitized(float... f) {
        for (float x : f) {
            assertUnitized(x);
        }
    }

    public static float assertUnitized(float x) {
        if (!Float.isFinite(x) || x < (float) 0 || x > 1.0F)
            throw new UnsupportedOperationException("non-unitized value: " + x);
        return x;
    }

    public static double assertUnitized(double x) {
        if (!Double.isFinite(x) || x < (double) 0 || x > 1.0)
            throw new UnsupportedOperationException("non-unitized value: " + x);
        return x;
    }

    /**
     * a number, or... (otherwise)
     */
    public static float numOr(float x, float otherwise) {
        if (x == x) return x;
        else return otherwise;
    }

    /**
     * tests if the array is already in natural order
     */
    public static <X extends Comparable> boolean isSorted(X[] x) {
        if (x.length < 2) return true;
        for (int i = 1; i < x.length; i++) {
            if (x[i - 1].compareTo(x[i]) > 0) {
                return false;
            }
        }
        return true;
    }

    public static int[] bytesToInts(byte[] array) {
        int n = array.length;
        if (n == 0)
            return ArrayUtil.EMPTY_INT_ARRAY;
        int[] t = new int[10];
        int count = 0;
        for (int i = 0; i < n; i++) {
            int i1 = (int) array[i];
            if (t.length == count) t = Arrays.copyOf(t, count * 2);
            t[count++] = i1;
        }
        t = Arrays.copyOfRange(t, 0, count);
        return t;
    }

    public static Class[] typesOfArray(Object[] orgs) {
        return typesOfArray(orgs, 0, orgs.length);
    }

    public static Class[] typesOfArray(Object[] orgs, int from, int to) {
        if (orgs.length == 0)
            return ArrayUtil.EMPTY_CLASS_ARRAY;
        else {
            return map(new Function<Object, Class>() {
                           @Override
                           public Class apply(Object x) {
                               return Primitives.unwrap(x.getClass());
                           }
                       },
                    new Class[to - from], 0, orgs, from, to);
        }
    }

    public static FasterList<Class<?>> typesOf(Object[] orgs, int from, int to) {
        return new FasterList<>(typesOfArray(orgs, from, to));
    }


    /**
     * fits a polynomial curve to the specified points and compiles an evaluator for it
     */
    public static <X> ToIntFunction<X> curve(ToIntFunction<X> toInt, int... pairs) {
        if (pairs.length % 2 != 0)
            throw new RuntimeException("must be even # of arguments");

        int points = pairs.length / 2;
        if (points < 2) {

            throw new RuntimeException("must provide at least 2 points");
        }


        List<WeightedObservedPoint> obs = new FasterList(points);
        int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
        for (int i = 0; i < pairs.length; ) {
            int y = (int) pairs[i++];
            obs.add(new WeightedObservedPoint(1, (double) pairs[i++], y));
            if (y < yMin) yMin = y;
            if (y > yMax) yMax = y;
        }


        int degree =
                points - 1;


        float[] coefficients = Util.toFloat(PolynomialCurveFitter.create(degree).fit(obs));

        /* adapted from: PolynomialFunction
           https:
           */
        int YMin = yMin, YMax = yMax;
        assert (yMin < yMax);
        return new ToIntFunction<X>() {
            @Override
            public int applyAsInt(X X) {
                int n = coefficients.length;
                float x = (float) toInt.applyAsInt(X);
                float y = coefficients[n - 1];
                for (int j = n - 2; j >= 0; j--) {
                    y = x * y + coefficients[j];
                }
                return Util.clampSafe(Math.round(y), YMin, YMax);
            }
        };
    }

    public static int sqrtInt(int x) {
        if (x < 0)
            throw new NumberException("sqrt of negative value", x);
        return (int) Math.round(Math.sqrt((double) x));
    }


    /**
     * scan either up or down within a capacity range
     */
    public static int next(int current, boolean direction, int cap) {
        if (direction) {
            if (++current == cap) return 0;
        } else {
            if (--current == -1) return cap - 1;
        }
        return current;
    }

    /**
     * if the collection is known to be of size==1, get that item in a possibly better-than-default way
     * according to the Collection's implementation
     */
    public static @Nullable <X> X only(Collection<X> next) {
        if (next instanceof List)
            return ((List<X>) next).get(0);
        else if (next instanceof MutableSet)
            return ((MutableSet<X>) next).getOnly();
        else if (next instanceof SortedSet)
            return ((SortedSet<X>) next).first();
        else
            return next.iterator().next();

    }

    @SafeVarargs
    public static <X> IntSet intSet(ToIntFunction<X> f, X... items) {
        switch (items.length) {
            case 0:
                return IntSets.immutable.empty();
            case 1:
                return IntSets.immutable.of(f.applyAsInt(items[0]));
            case 2:
                return IntSets.immutable.of(f.applyAsInt(items[0]), f.applyAsInt(items[1]));

            default:
                IntHashSet i = new IntHashSet(items.length);
                for (X x : items) {
                    i.add(f.applyAsInt(x));
                }
                return i;
        }
    }

    public static float intProperty(String name, int defaultValue) {
        String p = System.getProperty(name);
        return (float) (p != null ? Integer.parseInt(p) : defaultValue);
    }

    public static double interpSum(float[] data, double sStart, double sEnd) {
        return interpSum(new IntToFloatFunction() {
            @Override
            public float valueOf(int i) {
                return data[i];
            }
        }, data.length, sStart, sEnd, false);
    }

    public static double interpSum(IntToFloatFunction data, int capacity, double sStart, double sEnd, boolean wrap) {
        int iStart = (int) Math.ceil(sStart);
        int iEnd = (int) Math.floor(sEnd);
        if (iEnd < 0 || iStart >= capacity)
            return (double) 0;

        if (iEnd == iStart)
            return (double) data.valueOf(iStart);

        int i = iStart - 1;

        if (i < 0) {
            if (wrap)
                while (i < 0) i += capacity;
            else
                i = 0;
        } else if (i >= capacity) {
            i = 0;
        }

        double sum = (double) 0;
        sum += iStart > 0 ? ((double) iStart - sStart) * (double) data.valueOf(i++) : (double) 0;

        for (int k = iStart; k < iEnd; k++) {
            if (i == capacity) i = 0;
            sum = sum + (double) data.valueOf(i++);
        }

        if (i == capacity) i = 0;
        sum += (sEnd - (double) iEnd) * (double) data.valueOf(i);
        return sum;
    }


    public static int longToInt(long x) {
        if (x > (long) (Integer.MAX_VALUE - 1) || x < (long) (Integer.MIN_VALUE + 1))
            throw new NumberException("long exceeds int capacity", x);
        return (int) x;
    }

    /**
     * faster than cartesian distance
     */
    public static void normalizeHamming(float[] v, float target) {
        float current = (float) 0;
        for (int i = 0; i < v.length; i++) {
            current += Math.abs(v[i]);
        }
        if (current < ScalarValue.Companion.getEPSILON()) {
            Arrays.fill(v, target / (float) v.length);
        } else {
            float scale = target / current;
            for (int i = 0; i < v.length; i++) {
                v[i] *= scale;
            }
        }
    }

    public static long readToWrite(long l, StampedLock lock) {
        return readToWrite(l, lock, true);
    }

    public static long readToWrite(long l, StampedLock lock, boolean strong) {
        if (l != 0L) {
            long ll = lock.tryConvertToWriteLock(l);
            if (ll != 0L) return ll;

            if (!strong) return 0L;

            lock.unlockRead(l);
        }

        return strong ? lock.writeLock() : lock.tryWriteLock();
    }

    public static long writeToRead(long l, StampedLock lock) {
        if (l != 0L) {
            long ll = lock.tryConvertToReadLock(l);
            if (ll != 0L) return ll;

            lock.unlockWrite(l);
        }

        return lock.readLock();
    }

    public static <X, Y extends X, Z extends X> X maybeEqual(Y current, Z next) {
        return Objects.equals(current, next) ? current : next;
    }

    public static <X> X maybeEqual(X x, X a, X b) {
        if (Objects.equals(x, a)) return a;
        if (Objects.equals(x, b)) return b;
        return x;
    }

    public static void nop() {
        Thread.onSpinWait();
    }

    public static float sqrtBipolar(float d) {
        return Math.signum(d) * Util.sqrt(Math.abs(d));
    }
}