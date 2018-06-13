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
package org.oakgp.function.choice;

import org.oakgp.NodeType;
import org.oakgp.function.AbstractFnTest;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.node.VariableNode;
import org.oakgp.util.NodeSimplifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.oakgp.NodeType.nullableType;
import static org.oakgp.NodeType.stringType;

public class OrElseTest extends AbstractFnTest {
    private static final OrElse EXAMPLE = new OrElse(stringType());

    @Override
    protected OrElse getFunction() {
        return EXAMPLE;
    }

    @Override
    public void testEvaluate() {
        ConstantNode neverNullValue = new ConstantNode("default", stringType());

        ConstantNode nullValue = new ConstantNode(null, nullableType(stringType()));
        evaluate("(orelse v0 v1)").assigned(nullValue, neverNullValue).to("default");

        ConstantNode nonNullValue = new ConstantNode("hello", nullableType(stringType()));
        evaluate("(orelse v0 v1)").assigned(nonNullValue, neverNullValue).to("hello");
    }

    @Override
    public void testCanSimplify() {
        ConstantNode arg1 = new ConstantNode("hello", nullableType(stringType()));
        ConstantNode arg2 = new ConstantNode("world!", stringType());
        simplify(new FnNode(getFunction(), arg1, arg2), new ConstantNode("hello", stringType()));

        VariableNode v0 = new VariableNode(0, NodeType.stringType());
        FnNode fn = new FnNode(getFunction(), v0, arg2);
        simplify(new FnNode(getFunction(), v0, fn), fn);

        simplify(new FnNode(getFunction(), v0, new FnNode(getFunction(), v0, new FnNode(getFunction(), v0, fn))), fn);

        VariableNode v1 = new VariableNode(1, NodeType.stringType());
        simplify(new FnNode(getFunction(), v0, new FnNode(getFunction(), v1, new FnNode(getFunction(), v0, fn))), new FnNode(
                getFunction(), v0, new FnNode(getFunction(), v1, arg2)));
    }

    private void simplify(FnNode input, Node expected) {
        assertEquals(expected, NodeSimplifier.simplify(input));
    }

    @Override
    public void testCannotSimplify() {
        cannotSimplify("(orelse v0 v1)", nullableType(stringType()), stringType());
    }
}
