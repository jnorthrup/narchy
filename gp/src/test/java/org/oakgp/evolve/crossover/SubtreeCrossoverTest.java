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
package org.oakgp.evolve.crossover;

import jcog.random.XoRoShiRo128PlusRandom;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.oakgp.TestUtils;
import org.oakgp.evolve.GeneticOperator;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.select.DummyNodeSelector;
import org.oakgp.select.NodeSelector;
import org.oakgp.util.DummyRandom;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.oakgp.TestUtils.assertNodeEquals;
import static org.oakgp.util.DummyRandom.GetIntExpectation.nextInt;
import static org.oakgp.util.DummyRandom.random;

public class SubtreeCrossoverTest {
    private static final int DEFAULT_DEPTH = 9;

    @Test
    public void testFunctionNodes() {
        DummyRandom dummyRandom = random().nextInt(2).returns(1).nextInt(5).returns(3).build();
        DummyNodeSelector dummySelector = new DummyNodeSelector("(+ 9 5)", "(* 7 (- 8 v5))");

        GeneticOperator c = new SubtreeCrossover(dummyRandom, DEFAULT_DEPTH);


        assertNodeEquals("(+ 5 (- 8 v5))", c.apply(dummySelector));
        

        dummyRandom.assertEmpty();
        dummySelector.assertEmpty();
    }

    @Test
    public void testConstantNodes() {
        DummyRandom dummyRandom = nextInt(1).returns(0);
        DummyNodeSelector dummySelector = new DummyNodeSelector("1", "2");

        GeneticOperator c = new SubtreeCrossover(dummyRandom, DEFAULT_DEPTH);

        Node result = c.apply(dummySelector);
        assertNodeEquals("2", result);
        dummySelector.assertEmpty();
    }

    /**
     * Test crossover using trees that use a mix of types (booleans and integers)
     */
    @Test
    public void testMixedTypes() {
        String input = "(+ 4 5)";
        String output = "(if (< 6 7) 8 9)";

        DummyRandom dummyRandom = random().nextInt(2).returns(0, 1, 1, 1, 1, 1).nextInt(5).returns(0, 0, 1, 2, 3, 4).build();
        DummyNodeSelector dummySelector = new DummyNodeSelector(input, output, input, output, input, output, input, output, input, output, input, output);

        GeneticOperator c = new SubtreeCrossover(dummyRandom, DEFAULT_DEPTH);

        assertNodeEquals("(+ 5 6)", c.apply(dummySelector));
        assertNodeEquals("(+ 4 6)", c.apply(dummySelector));
        assertNodeEquals("(+ 4 7)", c.apply(dummySelector));
        assertNodeEquals("(+ 4 8)", c.apply(dummySelector));
        assertNodeEquals("(+ 4 9)", c.apply(dummySelector));
        assertNodeEquals("(+ 4 " + output + ')', c.apply(dummySelector));

        dummyRandom.assertEmpty();
        dummySelector.assertEmpty();
    }

    /**
     * Test attempted crossover when selected node in first parent has a type that is not present in the second parent
     */
    @Test
    public void testNoMatchingTypes() {
        String input = "(+ (if (< 6 7) 8 9) 5)";
        String output = "(+ 1 2)";


        for (int i = 0; i < 15; i++) {
            Random rng =
                    //nextInt(7).returns(2);
                    new XoRoShiRo128PlusRandom(i);
            NodeSelector s = new DummyNodeSelector(input, output);
            GeneticOperator c = new SubtreeCrossover(rng, DEFAULT_DEPTH);
            Node evolve = c.apply(s);
            System.out.println(evolve);
            //assertNodeEquals(input, evolve);
        }

//        rng.assertEmpty();
        //dummySelector.assertEmpty();
    }

    /**
     * Test max depth limit not exceeded
     */
    @Test @Disabled
    public void testMaxDepthLimit() {
        assertCrossoverPossibilities(7);
        assertCrossoverPossibilities(6);
        assertCrossoverPossibilities(5);
        assertCrossoverPossibilities(4);
    }

    private void assertCrossoverPossibilities(int maxDepth) {
        String[] possibilities = {
                "(+ (+ 9 (+ 3 4)) 5)",
                "(+ (+ 7 (+ 3 4)) 5)",
                "(+ (+ 8 (+ 3 4)) 5)",
                "(+ (+ (* 7 8) (+ 3 4)) 5)",
                "(+ (+ (* 9 (* 7 8)) (+ 3 4)) 5)",
                "(+ (+ 6 (+ 3 4)) 5)",
                "(+ (+ (* (* 9 (* 7 8)) 6) (+ 3 4)) 5)"};
        List<FnNode> f = Arrays.stream(possibilities).map(TestUtils::readFunctionNode).filter(n -> n.depth() <= maxDepth).collect(Collectors.toList());
        int numPossibilities = f.size();
        assertTrue(numPossibilities > 0);
        for (int i = 0; i < numPossibilities; i++) {
            Node result = subtreeCrossover(maxDepth, 6, 0, numPossibilities, i);
            assertEquals(f.get(i), result);
            assertTrue(result.depth() <= maxDepth);
        }
    }

    private Node subtreeCrossover(int maxDepth, int first, int second, int third, int fourth) {
        String input = "(+ (+ 2 (+ 3 4)) 5)";
        String output = "(* (* 9 (* 7 8)) 6)";
        DummyNodeSelector dummySelector = new DummyNodeSelector(input, output);
        DummyRandom dummyRandom;
        if (first == third) {
            dummyRandom = random().nextInt(first).returns(second, fourth).build();
        } else {
            dummyRandom = random().nextInt(first).returns(second).nextInt(third).returns(fourth).build();
        }

        GeneticOperator c = new SubtreeCrossover(dummyRandom, maxDepth);
        Node result = c.apply(dummySelector);

        dummyRandom.assertEmpty();
        dummySelector.assertEmpty();

        return result;
    }
}
