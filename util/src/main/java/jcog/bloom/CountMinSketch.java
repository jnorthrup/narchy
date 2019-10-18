/**
 * Copyright 2014 Prasanth Jayachandran
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http:
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcog.bloom;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import jcog.bloom.hash.Murmur3Hash;

import java.nio.ByteBuffer;

/**
 * Count Min sketch is a probabilistic data structure for finding the frequency of events in a
 * stream of data. The data structure accepts two parameters epsilon and delta, epsilon specifies
 * the error in estimation and delta specifies the probability that the estimation is wrong (or the
 * confidence interval). The default values are 1% estimation error (epsilon) and 99% confidence
 * (1 - delta). Tuning these parameters results in increase or decrease in the size of the count
 * min sketch. The constructor also accepts width and depth parameters. The relationship between
 * width and epsilon (error) is width = Math.ceil(Math.exp(1.0)/epsilon). In simpler terms, the
 * lesser the error is, the greater is the width and hence the size of count min sketch.
 * The relationship between delta and depth is depth = Math.ceil(Math.log(1.0/delta)). In simpler
 * terms, the more the depth of the greater is the confidence.
 * The way it works is, if we need to estimate the number of times a certain key is inserted (or appeared in
 * the stream), count min sketch uses pairwise independent hash functions to map the key to
 * different locations in count min sketch and increment the counter.
 * <p/>
 * For example, if width = 10 and depth = 4, lets assume the hashcodes
 * for key "HELLO" using pairwise independent hash functions are 9812121, 6565512, 21312312, 8787008
 * respectively. Then the counter in hashcode % width locations are incremented.
 * <p/>
 * 0   1   2   3   4   5   6   7   8   9
 * --- --- --- --- --- --- --- --- --- ---
 * | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
 * --- --- --- --- --- --- --- --- --- ---
 * --- --- --- --- --- --- --- --- --- ---
 * | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
 * --- --- --- --- --- --- --- --- --- ---
 * --- --- --- --- --- --- --- --- --- ---
 * | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
 * --- --- --- --- --- --- --- --- --- ---
 * --- --- --- --- --- --- --- --- --- ---
 * | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
 * --- --- --- --- --- --- --- --- --- ---
 * <p/>
 * Now for a different key "WORLD", let the hashcodes be 23123123, 45354352, 8567453, 12312312.
 * As we can see below there is a collision for 2nd hashcode
 * <p/>
 * 0   1   2   3   4   5   6   7   8   9
 * --- --- --- --- --- --- --- --- --- ---
 * | 0 | 1 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 |
 * --- --- --- --- --- --- --- --- --- ---
 * --- --- --- --- --- --- --- --- --- ---
 * | 0 | 0 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
 * --- --- --- --- --- --- --- --- --- ---
 * --- --- --- --- --- --- --- --- --- ---
 * | 0 | 0 | 1 | 1 | 0 | 0 | 0 | 0 | 0 | 0 |
 * --- --- --- --- --- --- --- --- --- ---
 * --- --- --- --- --- --- --- --- --- ---
 * | 0 | 0 | 2 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
 * --- --- --- --- --- --- --- --- --- ---
 * <p/>
 * Now, to get the estimated count for key "HELLO", same process is repeated again to find the
 * values in each position and the estimated count will be the minimum of all values (to account for
 * hash collisions).
 * <p/>
 * estimatedCount("HELLO") = min(1, 2, 1, 1)
 * <p/>
 * so even if there are multiple hash collisions, the returned value will be the best estimate
 * (upper bound) for the given key. The actual count can never be greater than this value.
 *
 * http:
 */
public class CountMinSketch {
    
    private static final float DEFAULT_DELTA = 0.01f;
    private static final float DEFAULT_EPSILON = 0.01f;
    public final int w;
    public final int d;
    protected final int[][] multiset; 

    public CountMinSketch() {
        this(DEFAULT_DELTA, DEFAULT_EPSILON);
    }

    public CountMinSketch(float delta, float epsilon) {
        this((int) Math.ceil(Math.exp(1.0) / epsilon),
             (int) Math.ceil(Math.log(1.0 / delta)));
    }

    public CountMinSketch(int width, int depth) {
        this(width, depth, new int[depth][width]);
    }

    private CountMinSketch(int width, int depth, int[][] ms) {
        this.w = width;
        this.d = depth;
        this.multiset = ms;
    }

    public int depth() {
        return d;
    }

    /**
     * Returns the size in bytes after serialization.
     *
     * @return serialized size in bytes
     */
    public long byteSize() {
        return ((w * d) + 2) * (Integer.SIZE / 8);
    }

    public void add(byte... key) {
        
        
        
        
        

        
        
        long hash64 = Murmur3Hash.hash64(key);
        int hash1 = (int) hash64;
        int hash2 = (int) (hash64 >>> 32);
        for (int i = 0; i < d; i++) {
            int combinedHash = hash1 + ((i - 1) * hash2);
            
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            int pos = combinedHash % w;
            multiset[i][pos] += 1;
        }
    }

    public void add(String val) {
        add(val.getBytes());
    }

    public void add(byte val) {
        add(new byte[] { val });
    }

    public void add(short val) {
        add(Shorts.toByteArray(val));
    }

    public void add(int val) {
        add(Ints.toByteArray(val));
    }

    public int countAndAdd(int val) {
        byte[] b = Ints.toByteArray(val);
        int c = count(b);
        add(b);
        return c;
    }

    public int countAndAdd(long val) {
        byte[] b = Longs.toByteArray(val);
        int c = count(b);
        add(b);
        return c;
    }

    public void add(long val) {
        add(Longs.toByteArray(val));
    }

    public void add(float val) {
        add(Float.floatToIntBits(val));
    }

    public void add(double val) {
        add(Double.doubleToLongBits(val));
    }


    public int count(byte[] key) {
        long hash64 = Murmur3Hash.hash64(key);
        int hash1 = (int) hash64;
        int hash2 = (int) (hash64 >>> 32);
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < d; i++) {
            int combinedHash = hash1 + ((i-1) * hash2);
            
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            int pos = combinedHash % w;
            min = Math.min(min, multiset[i][pos]);
        }

        return min;
    }

    public int count(String val) {
        return count(val.getBytes());
    }

    public int count(byte val) {
        return count(new byte[]{val});
    }

    public int count(int val) {
        return count(Ints.toByteArray(val));
    }
    public int count(short val) {
        return count(Shorts.toByteArray(val));
    }
    public int count(long val) {
        return count(Longs.toByteArray(val));
    }

    public int count(float val) {
        return count(Float.floatToIntBits(val));
    }

    public int count(double val) {
        return count(Double.doubleToLongBits(val));
    }

    /**
     * Merge the give count min sketch with current one. Merge will throw RuntimeException if the
     * provided CountMinSketch is not compatible with current one.
     *
     * @param that - the one to be merged
     */
    public CountMinSketch merge(CountMinSketch that) {
        if (that == null || that == this) {
            return this;
        }

        if (this.w != that.w) {
            throw new RuntimeException("Merge failed! Width of count min sketch do not match!" +
                    "this.width: " + w + " that.width: " + that.w);
        }

        if (this.d != that.d) {
            throw new RuntimeException("Merge failed! Depth of count min sketch do not match!" +
                    "this.depth: " + this.depth() + " that.depth: " + that.depth());
        }

        for (int i = 0; i < d; i++) {
            int[] xi = this.multiset[i];
            int[] yi = that.multiset[i];
            for (int j = 0; j < w; j++) {
                xi[j] += yi[j];
            }
        }

        return this;
    }

    /**
     * Serialize the count min sketch to byte array. The format of serialization is width followed by
     * depth followed by integers in multiset from row1, row2 and so on..
     *
     * @return serialized byte array
     */
    public static byte[] serialize(CountMinSketch cms) {
        long serializedSize = cms.byteSize();
        ByteBuffer bb = ByteBuffer.allocate((int) serializedSize);
        bb.putInt(cms.w);
        bb.putInt(cms.depth());
        for (int i = 0; i < cms.depth(); i++) {
            for (int j = 0; j < cms.w; j++) {
                bb.putInt(cms.multiset[i][j]);
            }
        }
        bb.flip();
        return bb.array();
    }

    /**
     * Deserialize the serialized count min sketch.
     *
     * @param serialized - serialized count min sketch
     * @return deserialized count min sketch object
     */
    public static CountMinSketch deserialize(byte[] serialized) {
        ByteBuffer bb = ByteBuffer.allocate(serialized.length);
        bb.put(serialized);
        bb.flip();
        int width = bb.getInt();
        int depth = bb.getInt();
        int[][] multiset = new int[depth][width];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                multiset[i][j] = bb.getInt();
            }
        }
        return new CountMinSketch(width, depth, multiset);
    }
}