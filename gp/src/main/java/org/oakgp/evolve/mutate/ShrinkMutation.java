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
import org.oakgp.node.Node;
import org.oakgp.node.NodeType;
import org.oakgp.node.walk.StrategyWalk;
import org.oakgp.primitive.PrimitiveSet;
import org.oakgp.select.NodeSelector;
import org.oakgp.util.Utils;

import java.util.Random;

/**
 * Replaces a randomly selected function node of the parent with a terminal node.
 * <p>
 * The resulting offspring will be smaller than the parent.
 */
public final class ShrinkMutation implements GeneticOperator {
    private final Random random;
    private final PrimitiveSet primitiveSet;

    /**
     * Creates a {@code ShrinkMutation} that uses a {@code Random} to select function nodes to replace with terminals from a {@code PrimitiveSet}.
     *
     * @param random       used to randomly select function nodes
     * @param primitiveSet used to select terminal nodes to replace function nodes with
     */
    public ShrinkMutation(Random random, PrimitiveSet primitiveSet) {
        this.random = random;
        this.primitiveSet = primitiveSet;
    }

    @Override
    public Node apply(NodeSelector selector) {
        Node root = selector.get();
        int nodeCount = StrategyWalk.getNodeCount(root, NodeType::isFunction);
        if (nodeCount == 0) {
            
            
            return root;
        } else if (nodeCount == 1) {
            
            
            return primitiveSet.nextTerminal(root);
        } else {
            int index = Utils.selectSubNodeIndex(random, nodeCount);
            return StrategyWalk.replaceAt(root, index, primitiveSet::nextTerminal, NodeType::isFunction);
        }
    }
}
