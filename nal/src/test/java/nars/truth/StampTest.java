package nars.truth;

import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.truth.Stamp.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author me
 */


public class StampTest {

    static long[] a(long... x) {
        return x;
    }

    @Test
    public void testOverlap() {


        assertTrue(overlapping(a(1, 2), a(2)));
        assertTrue(overlapping(a(1), a(1, 2)));
        assertFalse(overlapping(a(1), a(2)));
        assertFalse(overlapping(a(2), a(1)));
        assertFalse(overlapping(a(1, 2), a(3, 4)));
        assertTrue(overlapping(a(1, 2), a(2, 3)));
        assertTrue(overlapping(a(2, 3), a(1, 2)));
        assertFalse(overlapping(a(2, 3), a(1)));

        assertFalse(overlapping(a(1), a(2, 3, 4, 5, 6)));
        assertFalse(overlapping(a(2, 3, 4, 5, 6), a(1)));


    }

    @Test public void testStampZipForward() {
        assertEquals(
                Arrays.toString(new long[] { 7, 8, 12, 13 }),
                Arrays.toString(zipForward(
                        new long[] { 1, 2, 8, 12},
                        new long[] { 3, 4, 7, 13}, 4))
        );
    }

    @Disabled
    @Test public void testStampZipReverse() {

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

        //no duplicates
        assertArrayEquals(
            new long[] { 0, 1, 2, 3 },
                zipReverse(new long[] { 0, 1,2 }, new long[] { 2, 3, 4 }, 4)
        );
    }

    @Test public void directionInvariance() {
        //this one should behave the same regardless of the direction (since there is enough space)
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

    @NotNull public static long[] zipReverse(@NotNull long[] a, @NotNull long[] b, int i) {
        return zip(a, b, 0.5f, i, false);
    }
    @NotNull public static long[] zipForward(@NotNull long[] a, @NotNull long[] b, int i) {
        return zip(a, b, 0.5f, i, true);
    }
    @NotNull public static long[] zipForward(@NotNull long[] a, @NotNull long[] b, float aToB, int i) {
        return zip(a, b, aToB, i, true);
    }

    @Test
    public void testStampToSetArray() {
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
    public void testStampReversePreservesOldestEvidence() {
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


    @Test public void testStampZipForwardWeighted() {

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
                Arrays.toString(zipForward(a,b, 0.95f, 4)) //same as 0.75, at minimum includes 1 (not 0) from either
        );
        assertEquals(
                Arrays.toString(new long[] { 4, 7, 12, 13 }),
                Arrays.toString(zipForward(a,b, 0.2f, 4))
        );

    }

    @Test public void testStampZipForwardWeighted2() {

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
    @Test public void testStampZipForwardWeighted3() {

        //imbalanced
        long[] a = {0, 2};
        long[] b = {1, 3, 5, 7};

        assertEquals(
                Arrays.toString(new long[] { 0, 2, 3, 5, 7 }),
                Arrays.toString(zipForward(a,b, 0.5f, 5))
        );
        assertEquals(
                Arrays.toString(new long[] { 0, 2, 3, 5, 7 }), //same as before
                Arrays.toString(zipForward(a,b, 0.8f, 5))
        );
        assertEquals(
                Arrays.toString(new long[] { 1, 2, 3, 5, 7 }),
                Arrays.toString(zipForward(a,b, 0.2f, 5))
        );

    }

    @Test public void testOverlapFractionIndependent() {
        assertEquals(0f, Stamp.overlapFraction(a(1), a(3)), 0.01f);
        assertEquals(0f, Stamp.overlapFraction(a(1, 2), a(3)), 0.01f);
        assertEquals(0f, Stamp.overlapFraction(a(1, 2), a(3, 4)), 0.01f);
    }
    @Test public void testOverlapFraction2() {
        assertEquals(1/2f, Stamp.overlapFraction(a(1,2), a(2,3)), 0.01f);
        assertEquals(1f, Stamp.overlapFraction(a(1,2), a(1,2,3)), 0.01f);

        //12345
        //------
        //123345
        assertEquals(1/3f, Stamp.overlapFraction(a(1,2,3), a(3,4,5)), 0.01f);
        assertEquals(2/3f, Stamp.overlapFraction(a(1,2,3,4), a(3,4,5)), 0.01f);

        //one is completely subsumed in another
        assertEquals(1/2f, Stamp.overlapFraction(a(1,2), a(2)), 0.01f);
        assertEquals(3/4f, Stamp.overlapFraction(a(1,2,3,4), a(2,3,4)), 0.01f);
    }

}
