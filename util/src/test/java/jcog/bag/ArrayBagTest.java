package jcog.bag;

import jcog.Util;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.math.random.XorShift128PlusRandom;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.DoubleSupplier;

import static jcog.bag.BagTest.testBasicInsertionRemoval;
import static jcog.pri.op.PriMerge.plus;
import static org.junit.jupiter.api.Assertions.*;

public class ArrayBagTest {


    CurveBag<PLink<String>> curveBag(int n, PriMerge mergeFunction) {
        return new CurveBag<>(mergeFunction, new HashMap<>(n),
                new XoRoShiRo128PlusRandom(1),
                //new XorShift128PlusRandom(1),
                //new Random(1),
                n);
    }

    @Test
    public void testBasicInsertionRemovalArray() {
        testBasicInsertionRemoval(new PLinkArrayBag<>(1, plus, new HashMap<>(1)));
    }


    @Test
    public void testBudgetMerge() {
        PLinkArrayBag<String> a = new PLinkArrayBag<String>(4, plus, new HashMap<>(4));
        assertEquals(0, a.size());

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("x", 0.1f));
        a.commit(null);
        assertEquals(1, a.size());


        PriReference<String> agx = a.get("x");
        Pri expect = new Pri(0.2f);
        assertTrue(Util.equals(expect.priElseNeg1(), agx.priElseNeg1(), 0.01f), agx + "==?==" + expect);

    }

    @NotNull
    private CurveBag<PLink<String>> populated(int n, @NotNull DoubleSupplier random) {


        CurveBag<PLink<String>> a = curveBag(n, plus);


        //fill with uniform randomness
        for (int i = 0; i < n; i++) {
            a.put(new PLink("x" + i, (float) random.getAsDouble()));
        }

        a.commit();
        //a.printAll();

        return a;

    }

    @Test
    public void testSort() {
        PLinkArrayBag a = new PLinkArrayBag(4, plus, new HashMap<>(4));

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("y", 0.2f));

        a.commit(null);

        Iterator<PriReference<String>> ii = a.iterator();
        assertEquals("y", ii.next().get());
        assertEquals("x", ii.next().get());

        assertEquals("[$0.2000 y, $0.1000 x]", a.listCopy().toString());

        System.out.println(a.listCopy());

        a.put(new PLink("x", 0.2f));

        System.out.println(a.listCopy());

        a.commit();

        //x should now be ahead
        assertTrue(a.listCopy().toString().contains("x,")); //x first
        assertTrue(a.listCopy().toString().contains("y]")); //y second

        ii = a.iterator();
        assertEquals("x", ii.next().get());
        assertEquals("y", ii.next().get());

    }

    @Test
    public void testCapacity() {
        PLinkArrayBag a = new PLinkArrayBag(2, plus, new HashMap<>(2));

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("y", 0.2f));
        a.print();
        System.out.println();
        assertEquals(2, a.size());

        a.commit(null);
        assertEquals(0.1f, a.priMin(), 0.01f);

        a.put(new PLink("z", 0.05f));
        a.print();
        System.out.println();
        assertEquals(2, a.size());
        assertTrue(a.contains("x") && a.contains("y"));
        assertFalse(a.contains("z"));

    }

    @Test
    public void testRemoveByKey() {
        BagTest.testRemoveByKey(new PLinkArrayBag(2, plus, new HashMap<>(2)));
    }

    @Disabled
    @Test
    public void testInsertOrBoostDoesntCauseSort() {
        final int[] sorts = {0};
        @NotNull CurveBag<PLink<String>> x = new CurveBag(PriMerge.plus, new HashMap<>(), new XorShift128PlusRandom(1), 4) {
            @Override
            protected void sort() {
                sorts[0]++;
                super.sort();
            }
        };

        x.put(new PLink("x", 0.2f));
        x.put(new PLink("y", 0.1f));
        x.put(new PLink("z", 0f));

        assertEquals(0, sorts[0]);

        x.commit();

        assertEquals(0, sorts[0]);

    }

    @Test
    public void testCurveBagDistributionSmall() {
        for (int cap : new int[] { 2, 3, 4, 5, 6, 7, 8 }) {
            for (float batchSizeProp : new float[]{0.001f, 0.1f, 0.3f}) {
                BagTest.testBagSamplingDistributionLinear(curveBag(cap, PriMerge.plus), batchSizeProp);
            }
        }
    }

    @Test
    public void testCurveBagDistribution8_BiggerBatch() {
        for (float batchSizeProp : new float[]{0.5f}) {
            BagTest.testBagSamplingDistributionLinear(curveBag(8, PriMerge.plus), batchSizeProp);
        }
    }

    @Test
    public void testCurveBagDistribution32() {
        for (float batchSizeProp : new float[]{ 0.05f, 0.1f, 0.2f}) {
            BagTest.testBagSamplingDistributionLinear(curveBag(32, PriMerge.plus), batchSizeProp);
        }
    }

    @Test
    public void testCurveBagDistribution64() {
        for (float batchSizeProp : new float[]{ 0.05f, 0.1f, 0.2f}) {
            BagTest.testBagSamplingDistributionLinear(curveBag(64, PriMerge.plus), batchSizeProp);
            BagTest.testBagSamplingDistributionRandom(curveBag(64, PriMerge.plus), batchSizeProp);
        }
    }

    @Test public void testCurveBagDistribution32_64__small_batch() {
        for (int cap : new int[] { 32, 64 }) {
            for (float batchSizeProp : new float[]{ 0.001f }) {
                BagTest.testBagSamplingDistributionLinear(curveBag(cap, PriMerge.plus), batchSizeProp);
            }
        }
    }

}
