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
package org.oakgp.evolve.mutate;

import org.oakgp.evolve.GeneticOperator;
import org.oakgp.function.Fn;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.node.walk.NodeWalk;
import org.oakgp.primitive.PrimitiveSet;
import org.oakgp.select.NodeSelector;
import org.oakgp.util.Utils;

import java.util.Random;

import static org.oakgp.node.NodeType.isFunction;

/**
 * Randomly changes a point (node) in the parent.
 * <p>
 * A node in the parent is selected at random and replaced with another primitive of the same type and arity.
 * </p>
 * <p>
 * Also known as node replacement mutation.
 * </p>
 */
public final class PointMutation implements GeneticOperator {
    private final Random random;
    private final PrimitiveSet primitiveSet;

    /**
     * Creates a {@code PointMutation} that uses the given {@code Random} to select nodes to mutate.
     *
     * @param random       used to randomly select nodes to mutate
     * @param primitiveSet used to select replacements for nodes selected for mutation
     */
    public PointMutation(Random random, PrimitiveSet primitiveSet) {
        this.random = random;
        this.primitiveSet = primitiveSet;
    }

    @Override
    public Node apply(NodeSelector selector) {
        Node root = selector.get();
        int mutationPoint = Utils.selectSubNodeIndex(random, root);
        return NodeWalk.replaceAt(root, mutationPoint, node -> {
            if (isFunction(node)) {
                FnNode functionNode = (FnNode) node;
                Fn function = primitiveSet.next(functionNode.func());
                return new FnNode(function, functionNode.args());
            } else {
                return primitiveSet.nextTerminal(node);
            }
        });
    }
}
