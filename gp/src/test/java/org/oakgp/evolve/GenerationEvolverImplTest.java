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

import org.junit.jupiter.api.Test;
import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.RankedCandidate;
import org.oakgp.select.NodeSelector;
import org.oakgp.select.NodeSelectorFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.oakgp.TestUtils.mockNode;

public class GenerationEvolverImplTest {
    @Test
    public void test() {
        
        int elitismSize = 3;
        NodeSelectorFactory selectorFactory = mock(NodeSelectorFactory.class);
        Map<GeneticOperator, Integer> operators = new HashMap<>();
        GeneticOperator generator1 = mock(GeneticOperator.class);
        GeneticOperator generator2 = mock(GeneticOperator.class);
        operators.put(generator1, 3);
        operators.put(generator2, 5);

        
        
        Node[] expectedOutput = new Node[10];
        for (int i = 0; i < expectedOutput.length; i++) {
            expectedOutput[i] = mockNode();
        }

        
        Candidates input = new Candidates(new RankedCandidate[]{new RankedCandidate(expectedOutput[0], 1),
                new RankedCandidate(expectedOutput[1], 2), new RankedCandidate(expectedOutput[2], elitismSize), new RankedCandidate(mockNode(), 4),
                new RankedCandidate(mockNode(), 5)});

        
        NodeSelector selector = mock(NodeSelector.class);
        when(selectorFactory.getSelector(input)).thenReturn(selector);
        when(generator1.apply(selector)).thenReturn(expectedOutput[3], expectedOutput[4], expectedOutput[5]);
        
        
        when(generator2.apply(selector)).thenReturn(expectedOutput[6], expectedOutput[7], expectedOutput[8], expectedOutput[7], expectedOutput[9]);

        
        GenerationEvolverImpl evolver = new GenerationEvolverImpl(elitismSize, selectorFactory, operators);
        Collection<Node> actualOutput = evolver.apply(input).collect(toList());

        
        assertEquals(expectedOutput.length, actualOutput.size());
        for (Node n : expectedOutput) {
            assertTrue(actualOutput.contains(n));
        }
    }
}
