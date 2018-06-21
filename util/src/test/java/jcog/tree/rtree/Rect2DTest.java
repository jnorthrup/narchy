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

import jcog.tree.rtree.rect.RectDouble2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by jcovert on 6/16/15.
 */
class Rect2DTest {

    @Test
    void centroidTest() {

        RectDouble2D rect = new RectDouble2D(0, 0, 4, 3);

        HyperPoint centroid = rect.center();
        double x = centroid.coord(0);
        double y = centroid.coord(1);
        assertTrue(x == 2.0d, "Bad X-coord of centroid - expected " + 2.0 + " but was " + x);
        assertTrue(y == 1.5d, "Bad Y-coord of centroid - expected " + 1.5 + " but was " + y);
    }

    @Test
    void mbrTest() {

        RectDouble2D rect = new RectDouble2D(0, 0, 4, 3);

        
        RectDouble2D rectInside = new RectDouble2D(0, 0, 1, 1);
        RectDouble2D mbr = rect.mbr(rectInside);
        double expectedMinX = rect.min.x;
        double expectedMinY = rect.min.y;
        double expectedMaxX = rect.max.x;
        double expectedMaxY = rect.max.y;
        double actualMinX = mbr.min.x;
        double actualMinY = mbr.min.y;
        double actualMaxX = mbr.max.x;
        double actualMaxY = mbr.max.y;
        assertTrue(actualMinX == expectedMinX, "Bad minX - Expected: " + expectedMinX + " Actual: " + actualMinX);
        assertTrue(actualMinY == expectedMinY, "Bad minY - Expected: " + expectedMinY + " Actual: " + actualMinY);
        assertTrue(actualMaxX == expectedMaxX, "Bad maxX - Expected: " + expectedMaxX + " Actual: " + actualMaxX);
        assertTrue(actualMaxY == expectedMaxY, "Bad maxY - Expected: " + expectedMaxY + " Actual: " + actualMaxY);

        
        RectDouble2D rectOverlap = new RectDouble2D(3, 1, 5, 4);
        mbr = rect.mbr(rectOverlap);
        expectedMinX = 0.0d;
        expectedMinY = 0.0d;
        expectedMaxX = 5.0d;
        expectedMaxY = 4.0d;
        actualMinX = mbr.min.x;
        actualMinY = mbr.min.y;
        actualMaxX = mbr.max.x;
        actualMaxY = mbr.max.y;
        assertTrue(actualMinX == expectedMinX, "Bad minX - Expected: " + expectedMinX + " Actual: " + actualMinX);
        assertTrue(actualMinY == expectedMinY, "Bad minY - Expected: " + expectedMinY + " Actual: " + actualMinY);
        assertTrue(actualMaxX == expectedMaxX, "Bad maxX - Expected: " + expectedMaxX + " Actual: " + actualMaxX);
        assertTrue(actualMaxY == expectedMaxY, "Bad maxY - Expected: " + expectedMaxY + " Actual: " + actualMaxY);
    }

    @Test
    void rangeTest() {

        RectDouble2D rect = new RectDouble2D(0, 0, 4, 3);

        double xRange = rect.range(0);
        double yRange = rect.range(1);
        assertTrue(xRange == 4.0d, "Bad range in dimension X - expected " + 4.0 + " but was " + xRange);
        assertTrue(yRange == 3.0d, "Bad range in dimension Y - expected " + 3.0 + " but was " + yRange);
    }


    @Test
    void containsTest() {

        RectDouble2D rect = new RectDouble2D(0, 0, 4, 3);

        
        RectDouble2D rectOutsideNotContained = new RectDouble2D(4, 2, 5, 3);
        assertTrue(!rect.contains(rectOutsideNotContained), "Shares an edge but should not be 'contained'");

        
        RectDouble2D rectInsideNotContained = new RectDouble2D(0, 1, 4, 5);
        assertTrue(!rect.contains(rectInsideNotContained), "Shares an edge but should not be 'contained'");

        
        RectDouble2D rectInsideContained = new RectDouble2D(0, 1, 1, 2);
        assertTrue(rect.contains(rectInsideContained), "Shares an edge and should be 'contained'");

        
        RectDouble2D rectIntersects = new RectDouble2D(3, 2, 5, 4);
        assertTrue(!rect.contains(rectIntersects), "Intersects but should not be 'contained'");

        
        RectDouble2D rectContained = new RectDouble2D(1, 1, 2, 2);
        assertTrue(rect.contains(rectContained), "Contains and should be 'contained'");

        
        RectDouble2D rectNotContained = new RectDouble2D(5, 0, 6, 1);
        assertTrue(!rect.contains(rectNotContained), "Does not contain and should not be 'contained'");
    }

    @Test
    void intersectsTest() {

        RectDouble2D rect = new RectDouble2D(0, 0, 4, 3);

        
        RectDouble2D rectOutsideIntersects = new RectDouble2D(4, 2, 5, 3);
        assertTrue(rect.intersects(rectOutsideIntersects), "Shares an edge and should 'intersect'");

        
        RectDouble2D rectInsideIntersects = new RectDouble2D(0, 1, 4, 5);
        assertTrue(rect.intersects(rectInsideIntersects), "Shares an edge and should 'intersect'");

        
        RectDouble2D rectInsideIntersectsContained = new RectDouble2D(0, 1, 1, 2);
        assertTrue(rect.intersects(rectInsideIntersectsContained), "Shares an edge and should 'intersect'");

        
        RectDouble2D rectIntersects = new RectDouble2D(3, 2, 5, 4);
        assertTrue(rect.intersects(rectIntersects), "Intersects and should 'intersect'");

        
        RectDouble2D rectContained = new RectDouble2D(1, 1, 2, 2);
        assertTrue(rect.intersects(rectContained), "Contains and should 'intersect'");

        
        RectDouble2D rectNotIntersects = new RectDouble2D(5, 0, 6, 1);
        assertTrue(!rect.intersects(rectNotIntersects), "Does not intersect and should not 'intersect'");
    }

    @Test
    void costTest() {

        RectDouble2D rect = new RectDouble2D(0, 0, 4, 3);
        double cost = rect.cost();
        assertTrue(cost == 12.0d, "Bad cost - expected " + 12.0 + " but was " + cost);
    }
}
