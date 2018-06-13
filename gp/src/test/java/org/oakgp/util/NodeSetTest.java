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

import org.junit.jupiter.api.Test;
import org.oakgp.node.Node;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.TestUtils.readNode;
import static org.oakgp.util.NodeSimplifier.simplify;

public class NodeSetTest {
    @Test
    public void testAdd() {
        Set<Node> s = new HashSet();

        Node n1 = readNode("(+ v0 (+ 3 1))");
        Node n2 = readNode("(+ v0 (- 7 3))");

        assertTrue(s.add(simplify(n1)));
        assertFalse(s.add(simplify(n2)));
        assertEquals(1, s.size());
        assertFalse(s.contains(n1));
        assertFalse(s.contains(n2));

        Node simplifiedVersion = readNode("(+ 4 v0)");
        assertTrue(s.contains(simplifiedVersion));
        assertFalse(s.add(simplifiedVersion));
        assertEquals(1, s.size());

        Node n3 = readNode("(* 4 v0)");
        Node n4 = readNode("(+ 5 v0)");
        assertTrue(s.add(n3));
        assertTrue(s.add(n4));

        assertTrue(s.contains(simplifiedVersion));
        assertTrue(s.contains(n3));
        assertTrue(s.contains(n4));
        assertEquals(3, s.size());
    }

    @Test
    public void testAddAll() {
        Node n1 = readNode("(+ (+ 3 1) v0)");
        Node n2 = readNode("(+ (- 7 3) v0)");
        Node n3 = readNode("(* 4 v0)");
        Node n4 = readNode("(+ 5 v0)");

        Set<Node> s = Stream.of(n1, n2, n3, n4).
                map(NodeSimplifier::simplify).collect(toSet());;

        Node simplifiedVersion = readNode("(+ 4 v0)");
        assertEquals(3, s.size());
        assertTrue(s.contains(simplifiedVersion));
        assertTrue(s.contains(n3));
        assertTrue(s.contains(n4));
    }
}
