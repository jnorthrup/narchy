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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.oakgp.Evolution;
import org.oakgp.evolve.GenerationEvolver;
import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.GenerationRanker;
import org.oakgp.rank.RankedCandidate;

import java.util.Collection;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

public class RunnerTest {
    @SuppressWarnings("unchecked")
    @Disabled
    @Test
    public void test() {
        
        GenerationRanker ranker = mock(GenerationRanker.class);
        GenerationEvolver evolver = mock(GenerationEvolver.class);
        Predicate<Candidates> terminator = mock(Predicate.class);
        Collection<Node> initialPopulation = mock(Collection.class);

        RankedCandidate expected = EvolutionTest.createRunExpectations(ranker, evolver, terminator, initialPopulation);

        Candidates output = Evolution.process(ranker, evolver, terminator, initialPopulation.stream());
        RankedCandidate actual = output.best();

        
        assertSame(expected, actual);
    }
}
