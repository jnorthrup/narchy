/*
 * Copyright 2015 S. Webber
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
package org.oakgp.util;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.oakgp.rank.fitness.FitnessRanker.createCache;

@Disabled
public class CacheMapTest {





    @Test
    public void testSizeThree() {
        final int maxSize = 3;
        Cache<String, Integer> m = createCache(maxSize*10);

        m.put("a", 1);
        m.put("b", 1);
        m.put("c", 1);
        m.put("d", 1);
        m.put("e", 1);

        assertEquals(maxSize, m.estimatedSize());
        assertTrue(m.getIfPresent("c")!=null);
        assertTrue(m.getIfPresent("d")!=null);
        assertTrue(m.getIfPresent("e")!=null);

        m.put("a", 1);

        assertEquals(maxSize, m.estimatedSize());
        assertTrue(m.getIfPresent("a")!=null);
        assertTrue(m.getIfPresent("d")!=null);
        assertTrue(m.getIfPresent("e")!=null);

        m.getIfPresent("d");
        m.put("x", 1);
        m.getIfPresent("d");
        m.put("y", 1);
        m.getIfPresent("d");
        m.put("z", 1);

        assertEquals(maxSize, m.estimatedSize());
        assertTrue(m.getIfPresent("d")!=null);
        assertTrue(m.getIfPresent("y")!=null);
        assertTrue(m.getIfPresent("z")!=null);
























    }



















}
