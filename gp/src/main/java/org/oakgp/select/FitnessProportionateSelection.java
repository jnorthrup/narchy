/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.select;

import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.RankedCandidate;
import org.oakgp.util.GPRandom;

/**
 * The fitness of candidates is used to determine the probability that they will be selected.
 * <p>
 * Also known as roulette wheel selection.
 */
public final class FitnessProportionateSelection implements NodeSelector {
    private final GPRandom random;
    private final Candidates candidates;
    private final int size;
    private final double sumFitness;

    /**
     * Creates a {@code FitnessProportionateSelection} that uses the given {@code Random} to select from the given {@code RankedCandidates}.
     */
    public FitnessProportionateSelection(GPRandom random, Candidates candidates) {
        this.random = random;
        this.candidates = candidates;
        this.size = candidates.size();
        this.sumFitness = sumFitness(candidates);
    }

    private static double sumFitness(Candidates candidates) {
        return candidates.stream().mapToDouble(rankedCandidate -> rankedCandidate.fitness).sum();
    }

    @Override
    public Node next() {
        final double r = random.nextDouble();
        double p = 0;
        for (int i = 0; i < size; i++) {
            RankedCandidate c = candidates.get(i);
            p += c.fitness / sumFitness;
            if (r < p) {
                return c.node;
            }
        }
        // should only get here if rounding error - default to selecting the best candidate
        return candidates.best().node;
    }
}
