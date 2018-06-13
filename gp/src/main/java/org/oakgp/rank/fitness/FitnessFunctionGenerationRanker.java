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

import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.GenerationRanker;
import org.oakgp.rank.RankedCandidate;

import java.util.stream.Stream;

/**
 * Ranks and sorts the fitness of {@code Node} instances using a {@code FitnessFunction}.
 */
abstract public class FitnessFunctionGenerationRanker implements GenerationRanker {
    protected final FitnessFunction fitnessFunction;

    /**
     * Constructs a {@code GenerationRanker} with the specified {@code FitnessFunction}.
     *
     * @param fitnessFunction the {@code FitnessFunction} to use when determining the fitness of candidates
     */
    public FitnessFunctionGenerationRanker(FitnessFunction fitnessFunction) {
        this.fitnessFunction = fitnessFunction;
    }

    public static final class SingleThread extends FitnessFunctionGenerationRanker {

        /**
         * Constructs a {@code GenerationRanker} with the specified {@code FitnessFunction}.
         *
         * @param fitnessFunction the {@code FitnessFunction} to use when determining the fitness of candidates
         */
        public SingleThread(FitnessFunction fitnessFunction) {
            super(fitnessFunction);
        }

        /**
         * Returns the sorted result of applying this object's {@code FitnessFunction} against each of the specified nodes.
         *
         * @param input the {@code Node} instances to apply this object's {@code FitnessFunction} against
         * @return a {@code List} of {@code RankedCandidate} - one for each {@code Node} specified in {@code input} - sorted by fitness
         */
        @Override
        public Candidates apply(Stream<Node> input) {
            return new Candidates(input.map(this::rankCandidate));
        }
    }

    public static final class Parallel extends FitnessFunctionGenerationRanker   {
        /**
         * Constructs a {@code GenerationRanker} with the specified {@code FitnessFunction}.
         *
         * @param fitnessFunction the {@code FitnessFunction} to use when determining the fitness of candidates
         */
        public Parallel(FitnessFunction fitnessFunction) {
            super(fitnessFunction);
        }

        @Override
        public Candidates apply(Stream<Node> input) {
            return new Candidates( input.parallel().map(this::rankCandidate));
        }
    }


    protected RankedCandidate rankCandidate(Node n) {
        return new RankedCandidate(n, fitnessFunction.doubleValueOf(n));
    }
}
