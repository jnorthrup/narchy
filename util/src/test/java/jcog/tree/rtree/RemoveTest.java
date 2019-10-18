package jcog.tree.rtree;

import jcog.tree.rtree.rect.RectDouble;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RemoveTest {

    @Test
    void testRemoveRebalance() {
        final int LEAF_CAP = 3;
        RTree<RectDouble> r = RTree2DTest.createRect2DTree(Spatialization.DefaultSplits.AXIAL, LEAF_CAP);

        RectDouble[] rects = RTree2DTest.generateRandomRects(6);

        for (RectDouble x : rects)
            r.add(x);

        assertEquals(rects.length, r.size());
        assertEquals(1, r.streamNodes().filter(x ->x instanceof RBranch).count());
        assertEquals(2, r.streamNodes().filter(x ->x instanceof RLeaf).count());
        r.streamNodesRecursively().forEach(x1 -> System.out.println(x1.getClass() + " " + x1.size()));
        assertEquals(6, r.streamNodesRecursively().count());

        {
            boolean removed = r.remove(rects[0]);
            assertTrue(removed);
            assertEquals(rects.length - 1, r.size());

            assertFalse(r.remove(rects[0])); //already removed
        }

        assertTrue(r.remove(rects[4]));
        assertTrue(r.remove(rects[5]));

        assertEquals(LEAF_CAP, r.size());
        r.stats().print();

        assertEquals(0, r.streamNodesRecursively().filter(x ->x instanceof RBranch).count());
        assertEquals(1, r.streamNodesRecursively().filter(x ->x instanceof RLeaf).count());

    }
}
