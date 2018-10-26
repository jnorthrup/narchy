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
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import jcog.random.XoRoShiRo128PlusRandom;
import org.junit.jupiter.api.Test;
import org.oakgp.node.Node;
import org.oakgp.rank.Evolved;
import org.oakgp.rank.Ranking;
import org.oakgp.select.NodeSelector;
import org.oakgp.select.RankSelector;

import java.util.Collection;
import java.util.HashMap;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.oakgp.TestUtils.mockNode;

public class GenerationEvolverImplTest {


    @Test
    public void test() {
        
        float elitismRate = 0.5f;

        ArrayBag<GeneticOperator, PLink<GeneticOperator>> operators = new PLinkArrayBag<>(2, PriMerge.plus, new HashMap());
        GeneticOperator generator1 = mock(GeneticOperator.class);
        GeneticOperator generator2 = mock(GeneticOperator.class);
        operators.put(new PLink(generator1, 0.3f));
        operators.put(new PLink(generator2, 0.5f));

        
        
        Node[] expectedOutput = new Node[10];
        for (int i = 0; i < expectedOutput.length; i++) {
            expectedOutput[i] = mockNode();
        }

        
        Ranking input = new Ranking(new Evolved[]{new Evolved(expectedOutput[0], 1),
                new Evolved(expectedOutput[1], 2), new Evolved(expectedOutput[2], 3), new Evolved(mockNode(), 4),
                new Evolved(mockNode(), 5)});

        
        NodeSelector selector = mock(NodeSelector.class);
        when(generator1.apply(selector)).thenReturn(expectedOutput[3], expectedOutput[4], expectedOutput[5]);
        
        
        when(generator2.apply(selector)).thenReturn(expectedOutput[6], expectedOutput[7], expectedOutput[8], expectedOutput[7], expectedOutput[9]);


        XoRoShiRo128PlusRandom rng = new XoRoShiRo128PlusRandom(1);
        GenerationEvolverImpl evolver = new GenerationEvolverImpl(elitismRate, new RankSelector(rng), operators,
                rng);
        Collection<Node> actualOutput = evolver.apply(input).collect(toList());

        
        assertEquals(expectedOutput.length, actualOutput.size());
        for (Node n : expectedOutput) {
            assertTrue(actualOutput.contains(n));
        }
    }
}
