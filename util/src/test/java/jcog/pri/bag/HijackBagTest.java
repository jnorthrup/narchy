package jcog.pri.bag;

import com.google.common.base.Joiner;
import jcog.data.list.table.Table;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.impl.hijack.DefaultHijackBag;
import jcog.pri.op.PriMerge;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.TreeSet;

import static jcog.pri.bag.BagTest.*;
import static jcog.pri.op.PriMerge.max;
import static jcog.pri.op.PriMerge.plus;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO test packing efficiency (lack of sparsity)
 */
class HijackBagTest {

//    static void max(Prioritizable existing, Prioritized incoming) {
//        float p = incoming.priElseZero();
//        if (p > 0)
//            existing.priMax(p);
//    }
    @Test
    void testPutMinMaxAndUniquenesses() {
        for (int reprobes : new int[] { 2, 4, 8 }) {
            for (int capacity : new int[] { 2, 4, 8, 16, 32, 64, 128 }) {
                testPutMinMaxAndUniqueness(
                        new DefaultHijackBag(max, capacity, reprobes));
            }
        }
    }

    private static PLink<String> p(String s, float pri) {
        return new PLink<>(s, pri);
    }

    @Test
    void testGrowToCapacity() {
        int cap = 16;
        int reprobes = 3;
        DefaultHijackBag<String> b = new DefaultHijackBag<>(max, cap, reprobes);
        assertEquals(0, b.size());
//        assertEquals(b.spaceMin(), b.space());
        assertEquals(cap, b.capacity());

        b.put(p("x",0.5f));
        assertEquals(1, b.size());
        assertEquals(b.spaceMin(), b.space());

        b.put(p("y",0.25f));
        assertEquals(b.spaceMin(), b.space());

        for (int i = 0; i <cap*2 /* ensure filled */; i++)
            b.put(p("z" + i, 0.5f));
        assertEquals(b.capacity(), b.space());

        
        for (int i = 0; i < 64; i++)
            b.put(p("w" + i, 0.8f));
        assertEquals(b.capacity(), b.size());
        assertEquals(b.capacity(), b.space());
        assertTrue(Math.abs(b.capacity() - b.size()) <= 2); 

        
        b.setCapacity(cap = cap/2);
        assertEquals(cap, b.capacity());
        assertEquals(cap, b.space());
        assertTrue(cap >= b.size());

        b.clear();
        assertEquals(0, b.size());
        assertEquals(cap, b.capacity());
        assertEquals(b.spaceMin(), b.space());
    }

    @Test
    void testRemoveByKey() {
        BagTest.testRemoveByKey(new DefaultHijackBag(plus, 2, 3));
    }

    @Test
    void testBasicInsertionRemovalHijack() {
        testBasicInsertionRemoval(new DefaultHijackBag(max, 1, 1));
    }

    @Test
    void testHijackFlatBagRemainsRandomInNormalizedSampler() {

        int n = 256;

        Bag<String,PriReference<String>> a = new DefaultHijackBag(max, n, 4);
        for (int i = 0; i < n*8; i++) {
            a.put(new PLink('x' + Integer.toString(Float.floatToIntBits(1f/i),5), ((float)(i))/(n)));
        }

        a.commit();
        int size = a.size();
        





        TreeSet<String> keys2 = new TreeSet();
        a.forEach((b)->{
            if (!keys2.add(b.get()))
                throw new RuntimeException("duplicate detected");
        });
        System.out.println( keys2.size() + " " + Joiner.on(' ').join(keys2) );

        assertEquals(size, keys2.size());








        
        

        
    }

    @Test
    void testHijackSampling() {
        for (int cap : new int[] { 63, 37 }) {
            int rep = 3;
            int batch = 4;
            int extraSpace = Math.round(cap *0.2f);
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
            bag.print();
            testBagSamplingDistribution(bag, batch);
        }

    }

    @Test
    void testHijackResize() {
        Random rng = rng();
        DefaultHijackBag b = new DefaultHijackBag(PriMerge.max, 0, 7);
        BagTest.populate(b, rng, 10, 20, 0f, 1f, 0.5f);
        


        int dimensionality = 50;
        b.setCapacity(dimensionality * 2);

        BagTest.populate(b, rng, dimensionality*5, dimensionality, 0f, 1f, 0.5f);
        
        b.print();
        assertApproximatelySized(b, dimensionality, 0.5f);

        b.setCapacity(dimensionality/2*2);

        
        b.print();

        assertApproximatelySized(b, dimensionality/2*2, 0.5f);

        BagTest.populate(b, rng, dimensionality*3, dimensionality, 0f, 1f, 0.5f);
        
        b.print();

        


        b.setCapacity(dimensionality*2);

        BagTest.populate(b, rng, dimensionality*3, dimensionality, 0f, 1f, 0.5f);
        System.out.println("under capacity, expanded");
        b.print();

        assertApproximatelySized(b, dimensionality, 0.25f);
        


    }

    private void assertApproximatelySized(Table<String, ?> b, int expected, float closeness) {
        int bSize = b.size();
        float error = Math.abs(expected - bSize) / (Math.max(bSize, (float) expected));
        System.out.println(bSize + "  === " + expected + ", diff=" + error);
        assertTrue(error < closeness);
    }



























}