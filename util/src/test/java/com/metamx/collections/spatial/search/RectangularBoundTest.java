/*
 * Copyright 2016 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http:
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.metamx.collections.spatial.search;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RectangularBoundTest {
    @Test
    void testCacheKey() {
        assertArrayEquals(
                new RectangularBound(new float[]{1F, 1F}, new float[]{2F, 2F}, 1).getCacheKey(),
                new RectangularBound(new float[]{1F, 1F}, new float[]{2F, 2F}, 1).getCacheKey()
        );
        assertFalse(Arrays.equals(
                new RectangularBound(new float[]{1F, 1F}, new float[]{2F, 2F}, 1).getCacheKey(),
                new RectangularBound(new float[]{1F, 1F}, new float[]{2F, 3F}, 1).getCacheKey()
        ));
        assertFalse(Arrays.equals(
                new RectangularBound(new float[]{1F, 1F}, new float[]{2F, 2F}, 1).getCacheKey(),
                new RectangularBound(new float[]{1F, 0F}, new float[]{2F, 2F}, 1).getCacheKey()
        ));
        assertFalse(Arrays.equals(
                new RectangularBound(new float[]{1F, 1F}, new float[]{2F, 2F}, 1).getCacheKey(),
                new RectangularBound(new float[]{1F, 1F}, new float[]{2F, 2F}, 2).getCacheKey()
        ));
    }
}
