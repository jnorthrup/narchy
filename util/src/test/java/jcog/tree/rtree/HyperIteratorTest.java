package jcog.tree.rtree;

import jcog.tree.rtree.rect.RectDouble2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HyperIteratorTest {
    @Test
    void test1() {
        final int entryCount = 128;
        final RectDouble2D[] rects = RTree2DTest.generateRandomRects(entryCount);

        final RTree<RectDouble2D> r = RTree2DTest.createRect2DTree(2, 4, Spatialization.DefaultSplits.AXIAL);
        for (int i = 0; i < rects.length; i++)
            r.add(rects[i]);

        HyperIterator<RectDouble2D> rr = new HyperIterator(r.model, r.root(), new RectDouble2D(260.0,21.0,584.0,344.0),
                Space.BoundsMatch.CONTAINS, null);
        int count = 0;
        while (rr.hasNext()) {
            RectDouble2D x = rr.next();
            assertNotNull(x);
            count++;
            System.out.println(x);
        }
        assertTrue(count > 5);

    }
}
