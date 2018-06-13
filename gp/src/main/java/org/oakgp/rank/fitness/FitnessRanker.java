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
package org.oakgp.rank.fitness;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.oakgp.node.Node;
import org.oakgp.rank.Evolved;
import org.oakgp.rank.GenerationRanker;
import org.oakgp.rank.Ranking;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Ranks and sorts the fitness of {@code Node} instances using a {@code FitnessFunction}.
 */
abstract public class FitnessRanker implements GenerationRanker {
    protected final FitFn fitnessFunction;
    private final Cache<Node, Double> cache;

    FitnessRanker(FitFn fitnessFunction, int cacheSize) {
        this.fitnessFunction = fitnessFunction;
        this.cache = createCache(cacheSize);
    }

    /**
     * Returns a size-limited map of keys to values.
     *
     * @param maxSize the maximum size restriction to enforce on the returned map
     * @param <K>     the type of keys maintained by this map
     * @param <V>     the type of mapped values
     * @return a size-limited map of keys to values
     */
    public static <K, V> Cache<K, V> createCache(int maxSize) {
        return Caffeine.newBuilder().maximumSize(maxSize)
                .recordStats()
                .build();
    }

    public CacheStats stats() {
        return cache.stats();
    }



    /** returns null if pre-computed value exists in the cache */
    protected Evolved rank(Node n) {
        final boolean[] computed = {false};
        Double x = cache.asMap().computeIfAbsent(n, (nn)->{
            computed[0] = true;
            return fitnessFunction.doubleValueOf(n);
        });
        return (computed[0]) ? new Evolved(n, x) : null;
    }

    public static class SingleThread extends FitnessRanker {

        /**
         * Constructs a {@code GenerationRanker} with the specified {@code FitnessFunction}.
         *
         * @param fitnessFunction the {@code FitnessFunction} to use when determining the fitness of candidates
         */
        public SingleThread(FitFn fitnessFunction, int cacheSize) {
            super(fitnessFunction, cacheSize);
        }


        @Override
        public void accept(Stream<Node> nodeStream, Ranking evolveds) {
            Iterator<Evolved> ii = nodeStream.map(this::rank).filter(Objects::nonNull).iterator();

            int limit = evolveds.capacity() * 2;
            int attempts = 0;
            while (!evolveds.isFull()) {
                Evolved e = ii.next();
                if (e != null)
                    evolveds.add(e);
                if (attempts++ > limit)
                    break; //?
            }
        }
    }

    public static final class Parallel extends SingleThread {
        /**
         * Constructs a {@code GenerationRanker} with the specified {@code FitnessFunction}.
         *
         * @param fitnessFunction the {@code FitnessFunction} to use when determining the fitness of candidates
         */
        public Parallel(FitFn fitnessFunction, int cacheSize) {
            super(fitnessFunction, cacheSize);
        }

        @Override
        public void accept(Stream<Node> input, Ranking ranking) {
            super.accept(input.parallel(), ranking);
        }
    }
}
