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
package org.oakgp.primitive;

import org.junit.jupiter.api.Test;
import org.oakgp.node.VariableNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.TestUtils.assertUnmodifiable;
import static org.oakgp.Type.*;

public class VariableSetTest {
    @Test
    public void testGetById() {
        VariableSet s = VariableSet.createVariableSet(integerType(), booleanType(), integerType());

        VariableNode v0 = s.getById(0);
        VariableNode v1 = s.getById(1);
        VariableNode v2 = s.getById(2);

        assertSame(v0, s.getById(0));
        assertSame(integerType(), v0.returnType());

        assertSame(v1, s.getById(1));
        assertSame(booleanType(), v1.returnType());

        assertSame(v2, s.getById(2));
        assertSame(integerType(), v2.returnType());

        assertNotEquals(v0, v1);
        assertNotEquals(v0, v2);
        assertNotEquals(v1, v2);
    }

    @Test
    public void assertGetByType() {
        VariableSet s = VariableSet.createVariableSet(integerType(), booleanType(), integerType());

        VariableNode v0 = s.getById(0);
        VariableNode v1 = s.getById(1);
        VariableNode v2 = s.getById(2);

        List<VariableNode> integers = s.getByType(integerType());
        assertEquals(2, integers.size());
        assertSame(v0, integers.get(0));
        assertSame(v2, integers.get(1));

        List<VariableNode> booleans = s.getByType(booleanType());
        assertEquals(1, booleans.size());
        assertSame(v1, booleans.get(0));

        assertNull(s.getByType(stringType()));
    }

    @Test
    public void assertGetByTypeUnmodifiable() {
        VariableSet s = VariableSet.createVariableSet(integerType(), booleanType(), integerType());
        List<VariableNode> integers = s.getByType(integerType());
        assertUnmodifiable(integers);
    }
}
