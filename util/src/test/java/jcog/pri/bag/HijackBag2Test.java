package jcog.pri.bag;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
import jcog.pri.bag.impl.HijackBag2;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.Tensor;
import jcog.util.FloatFloatToFloatFunction;
import org.apache.commons.math3.stat.Frequency;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static jcog.pri.bag.BagTest.rng;
import static org.junit.jupiter.api.Assertions.*;

class HijackBag2Test {

    @Test
    void testPutMinMaxAndUniquenesses() {
        for (int reprobes : new int[]{2, 4, 8}) {
            for (int capacity : new int[]{2, 4, 8, 16, 32, 64, 128}) {
                testPutMinMaxAndUniqueness(
                        new DefaultHijackBag<>(MAX, capacity, reprobes));
            }
        }
    }

    private static void testPutMinMaxAndUniqueness(DefaultHijackBag<Integer> a) {
        float pri = 0.5f;
        int n = a.capacity() * 16; 


        for (int i = 0; i < n; i++) {
            a.put(i, pri);
        }

        a.commit(null); 
        assertEquals(a.capacity(), a.size());

        

        

        List<Integer> keys = new FasterList(a.capacity());
        a.forEachKey(keys::add);
        assertEquals(a.size(), keys.size());
        assertEquals(new HashSet(keys).size(), keys.size());

        assertEquals(pri, a.priMin(), 0.01f);
        assertEquals(a.priMin(), a.priMax(), 0.08f);

        assertTrue(((HijackBag2)a).density() > 0.75f);
    }


    @Test
    void testGrowToCapacity() {
        int cap = 16;
        int reprobes = 3;
        DefaultHijackBag<String> b = new DefaultHijackBag<>(MAX, cap, reprobes);
        assertEquals(0, b.size());
        assertEquals(reprobes, b.space());
        assertEquals(cap, b.capacity());

        b.put("x", 0.5f);
        assertEquals(1, b.size());
        assertEquals(10, b.space());

        b.put("y", 0.25f);
        assertEquals(10, b.space());

        for (int i = 0; i < 12; i++)
            b.put("z" + i, 0.5f);
        assertEquals(b.capacity(), b.space());

        
        for (int i = 0; i < 64; i++)
            b.put("w" + i, 0.8f);
        assertEquals(b.capacity(), b.space());
        assertTrue(Math.abs(b.capacity() - b.size()) <= 2); 

        
        b.setCapacity(cap / 2);
        assertEquals(cap / 2, b.capacity());
        assertEquals(cap / 2, b.space());
        assertTrue(cap / 2 >= b.size());

    }
    private static void testPutRemoveByKey(DefaultHijackBag<String> a) {

        a.put("x", 0.1f);
        assertEquals(1, a.size());

        a.remove("x");
        assertEquals(0, a.size());
        assertTrue(a.isEmpty());

    }
    @Test
    void testPutRemoveByKey() {
        testPutRemoveByKey(new DefaultHijackBag(PLUS, 2, 3));
    }

    private static void testBasicInsertionRemoval(DefaultHijackBag<String> c) {


        assertEquals(1, c.capacity());

        
        c.put("x", 2 * ScalarValue.EPSILON);
        c.commit();

        assertEquals(1, c.size());


        assertEquals(0, c.priMin(), ScalarValue.EPSILON * 2);

        String x = c.get("x");
        assertNotNull(x);
        assertEquals("x", x);
        assertTrue(Util.equals(Prioritized.Zero.priElseNeg1(), c.priElse("x",-1f), 0.01f));

    }

    @Test
    void testBasicInsertionRemovalHijack() {
        testBasicInsertionRemoval(new DefaultHijackBag(MAX, 1, 1));
    }

    @Test
    void testHijackFlatBagRemainsRandomInNormalizedSampler() {

        int n = 256;

        DefaultHijackBag<String> a = new DefaultHijackBag<>(MAX, n, 4);
        for (int i = 0; i < n * 8; i++) {
            a.put(("x" + Integer.toString(Float.floatToIntBits(1f / i), 5)),
                    ((float) (i)) / (n));
        }

        a.commit();
        int size = a.size();
        





        TreeSet<String> keys2 = new TreeSet();
        a.forEach((b) -> {
            if (!keys2.add(b))
                throw new RuntimeException("duplicate detected");
        });
        System.out.println(keys2.size() + " " + Joiner.on(' ').join(keys2));

        assertEquals(size, keys2.size());








        
        

        
    }

    @Test
    void testHijackSampling() {
        for (int cap : new int[]{63, 37}) {
            int rep = 3;
            int batch = 4;
            int extraSpace = 5;
            DefaultHijackBag bag = new DefaultHijackBag(PLUS, cap * extraSpace, rep) {

                @Override
                public void onRemove(Object value) {
                    fail("");
                }

                @Override
                public void onReject(Object value) {
                    fail("");
                }


            };


            fillLinear(bag, cap);
            testBagSamplingDistribution(bag, batch);
            bag.print();
        }

    }
    private static void testBagSamplingDistribution(DefaultHijackBag<String> bag, float batchSizeProp) {


        

        int cap = bag.capacity();
        int batchSize = (int)Math.ceil(batchSizeProp * cap);
        int batches = cap * 1000 / batchSize;

        Tensor f1 = samplingPriDist(bag, batches, batchSize, Math.min(10,Math.max(2, cap/2)));

        String h = "cap=" + cap + " total=" + (batches * batchSize);
        System.out.println(h + ":\n\t" + f1.tsv2());
        System.out.println();

        float[] ff = f1.snapshot();

        

        float orderThresh = 0.1f; 
        for (int j = 0; j < ff.length; j++) {
            for (int i = j+1; i < ff.length; i++) {
                float diff = ff[j] - ff[i];
                boolean unordered = diff > orderThresh;
                if (unordered) {
                    fail("sampling distribution not ordered. contents=" + bag.toString());
                }
            }
        }

        final float MIN_RATIO = 1.5f; 

        for (int lows : ff.length > 4 ? new int[] { 0, 1} : new int[] { 0 }  ) {
            for (int highs : ff.length > 4 ? new int[] { ff.length-1, ff.length-2} : new int[] { ff.length-1 }  ) {
                float maxMinRatio = ff[highs] / ff[lows];
                assertTrue(
                        maxMinRatio > MIN_RATIO,
                        maxMinRatio + " ratio between max and min"
                );
            }
        }


        
        
        













    }
    private static Tensor samplingPriDist(@NotNull DefaultHijackBag<String> b, int batches, int batchSize, int bins) {

        assert(bins > 1);

        Set<String> hit = new TreeSet();
        Frequency hits = new Frequency();
        ArrayTensor f = new ArrayTensor(bins);
        assertFalse(b.isEmpty());
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 0; i < batches; i++) {
            b.sample(rng, batchSize, x -> {
                f.data[Util.bin(b.pri(x), bins)]++;
                hits.addValue(x);
                hit.add(x);
            });
        }

        int total = batches * batchSize;
        assertEquals(total, Util.sum(f.data), 0.001f);

        if (hits.getUniqueCount() != b.size()) {

            System.out.println(hits.getUniqueCount() + " != " + b.size());

            Set<String> items = b.stream().collect(Collectors.toSet());
            items.removeAll(hit);
            System.out.println("not hit: " + items);

            System.out.println(hits);
            fail("all elements must have been sampled at least once");
        }

        

        return f.scale(1f / total);
    }

    private static void fillLinear(DefaultHijackBag<String> bag, int c) {
        assertTrue(bag.isEmpty());

        
        for (int i = c-1; i >= 0; i--) {
            bag.put(i + "x", (i + 0.5f) / c); 
        }
        bag.commit(null);
        assertEquals(c, bag.size());
        assertEquals(0.5f / c, bag.priMin(), 0.03f);
        assertEquals(1 - 1f/(c*2f), bag.priMax(), 0.03f); 
    }
    private static void populate(DefaultHijackBag<String> a, Random rng, int count, int dimensionality, float minPri, float maxPri) {
        float dPri = maxPri - minPri;
        for (int i = 0; i < count; i++) {
            a.put(
                    "x" + rng.nextInt(dimensionality),
                    rng.nextFloat() * dPri + minPri
            );
        }
        a.commit(null);
    }

    @Test
    void testHijackResize() {
        Random rng = rng();
        DefaultHijackBag<String> b = new DefaultHijackBag(MAX, 0, 7);
        populate(b, rng, 10, 20, 0f, 1f);
        


        int dimensionality = 50;
        b.setCapacity(dimensionality * 2);

        populate(b, rng, dimensionality * 5, dimensionality, 0f, 1f);
        
        b.print();
        assertApproximatelySized(b, dimensionality, 0.5f);

        b.setCapacity(dimensionality / 2 * 2);

        
        b.print();

        assertApproximatelySized(b, dimensionality / 2 * 2, 0.5f);

        populate(b, rng, dimensionality * 3, dimensionality, 0f, 1f);
        
        b.print();

        


        b.setCapacity(dimensionality * 2);

        populate(b, rng, dimensionality * 3, dimensionality, 0f, 1f);
        System.out.println("under capacity, expanded");
        b.print();

        assertApproximatelySized(b, dimensionality, 0.25f);
        


    }

    private static void assertApproximatelySized(Table<String, ?> b, int expected, float closeness) {
        int bSize = b.size();
        float error = Math.abs(expected - bSize) / (Math.max(bSize, (float) expected));
        System.out.println(bSize + "  === " + expected + ", diff=" + error);
        assertTrue(error < closeness);
    }


    private static final FloatFloatToFloatFunction PLUS = (existing, incoming)->
            Util.numOr(existing, 0) + incoming;
    private static final FloatFloatToFloatFunction MAX = (existing, incoming)->
            Math.max(Util.numOr(existing, 0), incoming);

    static class DefaultHijackBag<X> extends HijackBag2<X, X> {
        private final FloatFloatToFloatFunction merge;

        DefaultHijackBag(FloatFloatToFloatFunction merge, int initialCapacity, int reprobes) {
            super(initialCapacity, reprobes);
            this.merge = merge;
        }

        @Override
        protected float merge(float existing, float incoming) {
            return merge.apply(existing, incoming);
        }

        @Override
        public Object key(Object value) {
            return value;
        }
    }


}
