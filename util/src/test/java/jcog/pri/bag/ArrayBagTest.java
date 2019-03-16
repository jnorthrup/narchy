package jcog.pri.bag;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.UnitPri;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.DoubleSupplier;

import static jcog.pri.bag.BagTest.testBasicInsertionRemoval;
import static jcog.pri.op.PriMerge.plus;
import static org.junit.jupiter.api.Assertions.*;

class ArrayBagTest {


    private ArrayBag<PLink<String>, PLink<String>> curveBag(int n, PriMerge mergeFunction) {
        return new PLinkArrayBag(n, mergeFunction, new HashMap<>(n));
    }

    @Test
    void testBasicInsertionRemovalArray() {
        testBasicInsertionRemoval(new PLinkArrayBag<>(1, plus, new HashMap<>(1)));
    }


    @Test
    void testBudgetMerge() {
        PLinkArrayBag<String> a = new PLinkArrayBag<String>(4, plus, new HashMap<>(4));
        assertEquals(0, a.size());

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("x", 0.1f));
        a.commit(null);
        assertEquals(1, a.size());


        PriReference<String> agx = a.get("x");
        UnitPri expect = new UnitPri(0.2f);
        assertTrue(Util.equals(expect.priElseNeg1(), agx.priElseNeg1(), 0.01f), agx + "==?==" + expect);

    }

    @NotNull ArrayBag<PLink<String>, PLink<String>> populated(int n, @NotNull DoubleSupplier random) {


        ArrayBag<PLink<String>, PLink<String>> a = curveBag(n, plus);


        
        for (int i = 0; i < n; i++) {
            a.put(new PLink("x" + i, (float) random.getAsDouble()));
        }

        a.commit();
        

        return a;

    }

    @Test
    void testSort() {
        PLinkArrayBag a = new PLinkArrayBag(4, plus, new HashMap<>(4));

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("y", 0.2f));

        a.commit(null);

        Iterator<PriReference<String>> ii = a.iterator();
        assertEquals("y", ii.next().get());
        assertEquals("x", ii.next().get());

        assertEquals("[$0.2 y, $0.1 x]", a.listCopy().toString());

        System.out.println(a.listCopy());

        a.put(new PLink("x", 0.2f));

        System.out.println(a.listCopy());

        a.commit();

        System.out.println(a.listCopy());

        assertTrue(a.listCopy().toString().contains("x,"));
        assertTrue(a.listCopy().toString().contains("y]"));

        ii = a.iterator();
        assertEquals("x", ii.next().get());
        assertEquals("y", ii.next().get());

    }

    @Test
    void testCapacity() {
        PLinkArrayBag a = new PLinkArrayBag(2, plus, new HashMap<>(2));

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("y", 0.2f));
        a.print();
        System.out.println();
        assertEquals(2, a.size());

        a.commit(null);
        assertSorted(a);
        assertEquals(0.1f, a.priMin(), 0.01f);

        a.put(new PLink("z", 0.05f));
        a.print();
        System.out.println();
        assertEquals(2, a.size());
        assertTrue(a.contains("x") && a.contains("y"));
        assertFalse(a.contains("z"));

    }

    @Test
    void testRemoveByKey() {
        BagTest.testRemoveByKey(new PLinkArrayBag(2, plus, new HashMap<>(2)));
    }

    @Disabled
    @Test
    void testInsertOrBoostDoesntCauseSort() {
        final int[] sorts = {0};
        @NotNull ArrayBag<PLink<String>, PLink<String>> x = new PLinkArrayBag(4, PriMerge.plus, new HashMap<>()) {
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

        assertSorted(x);

    }

    static void assertSorted(ArrayBag x) {
        assertTrue(x.items.isSorted(x), ()-> Joiner.on("\n").join(x));
    }

    @Test
    void testCurveBagDistributionSmall() {
        for (int cap : new int[] { 2, 3, 4, 5, 6, 7, 8 }) {
            for (float batchSizeProp : new float[]{0.001f, 0.1f, 0.3f }) {

                BagTest.testBagSamplingDistributionLinear(curveBag(cap, PriMerge.plus), batchSizeProp);

                BagTest.testBagSamplingDistributionSquashed(curveBag(cap, PriMerge.plus), batchSizeProp);

                BagTest.testBagSamplingDistributionCurved(curveBag(cap, PriMerge.plus), batchSizeProp);

            }
        }
    }

    @Test
    void testCurveBagDistribution8_BiggerBatch() {
        for (float batchSizeProp : new float[]{0.5f}) {

            BagTest.testBagSamplingDistributionLinear(curveBag(8, PriMerge.plus), batchSizeProp);

            BagTest.testBagSamplingDistributionSquashed(curveBag(8, PriMerge.plus), batchSizeProp);
        }
    }

    @Test
    void testCurveBagDistribution32() {
        for (float batchSizeProp : new float[]{ 0.05f, 0.1f, 0.2f}) {
            BagTest.testBagSamplingDistributionLinear(curveBag(32, PriMerge.plus), batchSizeProp);
        }
    }

    @Test
    void testCurveBagDistribution64() {
        for (float batchSizeProp : new float[]{ 0.05f, 0.1f, 0.2f}) {
            BagTest.testBagSamplingDistributionLinear(curveBag(64, PriMerge.plus), batchSizeProp);
            BagTest.testBagSamplingDistributionRandom(curveBag(64, PriMerge.plus), batchSizeProp);
        }
    }

    @Test
    void testCurveBagDistribution32_64__small_batch() {
        for (int cap : new int[] { 32, 64 }) {
            for (float batchSizeProp : new float[]{ 0.001f }) {
                BagTest.testBagSamplingDistributionLinear(curveBag(cap, PriMerge.plus), batchSizeProp);
            }
        }
    }

}
