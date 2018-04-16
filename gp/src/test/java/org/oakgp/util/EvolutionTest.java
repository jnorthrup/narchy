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
package org.oakgp.util;

import org.junit.jupiter.api.Test;
import org.oakgp.Evolution;
import org.oakgp.Evolution.Config;
import org.oakgp.Evolution.InitialPopulationSetter;
import org.oakgp.Evolution.TreeDepthSetter;
import org.oakgp.Type;
import org.oakgp.evolve.GenerationEvolver;
import org.oakgp.node.Node;
import org.oakgp.primitive.DummyPrimitiveSet;
import org.oakgp.rank.GenerationRanker;
import org.oakgp.rank.RankedCandidate;
import org.oakgp.rank.Candidates;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.oakgp.TestUtils.singletonRankedCandidates;

public class EvolutionTest {
    private static final DummyPrimitiveSet DUMMY_PRIMITIVE_SET = new DummyPrimitiveSet();
    private static final GPRandom DUMMY_RANDOM = DummyRandom.EMPTY;
    private static final Type RETURN_TYPE = Type.type("runBuilderTest");

    @SuppressWarnings("unchecked")
    static RankedCandidate createRunExpectations(GenerationRanker ranker, GenerationEvolver evolver, Predicate<Candidates> terminator,
                                                 Collection<Node> initialPopulation) {
        // create mock objects used in processing
        Candidates rankedInitialPopulation = singletonRankedCandidates();
        Collection<Node> secondGeneration = mock(Collection.class);
        Candidates rankedSecondGeneration = singletonRankedCandidates();
        Collection<Node> thirdGeneration = mock(Collection.class);
        Candidates rankedThirdGeneration = singletonRankedCandidates();
        Collection<Node> fourthGeneration = mock(Collection.class);
        Candidates rankedFourthGeneration = singletonRankedCandidates();

        // expectations for initial population
        when(ranker.rank(initialPopulation)).thenReturn(rankedInitialPopulation);
        when(evolver.evolve(rankedInitialPopulation)).thenReturn(secondGeneration);

        // expectations for second generation
        when(ranker.rank(secondGeneration)).thenReturn(rankedSecondGeneration);
        when(evolver.evolve(rankedSecondGeneration)).thenReturn(thirdGeneration);

        // expectations for third generation
        when(ranker.rank(thirdGeneration)).thenReturn(rankedThirdGeneration);
        when(evolver.evolve(rankedThirdGeneration)).thenReturn(fourthGeneration);

        // expectations for fourth generation
        when(ranker.rank(fourthGeneration)).thenReturn(rankedFourthGeneration);
        when(terminator.test(rankedFourthGeneration)).thenReturn(true);

        return rankedFourthGeneration.best();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        // Create mock objects to pass as arguments to process method of Runner
        GenerationRanker ranker = mock(GenerationRanker.class);
        GenerationEvolver evolver = mock(GenerationEvolver.class);
        Predicate<Candidates> terminator = mock(Predicate.class);
        Collection<Node> initialPopulation = mock(Collection.class);

        Function<Config, Collection<Node>> initialPopulationCreator = c -> {
            assertSame(c.getPrimitiveSet(), DUMMY_PRIMITIVE_SET);
            assertSame(c.getRandom(), DUMMY_RANDOM);
            assertSame(c.getReturnType(), RETURN_TYPE);
            return initialPopulation;
        };
        Function<Config, GenerationEvolver> generationEvolverCreator = c -> {
            assertSame(c.getPrimitiveSet(), DUMMY_PRIMITIVE_SET);
            assertSame(c.getRandom(), DUMMY_RANDOM);
            assertSame(c.getReturnType(), RETURN_TYPE);
            return evolver;
        };

        RankedCandidate expected = createRunExpectations(ranker, evolver, terminator, initialPopulation);

        Candidates output = new Evolution().returns(RETURN_TYPE).setRandom(DUMMY_RANDOM).setPrimitiveSet(DUMMY_PRIMITIVE_SET)
                .ranked(ranker).setInitialPopulation(initialPopulationCreator).setGenerationEvolver(generationEvolverCreator)
                .setTerminator(terminator).get();
        RankedCandidate actual = output.best();

        // confirm output matches expected behaviour
        assertSame(expected, actual);
    }

    @Test
    public void testInvalidPopulationSize() {
        InitialPopulationSetter setter = createInitialPopulationSetter();
        assertInvalidSizes(setter::population);
    }

    @Test
    public void testInvalidTreeDepth() {
        TreeDepthSetter setter = createInitialPopulationSetter().population(1000);
        assertInvalidSizes(setter::depth);
    }

    private InitialPopulationSetter createInitialPopulationSetter() {
        GenerationRanker ranker = mock(GenerationRanker.class);
        return new Evolution().returns(RETURN_TYPE).setRandom(DUMMY_RANDOM).setPrimitiveSet(DUMMY_PRIMITIVE_SET).ranked(ranker);
    }

    private void assertInvalidSizes(IntFunction<?> setter) {
        // confirm negative values and zero are not valid
        for (int i : new int[]{Integer.MIN_VALUE, -2, -1, 0}) {
            assertInvalidSize(setter, i);
        }
    }

    private void assertInvalidSize(IntFunction<?> setter, int size) {
        try {
            setter.apply(size);
        } catch (IllegalArgumentException e) {
            assertEquals("Expected a positive integer but got: " + size, e.getMessage());
        }
    }
}
