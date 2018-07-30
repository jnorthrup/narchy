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

import com.google.common.collect.Iterators;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.point.Double2D;
import jcog.tree.rtree.rect.RectDouble2D;
import jcog.tree.rtree.rect.RectFloatND;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by jcairns on 4/30/15.
 */
class RTree2DTest {

    @Test
    void pointSearchTest() {

        final RTree<Double2D> pTree = new RTree<>(new Double2D.Builder(), 2, 8, Spatialization.DefaultSplits.AXIAL.get());

        for(int i=0; i<10; i++) {
            pTree.add(new Double2D(i, i));
            assertEquals(i+1, pTree.size());
            assertEquals(i+1, Iterators.size(pTree.iterator()));
        }

        final RectDouble2D rect = new RectDouble2D(new Double2D(2,2), new Double2D(8,8));
        final Double2D[] result = new Double2D[10];

        final int n = pTree.containedToArray(rect, result);
        assertEquals(7, n, ()->Arrays.toString(result));

        for(int i=0; i<n; i++) {
            assertTrue(result[i].x >= 2);
            assertTrue(result[i].x <= 8);
            assertTrue(result[i].y >= 2);
            assertTrue(result[i].y <= 8);
        }
    }

    /**
     * Use an small bounding box to ensure that only expected rectangles are returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    void rect2DSearchTest() {

        final int entryCount = 20;

        for (Spatialization.DefaultSplits type : Spatialization.DefaultSplits.values()) {
            RTree<RectDouble2D> rTree = createRect2DTree(2, 8, type);
            for (int i = 0; i < entryCount; i++) {
                rTree.add(new RectDouble2D(i, i, i+3, i+3));
            }

            final RectDouble2D searchRect = new RectDouble2D(5, 5, 10, 10);
            List<RectDouble2D> results = new ArrayList();

            rTree.whileEachIntersecting(searchRect, results::add);
            int resultCount = 0;
            for(int i = 0; i < results.size(); i++) {
                if(results.get(i) != null) {
                    resultCount++;
                }
            }

            final int expectedCount = 9;
            
            assertEquals(expectedCount, resultCount, "[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount);

            Collections.sort(results);

            
            for (int i = 0; i < resultCount; i++) {
                assertTrue(results.get(i).min.x == i + 2 && results.get(i).min.y == i + 2 && results.get(i).max.x == i + 5 && results.get(i).max.y == i + 5, "Unexpected result found");
            }
        }
    }

    /**
     * Use an enormous bounding box to ensure that every rectangle is returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    void rect2DSearchAllTest() {

        final int entryCount = 10000;
        final RectDouble2D[] rects = generateRandomRects(entryCount);

        for (Spatialization.DefaultSplits type : Spatialization.DefaultSplits.values()) {
            RTree<RectDouble2D> rTree = createRect2DTree(2, 8, type);
            for (int i = 0; i < rects.length; i++) {
                rTree.add(rects[i]);
            }

            final RectDouble2D searchRect = new RectDouble2D(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
            RectDouble2D[] results = new RectDouble2D[entryCount];

            final int foundCount = rTree.containedToArray(searchRect, results);
            int resultCount = 0;
            for(int i = 0; i < results.length; i++) {
                if(results[i] != null) {
                    resultCount++;
                }
            }

            final int expectedCount = entryCount;
            assertTrue(Math.abs(expectedCount - foundCount) < 10,
                    "[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount /* in case of duplicates */);
            assertTrue(Math.abs(expectedCount - resultCount) < 10,
                    "[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount /* in case of duplicates */);

        }
    }

    /**
     * Collect stats making the structure of trees of each split type
     * more visible.
     */
    @Disabled
    void treeStructureStatsTest() {

        final int entryCount = 50_000;

        final RectDouble2D[] rects = generateRandomRects(entryCount);
        for (Spatialization.DefaultSplits type : Spatialization.DefaultSplits.values()) {
            RTree<RectDouble2D> rTree = createRect2DTree(2, 8, type);
            for (int i = 0; i < rects.length; i++) {
                rTree.add(rects[i]);
            }

            Stats stats = rTree.stats();
            stats.print(System.out);
        }
    }

    /**
     * Do a search and collect stats on how many nodes we hit and how many
     * bounding boxes we had to evaluate to get all the results.
     *
     * Preliminary findings:
     *  - Evals for QUADRATIC tree increases with size of the search bounding box.
     *  - QUADRATIC seems to be ideal for small search bounding boxes.
     */
    @Disabled
    void treeSearchStatsTest() {

        final int entryCount = 5000;

        final RectDouble2D[] rects = generateRandomRects(entryCount);
        for (Spatialization.DefaultSplits type : Spatialization.DefaultSplits.values()) {
            RTree<RectDouble2D> rTree = createRect2DTree(2, 8, type);
            for (int i = 0; i < rects.length; i++) {
                rTree.add(rects[i]);
            }

            rTree.instrumentTree();

            final RectDouble2D searchRect = new RectDouble2D(100, 100, 120, 120);
            RectDouble2D[] results = new RectDouble2D[entryCount];
            int foundCount = rTree.containedToArray(searchRect, results);

            CounterNode<RectDouble2D> root = (CounterNode<RectDouble2D>) rTree.root();

            System.out.println("[" + type + "] searched " + CounterNode.searchCount + " nodes, returning " + foundCount + " entries");
            System.out.println("[" + type + "] evaluated " + CounterNode.bboxEvalCount + " b-boxes, returning " + foundCount + " entries");
        }
    }

    @Test
    void treeRemovalTest() {
        final RTree<RectDouble2D> rTree = createRect2DTree(Spatialization.DefaultSplits.QUADRATIC);

        RectDouble2D[] rects = new RectDouble2D[1000];
        for(int i = 0; i < rects.length; i++){
            rects[i] = new RectDouble2D(i, i, i+1, i+1);
            rTree.add(rects[i]);
        }
        for(int i = 0; i < rects.length; i++) {
            rTree.remove(rects[i]);
        }
        RectDouble2D[] searchResults = new RectDouble2D[10];
        for(int i = 0; i < rects.length; i++) {
            assertTrue(rTree.containedToArray(rects[i], searchResults) == 0, "Found hyperRect that should have been removed on search " + i);
        }

        rTree.add(new RectDouble2D(0,0,5,5));
        assertTrue(rTree.size() != 0, "Found hyperRect that should have been removed on search ");
    }

    @Test
    void treeSingleRemovalTest() {
        final RTree<RectDouble2D> rTree = createRect2DTree(Spatialization.DefaultSplits.QUADRATIC);

        RectDouble2D rect = new RectDouble2D(0,0,2,2);
        rTree.add(rect);
        assertTrue(rTree.size() > 0, "Did not add HyperRect to Tree");
        assertTrue( rTree.remove(rect) );
        assertTrue(rTree.size() == 0, "Did not remove HyperRect from Tree");
        rTree.add(rect);
        assertTrue(rTree.size() > 0, "Tree nulled out and could not add HyperRect back in");
    }

    @Disabled
    void treeRemoveAndRebalanceTest() {
        final RTree<RectDouble2D> rTree = createRect2DTree(Spatialization.DefaultSplits.QUADRATIC);

        RectDouble2D[] rect = new RectDouble2D[65];
        for(int i = 0; i < rect.length; i++){
            if(i < 4){ rect[i] = new RectDouble2D(0,0,1,1); }
            else if(i < 8) { rect[i] = new RectDouble2D(2, 2, 4, 4); }
            else if(i < 12) { rect[i] = new RectDouble2D(4,4,5,5); }
            else if(i < 16) { rect[i] = new RectDouble2D(5,5,6,6); }
            else if(i < 20) { rect[i] = new RectDouble2D(6,6,7,7); }
            else if(i < 24) { rect[i] = new RectDouble2D(7,7,8,8); }
            else if(i < 28) { rect[i] = new RectDouble2D(8,8,9,9); }
            else if(i < 32) { rect[i] = new RectDouble2D(9,9,10,10); }
            else if(i < 36) { rect[i] = new RectDouble2D(2,2,4,4); }
            else if(i < 40) { rect[i] = new RectDouble2D(4,4,5,5); }
            else if(i < 44) { rect[i] = new RectDouble2D(5,5,6,6); }
            else if(i < 48) { rect[i] = new RectDouble2D(6,6,7,7); }
            else if(i < 52) { rect[i] = new RectDouble2D(7,7,8,8); }
            else if(i < 56) { rect[i] = new RectDouble2D(8,8,9,9); }
            else if(i < 60) { rect[i] = new RectDouble2D(9,9,10,10); }
            else if(i < 65) { rect[i] = new RectDouble2D(1,1,2,2); }
        }
        for(int i = 0; i < rect.length; i++){
            rTree.add(rect[i]);
        }
        Stats stat = rTree.stats();
        stat.print(System.out);
        for(int i = 0; i < 5; i++){
            rTree.remove(rect[64]);
        }
        Stats stat2 = rTree.stats();
        stat2.print(System.out);
    }

    @Test
    void treeUpdateTest() {
        final RTree<RectDouble2D> rTree = createRect2DTree(Spatialization.DefaultSplits.QUADRATIC);

        RectDouble2D rect = new RectDouble2D(0, 1, 2, 3);
        rTree.add(rect);
        RectDouble2D oldRect = new RectDouble2D(0,1,2,3);
        RectDouble2D newRect = new RectDouble2D(1,2,3,4);
        rTree.replace(oldRect, newRect);
        RectDouble2D[] results = new RectDouble2D[2];
        int num = rTree.containedToArray(newRect, results);
        assertTrue(num == 1, "Did not find the updated HyperRect");
        String st = results[0].toString();
        System.out.print(st);
    }

    /**
     * Generate 'count' random rectangles with fixed ranges.
     * The returned array will be free of duplicates
     *
     * @param count - number of rectangles to generate
     * @return array of generated rectangles
     */
    public static RectDouble2D[] generateRandomRects(int count) {
        final Random rand = new XoRoShiRo128PlusRandom(1);

        
        final int minX = 500;
        final int minY = 500;
        final int maxXRange = 25;
        final int maxYRange = 25;

        final double hitProb = 1.0 * count * maxXRange * maxYRange / (minX * minY);

        Set<RectDouble2D> added = new HashSet(count);
        final RectDouble2D[] rects = new RectDouble2D[count];
        for (int i = 0; i < count; ) {
            final int x1 = rand.nextInt(minX);
            final int y1 = rand.nextInt(minY);
            final int x2 = x1 + rand.nextInt(maxXRange);
            final int y2 = y1 + rand.nextInt(maxYRange);
            RectDouble2D next = new RectDouble2D(x1, y1, x2, y2);
            if (added.add(next))
                rects[i++] = next;
        }

        return rects;
    }

    /**
     * Generate 'count' random rectangles with fixed ranges.
     *
     * @param count - number of rectangles to generate
     * @return array of generated rectangles
     */
    public static RectFloatND[] generateRandomRects(int dimension, int count) {
        final Random rand = new Random(13);

        
        final int minX = 500;
        final int maxXRange = 25;



        final RectFloatND[] rects = new RectFloatND[count];
        for (int i = 0; i < count; i++) {

            float[] min = new float[dimension];
            float[] max = new float[dimension];
            for (int d = 0; d < dimension; d++){
                float x1 = min[d] = rand.nextInt(minX);
                max[d] = x1 + rand.nextInt(maxXRange);
            }

            rects[i] = new RectFloatND(min, max);
        }

        return rects;
    }

    /**
     * Generate 'count' random rectangles with fixed ranges.
     *
     * @param count - number of rectangles to generate
     * @return array of generated rectangles
     */
    public static RectFloatND[] generateRandomRectsWithOneDimensionRandomlyInfinite(int dimension, int count) {
        final Random rand = new Random(13);

        
        final int minX = 500;
        final int maxXRange = 25;


        Set<RectFloatND> s = new HashSet(count);
        final RectFloatND[] rects = new RectFloatND[count];

        for (int i = 0; i < count; ) {

            float[] min = new float[dimension];
            float[] max = new float[dimension];
            for (int d = 0; d < dimension; d++){
                float x1 = min[d] = rand.nextInt(minX);
                max[d] = x1 + rand.nextInt(maxXRange);
            }

            
            
            

            
            if (rand.nextBoolean()) {
                int infDim = 0;
                min[infDim] = Float.NEGATIVE_INFINITY;
                max[infDim] = Float.POSITIVE_INFINITY;
            }

            RectFloatND m = new RectFloatND(min, max);
            if (s.add(m))
                rects[i++] = m;
        }

        return rects;
    }

    /**
     * Create a tree capable of holding rectangles with default minM (2) and maxM (8) values.
     *
     * @param splitType - type of leaf to use (affects how full nodes get split)
     * @return tree
     */
    public static RTree<RectDouble2D> createRect2DTree(Spatialization.DefaultSplits splitType) {
        return createRect2DTree(2, 8, splitType);
    }
    public static RTree<RectDouble2D> createRect2DTree(Spatialization.DefaultSplits splitType, int min, int max) {
        return createRect2DTree(min, max, splitType);
    }

    /**
     * Create a tree capable of holding rectangles with specified m and M values.
     *
     * @param minM - minimum number of entries in each leaf
     * @param maxM - maximum number of entries in each leaf
     * @param splitType - type of leaf to use (affects how full nodes get split)
     * @return tree
     */
    public static RTree<RectDouble2D> createRect2DTree(int minM, int maxM, Spatialization.DefaultSplits splitType) {
        return new RTree<>((r->r), minM, maxM, splitType.get());
    }
    public static RTree<RectFloatND> createRectNDTree(int minM, int maxM, Spatialization.DefaultSplits splitType) {
        return new RTree<>((r->r), minM, maxM, splitType.get());
    }
}
