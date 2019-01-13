package jcog;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Primitives;
import jcog.data.list.FasterList;
import jcog.io.BinTxt;
import jcog.math.FloatSupplier;
import jcog.math.NumberException;
import jcog.util.ArrayUtils;
import net.openhft.hashing.LongHashFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.stat.Frequency;
import org.eclipse.collections.api.block.function.primitive.DoubleToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.lang.Math.pow;
import static java.lang.Thread.onSpinWait;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 *
 *
 *
 */
public enum Util {
    ;

    public static final Unsafe unsafe;
    public static float sqrtMIN_NORMAL = (float)Math.sqrt(Float.MIN_NORMAL);

    static {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            unsafe = (Unsafe) singleoneInstanceField.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static final int PRIME3 = 524287;
    public static final int PRIME2 = 92821;
    public static final int PRIME1 = 31;

    //public static final int MAX_CONCURRENCY = Runtime.getRuntime().availableProcessors();
    public static final ImmutableByteList EmptyByteList = ByteLists.immutable.empty();
    public final static ObjectMapper msgPacker =
            new ObjectMapper(new MessagePackFactory())
                    .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    public final static ObjectMapper jsonMapper =
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

    public static final double log2 = Math.log(2);


    private static final int BIG_ENOUGH_INT = 16 * 1024;
    private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
//    private static final double BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5;


    //public static final VarHandle ITEM = MethodHandles.arrayElementVarHandle(Object[].class);

    /**
     * It is basically the same as a lookup table with 2048 entries and linear interpolation between the entries, but all this with IEEE floating point tricks.
     * http:
     */
    public static double expFast(double val) {
        long tmp = (long) (1512775 * val + (1072693248 - 60801));
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

    public static int hash(byte[] bytes) {
        return hash(bytes, 0, bytes.length);
    }

    public static int hash(long[] longs) {
        return Long.hashCode(_hashFn.hashLongs(longs));
    }

    static final LongHashFunction _hashFn = LongHashFunction.xx();

    public static int hash(byte[] bytes, int from, int to) {
        //return hashBytes(bytes, from, to);
        //return hashFNV(bytes, from, to);
        long hash = _hashFn.hashBytes(bytes, from, to - from);
        return Long.hashCode(hash);
    }

//    /**
//     * untested custom byte[] hash function
//     */
//    private static int hashBytes(byte[] bytes, int from, int to) {
//        int x = 1; //0x811c9dc5;
//        int y = 0;
//        int count = 3;
//        for (int i = from; i < to; i++) {
//            if (count-- == 0) {
//                x = Util.hashCombine(x, y);
//                y = 0;
//                count = 3;
//            }
//
//            y = (y << 8) | bytes[i];
//
//        }
//        return x | y;
//    }

    private static int hashFNV(byte[] bytes, int from, int to) {
        int h = 0x811c9dc5;
        for (int i = from; i < to; i++)
            h = (h * 16777619) ^ bytes[i];
        return h;
    }


    public static long hash64(byte[] bytes) {
        return hash64(bytes, 0, bytes.length);
    }

    public static long hash64(byte[] bytes, int from, int to) {
        return Util.hashELF(bytes, 1, from, to);
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



    /*
     **************************************************************************
     *                                                                        *
     *          General Purpose Hash Function Algorithms Library              *
     *                                                                        *
     * Author: Arash Partow - 2002                                            *
     * URL: http:
     * URL: http:
     *                                                                        *
     * Copyright notice:                                                      *
     * Free use of the General Purpose Hash Function Algorithms Library is    *
     * permitted under the guidelines and in accordance with the most current *
     * version of the Common Public License.                                  *
     * http:
     *                                                                        *
     **************************************************************************
     */

    public static <E> void assertNotEmpty(Collection<E> test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.isEmpty()) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }
    /* End Of  P. J. Weinberger Hash Function */

    public static int fastCompare(float f1, float f2) {

        if (f1 < f2)
            return -1;
        if (f1 > f2)
            return 1;

        return 0;
    }

    public static <K, V> void assertNotEmpty(Map<K, V> test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.isEmpty()) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }

    public static boolean equalsNullAware(Object obj1, Object obj2) {
        if (obj1 == null) {
            return obj2 == null;

        }
        if (obj2 == null) {
            return false;
        }

        return obj1.equals(obj2);
    }

    public static String globToRegEx(String line) {

        line = line.trim();
        int strLen = line.length();
        StringBuilder sb = new StringBuilder(strLen);

        if (line.length() > 0 && line.charAt(0) == '*') {
            line = line.substring(1);
            strLen--;
        }
        if (line.length() > 0 && line.charAt(line.length() - 1) == '*') {
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
        long BitsInUnsignedInt = (4 * 8);
        long ThreeQuarters = (BitsInUnsignedInt * 3) / 4;
        long OneEighth = BitsInUnsignedInt / 8;
        long HighBits = (0xFFFFFFFFL) << (BitsInUnsignedInt - OneEighth);
        long hash = 0;
        long test = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = (hash << OneEighth) + str.charAt(i);

            if ((test = hash & HighBits) != 0) {
                hash = ((hash ^ (test >> ThreeQuarters)) & (~HighBits));
            }
        }

        return hash;
    }

    public static long hashELF(String str) {
        long hash = 0;
        long x = 0;

        int l = str.length();
        for (int i = 0; i < l; i++) {
            hash = (hash << 4) + str.charAt(i);

            if ((x = hash & 0xF0000000L) != 0) {
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
     * also appears in https://www.boost.org/doc/libs/1_35_0/doc/html/boost/hash_combine_id241013.html
     */
    public static int hashCombine(int a, int b) {
        return a ^ (b + 0x9e3779b9 + (a << 6) + (a >> 2));
    }

    public static int hashCombine(int a, long b) {
        return Util.hashCombine(a, (int) b, (int) (b >> 32));
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
     * custom designed to preserve some alpha numeric natural ordering
     */
    public static int hashByteString(byte[] str) {
        switch (str.length) {
            case 0:
                return 0;
            case 1:
                return str[0];
            case 2:
                return str[0] << 8 | str[1];
            case 3:
                return str[0] << 16 | str[1] << 8 | str[2];
            case 4:
                return str[0] << 24 | str[1] << 16 | str[2] << 8 | str[3];
            default:
                return Long.hashCode(hashELF(str, 0));
        }

    }

    public static long hashELF(byte[] str, long seed) {

        long hash = seed;


        for (byte aStr : str) {
            hash = (hash << 4) + aStr;

            long x;
            if ((x = hash & 0xF0000000L) != 0) {
                hash ^= (x >> 24);
            }
            hash &= ~x;
        }

        return hash;
    }

    public static long hashELF(byte[] str, long seed, int start, int end) {

        long hash = seed;

        for (int i = start; i < end; i++) {
            hash = (hash << 4) + str[i];

            long x;
            if ((x = hash & 0xF0000000L) != 0) {
                hash ^= (x >> 24);
            }
            hash &= ~x;
        }

        return hash;
    }

    /**
     * http:
     */
    public static int hashROT(Object... x) {
        long h = 2166136261L;
        for (Object o : x)
            h = (h << 4) ^ (h >> 28) ^ o.hashCode();
        return (int) h;
    }

    /**
     * returns the next index
     */
    public static int longToBytes(long l, byte[] target, int offset) {
        for (int i = offset + 7; i >= offset; i--) {
            target[i] = (byte) (l & 0xFF);
            l >>= 8;
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

//    public static byte[] bytePlusIntToBytes(byte prefix, int l) {
//        byte[] target = new byte[/*5*/]{prefix, 0, 0, 0, 0};
//        for (int i = 4; i >= 1; i--) {
//            target[i] = (byte) (l & 0xFF);
//            l >>= 8;
//        }
//        return target;
//    }

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
        return (int) (x + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
    }

    /**
     * linear interpolate between target & current, factor is between 0 and 1.0
     * targetFactor=1:   full target
     * targetfactor=0.5: average
     * targetFactor=0:   full current
     */
    public static float lerp(float x, float min, float max) {
        return min + (max - min) * unitize(x);
    }

    /**
     * no checking of x
     */
//    public static float lerpSafe(float x, float min, float max) {
//        return min + (max - min) * unitizeSafe(x);
//    }

    public static double lerp(double x, double min, double max) {
        return min + (max - min) * unitize(x);
    }

    public static long lerp(float x, long min, long max) {
        return Math.round(min + (max - min) * unitize((double) x));
    }

    public static int lerp(float x, int min, int max) {
        return Math.round(min + (max - min) * unitize(x));
    }


    public static float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    public static float mean(float a, float b) {
        return (a + b) / 2;
    }

    public static long mean(long a, long b) {
        return (a + b) / 2;
    }

    public static float mean(float a, float b, float c) {
        return (a + b + c) / 3;
    }


    public static double mean(double a, double b) {
        return (a + b) / 2;
    }

    public static double mean(double... d) {
        double result = 0.0;

        for (double val : d)
            result += val;

        return result / d.length;
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
        return Util.clampSafe(x, 0.0, 1.0);
    }

    /**
     * clamps a value to 0..1 range
     */
    public static float unitize(float x) {
        assertFinite(x);
        return unitizeSafe(x);
    }
    public static float unitizeSafe(float x) {
        return Util.clampSafe(x, 0, 1f);
    }

    @Contract("_ -> param1")
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
     * clamps a value to -1..1 range
     */
    public static float clampBi(float p) {
        if (p > 1f)
            return 1f;
        if (p < -1f)
            return -1f;
        return p;
    }

    /**
     * discretizes values to nearest finite resolution real number determined by epsilon spacing
     */
    public static float round(float value, float epsilon) {
        assertFinite(epsilon);
        assertFinite(value);

        if (epsilon <= Float.MIN_NORMAL) return value;
        else return Math.round(value / epsilon) * epsilon;

        //return (float)round((double)value, (double)epsilon);
    }

    public static double round(double value, double epsilon) {
        assertFinite(epsilon);
        assertFinite(value);
        if (epsilon <= Double.MIN_NORMAL) return value;
        return Math.round(value / epsilon) * epsilon;
    }

    /**
     * rounds x to the nearest multiple of the dither parameter
     */
    public static int round(int x, int dither) {

        //return dither * Math.round(((float) x) / dither);
        return (int) round((long) x, dither);
    }

    public static long round(long x, int dither) {
        return dither * Math.round(((double) x) / dither);
    }


    public static int floatToInt(float f, int discretness) {

        return Math.round(f * discretness);
    }

    public static float intToFloat(int i, int discretness) {
        return ((float) i) / discretness;
    }


    public static boolean equals(float a, float b) {
        return equals(a, b, Float.MIN_NORMAL * 2);
    }

    /**
     * tests equivalence (according to epsilon precision)
     */
    public static boolean equals(float a, float b, float epsilon) {
        if (a == b)
            return true;
        if (Float.isFinite(a) && Float.isFinite(b))
            return Math.abs(a - b) < epsilon;
        else
            return (a != a) && (b != b); //both NaN
    }

    public static boolean equals(double a, double b) {
        return equals(a, b, Double.MIN_NORMAL*2);
    }

    /**
     * tests equivalence (according to epsilon precision)
     */
    public static boolean equals(double a, double b, double epsilon) {
        if (a == b)
            return true;
        if (Double.isFinite(a) && Double.isFinite(b))
            return Math.abs(a - b) < epsilon;
        else
            return (a != a) && (b != b); //both NaN
    }


    public static boolean equals(float[] a, float[] b, float epsilon) {
        if (a == b) return true;
        int l = a.length;
        for (int i = 0; i < l; i++) {
            if (!equals(a[i], b[i], epsilon))
                return false;
        }
        return true;
    }

    /**
     * applies a quick, non-lexicographic ordering compare
     * by first testing their lengths
     */
    public static int compare(long[] x, long[] y) {
        if (x == y) return 0;

        int xlen = x.length;

        int yLen = y.length;
        if (xlen != yLen) {
            return Integer.compare(xlen, yLen);
        } else {

            for (int i = 0; i < xlen; i++) {
                int c = Long.compare(x[i], y[i]);
                if (c != 0)
                    return c;
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

    public static int bin(float x, int bins) {
//        assertFinite(x);
        return Util.clamp((int)(x * bins), 0, bins-1);
        //return (int) Math.floor(x * bins);
        //return (int) (x  * bins);

        //return Math.round(x * (bins - 1));
        //return Util.clamp((int)((x * bins) + 0.5f/bins), 0, bins-1);


        //return (int) ((x + 0.5f/bins) * (bins-1));
        //        return (int) Math.floor((x + (0.5 / bins)) * bins);
        //        return Util.clamp(b, 0, bins-1);
    }

//    /**
//     * bins a priority value to an integer
//     */
//    public static int decimalize(float v) {
//        return bin(v, 10);
//    }

    /**
     * finds the mean value of a given bin
     */
    public static float unbinCenter(int b, int bins) {
        return ((float) b) / bins;
    }

//    public static <D> D runProbability(Random rng, float[] probs, D[] choices) {
//        float tProb = 0;
//        for (int i = 0; i < probs.length; i++) {
//            tProb += probs[i];
//        }
//        float s = rng.nextFloat() * tProb;
//        int c = 0;
//        for (int i = 0; i < probs.length; i++) {
//            s -= probs[i];
//            if (s <= 0) {
//                c = i;
//                break;
//            }
//        }
//        return choices[c];
//    }

    public static MethodHandle mhRef(Class<?> type, String name) {
        try {
            return MethodHandles
                    .lookup()

                    .unreflect(stream(type.getMethods()).filter(m -> m.getName().equals(name)).findFirst().get());
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
            return (byte) ('0' + index);
        else if (index < (10 + 26))
            return (byte) ((index - 10) + 'a');
        else
            throw new RuntimeException("out of bounds");
    }

    /**
     * clamps output to 0..+1.  y=0.5 at x=0
     */
    public static float sigmoid(float v) {
        return (float) (1 / (1 + Math.exp(-v)));
    }

    public static double sigmoid(double v) {
        return (1 / (1 + Math.exp(-v)));
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
                return ArrayUtils.EMPTY_BYTE_ARRAY;
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

    public static float[] normalize(float[] x, float min, float max) {
        int n = x.length;
        for (int i = 0; i < n; i++) {
            x[i] = normalize(x[i], min, max);
        }
        return x;
    }

    public static double normalize(double x, double min, double max) {
        assert (max >= min);
        if (max - min <= Double.MIN_NORMAL)
            return 0.5f;
        return (x - min) / (max - min);
    }

    public static float normalize(float x, float min, float max) {
        assert (max >= min);
        if (max - min <= Float.MIN_NORMAL)
            return 0.5f;
        return (x - min) / (max - min);
    }

    public static float variance(float[] population) {
        float average = 0.0f;
        for (float p : population) {
            average += p;
        }
        int n = population.length;
        average /= n;

        float variance = 0.0f;
        for (float p : population) {
            float d = p - average;
            variance += d * d;
        }
        return variance / n;
    }

    public static double[] avgvar(double[] population) {
        double average = 0.0;
        for (double p : population) {
            average += p;
        }
        int n = population.length;
        average /= n;

        double variance = 0.0;
        for (double p : population) {
            double d = p - average;
            variance += d * d;
        }
        variance /= n;

        return new double[]{average, variance};
    }

    public static double[] variance(DoubleStream s) {
        DoubleArrayList dd = new DoubleArrayList();
        s.forEach(dd::add);
        if (dd.isEmpty())
            return null;

        double avg = dd.average();

        double variance = 0.0;
        int n = dd.size();
        for (int i = 0; i < n; i++) {
            double p = dd.get(i);
            double d = p - avg;
            variance += d * d;
        }
        variance /= n;

        return new double[]{avg, variance};
    }

    public static String className(Object p) {
        String s = p.getClass().getSimpleName();
        if (s.isEmpty())
            return p.getClass().toString().replace("class ", "");
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
        double[] f = new double[l];
        for (int i = 0; i < l; i++)
            f[i] = d[i];
        return f;
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
        float sum = 0;
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
            logger.info("{} {}", procName, Texts.timeStr(dtNS));
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
    public static <X, Y> Y[] map(Function<X, Y> f, Y[] target, X... src) {
        return map(f, target, src.length, src);
    }

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
    public static <X, Y> Y[] map(Function<X, Y> f, IntFunction<Y[]> targetBuilder, X... src) {
        int i = 0;
        Y[] target = targetBuilder.apply(src.length);
        for (X x : src) {
            Y y = f.apply(x);
            target[i++] = y;
        }
        return target;
    }

    public static <X> float sum(FloatFunction<X> value, X... xx) {
        float y = 0;
        for (X x : xx)
            y += value.floatValueOf(x);
        return y;
    }

    public static <X> double sum(ToDoubleFunction<X> value, X... xx) {
        double y = 0;
        for (X x : xx)
            y += value.applyAsDouble(x);
        return y;
    }

    public static <X> int sum(ToIntFunction<X> value, X... xx) {
        return sum(value, xx.length, xx);
    }

    public static <X> int sum(ToIntFunction<X> value, int n, X... xx) {
        int y = 0;
        for (int i = 0, xxLength = n; i < xxLength; i++) {
            y += value.applyAsInt(xx[i]);
        }
        return y;
    }

    public static <X> long sum(ToLongFunction<X> value, X... xx) {
        long y = 0;
        for (X x : xx)
            y += value.applyAsLong(x);
        return y;
    }

    public static <X> long min(ToLongFunction<X> value, X... xx) {
        long y = Long.MAX_VALUE;
        for (X x : xx)
            y = Math.min(y, value.applyAsLong(x));
        return y;
    }

    public static <X> long min(ToLongFunction<X> value, Iterable<X> xx) {
        long y = Long.MAX_VALUE;
        for (X x : xx)
            y = Math.min(y, value.applyAsLong(x));
        return y;
    }

    public static <X> boolean sumBetween(ToIntFunction<X> value, int min, int max, X... xx) {
        int y = 0;
        for (X x : xx) {
            if ((y += value.applyAsInt(x)) > max)
                return false;
        }
        return (y >= min);
    }

    public static <X> boolean sumExceeds(ToIntFunction<X> value, int max, X... xx) {
        int y = 0;
        for (X x : xx) {
            if ((y += value.applyAsInt(x)) > max)
                return true;
        }
        return false;
    }

    public static <X> int or(ToIntFunction<X> value, X... xx) {
        int y = 0;
        for (X x : xx)
            y |= value.applyAsInt(x);
        return y;
    }

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

    public static <X> float max(FloatFunction<X> value, X... xx) {
        float y = Float.NEGATIVE_INFINITY;
        for (X x : xx)
            y = Math.max(y, value.floatValueOf(x));
        return y;
    }

    public static int sum(int... x) {
        int y = 0;
        for (int f : x)
            y += f;
        return y;
    }

    public static double max(double... x) {
        double y = Double.NEGATIVE_INFINITY;
        for (double f : x) {
            if (f > y)
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
        double y = Double.POSITIVE_INFINITY;
        for (double f : x) {
            if (f < y)
                y = f;
        }
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

    public static float sum(float... x) {
        float y = 0;
        for (float f : x)
            y += f;
        return y;
    }

    public static float sumAbs(float... x) {
        float y = 0;
        for (float f : x) {
            y += Math.abs(f);
        }
        return y;
    }

    public static int argmax(final double... vec) {
        int result = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0, l = vec.length; i < l; i++) {
            final double v = vec[i];
            if (v > max) {
                max = v;
                result = i;
            }
        }
        return result;
    }

    public static int argmax(final float... vec) {
        int result = -1;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0, l = vec.length; i < l; i++) {
            final float v = vec[i];
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
            final float v = vec[ii];
            if (v > max) {
                max = v;
                result = ii;
            }
        }
        return result;
    }

    public static Pair tuple(Object a, Object b) {
        return Tuples.pair(a, b);
    }

    public static Pair tuple(Object a, Object b, Object c) {
        return tuple(tuple(a, b), c);
    }

    public static Pair tuple(Object a, Object b, Object c, Object d) {
        return tuple(tuple(a, b, c), d);
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
        float weightSum = 0;
        for (int i = 0; i < count; i++) {
            float w = values.valueOf(i);
            assert (w == w && w >= 0);
            weightSum += w;
        }
        return weightSum;
    }

    public static float sumIfPositive(int count, IntToFloatFunction values) {
        float weightSum = 0;
        for (int i = 0; i < count; i++) {
            float w = values.valueOf(i);
            //assert (w == w);
            if (w == w && w > 0)
                weightSum += w;
        }
        return weightSum;
    }

    public static boolean equals(double[] a, double[] b, double epsilon) {
        if (a == b) return true;
        int l = a.length;
        for (int i = 0; i < l; i++) {
            if (!equals(a[i], b[i], epsilon))
                return false;
        }
        return true;
    }

    public static boolean equals(long[] a, long[] b, int firstN) {
        if (a == b) return true;
        for (int i = 0; i < firstN; i++) {
            if (a[i] != b[i])
                return false;
        }
        return true;
    }

    public static boolean equals(long[] a, long[] b) {
        if (a == b) return true;
        int l = a.length;
        if (b.length != l)
            return false;
        for (int i = 0; i < l; i++) {
            if (a[i] != b[i])
                return false;
        }
        return true;
    }

    public static boolean equals(short[] a, short[] b) {
        if (a == b) return true;
        int l = a.length;
        if (b.length != l)
            return false;
        for (int i = 0; i < l; i++) {
            if (a[i] != b[i])
                return false;
        }
        return true;
    }

    public static int short2Int(short high, short low) {
        return high << 16 | low;
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
        //return Math.max(Math.min(f, max), min);
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

    public static int clampI(float i, int min, int max) {
        return clamp(Math.round(i), min, max);
    }

    public static long clampI(double i, int min, int max) {
        return clamp(Math.round(i), min, max);
    }

    public static int clamp(int i, int min, int max) {
        assert (min <= max);
        if (i < min) i = min;
        if (i > max) i = max;
        return i;
    }

    public static long clamp(long i, long min, long max) {
        assert (min <= max);
        if (i < min) i = min;
        if (i > max) i = max;
        return i;
    }

    /**
     * range [a, b)
     */
    public static int[] intArray(int a, int b) {
        int ba = b - a;
        int[] x = new int[ba];
        for (int i = 0; i < ba; i++) {
            x[i] = a + i;
        }
        return x;
    }

    public static double sqr(long x) {
        return x * x;
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
        long b = u.getMostSignificantBits();
        StringBuilder sb = new StringBuilder(6);
        BinTxt.append(sb, a);
        BinTxt.append(sb, b);
        return sb.toString();
    }

//    final static Random insecureRandom = new XoRoShiRo128PlusRandom(System.nanoTime());

    public static String uuid64() {
        //UUID u = UUID.randomUUID();
        //long a = u.getLeastSignificantBits();
        //long b = u.getMostSignificantBits();
        //long c = a ^ b;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long c = rng.nextLong();
        return BinTxt.toString(c);
    }

    /**
     * adaptive spinlock behavior
     * see: https:
     */
    public static void pauseNextIterative(int previousContiguousPauses) {
        if (previousContiguousPauses < 512) {
            onSpinWait();
        } else if (previousContiguousPauses < 1024) {


            if ((previousContiguousPauses & 31) == 0) {

                //System.out.println("yield after: " + previousContiguousPauses);
                Thread.yield();

            } else {
                onSpinWait();
            }
        } else if (previousContiguousPauses < 2048) {

            if ((previousContiguousPauses & 15) == 0) {
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
                // "randomly" yield 1:8
                if((n & 0x7) == 0) {
                    LockSupport.parkNanos(PARK_TIMEOUT);
                } else {
                    onSpinWait();
                }
            } else if(n<MAX_PROG_YIELD) {
                // "randomly" yield 1:4
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

//    /**
//     * adaptive spinlock behavior
//     * see: https:
//     */
//    public static void pauseNextCountDown(long timeRemainNS) {
//        if (timeRemainNS < 10 * (1000 /* uS */))
//            onSpinWait();
//        else
//            Thread.yield();
//    }

    public static void sleepMS(long periodMS) {
        sleepNS(periodMS * 1_000_000);
    }


    /**
     * modified from guava:
     */
    public static void sleep(long sleepFor, TimeUnit unit) {
        sleepNS(unit.toNanos(sleepFor));
    }

    public static void sleepNS(long remainingNanos) {
        sleepNS(remainingNanos, 100 * 1000 /* uS */);
    }

    public static void sleepNS(long remainingNanos, long thresholdNS) {
        //try {

        long end = System.nanoTime() + remainingNanos;
        while (remainingNanos > thresholdNS) {
//                if (remainingNanos < 10 * 1000) {
//                    Thread.onSpinWait();
//                } else if (remainingNanos < 2 * 1000 * 1000 /* 2ms */) {
            LockSupport.parkNanos(remainingNanos);
//                } else {
//
//                    try {
//                        // TimeUnit.sleep() treats negative timeouts just like zero.
//                        NANOSECONDS.sleep(remainingNanos);
//                    } catch (InterruptedException e) {
//                        //interrupted = true;
//                        //throw new RuntimeException(e);
//                        break;
//                    }
//                }

            remainingNanos = end - System.nanoTime();
        }
//        } finally {
//            if (interrupted) {
//                Thread.currentThread().interrupt();
//            }
//        }
    }

//    public static void sleepNS(long periodNS) {
//        if (periodNS > 1_000_000_000 / 1000 / 2  /*0.5ms */) {
//            LockSupport.parkNanos(periodNS);
//            return;
//        }
//
//        final long thresholdNS = 1000; /** 1uS = 0.001ms */
//        if (periodNS <= thresholdNS)
//            return;
//
//        long end = System.nanoTime() + periodNS;
//        //long remainNS = end - System.nanoTime();
//        int pauses = 0;
//        long now;
//        while ((now = System.nanoTime()) < end) {
//            Util.pauseNextCountDown(end - now);
//            //while (remainNS > thresholdNS) {
//
////            if (remainNS <= 500000 /** 100uS = 0.5ms */) {
////                Thread.yield();
////            } else {
////                Thread.onSpinWait();
////            }
//            //Util.pauseNextIterative(pauses++);
//
//            //remainNS = end - System.nanoTime();
//        }
//
//
//    }


    public static void sleepNSwhile(long periodNS, long napTimeNS, BooleanSupplier keepSleeping) {
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
            double p_of_2 = (Math.log(n) / log2);
            return Math.abs(p_of_2 - Math.round((int) p_of_2)) == 0;
        }
    }

    /**
     * http:
     * calculate height on a uniform grid, by splitting a quad into two triangles:
     */
    public static final float lerp2d(float x, float z, float nw, float ne, float se, float sw) {

        x = x - (int) x;
        z = z - (int) z;


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
                    return Texts.n2(s) + 's';
                case 3:
                    return Texts.n2(s * 1000) + "ms";
                case 6:
                    return Texts.n2(s * 1.0E6) + "us";
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

    public static <X> X[] sortUniquely(@NotNull X[] arg) {
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

    public static <X> boolean and(X[] xx, Predicate<X> p) {
        for (X x : xx)
            if (!p.test(x))
                return false;
        return true;
    }

    public static boolean and(float[] xx, FloatPredicate p) {
        for (float x : xx)
            if (!p.accept(x))
                return false;
        return true;
    }

    public static <X> boolean andReverse(X[] xx, Predicate<X> p) {
        for (int i = xx.length - 1; i >= 0; i--) {
            if (!p.test(xx[i]))
                return false;
        }
        return true;
    }

    public static <X> boolean and(Predicate<X> p, X... xx) {
        return and(xx, p);
    }

    public static <X> boolean or(Predicate<X> p, X... xx) {
        for (X x : xx)
            if (p.test(x))
                return true;
        return false;
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
        return msgPacker.writeValueAsBytes(x);
    }

    public static byte[] toBytes(Object x, Class cl) throws JsonProcessingException {
        return msgPacker.writerFor(cl).writeValueAsBytes(x);
    }


    /**
     * msgpack deserialization
     */
    public static <X> X fromBytes(byte[] msgPacked, Class<? extends X> type) throws IOException {
        return msgPacker/*.reader(type)*/.readValue(msgPacked, type);
    }

    public static <X> X fromBytes(byte[] msgPacked, int len, Class<? extends X> type) throws IOException {
        return msgPacker/*.reader(type)*/.readValue(msgPacked, 0, len, type);
    }


    public static JsonNode jsonNode(Object x) {
        if (x instanceof String) {
            try {
                return msgPacker.readTree(x.toString());
            } catch (IOException e) {
                e.printStackTrace();

            }
        }

        return msgPacker.valueToTree(x);
    }

    /**
     * x in -1..+1, y in -1..+1.   typical value for sharpen will be ~ >5
     * http:
     */
    public static float sigmoidBipolar(float x, float sharpen) {
        return (float) ((1.0 / (1 + Math.exp(-sharpen * x)) - 0.5) * 2);
    }


    /**
     * doesnt do null tests
     */
    public static boolean equalArraysDirect(Object[] a, Object[] b) {
        int len = a.length;
        if (b.length != len)
            return false;

        for (int i = 0; i < len; i++) {
            if (!a[i].equals(b[i]))
                return false;
        }

        return true;
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

    public static void mul(float scale, float[] ff) {
        for (int i = 0; i < ff.length; i++)
            ff[i] *= scale;
    }

    public static <X> X[] map(int n, IntFunction<X[]> arrayizer, IntFunction<X> build) {
        return map(0, n, arrayizer, build);
    }

    public static <X> X[] map(int from, int to, IntFunction<X[]> arrayizer, IntFunction<X> build) {
        assert (to >= from);
        X[] x = arrayizer.apply(to - from);
        for (int i = from, j = 0; i < to; ) {
            x[j++] = build.apply(i++);
        }
        return x;
    }

    public static float[] map(int num, IntToFloatFunction build) {
        return map(num, build, null);
    }

    /**
     * builds a MarginMax weight array, which can be applied in a Roulette decision
     * a lower margin > 0 controls the amount of exploration while values
     * closer to zero prefer exploitation of provided probabilities
     */
    @Paper
    public static float[] marginMax(int num, IntToFloatFunction build, float lower, float upper) {
        float[] minmax = {Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};

        float[] w = Util.map(num, i -> {
            float v = build.valueOf(i);
            if (v < minmax[0]) minmax[0] = v;
            if (v > minmax[1]) minmax[1] = v;
            return v;
        });

        if (Util.equals(minmax[0], minmax[1], Float.MIN_NORMAL * 2)) {
            Arrays.fill(w, 0.5f);
        } else {


            Util.normalize(w, minmax[0], minmax[1]);
            Util.normalize(w, 0 - lower, 1 + upper);
        }
        return w;
    }


    public static float softmax(float x, float temp) {
        float f = (float) Math.exp(x / temp);
        if (!Float.isFinite(f))
            throw new RuntimeException("softmax(" + f + "," + temp + ") is non-finite");
        return f;
    }


    public static float[] map(IntToFloatFunction build, @Nullable float[] reuse) {
        return map(reuse.length, build, reuse);
    }

    public static float[] map(int num, IntToFloatFunction build, @Nullable float[] reuse) {
        float[] f = (reuse != null && reuse.length == num) ? reuse : new float[num];
        for (int i = 0; i < num; i++) {
            f[i] = build.valueOf(i);
        }
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
        float ratio = 1f - ((float) availableMemory) / max;

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
        f.forEachWithIndex((e, n) -> {
            x.accept(header + " " + e, n);
        });
    }

    public static void toMap(Iterator<? extends Map.Entry<?, ?>> f, String header, BiConsumer<String, Object> x) {
        f.forEachRemaining((e) -> {
            x.accept(header + " " + e.getKey(), e.getValue());
        });
    }


    /**
     * pretty close
     * http:
     * https:
     * http:
     */
    public static float tanhFast(float x) {
        if (x <= -3) return -1f;
        if (x >= 3f) return +1f;
        return x * (27 + x * x) / (27 + 9 * x * x);
    }

    public static Object toString(Object x) {
        return x.getClass() + "@" + System.identityHashCode(x);
    }

    public static BlockingQueue blockingQueue(int capacity) {
        try {
            return new DisruptorBlockingQueue<>(capacity);

        } catch (Throwable e) {

            return new ArrayBlockingQueue<>(capacity);
        }
    }

    public static int compare(byte[] a, byte[] b) {
        if (a == b) return 0;
        int al = a.length;
        int l = Integer.compare(al, b.length);
        if (l != 0)
            return l;
        for (int i = 0; i < al; i++) {
            int d = a[i] - b[i];
            if (d != 0)
                return d;
        }
        return 0;
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
                return ByteLists.immutable.of(x.get(a++), x.get(a++), x.get(a++));
            case 4:
                return ByteLists.immutable.of(x.get(a++), x.get(a++), x.get(a++), x.get(a++));
            case 5:
                return ByteLists.immutable.of(x.get(a++), x.get(a++), x.get(a++), x.get(a++), x.get(a++));
            case 6:
                return ByteLists.immutable.of(x.get(a++), x.get(a++), x.get(a++), x.get(a++), x.get(a++), x.get(a++));
            default:
                byte[] xx = x.toArray();
                return ByteLists.immutable.of(ArrayUtils.subarray(xx, a, b));
        }
    }

    public static byte branchOr(byte key, ByteByteHashMap count, byte branch) {
        byte branchBit = (byte) (1 << branch);
        return count.updateValue(key, branchBit, (x) -> (byte) (x | branchBit));
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
        return (float) (a * Math.sqrt(1f - x0 * x0 * 4) + b);
    }

//    /* domain: [0..1], range: [0..1] */
//    public static float smoothDischarge(float x) {
//        x = unitize(x);
//        return 2 * (x - 1) / (x - 2);
//    }

    /**
     * Get the location from which the supplied object's class was loaded.
     *
     * @param object the object for whose class the location should be retrieved
     * @return an {@code Optional} containing the URL of the class' location; never
     * {@code null} but potentially empty
     */
    @Nullable
    public static URL locate(ClassLoader loader, String className) {


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

    /**
     * returns lev distance divided by max(a.length(), b.length()
     */
    public static float levenshteinFraction(CharSequence a, CharSequence b) {
        int len = Math.max(a.length(), b.length());
        if (len == 0) return 0f;
        return Texts.levenshteinDistance(a, b) / ((float) len);
    }

    public static short[] toShort(int[] x) {
        if (x.length == 0)
            return ArrayUtils.EMPTY_SHORT_ARRAY;

        short[] s = new short[x.length];
        int i = 0;
        for (int xx : x) {
            assert (xx <= Short.MAX_VALUE && xx >= Short.MIN_VALUE);
            s[i++] = (short) xx;
        }
        return s;
    }

    public static FloatSupplier compose(FloatSupplier f, FloatToFloatFunction g) {
        return () -> {
            float fx = f.asFloat();
            return g.valueOf(fx);
        };
    }

    public static FloatToFloatFunction compose(FloatToFloatFunction f, FloatToFloatFunction g) {
        return (x) -> {
            float fx = f.valueOf(x);
            return g.valueOf(fx);
        };
    }

    public static int concurrency() {
        return concurrencyExcept(0);
    }

    public static int concurrencyExcept(int reserve) {
        int minThreads = 2;
        int maxThreads = Integer.MAX_VALUE;

        String specifiedThreads = System.getenv("threads");
        int threads;
        if (specifiedThreads != null)
            threads = Texts.i(specifiedThreads);
        else
            threads = Runtime.getRuntime().availableProcessors() - reserve;

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

    public static <X> X[] replaceDirect(X[] xx, Function<X, X> f) {
        return replaceDirect(xx, 0, xx.length, f);
    }

    public static <X> X[] replaceDirect(X[] xx, int start, int end, Function<X, X> f) {
        for (int i = start; i < end; i++) {
            X x = xx[i];
            xx[i] = f.apply(x);
        }
        return xx;
    }

    public static <X> FloatFunction<X> softmaxFunc(FloatFunction<X> f, float temperature) {
        return (x) -> softmax(f.floatValueOf(x), temperature);
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
    public static float aveAri(@NotNull float... arr) {
        float sum = 0;
        for (float f : arr) {
            sum += f;
        }
        return sum / arr.length;
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
    public static float aveGeo(@NotNull float... arr) {
        float product = 1;
        for (float f : arr) {
            if (f == 0) return 0;
            product *= f;
        }
        return (float) pow(product, 1.00 / arr.length);
    }

    public static float aveGeo(float a, float b) {
        return (float) Math.sqrt(a * b);
    }

    public static void assertUnitized(float... f) {
        for (float x : f) {
            assertUnitized(x);
        }
    }

    public static float assertUnitized(float x) {
        if (!Float.isFinite(x) || x < 0 || x > 1)
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
            if (x[i - 1].compareTo(x[i]) > 0)
                return false;
        }
        return true;
    }

    public static int[] bytesToInts(byte[] array) {
        int n = array.length;
        if (n == 0)
            return ArrayUtils.EMPTY_INT_ARRAY;
        int[] t = new int[n];
        for (int i = 0; i < n; i++) {
            t[i] = array[i];
        }
        return t;
    }

    public static Class[] typesOfArray(Object[] orgs) {
        return typesOfArray(orgs, 0, orgs.length);
    }

    public static Class[] typesOfArray(Object[] orgs, int from, int to) {
        if (orgs.length == 0)
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        else {
            return map(x -> Primitives.unwrap(x.getClass()),
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
            //TODO return constant function
            throw new RuntimeException("must provide at least 2 points");
        }

        //https://commons.apache.org/proper/commons-math/userguide/fitting.html
        final List<WeightedObservedPoint> obs = new FasterList(points);
        int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
        for (int i = 0; i < pairs.length; ) {
            int y;
            obs.add(new WeightedObservedPoint(1f, pairs[i++], y = pairs[i++]));
            if (y < yMin) yMin = y;
            if (y > yMax) yMax = y;
        }
        //TODO if yMin==yMax return constant function

        int degree =
                points - 1;
        //points;

        float coefficients[] = Util.toFloat(PolynomialCurveFitter.create(degree).fit(obs));

        /* adapted from: PolynomialFunction
           https://en.wikipedia.org/wiki/Horner%27s_method
           */
        int YMin = yMin;
        int YMax = yMax;
        return (X) -> {
            int n = coefficients.length;
            float x = toInt.applyAsInt(X);
            float y = coefficients[n - 1];
            for (int j = n - 2; j >= 0; j--) {
                y = x * y + coefficients[j];
            }
            return Util.clamp(Math.round(y), YMin, YMax);
        };
    }

    public static int sqrt(int x) {
        if (x < 0)
            throw new NumberException("sqrt of negative value", x);
        return (int) Math.round(Math.sqrt(x));
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
    @Nullable
    public static <X> X only(@NotNull Collection<X> next) {
        if (next instanceof List)
            return ((List<X>) next).get(0);
        else if (next instanceof MutableSet)
            return ((MutableSet<X>) next).getOnly();
        else if (next instanceof SortedSet)
            return ((SortedSet<X>) next).first();
        else
            return next.iterator().next();
        //TODO SortedSet.getFirst() etc
    }

    public static <X> IntSet intSet(ToIntFunction<X> f, X... items) {
        switch (items.length) {
            case 0:
                return IntSets.immutable.empty();
            case 1:
                return IntSets.immutable.of(f.applyAsInt(items[0]));
            case 2:
                return IntSets.immutable.of(f.applyAsInt(items[0]), f.applyAsInt(items[1]));
            //...
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
        return p != null ? Integer.parseInt(p) : defaultValue;
    }

    public static float interpSum(float[] data, float sStart, float sEnd) {
        return interpSum(data, sStart, sEnd, false);
    }

    public static float interpSum(float[] data, float sStart, float sEnd, boolean wrap) {
        int iStart = (int) Math.ceil(sStart);
        int iEnd = (int) Math.floor(sEnd);

        float sum = 0;
        int i = iStart - 1;

        if (i < 0) {
            if (wrap)
                while (i < 0) i += data.length;
            else
                i = 0;
        }

        sum += iStart > 0 ? (iStart - sStart) * data[i++] : 0;

        for (int k = iStart; k < iEnd; k++) {
            if (i == data.length) i = 0;
            sum += data[i++];
        }

        if (i == data.length) i = 0;
        sum += (sEnd - iEnd) * data[i];
        return sum;
    }
}