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

import jcog.Util;
import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;

/**
 * Contains a function (operator) and the arguments (operands) to apply to it.
 */
public final class FnNode implements Node {

    private final Fn function;
    private final Arguments arguments;
    private final int nodeCount;
    private final int hash;

    /**
     * Constructs a new {@code FunctionNode} with the specified function function and arguments.
     *
     * @param function  the function to associate with this {@code FunctionNode}
     * @param arguments the arguments (i.e. operands) to apply to {@code function} when evaluating this {@code FunctionNode}
     */
    public FnNode(Fn function, Node... arguments) {
        this(function, Arguments.get(function, arguments));
    }

    /**
     * Constructs a new {@code FunctionNode} with the specified function function and arguments.
     *
     * @param function  the function to associate with this {@code FunctionNode}
     * @param arguments the arguments (i.e. operands) to apply to {@code function} when evaluating this {@code FunctionNode}
     */
    public FnNode(Fn function, Arguments arguments) {
        if (function.argsSorted()) {



            arguments = arguments.sorted();
        }

        this.function = function;
        this.arguments = arguments;
        this.nodeCount = calculateNodeCount(arguments);
        
        this.hash = Util.hashCombine(function.hashCode(), arguments.hashCode());
    }

    private static int calculateNodeCount(Arguments arguments) {
        int total = 1;
        int n = arguments.length();
        for (int i = 0; i < n; i++) {
            total += arguments.get(i).size();
        }
        return total;
    }

    public Fn func() {
        return function;
    }

    public Arguments args() {
        return arguments;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Object eval(Assignments assignments) {
        return function.evaluate(arguments, assignments);
    }

    @Override
    public int size() {
        return nodeCount;
    }

    @Override
    public int depth() {
        return arguments.depth;
    }

    @Override
    public NodeType returnType() {
        return function.sig().returnType();
    }

    @Override
    public final org.oakgp.node.NodeType nodeType() {
        return org.oakgp.node.NodeType.FUNCTION;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || this.hash != o.hashCode()) {
            return false;
        } else if (o instanceof FnNode) {
            FnNode fn = (FnNode) o;
            
            return this.function == fn.function && this.arguments.equals(fn.arguments);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('(').append(function.name());
        int n = arguments.length();
        for (int i = 0; i < n; i++) {
            sb.append(' ').append(arguments.get(i));
        }
        return sb.append(')').toString();
    }
}
