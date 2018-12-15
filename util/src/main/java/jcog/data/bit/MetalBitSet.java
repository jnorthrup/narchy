package jcog.data.bit;

import jcog.TODO;
import jcog.util.ArrayUtils;

import java.util.Arrays;

/**
 * Bare Metal Fixed-Size BitSets
 * <p>
 * for serious performance. implementations will not check index bounds
 * nor grow in capacity
 *
 * TODO methods from: http://hg.openjdk.java.net/jdk/jdk/file/2cc1ae79b303/src/java.xml/share/classes/com/sun/org/apache/xalan/internal/xsltc/dom/BitArray.java
 */
abstract public class MetalBitSet {

    public abstract boolean get(int i);

    public abstract void set(int i);

    public abstract void clear(int i);

    public abstract void clear();

    public abstract void setAll();

    public abstract int cardinality();

    public boolean isEmpty() {
        return cardinality() == 0;
    }

    public final void set(int i, boolean v) {
        if (v) set(i);
        else clear(i);
    }

    public void set(int start, int end, boolean v) {
        if (v) {
            for (int i = start; i < end; i++)
                set(i);
        } else {
            for (int i = start; i < end; i++)
                clear(i);
        }
    }


    /**
     * finds the next bit matching 'what' between from (inclusive) and to (exclusive), or -1 if nothing found
     */
    public int next(boolean what, int from, int to) {
        for (int i = from; i < to; i++) {
            if (get(i) == what)
                return i;
        }
        return -1;
    }


    /** TODO implement better bulk set(start,end,v) impl */
    public static class LongArrayBitSet extends MetalBitSet {
        long[] data;

        /**
         * Deserialize long array as bitset.
         *
         * @param data
         */
        public LongArrayBitSet(long[] data) {
            assert data.length > 0;
            this.data = data;
        }

        protected LongArrayBitSet(long bits) {
            resize(bits);
        }

        public int capacity() {
            return data.length * 64;
        }
        public void resize(long bits) {


            long[] prev = data;

            if (bits == 0)
                data = ArrayUtils.EMPTY_LONG_ARRAY;
            else
                data = new long[Math.max(1, (int) Math.ceil(((double) bits) / Long.SIZE))];

            if (prev!=null) {
                System.arraycopy(prev, 0, data, 0, Math.min(data.length, prev.length));
            }
        }


        public void clear() {
            Arrays.fill(data, 0);
        }

        @Override
        public void setAll() {
            Arrays.fill(data, 0xffffffffffffffffL);
        }

        /**
         * number of bits set to true
         */
        public int cardinality() {
            int sum = 0;
            for (long l : data)
                sum += Long.bitCount(l);
            return sum;
        }

        @Override
        public boolean isEmpty() {
            for (long l : data)
                if (l != 0)
                    return false;
            return true;
        }

        /**
         * Sets the bit at specified index.
         *
         * @param i
         */
        @Override
        public void set(int i) {
            data[i >>> 6] |= (1L << i);
        }

        @Override
        public void clear(int i) {
            data[i >>> 6] &= ~(1L << i);
        }


        public boolean getAndSet(int index, boolean next) {
            int i = index >>> 6;
            int j = (int) (1L << index);
            long[] d = this.data;
            boolean prev = (d[i] & j) != 0;
            if (prev != next) {
                if (next) {
                    d[i] |= j;
                } else {

                    d[i] &= ~j;
                }
            }
            return prev;
        }

        /**
         * Returns true if the bit is set in the specified index.
         *
         * @param i
         * @return
         */
        @Override
        public boolean get(int i) {
            return (data[i >>> 6] & (1L << i)) != 0;
        }

        /**
         * Number of bits
         */
        public long bitSize() {
            return (long) data.length * Long.SIZE;
        }

        public long[] getData() {
            return data;
        }

        /**
         * Combines the two BitArrays using bitwise OR.
         */
        public void putAll(LongArrayBitSet array) {
            assert data.length == array.data.length :
                    "BitArrays must be of equal length (" + data.length + "!= " + array.data.length + ')';
            for (int i = 0; i < data.length; i++) {
                data[i] |= array.data[i];
            }
        }


        /**
         * Returns the index of the first bit that is set to {@code false}
         * that occurs on or after the specified starting index.
         *
         * @param fromIndex the index to start checking from (inclusive)
         * @return the index of the next clear bit
         * @throws IndexOutOfBoundsException if the specified index is negative
         * @since 1.4
         */
        public int nextClearBit() {

            if (data.length > 1)
                throw new TODO();
            return Long.numberOfLeadingZeros(~data[0]);


        }


    }


    public static class IntBitSet extends MetalBitSet {

        private int x;

        @Override
        public boolean isEmpty() {
            return x == 0;
        }

        @Override
        public void setAll() {
            x = 0xffffffff;
        }

        @Override
        public boolean get(int i) {
            return (x & (1 << i)) != 0;
        }

        @Override
        public void set(int i) {
            x |= (1 << i);
        }

        @Override
        public void clear(int i) {
            x &= ~(1 << i);
        }

        @Override
        public void clear() {
            x = 0;
        }

        @Override
        public int cardinality() {
            int x = this.x;
            return x == 0 ? 0 : Integer.bitCount(x);
        }

    }

    public static MetalBitSet bits(int size) {
        if (size <= 32) {
            return new IntBitSet();
        } else {
            return new LongArrayBitSet(size);
        }
    }

}
