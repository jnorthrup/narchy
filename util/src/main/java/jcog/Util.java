/* Copyright 2009 - 2010 The Stajistics Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcog;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import jcog.io.BinTxt;
import jcog.math.NumberException;
import jcog.math.OneDHaar;
import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.DoubleHistogram;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.Frequency;
import org.apache.lucene.util.BytesRef;
import org.eclipse.collections.api.block.function.primitive.DoubleToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static jcog.Texts.iPad;
import static jcog.Texts.n4;

/**
 *
 *
 *
 */
public enum Util {
    ;

    public static final int PRIME3 = 524287;
    public static final int PRIME2 = 92821;
    public static final int PRIME1 = 31;
    public static final float[] EmptyFloatArray = new float[0]; //TODO find what class this is elsewhere something like ArrayUtils

    public static final int MAX_CONCURRENCY = Runtime.getRuntime().availableProcessors();


    /**
     * It is basically the same as a lookup table with 2048 entries and linear interpolation between the entries, but all this with IEEE floating point tricks.
     * http://stackoverflow.com/questions/412019/math-optimization-in-c-sharp#412988
     */
    public static double expFast(double val) {
        long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }


//    /**
//     * Fetch the Unsafe.  Use With Caution.
//     */
//    public static Unsafe getUnsafe() {
//        // Not on bootclasspath
//        if (Util.class.getClassLoader() == null)
//            return Unsafe.getUnsafe();
//        try {
//            Field fld = Unsafe.class.getDeclaredField("theUnsafe");
//            fld.setAccessible(true);
//            return (Unsafe) fld.get(Util.class);
//        } catch (Exception e) {
//            throw new RuntimeException("Could not obtain access to Unsafe", e);
//        }
//    }

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

//    public static int hash(int a, int b) {
//        return PRIME2 * (PRIME2 + a) + b;
//    }
//
//    public static int hash(int a, int b, int c) {
//        return PRIME2 * (PRIME2 * (PRIME2 + a) + b) + c;
//    }

//    public final static int hash(int a, int b, int c, int d) {
//        return PRIME2 * (PRIME2 * (PRIME2 * (PRIME2 + a) + b) + c) + d;
//    }

//    public final static int hash(int a, int b, int c, int d, long e) {
//        long x = PRIME2 * (PRIME2 * (PRIME2 * (PRIME2 * (PRIME2 + a) + b) + c) + d) + e;
//        return (int)x;
//    }


//    public final static int hash(Object a, Object b, Object c, Object d) {
//        return hash(a.hashCode(), b.hashCode(), c.hashCode(), d.hashCode());
//    }

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

    public static <E> void assertNotEmpty(Collection<E> test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.isEmpty()) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }

    public static int fastCompare(float f1, float f2) {

        if (f1 < f2)
            return -1;           // Neither val is NaN, thisVal is smaller
        if (f1 > f2)
            return 1;            // Neither val is NaN, thisVal is larger

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
        // Remove beginning and ending * globs because they're useless
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



    /*
     **************************************************************************
     *                                                                        *
     *          General Purpose Hash Function Algorithms Library              *
     *                                                                        *
     * Author: Arash Partow - 2002                                            *
     * URL: http://www.partow.net                                             *
     * URL: http://www.partow.net/programming/hashfunctions/index.html        *
     *                                                                        *
     * Copyright notice:                                                      *
     * Free use of the General Purpose Hash Function Algorithms Library is    *
     * permitted under the guidelines and in accordance with the most current *
     * version of the Common Public License.                                  *
     * http://www.opensource.org/licenses/cpl1.0.php                          *
     *                                                                        *
     **************************************************************************
     */


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
    /* End Of  P. J. Weinberger Hash Function */


    public static long hashELF(String str) {
        long hash = 0;
        long x = 0;

        for (int i = 0; i < str.length(); i++) {
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
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
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
     */
    public static int hashCombine(int a, int b) {
        return a ^ (b + 0x9e3779b9 + (a << 6) + (a >> 2));
    }

    /**
     * == hashCombine(1, b)
     */
    public static int hashCombine1(int b) {
        return 1 ^ (b + 0x9e3779b9 + (1 << 6) + (1 >> 2));
    }

    public static int hashCombine(int a, int b, int c) {

        return hashCombine(hashCombine(a, b), c); //TODO decide if this is efficient and hashes well

        //https://gist.github.com/badboy/6267743
//        a=a-b;  a=a-c;  a=a^(c >>> 13);
//        b=b-c;  b=b-a;  b=b^(a << 8);
//        c=c-a;  c=c-b;  c=c^(b >>> 13);
//        a=a-b;  a=a-c;  a=a^(c >>> 12);
//        b=b-c;  b=b-a;  b=b^(a << 16);
//        c=c-a;  c=c-b;  c=c^(b >>> 5);
//        a=a-b;  a=a-c;  a=a^(c >>> 3);
//        b=b-c;  b=b-a;  b=b^(a << 10);
//        c=c-a;  c=c-b;  c=c^(b >>> 15);
//        return c;
    }

    public static int hashNonZeroELF(byte[] str, int seed) {
        int i = (int) hashELF(str, seed);
        if (i == 0) i = 1;
        return i;
    }

    public static long hashELF(byte[] str, long seed) {

        long hash = seed;

        //int len = str.length;

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
     * http://www.eternallyconfuzzled.com/tuts/algorithms/jsw_tut_hashing.aspx
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
    public static int long2Bytes(long l, byte[] target, int offset) {
        for (int i = offset + 7; i >= offset; i--) {
            target[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return offset + 8;
    }

    /**
     * returns the next index
     */
    public static int int2Bytes(int l, byte[] target, int offset) {
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
     * http://www.java-gaming.org/index.php?topic=24194.0
     */
    public static int floorInt(float x) {
        return (int) (x + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
    }

    private static final int BIG_ENOUGH_INT = 16 * 1024;
    private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
    private static final double BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5;


    /**
     * linear interpolate between target & current, factor is between 0 and 1.0
     * targetFactor=1:   full target
     * targetfactor=0.5: average
     * targetFactor=0:   full current
     */
    public static float lerp(float x, float min, float max) {
        return min + (max - min) * unitize(x);
    }

    public static double lerp(double x, double min, double max) {
        return min + (max - min) * unitize(x);
    }

    public static long lerp(float x, long min, long max) {
        return min + Math.round((max - min) * unitize((double) x));
    }

    public static int lerp(float x, int min, int max) {
        return min + Math.round((max - min) * unitize(x));
    }


    public static float max(float a, float b, float c) {
        return Util.max(Util.max(a, b), c);
    }

    /**
     * maximum, simpler and faster than Math.max without its additional tests
     */
    public static float max(float a, float b) {
        /*Compares two Float objects numerically. There are two ways in which comparisons performed by this method differ from those performed by the Java language numerical comparison operators (<, <=, ==, >=, >) when applied to primitive float values:
        Float.NaN is considered by this method to be equal to itself and greater than all other float values (including Float.POSITIVE_INFINITY).
        0.0f is considered by this method to be greater than -0.0f.
        This ensures that the natural ordering of Float objects imposed by this method is consistent with equals. */
        //   if (a != a) {
        //            return a;
        //        } else if (a == 0.0F && b == 0.0F && (long)Float.floatToRawIntBits(a) == negativeZeroFloatBits) {
        //            return b;
        //        } else {
        //            return a >= b ? a : b;
        //        }
//        assert (a == a);
//        assert (b == b);
        return (a >= b) ? a : b;
    }


    public static float min(float a, float b) {
//        assert (a == a);
//        assert (b == b);
        return (a <= b) ? a : b;
    }

    public static float mean(float a, float b) {
        return (a + b) * 0.5f;
    }


    public static short f2s(float conf) {
        return (short) (conf * Short.MAX_VALUE);
    }

    public static byte f2b(float conf) {
        return (byte) (conf * Byte.MAX_VALUE);
    }

    /**
     * removal rates are approximately monotonically increasing function;
     * tests first, mid and last for this  ordering
     * first items are highest, so it is actually descending order
     * TODO improve accuracy
     */
    public static boolean isSemiMonotonicallyDec(double[] count) {


        int cl = count.length;
        return
                (count[0] >= count[cl - 1]) &&
                        (count[cl / 2] >= count[cl - 1]);
    }

    /* TODO improve accuracy */
    public static boolean isSemiMonotonicallyInc(int[] count) {

        int cl = count.length;
        return
                (count[0] <= count[cl - 1]) &&
                        (count[cl / 2] <= count[cl - 1]);
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

//    /**
//     * Generic utility method for running a list of tasks in current thread (concurrency == 1) or in multiple threads (> 1, in which case it will block until they finish)
//     */
//    public static void run(Deque<Runnable> tasks, int maxTasksToRun, int threads) {
//
//        //int concurrency = Math.min(threads, maxTasksToRun);
//        //if (concurrency == 1) {
//            tasks.forEach(Runnable::run);
////            return;
//  //      }
////
////        ConcurrentContext ctx = ConcurrentContext.enter();
////        ctx.setConcurrency(concurrency);
////
////        try {
////            run(tasks, maxTasksToRun, ctx::execute);
////        } finally {
////            // Waits for all concurrent executions to complete.
////            // Re-exports any exception raised during concurrent executions.
////            if (ctx != null)
////                ctx.exit();
////        }
//
//    }


    /**
     * clamps a value to 0..1 range
     */
    public static double unitize(double x) {
        if (x <= 1.0) {
            if (x >= 0.0) {
                return x;
            } else {
                notNaN(x);
                return 0.0;
            }
        } else {
            notNaN(x);
            return 1.0;
        }

    }

    /**
     * clamps a value to 0..1 range
     */
    public static float unitize(float x) {
        if (x <= 1f) {
            if (x >= 0f) {
                return x;
            } else {
                notNaN(x);
                return 0f;
            }
        } else {
            notNaN(x);
            return 1f;
        }
    }

    public static float notNaN(float x) throws NumberException {
        if (x != x)
            throw new NumberException("NaN");
        return x;
    }

    public static double notNaN(double x) throws NumberException {
        if (x != x)
            throw new NumberException("NaN");
        return x;
    }

//    public static float notNaNOrNeg(float x) throws NumberException {
//        if (notNaN(x) < 0)
//            throw new NumberException("Negative");
//        return x;
//    }


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
        return Math.round(value / epsilon) * epsilon;
    }

    /**
     * rounds x to the nearest multiple of the dither parameter
     */
    public static int round(int x, int dither) {
        return dither * Math.round(((float) x) / dither);
    }

    public static float floor(float value, float epsilon) {
        return (float) (Math.floor(value / epsilon) * epsilon);
    }

//    public static float clampround(float value, float epsilon) {
//        return unitize(round(value, epsilon));
//    }

    public static int floatToInt(float f, int discretness) {
        return (int) (f * discretness);
    }

    public static float intToFloat(int i, int discretness) {
        return ((float) i) / discretness;
    }

    public static boolean equals(double a, double b) {
        return equals(a, b, Double.MIN_VALUE * 2);
    }

    public static boolean equals(float a, float b) {
        return equals(a, b, Float.MIN_VALUE * 2);
    }

    /**
     * tests equivalence (according to epsilon precision)
     */
    public static boolean equals(float a, float b, float epsilon) {
        return a == b || Math.abs(a - b) < epsilon;
    }

    public static boolean equals(float[] a, float[] b, float epsilon) {
        if (a == b) return true;
        int l = a.length;
        for (int i = 0; i < l; i++) {
            if (!Util.equals(a[i], b[i], epsilon))
                return false;
        }
        return true;
    }

    /**
     * tests equivalence (according to epsilon precision)
     */
    public static boolean equals(double a, double b, double epsilon) {
        return a == b || Math.abs(a - b) < epsilon;
        //return Math.abs(a - b) < epsilon;
    }


    private final static Object waitLock = new Object();

    public static long pauseWaitUntil(long untilTargetTime) {
        long now = System.currentTimeMillis();
        long dt = untilTargetTime - now;
        if (dt > 0) {
            synchronized (waitLock) {
                try {
                    waitLock.wait(dt);
                } catch (InterruptedException e) {
                }
            }

            now = System.currentTimeMillis();
        }
        return now;
    }

//    /** from: http://stackoverflow.com/a/1205300 */
//    public static long pauseLockUntil(long untilTargetTime) {
//
//    // Wait until the desired next time arrives using nanosecond
//    // accuracy timer (wait(time) isn't accurate enough on most platforms)
//        long now = System.currentTimeMillis();
//        long dt = (untilTargetTime-now) * 1000000;
//        if (dt > 0) {
//            LockSupport.parkNanos(dt);
//            now = System.currentTimeMillis();
//        }
//        return now;
//    }

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
                    return c; //first different chra
            }

            return 0; //equal
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


//        int digits = (index >= 256 ? 3 : ((index >= 16) ? 2 : 1));
//        StringBuilder cb  = new StringBuilder(1 + digits).append(type);
//        do {
//            cb.append(  Character.forDigit(index % 16, 16) ); index /= 16;
//        } while (index != 0);
//        return cb.toString();

    }

    public static int bin(float x, int bins) {
        int b = (int) Math.floor((x + (0.5f / bins)) * bins);
        if (b >= bins)
            b = bins - 1; //???
        return b;
    }

    /**
     * bins a priority value to an integer
     */
    public static int decimalize(float v) {
        return bin(v, 10);
    }

    /**
     * finds the mean value of a given bin
     */
    public static float unbinCenter(int b, int bins) {
        return ((float) b) / bins;
    }

    public static <D> D runProbability(Random rng, float[] probs, D[] choices) {
        float tProb = 0;
        for (int i = 0; i < probs.length; i++) {
            tProb += probs[i];
        }
        float s = rng.nextFloat() * tProb;
        int c = 0;
        for (int i = 0; i < probs.length; i++) {
            s -= probs[i];
            if (s <= 0) {
                c = i;
                break;
            }
        }
        return choices[c];
    }


    public static MethodHandle mhRef(Class<?> type, String name) {
        try {
            return MethodHandles
                    .lookup()
                    //.publicLookup(
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

    /**
     * 2 decimal representation of values between 0 and 1. only the tens and hundredth
     * decimal point are displayed - not the ones, and not a decimal point.
     * for compact display.
     * if the value=1.0, then 'aa' is the result
     */
    @NotNull
    public static String n2u(float x) {
        if ((x < 0) || (x > 1)) throw new RuntimeException("values >=0 and <=1");
        int hundreds = (int) Texts.hundredths(x);
        if (x == 100) return "aa";
        return hundreds < 10 ? "0" + hundreds : Integer.toString(hundreds);
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

//    /**
//     * modifies the input array
//     */
//    public static <X> X[] reverse(X[] array) {
//        if (array != null) {
//            reverse(array, 0, array.length);
//        }
//        return array;
//    }

    public static int[] reverse(IntArrayList l) {
        switch (l.size()) {
            case 0:
                throw new UnsupportedOperationException(); //should never happen
            case 1:
                return new int[]{l.get(0)};
            case 2:
                return new int[]{l.get(1), l.get(0)};
            case 3:
                return new int[]{l.get(2), l.get(1), l.get(0)};
            default:
                //reverse the array since it has been constructed in reverse
                //TODO use more efficient array reversal
                return l.asReversed().toArray();//toReversed().toArray();
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


    public static double normalize(double x, double min, double max) {
        if (equals(min, max, Float.MIN_NORMAL))
            return min;
        return (x - min) / (max - min);
    }

    public static float[] normalize(float[] x, float min, float max) {
        int n = x.length;
        for (int i = 0; i < n; i++) {
            x[i] = normalize(x[i], min, max);
        }
        return x;
    }

    public static float normalize(float x, float min, float max) {
        if (max - min <= Float.MIN_NORMAL)
            return min;
        return (x - min) / (max - min);
    }

    public static int lastNonNull(Object... x) {
        int j = -1;
        if (x != null) {
            int k = x.length;
            for (int i = 0; i < k; i++) {
                if (x[i] != null)
                    j = i;
            }
        }
        return j;
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
        //float sum = 0;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float y : x) {
            //sum += y;
            if (y < min) min = y;
            if (y > max) max = y;
        }
        return new float[]{min, max/*, sum */};
    }

    public static void time(Logger logger, String procName, Runnable procedure) {
        long dt = time(procedure);
        logger.info("{} ({} ms)", procName, dt);
    }

    public static long time(Runnable procedure) {
        long start = System.currentTimeMillis();
        procedure.run();
        long end = System.currentTimeMillis();
        return end - start;
    }

//    public static File resourceAsFile(String resourcePath) {
//        try {
//            long modified = ClassLoader.getSystemClassLoader().getResource(resourcePath).openConnection().getLastModified();
//
//            InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
//            if (in == null) {
//                return null;
//            }
//
//            File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".tmp");
//            tempFile.deleteOnExit();
//
//            try (FileOutputStream out = new FileOutputStream(tempFile)) {
//                //copy stream
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                while ((bytesRead = in.read(buffer)) != -1) {
//                    out.write(buffer, 0, bytesRead);
//                }
//            }
//            tempFile.setLastModified(modified);
//
//            return tempFile;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public static String tempDir() {
        return System.getProperty("java.io.tmpdir");
    }


    /**
     * TODO make a version of this which can return the input array if no modifications occurr either by .equals() or identity
     */
    public static <X, Y> Y[] map(Function<X, Y> f, Y[] target, X... src) {
        for (int i = 0; i < target.length; i++) {
            target[i] = f.apply(src[i]);
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

    // Implementing Fisher–Yates shuffle
    public static void shuffle(Object[] ar, Random rnd) {
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
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
        else if (x > --max) x = max;
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
            assert (w == w);
            if (w > 0)
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

    public static int intFromShorts(short high, short low) {
        return high << 16 | low;
    }

    public static short intFromShorts(int x, boolean high) {
        return high ? (short) (x >> 16) : (short) (x & 0xffff);
    }


    public static float clamp(float f, float min, float max) {
        if (f < min) f = min;
        if (f > max) f = max;
        return f;
    }


    public static int clampI(float i, int min, int max) {
        return clamp(Math.round(i), min, max);
    }

    public static int clamp(int i, int min, int max) {
        if (i < min) i = min;
        if (i > max) i = max;
        return i;
    }

    /**
     * range [a, b)
     */
    public static int[] intSequence(int a, int b) {
        int ba = b - a;
        int[] x = new int[ba];
        for (int i = 0; i < ba; i++) {
            x[i] = a + i;
        }
        return x;
    }


    public static double sqr(long l) {
        return l * l;
    }

    public static int sqr(int l) {
        return l * l;
    }

    public static float sqr(float f) {
        return f * f;
    }

    public static double sqr(double f) {
        return f * f;
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

    public static String uuid64() {
        UUID u = UUID.randomUUID();
        long a = u.getLeastSignificantBits();
        long b = u.getMostSignificantBits();
        long c = a ^ b;
        return BinTxt.toString(c);
    }


    /**
     * semi-busy wait loop
     */
    public static void stall(int delayMS) {
        stall(System.currentTimeMillis(), delayMS);
    }

    public static void stall(long start, int delayMS) {
        long end = start + delayMS;
        int pauseCount = 0;
        do {
            pauseNext(pauseCount++);
        } while (System.currentTimeMillis() < end);
    }

    /**
     * adaptive spinlock behavior
     */
    public static void pauseNext(int previousContiguousPauses) {
        if (previousContiguousPauses < 8) {
            Thread.onSpinWait();
        } else if (previousContiguousPauses < 16) {
            Thread.yield();
        } else if (previousContiguousPauses < 32) {
            Util.sleep(0);
        } else if (previousContiguousPauses < 64) {
            Util.sleep(1);
        } else if (previousContiguousPauses < 128) {
            Util.sleep(2);
        } else if (previousContiguousPauses < 256) {
            Util.sleep(4);
        } else if (previousContiguousPauses < 512) {
            Util.sleep(16);
        } else {
            Util.sleep(32);
        }
    }

    /**
     * http://www.qat.com/using-waitnotify-instead-thread-sleep-java/
     */
    public static void pause(long milli) {
        if (milli <= 0) return;

        final Thread t = Thread.currentThread();
        long start = System.currentTimeMillis();
        long now = start;
        while (now - start < milli) {
            long ignore = milli - (now - start);
            if (ignore > 0L) {
                try {
                    synchronized (t) {
                        t.wait(ignore);
                    }
                } catch (InterruptedException var9) {
                }
            }
            now = System.currentTimeMillis();
        }
    }


    public static boolean sleep(long periodMS) {
        if (periodMS <= 0) {
            Thread.yield();
        } else {
            try {
                Thread.sleep(periodMS);
            } catch (InterruptedException e) {
                //TODO
                //e.printStackTrace();
                return false;
            }
        }
        return true;
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
            double p_of_2 = (Math.log(n) / OneDHaar.log2);
            return Math.abs(p_of_2 - Math.round((int) p_of_2)) == 0;
        }
    }

    public static void shallowCopy(Object source, Object dest, final boolean publicOnly) {
        if (!source.getClass().isInstance(dest))
            throw new IllegalArgumentException();

        for (Field f : Util.getAllDeclaredFields(source, publicOnly)) {
            try {

                final int mods = f.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods))
                    continue;

                f.setAccessible(true);

                Object sourceValue = f.get(source);
                f.set(dest, sourceValue);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 'publicOnly' just gets public fields (Class.getFields vs. Class.getDeclaredFields) so we can work with reduced
     * functionality in a sandboxed environment (ie. applets)
     */
    public static Collection<Field> getAllDeclaredFields(Object object, final boolean publicOnly) {
        Set<Field> result = new HashSet<Field>();

        Class<?> clazz = object.getClass();
        while (clazz != null) {
            Field[] fields;
            if (publicOnly)
                fields = clazz.getFields();
            else
                fields = clazz.getDeclaredFields();

            Collections.addAll(result, fields);

            clazz = clazz.getSuperclass();
        }

        return result;
    }

    /**
     * http://www.java-gaming.org/topics/utils-essentials/22144/30/view.html
     * calculate height on a uniform grid, by splitting a quad into two triangles:
     */
    public static final float lerp2d(float x, float z, float nw, float ne, float se, float sw) {
        // n -= n % dim -> n = 0..dim (local offset)
        x = x - (int) x;
        z = z - (int) z;

        // Which triangle of quad (left | right)
        if (x > z)
            sw = nw + se - ne;
        else
            ne = se + nw - sw;

        // calculate interpolation of selected triangle
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
     * A function where the output is disjunctively determined by the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The output that is no smaller than each input
     */
//    public static float or(@NotNull float... arr) {
//        float product = 1;
//        for (float f : arr) {
//            product *= (1 - f);
//        }
//        return 1.0f - product;
//    }

    /**
     * a and b should be in 0..1.0 unit domain; output will also
     */
    public static float or(float a, float b) {
        return 1.0f - ((1.0f - a) * (1.0f - b));
    }

    public static <X> boolean and(X[] xx, Predicate<X> p) {
        for (X x : xx) {
            if (!p.test(x))
                return false;
        }
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
        for (X x : xx) {
            if (p.test(x))
                return true;
        }
        return false;
    }

//    public static <X> boolean or(Predicate<X> p, X... xx) {
//        return or(p, xx);
//    }

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


    public final static ObjectMapper msgPackMapper =
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
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            //.disable(SerializationFeature.FAIL_ON_SELF_REFERENCES)
            ;

    /**
     * msgpack serialization
     */
    public static byte[] toBytes(Object x) throws JsonProcessingException {
        return msgPackMapper./*writerFor(c).*/writeValueAsBytes(x);
    }

//    public static byte[] pack(Object x, Class c) throws JsonProcessingException {
//        return msgPackMapper./*writerFor(c).*/writeValueAsBytes(x);
//    }

    /**
     * msgpack deserialization
     */
    public static <X> X fromBytes(byte[] msgPacked, Class<? extends X> type) throws IOException {
        return msgPackMapper/*.reader(type)*/.readValue(msgPacked, type);
    }

    public static <X> X fromBytes(byte[] msgPacked, int len, Class<? extends X> type) throws IOException {
        return msgPackMapper/*.reader(type)*/.readValue(msgPacked, 0, len, type);
    }

    //static final Logger jsonLogger = LoggerFactory.getLogger(JsonNode.class);

    public static JsonNode jsonNode(Object x) {
        if (x instanceof String) {
            try {
                return msgPackMapper.readTree(x.toString());
            } catch (IOException e) {
                e.printStackTrace();
                //jsonLogger.error(" {}", e);
            }
        }

        return msgPackMapper.valueToTree(x);
    }

    /**
     * x in -1..+1, y in -1..+1.   typical value for sharpen will be ~ >5
     * http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIoMS8oMStleHAoLTUqeCkpLTAuNSkqMiIsImNvbG9yIjoiIzAwMDAwMCJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIi0xIiwiMSIsIi0xIiwiMSJdfV0-
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

    public static <X> X[] map(int from, int to, IntFunction<X> build, IntFunction<X[]> arrayizer) {
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
            return w;
        }


        //TODO combine these into one normalize() by calculating an equivalent effective minmax range
        Util.normalize(w, minmax[0], minmax[1]);
        Util.normalize(w, 0 - lower, 1 + upper);
        return w;
    }

    public static float[] map(int num, IntToFloatFunction build, @Nullable float[] reuse) {
        float[] f = (reuse != null && reuse.length == num) ? reuse : new float[num];
        for (int i = 0; i < num; i++) {
            f[i] = build.valueOf(i);
        }
        return f;
    }

    /**
     * returns amount of memory used as a value between 0 and 100% (1.0)
     */
    public static float memoryUsed() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory(); // current heap allocated to the VM process
        long free = runtime.freeMemory(); // out of the current heap, how much is free
        long max = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
        long usedMemory = total - free; // how much of the current heap the VM is using
        long availableMemory = max - usedMemory; // available memory i.e. Maximum heap size minus the current amount used
        float ratio = 1f - ((float) availableMemory) / max;
        //logger.warn("max={}k, used={}k {}%, free={}k", max/1024, total/1024, Texts.n2(100f * ratio), free/1024);
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
        f.forEachWithIndex((e,n)->{
            x.accept(header + " " + e, n);
        });
    }

    public static void toMap(Iterator<? extends Map.Entry<?,?>> f, String header, BiConsumer<String, Object> x) {
        f.forEachRemaining((e) -> {
            x.accept(header + " " + e.getKey(), e.getValue());
        });
    }


    public static void decode(AbstractHistogram h, String header, int linearStep, BiConsumer<String, Object> x) {
        int digits = (int) (1 + Math.log10(h.getMaxValue())); //pad leading number for lexicographic / ordinal coherence
        h.linearBucketValues(linearStep).iterator().forEachRemaining((p) -> {
            x.accept(header + " [" +
                            iPad(p.getValueIteratedFrom(), digits) + ".." + iPad(p.getValueIteratedTo(), digits) + ']',
                    p.getCountAddedInThisIterationStep());
        });
    }

    public static void decode(DoubleHistogram h, String header, double linearStep, BiConsumer<String, Object> x) {
        final char[] order = {'a'};
        h.linearBucketValues(linearStep).iterator().forEachRemaining((p) -> {
            x.accept(header + " " + (order[0]++) +
                            "[" + n4(p.getValueIteratedFrom()) + ".." + n4(p.getValueIteratedTo()) + ']',
                    p.getCountAddedInThisIterationStep());
        });
    }

    public static void decodePercentile(AbstractHistogram h, String header, BiConsumer<String, Object> x) {
        h.percentiles(1).iterator().forEachRemaining(p -> {
            x.accept(header + " [" +
                            p.getValueIteratedFrom() + ".." + p.getValueIteratedTo() + ']',
                    p.getCountAddedInThisIterationStep());
        });
    }

    /**
     * pretty close
     * http://www.musicdsp.org/showone.php?id=238
     * https://en.wikipedia.org/wiki/Pad%C3%A9_approximant
     * http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiJ4KigyNyt4KngpLygyNys5KngqeCkiLCJjb2xvciI6IiMxQjM3QTgifSx7InR5cGUiOjAsImVxIjoidGFuaCh4KSIsImNvbG9yIjoiIzVBQzIzQSJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIi0xLjczMzE5OTc5MDA3OTk5NjIiLCIwLjAxMTYzMDY3MzkyMDAwMDI3OCIsIi0xLjA2MTExNDM0NzUxOTk5OCIsIjAuMDEyNjI3NDc2NDgwMDAwNjc3Il19XQ--
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
            //for JDK which doesnt support what Disruptor requires
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

    public static <X> Stream<X> buffer(Stream<X> x) {
        List<X> buffered = x.collect(toList());
        return buffered.stream();
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
                byte[] xx = x.toArray(); //TODO i want the array zero-copy
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

    /* domain: [0..1], range: [0..1] */
    public static float smoothDischarge(float x) {
        x = unitize(x);
        return 2*(x-1)/(x-2);
    }

    /**
     * Get the location from which the supplied object's class was loaded.
     *
     * @param object the object for whose class the location should be retrieved
     * @return an {@code Optional} containing the URL of the class' location; never
     * {@code null} but potentially empty
     */
    @Nullable public static URL locate(ClassLoader loader, String className) {
        // determine class loader

        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
            while (loader != null && loader.getParent() != null) {
                loader = loader.getParent();
            }
        }
        // try finding resource by name
        if (loader != null) {

            //className = className.replace(".", "/") + ".class";
            try {
                return (loader.getResource(className));
            }
            catch (Throwable ignore) {
                /* ignore */
            }
        }
//        // try protection domain
//        try {
//            CodeSource codeSource = c.getProtectionDomain().getCodeSource();
//            if (codeSource != null) {
//                return (codeSource.getLocation());
//            }
//        }
//        catch (SecurityException ignore) {
//            /* ignore */
//        }
        return null;
    }


//    public static <T>  Collector<T, ?, List<T>> toListOrNullIfEmpty() {
//        return new Collectors.CollectorImpl<>((Supplier<List<T>>) ArrayList::new, List::add,
//                                   (left, right) -> { left.addAll(right); return left; },
//                                   CH_ID);
//    }

}