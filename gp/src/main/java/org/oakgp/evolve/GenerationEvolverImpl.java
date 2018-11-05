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

import jcog.pri.PLink;
import jcog.pri.bag.impl.ArrayBag;
import org.oakgp.node.Node;
import org.oakgp.rank.Ranking;
import org.oakgp.select.NodeSelector;

import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Uses a user-defined collection of {@code GeneticOperator} instances to evolve new generations from existing generations.
 */
public final class GenerationEvolverImpl implements GenerationEvolver {
    private final float eliteRate;
    private final NodeSelector selector;
    private final Supplier<GeneticOperator> operators;



    /**
     * Creates a {@code GenerationEvolverImpl} that uses the given values to evolve new generations.
     *  @param elitismSize     the number of best candidates from an existing generation to automatically include "as-is" in the next generation
     * @param selector used to select candidates from an existing generation to be used as a basis for evolving candidates for the next generation
     * @param operators       the genetic operators to be used to evolve new candidates where the key = a genetic operator and the value = the number of times that genetic
 *                        operator should be applied during each single invocation of {@link #apply(Ranking)}
     * @param _random
     */



    public GenerationEvolverImpl(float eliteRate, NodeSelector selector, ArrayBag<GeneticOperator, PLink<GeneticOperator>> operators, Random random) {
        this(eliteRate, selector,
                //TODO: if operators.size()==1 ?
                () -> operators.sample(random).get()
        );
        assert(!operators.isEmpty());
    }

    public GenerationEvolverImpl(float eliteRate, NodeSelector selector, Supplier<GeneticOperator> operatorSupplier) {
        this.eliteRate = eliteRate;
        this.selector = selector;
        this.operators = operatorSupplier;
    }


    /**
     * Returns a new generation of {@code Node} instances evolved from the specified existing generation.
     *
     * @param living the existing generation to use as a basis for evolving a new generation
     * @return a new generation of {@code Node} instances evolved from the existing generation specified by {@code oldGeneration}
     */
    @Override public Stream<Node> apply(Ranking living) {
        NodeSelector selector = this.selector;



        int popBefore = living.size();
        living.removePercentage(eliteRate, false);
        int after = living.size();
        assert(popBefore < living.capacity() || popBefore!=after);

        selector.reset(living);

        final int[] nullLimit = {living.capacity()};

        return Stream.generate(()-> {
            Node result = operators.get().apply(selector);
            return result;
        }).takeWhile((j)->{
            if (j == null) {
                return --nullLimit[0] > 0;
            } else {
                return true;
            }
        }).filter(Objects::nonNull);
    }
}
