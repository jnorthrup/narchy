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
import org.oakgp.NodeType;
import org.oakgp.node.ConstantNode;

import static org.junit.jupiter.api.Assertions.*;

public class VoidTest {
    @Test
    public void testConstantNode() {
        assertSame(Void.VOID_TYPE, Void.VOID_CONSTANT.returnType());
        assertSame(Void.VOID, Void.VOID_CONSTANT.eval(null));
    }

    @Test
    public void testType() {
        assertSame(NodeType.type("void"), Void.VOID_TYPE);
    }

    @Test
    public void testToString() {
        assertEquals("void", Void.VOID.toString());
    }

    @Test
    public void testIsVoid() {
        assertTrue(Void.isVoid(Void.VOID_CONSTANT));
        assertTrue(Void.isVoid(new ConstantNode(Void.VOID, NodeType.type("void"))));
        assertTrue(Void.isVoid(new ConstantNode(Void.VOID, NodeType.type("dummy"))));
    }

    @Test
    public void testIsNotVoid() {
        assertFalse(Void.isVoid(new ConstantNode(new Object(), NodeType.type("void"))));
        assertFalse(Void.isVoid(new ConstantNode(1, NodeType.integerType())));
    }
}
