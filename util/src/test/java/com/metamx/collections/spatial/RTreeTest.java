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

package com.metamx.collections.spatial;

import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.bitmap.RoaringBitmapFactory;
import com.metamx.collections.spatial.split.LinearGutmanSplitStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
class RTreeTest {
    private RTree R;

    @BeforeEach
    void setUp() throws Exception {
        BitmapFactory rbf = new RoaringBitmapFactory();
        R = new RTree(2, new LinearGutmanSplitStrategy(0, 50, rbf), rbf);

    }
































    @Test
    void testInsertDuplicatesNoSplitRoaring() {
        R.insert(new float[]{1, 1}, 1);
        R.insert(new float[]{1, 1}, 1);
        R.insert(new float[]{1, 1}, 1);

        assertEquals(R.root().children.size(), 3);
    }
    @Test
    void testRemoval() {
        R.insert(new float[]{1, 2}, 1);
        R.insert(new float[]{3, 2}, 2);
        R.insert(new float[]{1, 3}, 3);

        assertEquals(3, R.root().children.size());

        RTreeUtils.print(R);

        assertTrue( R.remove(new float[]{1, 3}, 3) );

        RTreeUtils.print(R);

        assertEquals(2, R.root().children.size());
        assertFalse(R.root().contains(new float[]{1, 3}));

        assertEquals(2, R.root().max[1], 0.01f); 

    }











    @Test
    void testSplitOccursRoaring() {
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            R.insert(new float[]{rand.nextFloat(), rand.nextFloat()}, i);
        }

        assertTrue(R.root().children.size() > 1);
    }


}
