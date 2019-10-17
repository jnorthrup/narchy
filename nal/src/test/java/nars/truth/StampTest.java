package nars.truth;

import nars.$;
import nars.task.NALTask;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static nars.Op.QUESTION;
import static nars.truth.Stamp.toSetArray;
import static nars.truth.Stamp.zip;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author me
 */


class StampTest {

    private static long[] a(long... x) {
        return x;
    }

    @Test
    void testOverlap() {


        assertTrue(Stamp.overlapsAny(a(1, 2), a(2)));
        assertTrue(Stamp.overlapsAny(a(1), a(1, 2)));
        assertFalse(Stamp.overlapsAny(a(1), a(2)));
        assertFalse(Stamp.overlapsAny(a(2), a(1)));
        assertFalse(Stamp.overlapsAny(a(1, 2), a(3, 4)));
        assertTrue(Stamp.overlapsAny(a(1, 2), a(2, 3)));
        assertTrue(Stamp.overlapsAny(a(2, 3), a(1, 2)));
        assertFalse(Stamp.overlapsAny(a(2, 3), a(1)));

        assertFalse(Stamp.overlapsAny(a(1), a(2, 3, 4, 5, 6)));
        assertFalse(Stamp.overlapsAny(a(2, 3, 4, 5, 6), a(1)));


    }

    @Test
    void testStampZipForward() {
        assertEquals(
                Arrays.toString(new long[] { 7, 8, 12, 13 }),
                Arrays.toString(zipForward(
                        new long[] { 1, 2, 8, 12},
                        new long[] { 3, 4, 7, 13}, 4))
        );
    }

    @Disabled
    @Test
    void testStampZipReverse() {

        long[] a = {1, 2};
        long[] b = {3, 4};
        int i = 3;
        @NotNull long[] zip = zipReverse(a, b, i);
        assertArrayEquals(
            new long[] { 1, 2, 3 },
                zip
        );



        assertArrayEquals(
            new long[] { 1, 2, 3, 4 },
                zipReverse(new long[] { 1 }, new long[] { 2, 3, 4}, 4)
        );
        assertArrayEquals(
            new long[] { 1, 2, 3, 4 },
                zipReverse(new long[] { 1,2,3 }, new long[] { 4 }, 4)
        );
        assertArrayEquals(
            new long[] { 0, 1, 2, 4 },
                zipReverse(new long[] { 0, 1,2,3 }, new long[] { 4 }, 4)
        );

        
        assertArrayEquals(
            new long[] { 0, 1, 2, 3 },
                zipReverse(new long[] { 0, 1,2 }, new long[] { 2, 3, 4 }, 4)
        );
    }

    @Test
    void directionInvariance() {
        
        final boolean[] both = { false, true };
        for (boolean dir : both) {
            assertArrayEquals(
                    new long[]{1, 2, 3, 4},
                    Stamp.zip(
                            new long[]{1, 2},
                            new long[]{3, 4}, 0.5f, 4, dir)
            );
        }
    }

    private static @NotNull long[] zipReverse(@NotNull long[] a, @NotNull long[] b, int i) {
        return zip(a, b, 0.5f, i, false);
    }
    private static @NotNull long[] zipForward(@NotNull long[] a, @NotNull long[] b, int i) {
        return zip(a, b, 0.5f, i, true);
    }
    private static @NotNull long[] zipForward(@NotNull long[] a, @NotNull long[] b, float aToB, int i) {
        return zip(a, b, aToB, i, true);
    }

    @Test
    void testStampToSetArray() {
        assertEquals(3, toSetArray(new long[]{1, 2, 3}).length);
        assertEquals(2, toSetArray(new long[]{1, 1, 3}).length);
        assertEquals(1, toSetArray(new long[]{1}).length);
        assertEquals(0, toSetArray(new long[]{}).length);
        assertEquals(Arrays.hashCode(toSetArray(new long[]{3, 2, 1})), Arrays.hashCode(toSetArray(new long[]{2, 3, 1})));
        assertTrue(
                Arrays.hashCode(toSetArray(new long[] { 1,2,3 }))
                !=
                Arrays.hashCode(toSetArray(new long[] { 1,1,3 }))
        );
    }

    @Disabled
    @Test
    void testStampReversePreservesOldestEvidence() {
        assertArrayEquals(
                new long[] { 1, 3 },
                zipReverse(new long[] { 1, 2}, new long[] { 3, 4}, 2)
        );

        assertArrayEquals(
                new long[] { 1, 2, 3, 4 },
                zipReverse(new long[] { 1, 2, 8, 12}, new long[] { 3, 4, 7, 13}, 4)
        );


        long[] a = { 1, 2, 10, 11 };
        long[] b = { 3, 5, 7, 22 };
        assertEquals(
                new LongArrayList(1, 2, 3, 5),
                new LongArrayList(zipReverse(a, b, 4)));
    }


    @Test
    void testStampZipForwardWeighted() {

        long[] a = {1, 2, 8, 12};
        long[] b = {3, 4, 7, 13};

        assertEquals(
                Arrays.toString(new long[] { 7, 8, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.5f, 4))
        );
        assertEquals(
                Arrays.toString(new long[] { 2, 8, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.8f, 4))
        );
        assertEquals(
                Arrays.toString(new long[] { 2, 8, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.95f, 4)) 
        );
        assertEquals(
                Arrays.toString(new long[] { 4, 7, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.2f, 4))
        );

    }

    @Test
    void testStampZipForwardWeighted2() {

        long[] a = {0, 2, 4, 6, 8, 10, 12};
        long[] b = {1, 3, 5, 7, 9, 11, 13};

        assertEquals(
                Arrays.toString(new long[] { 7, 8, 9, 10, 11, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.5f, 7))
        );
        assertEquals(
                Arrays.toString(new long[] { 4, 6, 8, 10, 11, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.8f, 7))
        );
        assertEquals(
                Arrays.toString(new long[] { 2, 4, 6, 8, 10, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.95f, 7))
        );
        assertEquals(
                Arrays.toString(new long[] { 7, 8, 9, 10, 11, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.35f, 7))
        );
        assertEquals(
                Arrays.toString(new long[] { 3, 5, 7, 9, 11, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.1f, 7))
        );
    }
    @Test
    void testStampZipForwardWeighted3() {

        
        long[] a = {0, 2};
        long[] b = {1, 3, 5, 7};

        assertEquals(
                Arrays.toString(new long[] { 0, 2, 3, 5, 7 }),
                Arrays.toString(zipForward(a,b, 0.5f, 5))
        );
        assertEquals(
                Arrays.toString(new long[] { 0, 2, 3, 5, 7 }), 
                Arrays.toString(zipForward(a,b, 0.8f, 5))
        );
        assertEquals(
                Arrays.toString(new long[] { 1, 2, 3, 5, 7 }),
                Arrays.toString(zipForward(a,b, 0.2f, 5))
        );

    }

    @Test
    void testOverlapFractionIndependent() {
        assertEquals(0f, Stamp.overlapFraction(a(1), a(3)), 0.01f);
        assertEquals(0f, Stamp.overlapFraction(a(1, 2), a(3)), 0.01f);
        assertEquals(0f, Stamp.overlapFraction(a(1, 2), a(3, 4)), 0.01f);
    }
    @Test
    void testOverlapFraction2() {
        assertEquals(1 / 2f, Stamp.overlapFraction(a(1, 2), a(2, 3)), 0.01f);
        assertEquals(1f, Stamp.overlapFraction(a(1, 2), a(1, 2, 3)), 0.01f);
    }
    @Test
    void testOverlapFraction3() {
        
        
        
        assertEquals(1/3f, Stamp.overlapFraction(a(1,2,3), a(3,4,5)), 0.01f);
        assertEquals(2/3f, Stamp.overlapFraction(a(1,2,3,4), a(3,4,5)), 0.01f);

        
        assertEquals(1f, Stamp.overlapFraction(a(1,2), a(2)), 0.01f);
        assertEquals(1f, Stamp.overlapFraction(a(1,2,3,4), a(2,3,4)), 0.01f);
    }

    @Test
    void testDetectOverlapAfterZipOverflow() {
        ObjectFloatPair<long[]> p = Stamp.zip(List.of(
                NALTask.the($.the("x"), QUESTION, null, (long) 0, (long) 0, (long) 0, new long[]{1, 2, 8, 9}),
                NALTask.the($.the("y"), QUESTION, null, (long) 0, (long) 0, (long) 0, new long[]{3, 4, 5, 8})
        ), 2);
        assertEquals("[8, 9]", Arrays.toString(p.getOne()));
        assertEquals(1/8f, p.getTwo(), 0.01f);
    }

}
