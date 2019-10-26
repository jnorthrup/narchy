package jcog.pri.bag;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.pri.*;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.HijackBag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.bag.impl.hijack.PLinkHijackBag;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static jcog.Texts.*;
import static jcog.pri.bag.ArrayBagTest.assertSorted;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author me
 */
public class BagTest {



    public static void testBasicInsertionRemoval(Bag<String, PriReference<String>> c) {


        assertEquals(1, c.capacity());
        if (!(c instanceof PLinkHijackBag)) {
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
        assertTrue(Util.equals(Prioritized.Companion.getZero().priElseNeg1(), x.priElseNeg1(), 0.01f));

    }


    public static Random rng() {
        return new XoRoShiRo128PlusRandom(1);
    }































    public static void printDist(@NotNull EmpiricalDistribution f) {
        System.out.println(f.getSampleStats().toString().replace("\n", " "));
        /*if (s.getN() > 0)*/
        for (SummaryStatistics s : f.getBinStats()) {
            System.out.println(
                    INSTANCE.n4(s.getMin()) + ".." + INSTANCE.n4(s.getMax()) + ":\t" + s.getN());
        }
    }


    public static Tensor samplingPriDist(@NotNull Bag<PLink<String>, PLink<String>> b, int samples, int bins) {
        return samplingPriDist(b, 1, samples, bins);
    }

    public static Tensor samplingPriDist(@NotNull Bag<PLink<String>, PLink<String>> b, int batches, int batchSize, int bins) {

        assert(bins > 1);

        Frequency hits = new Frequency();
        ArrayTensor f = new ArrayTensor(bins);
        assertFalse(b.isEmpty());
        Random rng = new XoRoShiRo128PlusRandom(1);
        float min = b.priMin(), max = b.priMax(), range = max-min;
        Set<String> hit = new TreeSet();
        for (int i = 0; i < batches; i++) {
            b.sample(rng, batchSize, new Consumer<PLink<String>>() {
                @Override
                public void accept(PLink<String> x) {
                    f.data[Util.bin(b.pri(x), bins)]++;
                    String s = x.get();
                    hits.addValue(s);
                    hit.add(s);
                }
            });
        }

        int total = batches * batchSize;
        assertEquals(total, Util.sum(f.data), 0.001f);

        if (hits.getUniqueCount() != b.size()) {

            System.out.println(hits.getUniqueCount() + " != " + b.size());

            Set<String> items = b.stream().map(NLink::get).collect(Collectors.toSet());
            items.removeAll(hit);
            System.out.println("not hit: " + items);

            System.out.println(hits);
            fail("all elements must have been sampled at least once");
        }

        return f.multiplyEach(1f / total);
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
            assertTrue(((PriReferenceArrayBag) a).listCopy().isEmpty());
            //assertTrue(((PLinkArrayBag) a).keySet().isEmpty());
        }

    }


    static void testBagSamplingDistributionLinear(Bag<PLink<String>, PLink<String>> bag, float batchSizeProp) {
        fillLinear(bag, bag.capacity());
        testBagSamplingDistribution(bag, batchSizeProp);
    }

    static void testBagSamplingDistributionSquashed(Bag<PLink<String>, PLink<String>> bag, float batchSizeProp) {
        fill(bag, bag.capacity(), new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                return (x / 2f) + 0.5f;
            }
        });
        testBagSamplingDistribution(bag, batchSizeProp);
    }

    static void testBagSamplingDistributionCurved(Bag<PLink<String>, PLink<String>> bag, float batchSizeProp) {
        fill(bag, bag.capacity(), new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                return 1 - (1 - x) * (1 - x);
            }
        });
        testBagSamplingDistribution(bag, batchSizeProp);
    }

    static void testBagSamplingDistributionRandom(ArrayBag<PLink<String>, PLink<String>> bag, float batchSizeProp) {
        fillRandom(bag);
        testBagSamplingDistribution(bag, batchSizeProp);
    }

    public static void testBagSamplingDistribution(Bag<PLink<String>, PLink<String>> bag, float batchSizeProp) {


        

        int cap = bag.capacity();
        int batchSize = (int)Math.ceil(batchSizeProp * cap);

        if (bag.size() < 3)
            return; //histogram tests wont apply

        int bins = Math.max(8, Math.max(3, cap/4));

        int batches = cap * 1000 / batchSize;
        Tensor f1 = samplingPriDist(bag, batches, batchSize, bins);

        String h = "cap=" + cap + " total=" + (batches * batchSize);
        System.out.println(h + ":\n\t" + f1.tsv2());
        System.out.println();

        float[] ff = f1.snapshot();
        System.out.println(Arrays.toString(ff));


        int n = ff.length;
        if (ff[n-1] == 0)
            n--; //skip last empty histogram cell HACK
        if (ff[n-1] == 0)
            n--; //skip last empty histogram cell HACK

        float orderThresh = 0.1f;
        for (int j = 0; j < n; j++) {
//            assertTrue(ff[j] > 0); //no zero bins
            for (int i = j+1; i < n; i++) {
                float diff = ff[j] - ff[i];
                boolean unordered = diff > orderThresh;
                if (unordered) {
//                    bag.print();
                    fail(new Supplier<String>() {
                        @Override
                        public String get() {
                            return "sampling distribution not ordered. contents=" + bag;
                        }
                    });
                }
            }
        }

        final float MIN_RATIO = 1.5f; 

        for (int lows : n > 4 ? new int[] { 0, 1} : new int[] { 0 }  ) {
            for (int highs : n > 4 ? new int[] { n -1, n -2} : new int[] { n -1 }  ) {
                if (lows!=highs) {
                    float maxMinRatio = ff[highs] / ff[lows];
                    assertTrue(
                            maxMinRatio > MIN_RATIO,
                            new Supplier<String>() {
                                @Override
                                public String get() {
                                    return maxMinRatio + " ratio between max and min";
                                }
                            }
                    );
                }
            }
        }


        
        
        













    }




























    private static float maxMinRatio(@NotNull EmpiricalDistribution d) {
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

        if (a instanceof ArrayBag)
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
        fill(bag, c, new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                return x;
            }
        });
        assertEquals(1f / (c+1), bag.priMin(), 0.03f);
        assertEquals(1 - 1f/(c+1), bag.priMax(), 0.03f);
    }

    public static void fill(Bag<PLink<String>, PLink<String>> bag, int c, FloatToFloatFunction priCurve) {
        assertTrue(bag.isEmpty());

        
        for (int i = c-1; i >= 0; i--) {
            float x = (i + 1) / (c+1f); //center of index
            PLink inserted = bag.put(new PLink(i + "x", priCurve.valueOf(x)));
            ///assert(inserted!=null);
        }

        bag.commit(null);
        //assertEquals(c, bag.size());
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
