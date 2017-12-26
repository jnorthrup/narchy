package jcog.tree.rtree;

import jcog.tree.rtree.rect.RectDouble2D;
import jcog.tree.rtree.util.Stats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SplitTest {

        /**
     * Adds many random entries to trees of different types and confirms that
     * no entries are lost during insert/split.
     */
    @Test
    public void randomEntryTest() {

        int entryCount = 25000;
        final RectDouble2D[] rects = RTree2DTest.generateRandomRects(entryCount);

        for (Spatialization.DefaultSplits s : Spatialization.DefaultSplits.values()) {
            for (int min : new int[]{2, 3, 4}) {
                for (int max : new int[]{min+1, 8}) {


                    final RTree<RectDouble2D> rTree = RTree2DTest.createRect2DTree(s, min, max);
                    int i = 0;
                    for (RectDouble2D r : rects) {
                        boolean added = rTree.add(r);
                        if (!added) {
                            rTree.add(r); //for debugging: try again and see what happened
                            fail("");
                        }
                        assertTrue(added);
                        assertEquals(++i, rTree.size());
                        //assertEquals(i, rTree.stats().getEntryCount());

                        boolean tryAddingAgainToTestForNonMutation = rTree.add(r);
                        if (tryAddingAgainToTestForNonMutation) {
                            rTree.add(r); //for debugging: try again and see what happened
                            fail("");
                        }
                        assertFalse(tryAddingAgainToTestForNonMutation, i + "==?" + rTree.size()); //reinsertion of existing element will not affect size and will return false here
                        assertEquals(i, rTree.size()); //reinsertion should cause no change in size
                        //assertEquals(i, rTree.stats().getEntryCount());
                    }

                    assertEquals(entryCount, rTree.size());

                    final Stats stats = rTree.stats();
                    assertEquals(entryCount, stats.getEntryCount());

                    stats.print(System.out);

                }
            }
        }
    }

}
