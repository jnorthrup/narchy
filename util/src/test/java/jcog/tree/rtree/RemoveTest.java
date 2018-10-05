package jcog.tree.rtree;

import jcog.tree.rtree.rect.RectDouble;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoveTest {

    @Test
    void testRemoveRebalance() {
        RTree r = RTree2DTest.createRect2DTree(Spatialization.DefaultSplits.AXIAL, 2, 3);

        final RectDouble[] rects = RTree2DTest.generateRandomRects(32);

        for (RectDouble x : rects)
            r.add(x);

        assertEquals(rects.length, r.size());

        boolean removed = r.remove(rects[0]);
        assertTrue(removed);

        assertEquals(rects.length-1, r.size());

    }
}
