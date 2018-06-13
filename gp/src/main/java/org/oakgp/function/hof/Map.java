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
package org.oakgp.function.hof;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.util.Signature;

import java.util.ArrayList;
import java.util.List;

import static org.oakgp.NodeType.arrayType;
import static org.oakgp.NodeType.functionType;

/**
 * Returns the result of applying a function to each element of a collection.
 * <p>
 * Returns a new collection that exists of the result of applying the function (specified by the first argument) to each element of the collection (specified by
 * the second argument).
 *
 * @see <a href="http:
 */
public final class Map implements Fn {
    private final Signature signature;

    /**
     * Creates a higher order functions that applies a function to each element of a collection.
     *
     * @param from the type of the elements contained in the collection provided as an argument to the function
     * @param to   the type of the elements contained in the collection returned by the function
     */
    public Map(NodeType from, NodeType to) {
        signature = new Signature(arrayType(to), functionType(to, from), arrayType(from));
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        Fn f = arguments.firstArg().eval(assignments);
        NodeType returnType = f.sig().returnType();
        Arguments candidates = arguments.secondArg().eval(assignments);
        int args = candidates.length();

        List<Node> result = new ArrayList<>(args);
        for (int i = 0; i < args; i++) {
            Node inputNode = candidates.get(i);
            Object evaluateResult = f.evaluate(new Arguments(inputNode), assignments);
            ConstantNode outputNode = new ConstantNode(evaluateResult, returnType);
            result.add(outputNode);
        }
        Node[] args1 = result.toArray(new Node[0]);
        return new Arguments(args1);
    }

    @Override
    public Signature sig() {
        return signature;
    }
}
