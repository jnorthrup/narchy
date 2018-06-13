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
package org.oakgp.evolve;

import org.oakgp.node.Node;
import org.oakgp.rank.Ranking;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Creates a new generation of {@code Node} instances evolved from an existing generation.
 */
@FunctionalInterface
public interface GenerationEvolver extends Function<Ranking, Stream<Node>> {
    /**
     * Returns a new generation of {@code Node} instances evolved from the specified existing generation.
     *
     * @param oldGeneration the existing generation to use as a basis for evolving a new generation
     * @return a new generation of {@code Node} instances evolved from the existing generation specified by {@code oldGeneration}
     */
//    Stream<Node> apply(Candidates oldGeneration);
}
