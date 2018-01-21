package jcog.memoize;

import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
    based on Chronicle Core's ParsingCache<E>
        Created by Peter Lawrey on 29/02/16.
*/
public class QuickMemoize<X, Y> {

    protected Pair<X,Y>[] data;
    protected int mask;
    protected int shift;
    private final Function<X, Y> eFunction;
    protected boolean toggle = false;


    public QuickMemoize(int capacity, Function<X, Y> eFunction) throws IllegalArgumentException {
        this.eFunction = eFunction;
        resize(capacity);
    }

    public void resize(int capacity) {
        int n = nextPower2(capacity, 16);
        shift = intLog2(n);
        data = new Pair[n];
        mask = n - 1;
    }

    @Nullable
    public Y apply(@Nullable X x) {
        if (x == null)
            return null;
        int hash = this.hash(x);
        int h = hash & mask;
        Pair<X, Y> s1 = data[h];
        if (s1 != null && x.equals(s1.getOne())) {
            return s1.getTwo();
        }

        int h2 = (hash >> shift) & mask;
        Pair<X, Y> s2 = data[h2];
        if (s2 != null && x.equals(s2.getOne())) {
            return s2.getTwo();
        }

        Y xy = eFunction.apply(x);
        Pair<X, Y> s3 = pair(x, xy);
        data[s1 == null || (s2 != null && toggle()) ? h : h2] = s3;

        return xy;
    }

    private int hash(@Nullable X x) {
        return x.hashCode();
    }

    protected boolean toggle() {
        return toggle = !toggle;
    }

    public int valueCount() {
        return (int) Stream.of(data).filter(Objects::nonNull).count();
    }


    /**
     * Returns rounded down log<sub>2</sub>{@code num}, e. g.: {@code intLog2(1) == 0},
     * {@code intLog2(2) == 1}, {@code intLog2(7) == 2}, {@code intLog2(8) == 3}, etc.
     *
     * @throws IllegalArgumentException if the given number <= 0
     */
    public static int intLog2(long num) {
        if (num <= 0)
            throw new IllegalArgumentException("positive argument expected, " + num + " given");
        return 63 - Long.numberOfLeadingZeros(num);
    }


    public static int nextPower2(int n, int min) throws IllegalArgumentException {
        return (int) Math.min(1 << 30, nextPower2((long) n, (long) min));
    }

    public static long nextPower2(long n, long min) throws IllegalArgumentException {
        if (!isPowerOf2(min))
            throw new IllegalArgumentException(min + " must be a power of 2");
        if (n < min) return min;
        if (isPowerOf2(n))
            return n;
        long i = min;
        while (i < n) {
            i *= 2;
            if (i <= 0) return 1L << 62;
        }
        return i;
    }

    public static boolean isPowerOf2(long n) {
        return Long.bitCount(n) == 1;
    }


}

