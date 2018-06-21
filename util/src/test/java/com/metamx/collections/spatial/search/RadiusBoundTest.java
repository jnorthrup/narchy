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

class RadiusBoundTest {
    @Test
    void testCacheKey() {
        final float[] coords0 = {1.0F, 2.0F};
        final float[] coords1 = {1.1F, 2.1F};
        assertArrayEquals(
                new RadiusBound(coords0, 3.0F, 10).getCacheKey(),
                new RadiusBound(coords0, 3.0F, 10).getCacheKey()
        );
        assertFalse(Arrays.equals(
                new RadiusBound(coords0, 3.0F, 10).getCacheKey(),
                new RadiusBound(coords1, 3.0F, 10).getCacheKey()
        ));
        assertFalse(Arrays.equals(
                new RadiusBound(coords0, 3.0F, 10).getCacheKey(),
                new RadiusBound(coords0, 3.1F, 10).getCacheKey()
        ));
        assertFalse(Arrays.equals(
                new RadiusBound(coords0, 3.0F, 10).getCacheKey(),
                new RadiusBound(coords0, 3.0F, 9).getCacheKey()
        ));
    }
}
