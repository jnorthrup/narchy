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
import org.oakgp.Evolution.Config;
import org.oakgp.Evolution.InitialPopulationSetter;
import org.oakgp.Evolution.TreeDepthSetter;
import org.oakgp.Type;
import org.oakgp.evolve.GenerationEvolver;
import org.oakgp.node.Node;
import org.oakgp.primitive.DummyPrimitiveSet;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.GenerationRanker;
import org.oakgp.rank.RankedCandidate;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
        
        Candidates rankedInitialPopulation = singletonRankedCandidates();
        Stream<Node> secondGeneration = mock(Stream.class);
        Candidates rankedSecondGeneration = singletonRankedCandidates();
        Stream<Node> thirdGeneration = mock(Stream.class);
        Candidates rankedThirdGeneration = singletonRankedCandidates();
        Stream<Node> fourthGeneration = mock(Stream.class);
        Candidates rankedFourthGeneration = singletonRankedCandidates();

        
        when(ranker.apply(initialPopulation.stream())).thenReturn(rankedInitialPopulation);
        when(evolver.apply(rankedInitialPopulation)).thenReturn(secondGeneration);

        
        when(ranker.apply(secondGeneration)).thenReturn(rankedSecondGeneration);
        when(evolver.apply(rankedSecondGeneration)).thenReturn(thirdGeneration);

        
        when(ranker.apply(thirdGeneration)).thenReturn(rankedThirdGeneration);
        when(evolver.apply(rankedThirdGeneration)).thenReturn(fourthGeneration);

        
        when(ranker.apply(fourthGeneration)).thenReturn(rankedFourthGeneration);
        when(terminator.test(rankedFourthGeneration)).thenReturn(true);

        return rankedFourthGeneration.best();
    }

    @SuppressWarnings("unchecked")
    @Disabled
    @Test
    public void test() {
        
        GenerationRanker ranker = mock(GenerationRanker.class);
        GenerationEvolver evolver = mock(GenerationEvolver.class);
        Predicate<Candidates> terminator = mock(Predicate.class);
        Collection<Node> initialPopulation = mock(Collection.class);

        Function<Config, Stream<Node>> initialPopulationCreator = c -> {
            assertSame(c.getPrimitiveSet(), DUMMY_PRIMITIVE_SET);
            assertSame(c.getRandom(), DUMMY_RANDOM);
            assertSame(c.getReturnType(), RETURN_TYPE);
            return Stream.empty();
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
