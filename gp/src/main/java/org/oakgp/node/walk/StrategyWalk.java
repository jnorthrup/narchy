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
package org.oakgp.node.walk;

import org.oakgp.Arguments;
import org.oakgp.function.Fn;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.node.NodeType;

import java.util.function.Predicate;

/**
 * Provides a mechanism for recursively visiting nodes in a tree structure, controlled by a user-defined strategy.
 */
public final class StrategyWalk {
    /**
     * Private constructor as all methods are static.
     */
    private StrategyWalk() {
        
    }

    /**
     * Returns the total number of nodes contained in the tree-structure represented by the given {@code Node} that match the specified predicate.
     *
     * @param treeWalkerStrategy the predicate used to determine if a {@code Node} should be included in the count
     */
    public static int getNodeCount(Node node, Predicate<Node> treeWalkerStrategy) {
        int total = treeWalkerStrategy.test(node) ? 1 : 0;
        if (NodeType.isFunction(node)) {
            FnNode functionNode = (FnNode) node;
            Arguments arguments = functionNode.args();
            int n = arguments.length();
            for (int i = 0; i < n; i++) {
                total += getNodeCount(arguments.get(i), treeWalkerStrategy);
            }
        }
        return total;
    }

    /**
     * Returns a {@code Node} from the tree structure represented by the given {@code Node} that matches the specified predicate.
     */
    public static Node getAt(Node node, int index, Predicate<Node> treeWalkerStrategy) {
        getAt:
        while (true) {
            if (NodeType.isFunction(node)) {
                FnNode functionNode = (FnNode) node;
                Arguments arguments = functionNode.args();
                int total = 0;
                int n = arguments.length();
                for (int i = 0; i < n; i++) {
                    Node child = arguments.get(i);
                    int c = getNodeCount(child, treeWalkerStrategy);
                    if (total + c > index) {
                        index = index - total;
                        node = child;
                        continue getAt;
                    } else {
                        total += c;
                    }
                }
                if (!treeWalkerStrategy.test(node)) {
                    throw new IllegalStateException();
                }
            }
            return node;
        }
    }

    /**
     * Returns a new {@code Node} resulting from replacing the {@code Node} at position {@code index} of the given {@code Node} with the result of
     * {@code replacement}.
     *
     * @param index       the index of the {@code Node}, in the tree structure represented by this object, that needs to be replaced
     * @param replacement the function to apply to the {@code Node} at {@code index} to determine the {@code Node} that should replace it
     * @return a new {@code Node} derived from replacing the {@code Node} at {@code index} with the result of {@code replacement}
     */
    public static Node replaceAt(Node node, int index, java.util.function.Function<Node, Node> replacement, Predicate<Node> treeWalkerStrategy) {
        if (NodeType.isFunction(node)) {
            FnNode functionNode = (FnNode) node;
            Arguments args = functionNode.args();
            int total = 0;
            int n = args.length();
            for (int i = 0; i < n; i++) {
                Node child = args.get(i);
                int c = getNodeCount(child, treeWalkerStrategy);
                if (total + c > index) {
                    Fn f = functionNode.func();
                    return new FnNode(f,
                            args.replaceAt(i, replaceAt(child, index - total, replacement, treeWalkerStrategy)));
                } else {
                    total += c;
                }
            }
        }
        return replacement.apply(node);
    }
}
