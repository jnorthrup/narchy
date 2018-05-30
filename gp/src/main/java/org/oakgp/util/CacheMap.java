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
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Provides a size-limited map of keys to values.
 * <p>
 * When the maximum size limit has been reached, the least-recently accessed entry will be removed whenever a new entry is added.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public final class CacheMap<K, V>  {













    /**
     * Returns a size-limited map of keys to values.
     *
     * @param maxSize the maximum size restriction to enforce on the returned map
     * @param <K>     the type of keys maintained by this map
     * @param <V>     the type of mapped values
     * @return a size-limited map of keys to values
     */
    public static <K, V> Cache<K, V> createCache(int maxSize) {



        return Caffeine.newBuilder().maximumSize(maxSize).build();
    }





}
