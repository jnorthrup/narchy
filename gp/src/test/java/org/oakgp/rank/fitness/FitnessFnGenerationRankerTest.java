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

import org.junit.jupiter.api.Test;
import org.oakgp.node.Node;
import org.oakgp.rank.Ranking;
import org.oakgp.rank.GenerationRanker;

import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.oakgp.TestUtils.assertRankedCandidate;
import static org.oakgp.TestUtils.integerConstant;

public class FitnessFnGenerationRankerTest {
    @Test
    public void test() {
        
        Node a = integerConstant(1);
        double aFitness = 9;
        Node b = integerConstant(2);
        double bFitness = 12;
        Node c = integerConstant(3);
        double cFitness = 8;

        
        FitFn mockFitnessFunction = mock(FitFn.class);
        given(mockFitnessFunction.doubleValueOf(a)).willReturn(aFitness);
        given(mockFitnessFunction.doubleValueOf(b)).willReturn(bFitness);
        given(mockFitnessFunction.doubleValueOf(c)).willReturn(cFitness);

        
        GenerationRanker generationRanker = new FitnessRanker.SingleThread(mockFitnessFunction, 4);
        Ranking output = new Ranking(3);
        generationRanker.accept(Stream.of(a,b,c), output);

        
        assertRankedCandidate(output.get(0), c, cFitness);
        assertRankedCandidate(output.get(1), a, aFitness);
        assertRankedCandidate(output.get(2), b, bFitness);
    }
}
