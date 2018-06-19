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
package org.oakgp.serialize;

import org.junit.jupiter.api.Test;
import org.oakgp.Arguments;
import org.oakgp.function.classify.IsPositive;
import org.oakgp.function.hof.Filter;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.oakgp.NodeType.*;
import static org.oakgp.TestUtils.*;
import static org.oakgp.util.Void.VOID_CONSTANT;

public class NodeWriterTest {
    @Test
    public void testIntegerConstantNode() {
        NodeWriter writer = new NodeWriter();
        String output = writer.writeNode(integerConstant(768));
        assertEquals("768", output);
    }

    @Test
    public void testLongConstantNode() {
        NodeWriter writer = new NodeWriter();
        String output = writer.writeNode(longConstant(768));
        assertEquals("768L", output);
    }

    @Test
    public void testDoubleConstantNode() {
        NodeWriter writer = new NodeWriter();
        String output = writer.writeNode(doubleConstant(768));
        assertEquals("768.0", output);
    }

    @Test
    public void testBigDecimalConstantNode() {
        NodeWriter writer = new NodeWriter();
        String output = writer.writeNode(bigDecimalConstant("768"));
        assertEquals("768D", output);
    }

    @Test
    public void testBigIntegerConstantNode() {
        NodeWriter writer = new NodeWriter();
        String output = writer.writeNode(bigIntegerConstant("768"));
        assertEquals("768I", output);
    }

    @Test
    public void testVoidConstantNode() {
        NodeWriter writer = new NodeWriter();
        String output = writer.writeNode(VOID_CONSTANT);
        assertEquals("void", output);
    }

    @Test
    public void testVariableNode() {
        NodeWriter writer = new NodeWriter();
        String output = writer.writeNode(createVariable(2));
        assertEquals("v2", output);
    }

    @Test
    public void testFunctionNode() {
        NodeWriter writer = new NodeWriter();
        String output = writer.writeNode(new FnNode(IntFunc.the.add, integerConstant(5), createVariable(0)));
        assertEquals("(+ 5 v0)", output);
    }

    @Test
    public void testFunctionNodeWithFunctionNodeArguments() {
        NodeWriter writer = new NodeWriter();
        FnNode arg1 = new FnNode(IntFunc.the.subtract, integerConstant(5), createVariable(0));
        FnNode arg2 = new FnNode(IntFunc.the.multiply, createVariable(1), integerConstant(-6876));
        String output = writer.writeNode(new FnNode(IntFunc.the.add, arg1, arg2));
        assertEquals("(+ (* -6876 v1) (- 5 v0))", output);
    }

    @Test
    public void testArguments() {
        ConstantNode input = new ConstantNode(new Arguments(new Node[]{integerConstant(6), integerConstant(-2), integerConstant(17)}), arrayType(integerType()));
        String output = new NodeWriter().writeNode(input);
        assertEquals("[6 -2 17]", output);
    }

    @Test
    public void testFunctionAsArgument() {
        ConstantNode criteria = new ConstantNode(new IsPositive(), integerToBooleanFunctionType());
        ConstantNode args = new ConstantNode(new Arguments(new Node[]{integerConstant(6), integerConstant(-2), integerConstant(17)}), arrayType(integerType()));
        FnNode input = new FnNode(new Filter(integerType()), criteria, args);

        String output = new NodeWriter().writeNode(input);

        assertEquals("(filter pos? [6 -2 17])", output);
    }
}
