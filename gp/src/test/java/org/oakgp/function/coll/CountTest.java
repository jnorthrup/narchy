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
package org.oakgp.function.coll;

import org.oakgp.Arguments;
import org.oakgp.NodeType;
import org.oakgp.function.AbstractFnTest;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;

import static org.oakgp.NodeType.integerType;

public class CountTest extends AbstractFnTest {
    @Override
    protected Count getFunction() {
        return new Count(integerType());
    }

    @Override
    public void testEvaluate() {
        ConstantNode emptyList = new ConstantNode(new Arguments(new Node[]{}), NodeType.arrayType(NodeType.integerType()));
        evaluate("(count v0)").assigned(emptyList).to(0);
        evaluate("(count [2 -12 8])").to(3);
        evaluate("(count [2 -12 8 -3 -7])").to(5);
    }

    @Override
    public void testCanSimplify() {
        simplify("(count [2 -12 8])").to("3");
    }

    @Override
    public void testCannotSimplify() {
    }
}
