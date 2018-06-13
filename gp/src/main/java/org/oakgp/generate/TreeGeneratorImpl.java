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
package org.oakgp.generate;

import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.primitive.PrimitiveSet;
import org.oakgp.util.Signature;

import java.util.Objects;
import java.util.Random;
import java.util.function.IntPredicate;

/**
 * Provides different strategies for creating a tree data structure.
 * <p>
 * Can be used to create randomly generate the initial population of a genetic programming run.
 *
 * @see #full(PrimitiveSet)
 * @see #grow(PrimitiveSet, Random)
 */
public final class TreeGeneratorImpl implements TreeGenerator {
    private final PrimitiveSet primitiveSet;
    private final IntPredicate strategy;

    /**
     * @see #full(PrimitiveSet)
     * @see #grow(PrimitiveSet, Random)
     */
    private TreeGeneratorImpl(PrimitiveSet primitiveSet, IntPredicate strategy) {
        Objects.requireNonNull(primitiveSet);
        this.primitiveSet = primitiveSet;
        this.strategy = strategy;
    }

    /**
     * Creates a {@code TreeGenerator} that uses the "full" approach to creating trees.
     * <p>
     * The "full" approach constructs trees where all terminal nodes (i.e. leaf nodes) are at the same depth.
     *
     * @param primitiveSet the collection of functions, variables and constants from which tree will be constructed
     * @return a {@code TreeGenerator} that uses the "full" approach to creating trees.
     */
    public static TreeGenerator full(PrimitiveSet primitiveSet) {
        return new TreeGeneratorImpl(primitiveSet, d -> d > 0);
    }

    /**
     * Creates a {@code TreeGenerator} that uses the "grow" approach to creating trees.
     * <p>
     * The "grow" approach constructs trees where terminal nodes (i.e. leaf nodes) are located at random depths, within a maximum limit.
     *
     * @param primitiveSet the collection of functions, variables and constants from which tree will be constructed
     * @param random       used to randomly determine the structure of the generated trees
     * @return a {@code TreeGenerator} that uses the "grow" approach to creating trees.
     */
    public static TreeGenerator grow(PrimitiveSet primitiveSet, Random random) {
        return new TreeGeneratorImpl(primitiveSet, d -> d > 0 && random.nextBoolean());
    }

    @Override
    public Node generate(NodeType type, int depth) {
        if (shouldCreateFunction(type, depth)) {
            Fn function = primitiveSet.next(type);
            Signature signature = function.sig();
            Node[] args = new Node[signature.size()];
            for (int i = 0; i < args.length; i++) {
                NodeType argType = signature.argType(i);
                args[i] = generate(argType, depth - 1);
            }
            return new FnNode(function, args);
        } else {
            return primitiveSet.nextTerminal(type);
        }
    }

    private boolean shouldCreateFunction(NodeType type, int depth) {
        if (!primitiveSet.hasTerminals(type)) {
            return true;
        } else if (!primitiveSet.hasFunctions(type)) {
            return false;
        } else {
            return strategy.test(depth);
        }
    }
}
