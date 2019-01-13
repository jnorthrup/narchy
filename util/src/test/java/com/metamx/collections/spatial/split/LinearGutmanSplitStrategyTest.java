/*
 * Copyright 2011 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metamx.collections.spatial.split;

import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.bitmap.RoaringBitmapFactory;
import com.metamx.collections.spatial.Node;
import com.metamx.collections.spatial.Point;
import com.metamx.collections.spatial.RTree;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
class LinearGutmanSplitStrategyTest {
    @Test
    void testPickSeeds() {
        BitmapFactory bf = new RoaringBitmapFactory();

        for (SplitStrategy strategy : new SplitStrategy[]{
                new LinearGutmanSplitStrategy(0, 50, bf),
                
        }) {

            Node node = new Node(new float[2], new float[2], true, bf);

            node.addChild(new Point(new float[]{3, 7}, 1, bf));
            node.addChild(new Point(new float[]{1, 6}, 1, bf));
            node.addChild(new Point(new float[]{9, 8}, 1, bf));
            node.addChild(new Point(new float[]{2, 5}, 1, bf));
            node.addChild(new Point(new float[]{4, 4}, 1, bf));
            node.enclose();

            Node[] groups = strategy.split(node);
            assertEquals(groups[0].min[0], 1.0f);
            assertEquals(groups[0].min[1], 4.0f);
            assertEquals(groups[1].min[0], 9.0f);
            assertEquals(groups[1].min[1], 8.0f);
        }
    }

    @Test
    void testPickSeedsRoaring() {
        BitmapFactory bf = new RoaringBitmapFactory();
        LinearGutmanSplitStrategy strategy = new LinearGutmanSplitStrategy(0, 50, bf);
        Node node = new Node(new float[2], new float[2], true, bf);

        node.addChild(new Point(new float[]{3, 7}, 1, bf));
        node.addChild(new Point(new float[]{1, 6}, 1, bf));
        node.addChild(new Point(new float[]{9, 8}, 1, bf));
        node.addChild(new Point(new float[]{2, 5}, 1, bf));
        node.addChild(new Point(new float[]{4, 4}, 1, bf));
        node.enclose();

        Node[] groups = strategy.split(node);
        assertEquals(groups[0].min[0], 1.0f);
        assertEquals(groups[0].min[1], 4.0f);
        assertEquals(groups[1].min[0], 9.0f);
        assertEquals(groups[1].min[1], 8.0f);
    }


    @Test
    void testNumChildrenSize() {
        BitmapFactory bf = new RoaringBitmapFactory();
        RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, bf), bf);
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            tree.insert(new float[]{rand.nextFloat(), rand.nextFloat()}, i);
        }

        assertTrue(getNumPoints(tree.root()) >= tree.size());
    }

    @Test
    void testNumChildrenSizeRoaring() {
        BitmapFactory bf = new RoaringBitmapFactory();
        RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, bf), bf);
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            tree.insert(new float[]{rand.nextFloat(), rand.nextFloat()}, i);
        }

        assertTrue(getNumPoints(tree.root()) >= tree.size());
    }

    private int getNumPoints(Node node) {
        int total = 0;
        if (node.isLeaf) {
            total += node.children.size();
        } else {
            for (Node child : node.children) {
                total += getNumPoints(child);
            }
        }
        return total;
    }
}
