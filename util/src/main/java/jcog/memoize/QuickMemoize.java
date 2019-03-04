package jcog.memoize;

import jcog.TODO;
import jcog.util.HashCachedPair;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
    based on Chronicle Core's ParsingCache<E>
        Created by Peter Lawrey on 29/02/16.
*/
public class QuickMemoize<X, Y> {

    protected Pair<X,Y>[] data;
    protected int mask, shift;
    protected boolean toggle = false;


    public QuickMemoize(int capacity) throws IllegalArgumentException {
        resize(capacity);
    }

    public void resize(int _capacity) {
        int capacity = nextPower2(_capacity, 4 /* was: 16 */);
        if (data!=null && capacity == data.length)
            return; //same size

        int nextShift = intLog2(capacity);
        int nextMask = capacity - 1;
        Pair[] nextData = new Pair[capacity];

        if (this.data!=null) {
            throw new TODO("copy to the new instance and replace the fields here");
        }

        this.shift = nextShift; this.mask = nextMask; this.data = nextData;
    }

    @Nullable  public final Y apply(@Nullable X X, Function<X, Y> calc) {
        return apply(X, calc, (x, c)->c.apply(x));
    }

    @Nullable  public <P> Y apply(@Nullable X x, P p, BiFunction<X, P, Y> calc) {
        Pair<X, Y> s1, s2;

        Pair<X,Y>[] data = this.data;

        int hash = this.hash(x);
        int h = hash & mask;
        if ((s1 = data[h]) != null && x.equals(s1.getOne()))
            return s1.getTwo();


        int h2 = (hash >> shift) & mask;
        if ((s2 = data[h2]) != null && x.equals(s2.getOne()))
            return s2.getTwo();

        Y xy = calc.apply(x, p);
        if (store(x,xy)) {
            Pair<X, Y> s3 = new HashCachedPair(x, xy);
            data[s1 == null || (s2 != null && toggle()) ? h : h2] = s3;
        }
        return xy;
    }

    /** post-filter, deciding to keep the value */
    protected boolean store(X x, Y y) {
        return true;
    }

    private int hash(@Nullable X x) {
        return x.hashCode();
    }

    private boolean toggle() {
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

