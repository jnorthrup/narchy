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

package com.metamx.collections.bitmap;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.IntIterator;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RoaringBitmapFactoryTest {

    
    @Test
    void testIssue26() throws Exception {
        
        checkEmptyComplement(new RoaringBitmapFactory());
    }

    
    private void checkEmptyComplement(BitmapFactory bitmapFactory) throws Exception {
        int numRow = 5104234;
        ImmutableBitmap bitmap = bitmapFactory.complement(bitmapFactory.makeEmptyImmutableBitmap(), numRow);
        ImmutableBitmap notBitmap = bitmapFactory.complement(bitmap, numRow);
        assertTrue(notBitmap.size() == 0);
        assertTrue(notBitmap.isEmpty());
        IntIterator intIter = notBitmap.iterator();
        assertFalse(intIter.hasNext());
    }

    @Test
    void testUnwrapWithNull() throws Exception {
        RoaringBitmapFactory factory = new RoaringBitmapFactory();

        ImmutableBitmap bitmap = factory.union(
                Iterables.transform(
                        Lists.newArrayList(new WrappedRoaringBitmap()),
                        new Function<WrappedRoaringBitmap, ImmutableBitmap>() {
                            @Override
                            public ImmutableBitmap apply(WrappedRoaringBitmap input) {
                                return null;
                            }
                        }
                )
        );

        assertEquals(0, bitmap.size());
    }

    @Test
    void testUnwrapMerge() throws Exception {
        RoaringBitmapFactory factory = new RoaringBitmapFactory();

        WrappedRoaringBitmap set = new WrappedRoaringBitmap();
        set.add(1);
        set.add(3);
        set.add(5);

        ImmutableBitmap bitmap = factory.union(
                Arrays.asList(
                        factory.makeImmutableBitmap(set),
                        null
                )
        );

        assertEquals(3, bitmap.size());
    }
}
