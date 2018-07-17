package jcog.pri.bag;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.random.SplitMix64Random;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.math.tensor.ArrayTensor;
import jcog.math.tensor.Tensor;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.HijackBag;
import jcog.pri.bag.impl.hijack.DefaultHijackBag;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static jcog.Texts.n4;
import static jcog.pri.bag.ArrayBagTest.assertSorted;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author me
 */
class BagTest {



    public static void testBasicInsertionRemoval(Bag<String, PriReference<String>> c) {


        assertEquals(1, c.capacity());
        if (!(c instanceof DefaultHijackBag)) {
            assertEquals(0, c.size());
            assertTrue(c.isEmpty());
        }

        
        PLink x0 = new PLink("x", 2 * ScalarValue.EPSILON);
        PriReference added = c.put(x0);
        assertSame(added, x0);
        c.commit();

        assertEquals(1, c.size());


        assertEquals(0, c.priMin(), ScalarValue.EPSILON * 2);

        PriReference<String> x = c.get("x");
        assertNotNull(x);
        assertSame(x, x0);
        assertTrue(Util.equals(Prioritized.Zero.priElseNeg1(), x.priElseNeg1(), 0.01f));

    }


    public static Random rng() {
        return new SplitMix64Random(1);
    }































    public static void printDist(@NotNull EmpiricalDistribution f) {
        System.out.println(f.getSampleStats().toString().replace("\n", " "));
        f.getBinStats().forEach(
                s -> {
                    /*if (s.getN() > 0)*/
                    System.out.println(
                            n4(s.getMin()) + ".." + n4(s.getMax()) + ":\t" + s.getN());
                }
        );
    }


    private static Tensor samplingPriDist(@NotNull Bag<PLink<String>, PLink<String>> b, int batches, int batchSize, int bins) {

        assert(bins > 1);

        Set<String> hit = new TreeSet();
        Frequency hits = new Frequency();
        ArrayTensor f = new ArrayTensor(bins);
        assertFalse(b.isEmpty());
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 0; i < batches; i++) {
            b.sample(rng, batchSize, x -> {
                f.data[Util.bin(b.pri(x), bins)]++;
                String s = x.id;
                hits.addValue(s);
                hit.add(s);
            });
        }

        int total = batches * batchSize;
        assertEquals(total, Util.sum(f.data), 0.001f);

        if (hits.getUniqueCount() != b.size()) {

            System.out.println(hits.getUniqueCount() + " != " + b.size());

            Set<String> items = b.stream().map(stringPLink -> stringPLink.id).collect(Collectors.toSet());
            items.removeAll(hit);
            System.out.println("not hit: " + items);

            System.out.println(hits);
            fail("all elements must have been sampled at least once");
        }

        

        return f.scale(1f / total);
    }

    public static void testRemoveByKey(Bag<String, PriReference<String>> a) {

        a.put(new PLink("x", 0.1f));
        a.commit();
        assertEquals(1, a.size());

        a.remove("x");
        a.commit();
        assertEquals(0, a.size());
        assertTrue(a.isEmpty());
        if (a instanceof ArrayBag) {
            assertTrue(((ArrayBag) a).listCopy().isEmpty());
            assertTrue(((ArrayBag) a).keySet().isEmpty());
        }

    }


    static void testBagSamplingDistributionLinear(Bag<PLink<String>, PLink<String>> bag, float batchSizeProp) {
        fillLinear(bag, bag.capacity());
        testBagSamplingDistribution(bag, batchSizeProp);
    }
    static void testBagSamplingDistributionRandom(ArrayBag<PLink<String>, PLink<String>> bag, float batchSizeProp) {
        fillRandom(bag);
        testBagSamplingDistribution(bag, batchSizeProp);
    }

    static void testBagSamplingDistribution(Bag<PLink<String>, PLink<String>> bag, float batchSizeProp) {


        

        int cap = bag.capacity();
        int batchSize = (int)Math.ceil(batchSizeProp * cap);
        int batches = cap * 1000 / batchSize;

        Tensor f1 = samplingPriDist(bag, batches, batchSize, Math.min(10,Math.max(2, cap/2)));

        String h = "cap=" + cap + " total=" + (batches * batchSize);
        System.out.println(h + ":\n\t" + f1.tsv2());
        System.out.println();

        float[] ff = f1.get();

        

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




























    private float maxMinRatio(@NotNull EmpiricalDistribution d) {
        List<SummaryStatistics> bins = d.getBinStats();
        return ((float) bins.get(bins.size() - 1).getN() / (bins.get(0).getN()));
    }


    public static void testPutMinMaxAndUniqueness(Bag<Integer, PriReference<Integer>> a) {
        float pri = 0.5f;
        int n = a.capacity() * 16; 


        for (int i = 0; i < n; i++) {
            a.put(new PLink((i), pri));
        }

        a.commit(null); 
        assertEquals(a.capacity(), a.size());
        if (a instanceof ArrayBag) assertSorted((ArrayBag)a);

        

        

        List<Integer> keys = new FasterList(a.capacity());
        a.forEachKey(keys::add);
        assertEquals(a.size(), keys.size());
        assertEquals(new HashSet(keys).size(), keys.size());

        assertEquals(pri, a.priMin(), 0.01f);
        assertEquals(a.priMin(), a.priMax(), 0.08f);

        if (a instanceof HijackBag)
            assertTrue(((HijackBag)a).density() > 0.75f);
    }

    public static void populate(Bag<String, PriReference<String>> b, Random rng, int count, int dimensionality, float minPri, float maxPri, float qua) {
        populate(b, rng, count, dimensionality, minPri, maxPri, qua, qua);
    }

    private static void populate(Bag<String, PriReference<String>> a, Random rng, int count, int dimensionality, float minPri, float maxPri, float minQua, float maxQua) {
        float dPri = maxPri - minPri;
        for (int i = 0; i < count; i++) {
            a.put(new PLink(
                    "x" + rng.nextInt(dimensionality),
                    rng.nextFloat() * dPri + minPri)
            );
        }
        a.commit(null);
        if (a instanceof ArrayBag) assertSorted((ArrayBag)a);
    }

    /**
     * fill it exactly to capacity
     */
    public static void fillLinear(Bag<PLink<String>, PLink<String>> bag, int c) {
        assertTrue(bag.isEmpty());

        
        for (int i = c-1; i >= 0; i--) {
            PLink inserted = bag.put(new PLink(i + "x", (i + 0.5f) / c)); 
            if (inserted==null) {
                fail("");
            }
        }
        bag.commit(null);
        assertEquals(c, bag.size());
        assertEquals(0.5f / c, bag.priMin(), 0.03f);
        assertEquals(1 - 1f/(c*2f), bag.priMax(), 0.03f); 
        if (bag instanceof ArrayBag) assertSorted((ArrayBag)bag);
    }
    private static void fillRandom(ArrayBag<PLink<String>, PLink<String>> bag) {
        assertTrue(bag.isEmpty());

        int c = bag.capacity();

        Random rng = new XoRoShiRo128PlusRandom(1);

        
        for (int i = c-1; i >= 0; i--) {
            PLink inserted = bag.put(new PLink(i + "x", rng.nextFloat()));
            assertTrue(inserted!=null);
            assertSorted(bag);
        }
        bag.commit(null);
        assertEquals(c, bag.size());
        assertSorted(bag);
    }














































































































































































































































































































































































































































}
