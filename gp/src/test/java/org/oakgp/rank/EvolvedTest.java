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
package org.oakgp.rank;

import org.junit.jupiter.api.Test;
import org.oakgp.node.Node;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.oakgp.TestUtils.*;

public class EvolvedTest {
    @Test
    public void testGetters() {
        Node n = integerConstant(0);
        double f = 7.5;
        Evolved a = new Evolved(n, f);
        assertSame(n, a.get());
        assertEquals(f, a.pri(), 0.001f);
    }

    @Test
    public void testCompareTo() {
        final double baseFitness = 500d;
        final int baseNodeCount = 3;
        final double betterFitness = 5000d;
        final int betterNodeCount = 1;
        final double worseFitness = 250d;
        final int worseNodeCount = 5;

        Evolved candidate = mockRankedCandidate(baseFitness, baseNodeCount);

        assertEqual(candidate, candidate);

        assertBetter(candidate, mockRankedCandidate(worseFitness, worseNodeCount));
        assertBetter(candidate, mockRankedCandidate(worseFitness, betterNodeCount));
        assertBetter(candidate, mockRankedCandidate(worseFitness, baseNodeCount));

        assertWorse(candidate, mockRankedCandidate(betterFitness, worseNodeCount));
        assertWorse(candidate, mockRankedCandidate(betterFitness, betterNodeCount));
        assertWorse(candidate, mockRankedCandidate(betterFitness, baseNodeCount));

        assertBetter(candidate, mockRankedCandidate(baseFitness, worseNodeCount));
        assertWorse(candidate, mockRankedCandidate(baseFitness, betterNodeCount));
        assertEqual(candidate, mockRankedCandidate(baseFitness, baseNodeCount));
    }

    private Evolved mockRankedCandidate(double fitness, int nodeCount) {
        Node mockNode = mockNode();
        given(mockNode.size()).willReturn(nodeCount);
        return new Evolved(mockNode, fitness);
    }

    public void assertBetter(Evolved a, Evolved b) {
        assert(a.pri() > b.pri());
    }

    public void assertWorse(Evolved a, Evolved b) {
        assertBetter(b, a);
    }

    public void assertEqual(Evolved a, Evolved b) {
        assertNotSame(a, b);
        assertEquals(a, b);
//        assertEquals(0, a.compareTo(b));
//        assertEquals(0, b.compareTo(a));
    }

    @Test
    public void testEquals() {
        double f = 7.5;
        Evolved a = new Evolved(integerConstant(0), f);
        Evolved b = new Evolved(integerConstant(0), f);
        Evolved c = new Evolved(integerConstant(0), f * 2);
        Evolved d = new Evolved(integerConstant(7), f);

        assertTrue(a.equals(a));
        assertEquals(a.hashCode(), a.hashCode());

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        assertFalse(a.equals(c));

        assertFalse(a.equals(d));

        assertFalse(a.equals("string"));
    }

    @Test
    public void testToString() {
        Evolved rankedCandidate = new Evolved(readNode("(+ 2 v0)"), 85.75);
        assertEquals("[(+ 2 v0) 85.75]", rankedCandidate.toString());
    }
}
