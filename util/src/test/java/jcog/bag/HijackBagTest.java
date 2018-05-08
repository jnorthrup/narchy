package jcog.bag;

import com.google.common.base.Joiner;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.list.table.Table;
import jcog.math.random.XorShift128PlusRandom;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.TreeSet;

import static jcog.bag.BagTest.*;
import static jcog.pri.op.PriMerge.max;
import static jcog.pri.op.PriMerge.plus;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO test packing efficiency (lack of sparsity)
 */
public class HijackBagTest {

    @Test public void testPutMinMaxAndUniquenesses() {
        for (int reprobes : new int[] { 2, 4, 8 }) {
            for (int capacity : new int[] { 2, 4, 8, 16, 32, 64, 128 }) {
                testPutMinMaxAndUniqueness(
                        new DefaultHijackBag<>(max, capacity, reprobes));
            }
        }
    }

    static PLink<String> p(String s, float pri) {
        return new PLink<>(s, pri);
    }

    @Test public void testGrowToCapacity() {
        int cap = 16;
        int reprobes = 3;
        DefaultHijackBag<String> b = new DefaultHijackBag<String>(max, cap, reprobes);
        assertEquals(0, b.size());
        assertEquals(reprobes, b.space());
        assertEquals(cap, b.capacity());

        b.put(p("x",0.5f));
        assertEquals(1, b.size());
        assertEquals(10, b.space());

        b.put(p("y",0.25f));
        assertEquals(10, b.space());

        for (int i = 0; i <12; i++)
            b.put(p("z" + i, 0.5f));
        assertEquals(b.capacity(), b.space());

        //limit reached, nothing added will grow any further
        for (int i = 0; i < 64; i++)
            b.put(p("w" + i, 0.8f));
        assertEquals(b.capacity(), b.space());
        assertTrue(Math.abs(b.capacity() - b.size()) <= 2); //close to capacity

        //now try shrinking
        b.setCapacity(cap/2);
        assertEquals(cap/2, b.capacity());
        assertEquals(cap/2, b.space());
        assertTrue(cap/2 >= b.size());

    }

    @Test public void testRemoveByKey() {
        BagTest.testRemoveByKey(new DefaultHijackBag(plus, 2, 3));
    }

    @Test
    public void testBasicInsertionRemovalHijack() {
        testBasicInsertionRemoval(new DefaultHijackBag(max, 1, 1));
    }

    @Test public void testHijackFlatBagRemainsRandomInNormalizedSampler() {

        int n = 256;

        Bag<String,PriReference<String>> a = new DefaultHijackBag<>(max, n, 4);
        for (int i = 0; i < n*8; i++) {
            a.put(new PLink("x" + Integer.toString(Float.floatToIntBits(1f/i),5), ((float)(i))/(n)));
        }

        a.commit();
        int size = a.size();
        //assertTrue(size >= 20 && size <= 30);

//        TreeSet<String> keys = new TreeSet();
//        Iterators.transform(a.iterator(), x -> x.get()).forEachRemaining(keys::add);
//        System.out.println( keys.size() + " " + Joiner.on(' ').join(keys) );

        TreeSet<String> keys2 = new TreeSet();
        a.forEach((b)->{
            if (!keys2.add(b.get()))
                throw new RuntimeException("duplicate detected");
        });
        System.out.println( keys2.size() + " " + Joiner.on(' ').join(keys2) );

        assertEquals(size, keys2.size());

//        int b = 20;
//        EmpiricalDistribution e = BagTest.getSamplingPriorityDistribution(a, n * 500, b);
//
//        printDist(e);
//
//        //monotonically increasing:
//        assertTrue(e.getBinStats().get(0).getMean() < e.getBinStats().get(b-1).getMean());
        //assertTrue(e.getBinStats().get(0).getMean() < e.getBinStats().get(b/2).getMean());
        //assertTrue(e.getBinStats().get(b/2).getMean() < e.getBinStats().get(b-2).getMean());

        //a.print();
    }

    @Test public void testHijackSampling() {
        for (int cap : new int[] { 63, 37 }) {
            int rep = 3;
            int batch = 4;
            int extraSpace = 5;
            final Random rng = new XorShift128PlusRandom(1);
            DefaultHijackBag bag = new DefaultHijackBag(plus, cap * extraSpace, rep) {

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

    @Test
    public void testHijackResize() {
        Random rng = rng();
        DefaultHijackBag b = new DefaultHijackBag(PriMerge.max, 0, 7);
        BagTest.populate(b, rng, 10, 20, 0f, 1f, 0.5f);
        //        assertEquals(b.reprobes /*0*/, b.size());


        int dimensionality = 50;
        b.setCapacity(dimensionality * 2);

        BagTest.populate(b, rng, dimensionality*5, dimensionality, 0f, 1f, 0.5f);
        //System.out.println("under capacity");
        b.print();
        assertApproximatelySized(b, dimensionality, 0.5f);

        b.setCapacity(dimensionality/2*2);

        //System.out.println("half capacity");
        b.print();

        assertApproximatelySized(b, dimensionality/2*2, 0.5f);

        BagTest.populate(b, rng, dimensionality*3, dimensionality, 0f, 1f, 0.5f);
        //System.out.println("under capacity, refilled");
        b.print();

        //test


        b.setCapacity(dimensionality*2);

        BagTest.populate(b, rng, dimensionality*3, dimensionality, 0f, 1f, 0.5f);
        System.out.println("under capacity, expanded");
        b.print();

        assertApproximatelySized(b, dimensionality, 0.25f);
        //test


    }

    public void assertApproximatelySized(Table<String,?> b, int expected, float closeness) {
        int bSize = b.size();
        float error = Math.abs(expected - bSize) / (Math.max(bSize, (float) expected));
        System.out.println(bSize + "  === " + expected + ", diff=" + error);
        assertTrue(error < closeness);
    }



//    @Test
//    public void testScalePutArray() {
//        testScalePutHalfs(0.5f, new ArrayBag<>(2, max, new HashMap<>(2)), 1f, 0.5f);
//        testScalePutHalfs(0.75f, new ArrayBag<>(2, plus, new HashMap<>(2)), 1f, 0.5f);
//        testScalePut2(new ArrayBag(2, plus, new HashMap<>(2)));
//
//    }
//    @Test
//    public void testScalePutHijaMax() {
//        //second scale has no effect being smaller than the first one
//        BagTest.testScalePutHalfs(0.5f, new DefaultHijackBag(max, 2, 1), 1f, 0.5f);
//        BagTest.testScalePutHalfs(0.5f, new DefaultHijackBag(max, 2, 2), 1f, 0.5f);
//
//        BagTest.testScalePutHalfs(0.75f, new DefaultHijackBag(plus, 2, 2), 1f, 0.5f);
//    }
//    @Test
//    public void testScalePutHija3() {
//        BagTest.testScalePut2(new DefaultHijackBag(plus, 2, 1));
//    }
//    @Test
//    public void testScalePutHija4() {
//        BagTest.testScalePut2(new DefaultHijackBag(plus, 2, 2));
//    }

}