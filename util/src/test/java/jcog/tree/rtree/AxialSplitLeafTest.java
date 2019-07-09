package jcog.tree.rtree;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jcog.tree.rtree.rect.RectDouble;
import jcog.tree.rtree.util.Stats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by jcovert on 6/12/15.
 */
class AxialSplitLeafTest {

    private static final Spatialization.DefaultSplits TYPE = Spatialization.DefaultSplits.AXIAL;

    /**
     * Adds enough entries to force a single split and confirms that
     * no entries are lost.
     */
    @Test
    void basicSplitTest() {

        RTree<RectDouble> rTree = RTree2DTest.createRect2DTree(TYPE);
        rTree.add(new RectDouble(0, 0, 1, 1));
        rTree.add(new RectDouble(1, 1, 2, 2));
        rTree.add(new RectDouble(2, 2, 3, 3));
        rTree.add(new RectDouble(3, 3, 4, 4));
        rTree.add(new RectDouble(4, 4, 5, 5));
        rTree.add(new RectDouble(5, 5, 6, 6));
        rTree.add(new RectDouble(6, 6, 7, 7));
        rTree.add(new RectDouble(7, 7, 8, 8));
        
        rTree.add(new RectDouble(8, 8, 9, 9));

        Stats stats = rTree.stats();

        assertTrue(stats.getMaxDepth() == 1, "Unexpected max depth after basic split");
        assertTrue(stats.getBranchCount() == 1, "Unexpected number of branches after basic split");
        assertTrue(stats.getLeafCount() == 2, "Unexpected number of leaves after basic split");
        assertTrue(stats.getEntriesPerLeaf() == 4.5, "Unexpected number of entries per leaf after basic split");
    }


    @Test
    void splitCorrectnessTest() {

        RTree<RectDouble> rTree = RTree2DTest.createRect2DTree(4, TYPE);
        rTree.add(new RectDouble(0, 0, 3, 3));
        rTree.add(new RectDouble(1, 1, 2, 2));
        rTree.add(new RectDouble(2, 2, 4, 4));
        rTree.add(new RectDouble(4, 0, 5, 1));
        
        rTree.add(new RectDouble(0, 2, 1, 4));

        RBranch root = (RBranch) rTree.root();
        RNode<RectDouble>[] children = root.data;
        int childCount = 0;
        for(RNode c : children) {
            if (c != null) {
                childCount++;
            }
        }
        assertEquals( 2, childCount, "Expected different number of children after split");

        {
            RNode<RectDouble> child1 = children[0];
            RectDouble child1Mbr = (RectDouble) child1.bounds();
            RectDouble expectedChild1Mbr = new RectDouble(0, 0, 3, 4);
            assertEquals(2, child1.size(), "Child 1 size incorrect after split");
            assertEquals(expectedChild1Mbr, child1Mbr, "Child 1 mbr incorrect after split");
        }

        {
            RNode<RectDouble> child2 = children[1];
            RectDouble child2Mbr = (RectDouble) child2.bounds();
            RectDouble expectedChild2Mbr = new RectDouble(1, 0, 5, 4);
            assertEquals(3, child2.size(), "Child 2 size incorrect after split");
            assertEquals(expectedChild2Mbr, child2Mbr, "Child 2 mbr incorrect after split");
        }
    }

    /**
     * Adds several overlapping rectangles and confirms that no entries
     * are lost during insert/split.
     */
    @Test
    void overlappingEntryTest() {

        final RTree<RectDouble> rTree = RTree2DTest.createRect2DTree(TYPE);
        rTree.add(new RectDouble(0, 0, 1, 1));
        rTree.add(new RectDouble(0, 0, 2, 2));
        rTree.add(new RectDouble(0, 0, 2.1, 2));
        rTree.add(new RectDouble(0, 0, 3, 3));
        rTree.add(new RectDouble(0, 0, 3.1, 3));

        rTree.add(new RectDouble(0, 0, 4, 4));
        rTree.add(new RectDouble(0, 0, 5, 5));
        rTree.add(new RectDouble(0, 0, 6, 6));
        rTree.add(new RectDouble(0, 0, 7, 7));
        rTree.add(new RectDouble(0, 0, 7.1, 7));

        rTree.add(new RectDouble(0, 0, 8, 8));
        rTree.add(new RectDouble(0, 0, 9, 9));
        rTree.add(new RectDouble(0, 1, 2, 2));
        rTree.add(new RectDouble(0, 1, 3, 3));
        rTree.add(new RectDouble(0, 1, 4, 4));

        rTree.add(new RectDouble(0, 1, 4.1, 4));
        rTree.add(new RectDouble(0, 1, 5, 5));

        
        final int expectedEntryCount = 17;

        final Stats stats = rTree.stats();
        assertEquals( expectedEntryCount, stats.size(), "Unexpected number of entries in " + TYPE + " split tree: " + stats.size() + " entries - expected: " + expectedEntryCount + " actual: " + stats.size());
    }

    /**
     * Adds many random entries and confirm that no entries
     * are lost during insert/split.
     */
    @ParameterizedTest
    @ValueSource(ints = {2,3,4,5,8})
    void randomEntryTest(int maxLeaf) {

        final int entryCount = 10000;
        final RectDouble[] rects = RTree2DTest.generateRandomRects(entryCount);

        final RTree<RectDouble> rTree = RTree2DTest.createRect2DTree(maxLeaf, TYPE);
        for (int i = 0; i < rects.length; i++)
            rTree.add(rects[i]);

        final Stats stats = rTree.stats();
        assertTrue(Math.abs(entryCount - stats.size()) == 0,
                "Unexpected number of entries in " + TYPE + " split tree: " + stats.size() + " entries - expected: " + entryCount + " actual: " + stats.size() /* in case of duplicates */);
        stats.print(System.out);
    }

    /**
     * This test previously caused a StackOverflowException on LINEAR leaf.
     * It has since been fixed, but keeping the test here to ensure this leaf type
     * never falls victim to the same issue.
     */
    @Test
    void causeLinearSplitOverflow() {
        final RTree<RectDouble> rTree = RTree2DTest.createRect2DTree(TYPE);
        final Random rand = new Random(13);
        for (int i = 0; i < 500; i++) {
            final int x1 = rand.nextInt(10);
            final int y1 = rand.nextInt(10);
            final int x2 = x1 + rand.nextInt(200);
            final int y2 = y1 + rand.nextInt(200);

            rTree.add(new RectDouble(x1, y1, x2, y2));
        }
        final Stats stats = rTree.stats();
        stats.print(System.out);
    }


}
