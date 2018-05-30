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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.oakgp.TestUtils.*;
import static org.oakgp.util.NodeComparator.NODE_COMPARATOR;

public class NodeComparatorTest {
    @Test
    public void testCompareVariables() {
        assertOrdered(createVariable(0), createVariable(1));
    }

    @Test
    public void testCompareConstants() {
        assertOrdered(integerConstant(3), integerConstant(7));
    }

    @Test
    public void testCompareFunctionsSameReturnType() {
        
        
        assertOrdered(readNode("(+ 1 1)"), readNode("(- 1 1)"));
        assertOrdered(readNode("(* 3 3)"), readNode("(* 3 4)"));
    }

    @Test
    public void testCompareFunctionsDifferentReturnTypes() {
        
        
        
        assertOrdered(readNode("(pos? 1)"), readNode("(+ 1 1)"));
    }

    @Test
    public void testCompareConstantsToVariables() {
        assertOrdered(integerConstant(7), createVariable(3));
        assertOrdered(integerConstant(3), createVariable(7));
    }

    @Test
    public void testCompareConstantsToFunctions() {
        assertOrdered(integerConstant(7), readNode("(+ 1 1)"));
    }

    @Test
    public void testCompareVariablesToFunctions() {
        assertOrdered(createVariable(7), readNode("(+ 1 1)"));
    }

    private void assertOrdered(Node n1, Node n2) {
        assertEquals(0, NODE_COMPARATOR.compare(n1, n1));
        assertTrue(NODE_COMPARATOR.compare(n1, n2) < 0);
        assertTrue(NODE_COMPARATOR.compare(n2, n1) > 0);
    }
    @Test
    public void testCommutive() {
        Node x = readNode("(+ 2 1)");
        assertEquals(
            readNode("(+ 2 1)"),
            readNode("(+ 1 2)")
        );
        assertEquals(
                readNode("(* 2 1)"),
                readNode("(* 1 2)")
        );
    }

    @Test
    public void testSort() {
        Node f1 = readNode("(+ 1 1)");
        Node f2 = readNode("(- 1 1)");
        Node f3 = readNode("(* 1 1)");
        List<Node> nodes = new ArrayList<>();
        nodes.add(f1);
        nodes.add(f2);
        nodes.add(readNode("-1"));
        nodes.add(readNode("v1"));
        nodes.add(readNode("3"));
        nodes.add(f3);
        nodes.add(readNode("v0"));


        for (int i = 0; i < 10; i++) {
            Collections.shuffle(nodes);
            Collections.sort(nodes, NODE_COMPARATOR);
            assertEquals("[-1, 3, v0, v1, (* 1 1), (+ 1 1), (- 1 1)]", nodes.toString());
        }










    }
}
