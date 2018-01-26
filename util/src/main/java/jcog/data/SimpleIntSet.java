package jcog.data;

import jcog.data.bit.MetalBitSet;

import java.io.Serializable;
import java.util.*;

/**
 * A utility class for efficiently storing a set of integers. The implementation
 * is based on Algorithm D (Open addressing with double hashing) from Knuth's
 * TAOCP page 528.
 *
 * @author Edward Raff
 * https://raw.githubusercontent.com/EdwardRaff/jLZJD/master/src/main/java/com/edwardraff/jlzjd/IntSetNoRemove.java
 * https://github.com/EdwardRaff/JSAT/blob/master/JSAT/src/jsat/utils/ClosedHashingUtil.java
 */
public class SimpleIntSet extends AbstractSet<Integer> implements Serializable {

    private static final int DEFAULT_CAPACITY = 4;
    private static final float loadFactor = 0.75f;

    private int size = 0;
    /**
     * true if occupied, false otherwise (i.e., free)
     */
    private MetalBitSet status;
    private int[] keys;

    /**
     * Creates a new empty integer set
     */
    public SimpleIntSet() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Creates an empty integer set pre-allocated to store a specific number of
     * items
     *
     * @param capacity the number of items to store
     */
    public SimpleIntSet(int capacity) {
        int size = getNextPow2TwinPrime((int) Math.max(capacity / loadFactor, 4));
        alloc(size);
        this.size = 0;
    }

    private void alloc(int size) {
        status = MetalBitSet.bits(size);
        keys = new int[size];
    }

    /**
     * Creates a new set of integers from the given set
     *
     * @param set the set of integers to create a copy of
     */
    public SimpleIntSet(Set<Integer> set) {
        this(set.size());
        this.addAll(set);
    }

    /**
     * Creates a set of integers from the given collection
     *
     * @param collection a collection of integers to create a set from
     */
    public SimpleIntSet(Collection<Integer> collection) {
        this();
        this.addAll(collection);
    }


    /**
     * Gets the index of the given key. Based on that {@link #status} variable,
     * the index is either the location to insert OR the location of the key.
     * <p>
     * This method returns 2 integer table in the long. The lower 32 bits are
     * the index that either contains the key, or is the first empty index.
     * <p>
     * The upper 32 bits is the index of the first position marked as
     * {@link #DELETED} either {@link Integer#MIN_VALUE} if no position was
     * marked as DELETED while searching.
     *
     * @param key they key to search for
     * @return the mixed long containing the index of the first DELETED position
     * and the position that the key is in or the first EMPTY position found
     */
    private int index(int key) {
        //D1 
        final int hash = key & 0x7fffffff;

        int[] k = this.keys;
        int len = k.length;

        int i = hash % len;

        //D2
        MetalBitSet s = this.status;
        if (!s.get(i) || k[i] == key)
            return i;

        //D3
        final int c = 1 + (hash % (len - 2));

        while (true)//this loop will terminate
        {
            //D4
            i -= c;
            if (i < 0)
                i += len;
            //D5
            if (!s.get(i) || k[i] == key)
                return i;
        }
    }

    private void enlargeIfNeeded() {
        if (size+1 < keys.length * loadFactor)
            return;

        //enlarge
        final MetalBitSet oldSatus = status;
        final int[] oldKeys = keys;

        int newSize = getNextPow2TwinPrime(keys.length * 3 / 2);//it will actually end up doubling in size since we have twin primes spaced that was
        alloc(newSize);

        size = 0;
        for (int oldIndex = 0; oldIndex < oldKeys.length; oldIndex++)
            if (oldSatus.get(oldIndex))
                add(oldKeys[oldIndex]);
    }

    @Override
    public void clear() {
        size = 0;
        status.clearAll();
    }

    @Override
    public boolean add(Integer e) {
        return e != null && add(e.intValue());
    }

    /**
     * @param e element to be added to this set
     * @return true if this set did not already contain the specified element
     */
    public boolean add(int e) {
        final int key = e;
        int pair_index = index(key);
//        int deletedIndex = (int) (pair_index >>> 32);
//        int valOrFreeIndex = (int) (pair_index & INT_MASK);
        int valOrFreeIndex = pair_index;

        if (status.get(valOrFreeIndex))//easy case
            return false;//we already had this item in the set!

        //else, not present
        int i = valOrFreeIndex;
//        if(deletedIndex >= 0)//use occupied spot instead
//            i = deletedIndex;

        status.set(i);
        keys[i] = key;
        size++;

        enlargeIfNeeded();

        return true;//item was not in the set previously
    }

    public boolean contains(int o) {
        return status.get(index(o));//would be FREE if we didn't have the key
    }


    @Override
    public boolean contains(Object o) {
        if (o != null && o instanceof Integer)
            return contains(((Integer) o).intValue());
        else
            return false;
    }


//    public IntIterator intIterator() {
//                //find the first starting inded
//        int START = 0;
//        while (START < keys.length && !status.get(START))
//            START++;
//        if (START == keys.length)
//            return IntIterator.emptyIterator();
//
//        final int startPos = START;
//
//        return new IntegerIterator(startPos);
//    }

    @Override
    public Iterator<Integer> iterator() {
        //find the first starting inded
        int START = 0;
        while (START < keys.length && !status.get(START))
            START++;
        if (START == keys.length)
            return Collections.emptyIterator();

        final int startPos = START;

        return new IntegerIterator(startPos);
    }

    @Override
    public int size() {
        return size;
    }


    /**
     * Gets the next twin prime that is near a power of 2 and greater than or
     * equal to the given value
     *
     * @param m the integer to get a twine prime larger than
     * @return the a twin prime greater than or equal to
     */
    static int getNextPow2TwinPrime(int m) {
        int pos = Arrays.binarySearch(twinPrimesP2, m + 1);
        int p = (pos >= 0) ? pos : -pos - 1;
        return twinPrimesP2[p];
    }

    /**
     * This array lits twin primes that are just larger than a power of 2. The
     * prime in the list will be the larger of the twins, so the smaller can be
     * obtained by subtracting 2 from the value stored. The list is stored in
     * sorted order.<br>
     * Note, the last value stored is just under 2<sup>31</sup>, where the other
     * values are just over 2<sup>x</sup> for x &lt; 31
     */
    static final int[] twinPrimesP2 =
            {
                    7, //2^2 , twin with 5
                    13, //2^3 , twin with 11
                    19, //2^4 , twin with 17
                    43, //2^5 , twin with 41
                    73, //2^6 , twin with 71
                    139, //2^7 , twin with 137
                    271, //2^8 , twin with 269
                    523, //2^9 , twin with 632
                    1033, //2^10 , twin with 1031
                    2083, //2^11 , twin with 2081
                    4129, //2^12 , twin with 4127
                    8221, //2^13 , twin with 8219
                    16453, //2^14 , twin with 16451
                    32803, //2^15 , twin with 32801
                    65539, //2^16 , twin with 65537
                    131113, //2^17 , twin with 131111
                    262153, //2^18 , twin with 262151
                    524353, //2^19 , twin with 524351
                    1048891, //2^20 , twin with 1048889
                    2097259, //2^21 , twin with 2097257
                    4194583, //2^22 , twin with 4194581
                    8388619, //2^23 , twin with 8388617
                    16777291, //2^24 , twin with 16777289
                    33554503, //2^25 , twin with 33554501
                    67109323, //2^26 , twin with 67109321
                    134217781, //2^27 , twin with 134217779
                    268435579, //2^28 , twin with 268435577
                    536871019, //2^29 , twin with 536871017
                    1073741833, //2^30 , twin with 1073741831
                    2147482951, //first twin under 2^31, twin with 2147482949
            };


    private class IntegerIterator implements Iterator<Integer> {
        private final int startPos;
        int pos;
        int prevPos;

        public IntegerIterator(int startPos) {
            this.startPos = startPos;
            pos = startPos;
            prevPos = -1;
        }

        @Override
        public boolean hasNext() {
            return pos < keys.length;
        }

        @Override
        public Integer next() {
            //final int make so that object remains good after we call next again
            final int oldPos = prevPos = pos++;
            //find next
            int pos = this.pos;
            MetalBitSet s = SimpleIntSet.this.status;
            int[] k = SimpleIntSet.this.keys;
            while (pos < k.length && !s.get(pos))
                pos++;
            this.pos = pos;
            //and return new object
            return k[oldPos];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}