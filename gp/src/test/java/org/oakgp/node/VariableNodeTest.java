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
package org.oakgp.node;

import org.junit.jupiter.api.Test;
import org.oakgp.Assignments;
import org.oakgp.NodeType;

import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.TestUtils.createVariable;
import static org.oakgp.TestUtils.integerConstant;
import static org.oakgp.NodeType.integerType;

public class VariableNodeTest {
    @Test
    public void testGetters() {
        final int id = 7;
        final VariableNode v = createVariable(id);
        assertEquals(id, v.getId());
        assertEquals(1, v.size());
        assertEquals(1, v.depth());
        assertSame(integerType(), v.returnType());
    }

    @Test
    public void testToString() {
        assertEquals("v5", createVariable(5).toString());
    }

    @Test
    public void testEvaluate() {
        final Integer expected = 9;
        final VariableNode v = createVariable(0);
        final Assignments assignments = new Assignments(expected);
        final Object actual = (int) v.eval(assignments);
        assertSame(expected, actual);
    }

    @Test
    public void testEqualsAndHashCode() {
        final VariableNode n1 = new VariableNode(1, NodeType.integerType());
        final VariableNode n2 = new VariableNode(1, NodeType.integerType());
        assertNotSame(n1, n2);
        assertEquals(n1, n1);
        assertEquals(n2, n2);
        assertEquals(n1.hashCode(), n2.hashCode());
        
        assertNotEquals(n1, n2);
    }

    @Test
    public void testNotEquals() {
        final VariableNode n = createVariable(1);
        assertNotEquals(n, createVariable(0));
        assertNotEquals(n, createVariable(2));
        assertNotEquals(n, integerConstant(1));
        assertNotEquals(n, Integer.valueOf(1));
    }
}
