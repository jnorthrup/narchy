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

import org.oakgp.NodeType;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.groupingBy;
import static org.oakgp.NodeType.*;

/**
 * Utility methods that support the functionality provided by the rest of the framework.
 */
public enum Utils { ;
    /**
     * Represents the boolean value {@code true}.
     */
    public static final ConstantNode TRUE_NODE = new ConstantNode(TRUE, booleanType());
    /**
     * Represents the boolean value {@code false}.
     */
    public static final ConstantNode FALSE_NODE = new ConstantNode(FALSE, booleanType());



    /**
     * Returns an array consisting of a {@code ConstantNode} instance for each of the possible values of the specified enum.
     *
     * @param e the enum that the {@code ConstantNode} instances should wrap
     * @param t the {@code Type} that should be associated with the {@code ConstantNode} instances
     */
    public static ConstantNode[] enumConsts(Class<? extends Enum<?>> e, NodeType t) {
        Enum<?>[] enumConstants = e.getEnumConstants();
        ConstantNode[] constants = new ConstantNode[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            constants[i] = new ConstantNode(enumConstants[i], t);
        }
        return constants;
    }

    /**
     * Returns an array consisting of a {@code ConstantNode} instance for each of the integer values in the specified range.
     *
     * @param minInclusive the minimum value (inclusive) to be represented by a {@code ConstantNode} in the returned array
     * @param maxInclusive the minimum value (inclusive) to be represented by a {@code ConstantNode} in the returned array
     */
    public static ConstantNode[] intConsts(int minInclusive, int maxInclusive) {
        ConstantNode[] constants = new ConstantNode[maxInclusive - minInclusive + 1];
        for (int n = minInclusive, i = 0; n <= maxInclusive; i++, n++) {
            constants[i] = new ConstantNode(n, integerType());
        }
        return constants;
    }
    public static ConstantNode[] doubleConsts(int minInclusive, int maxInclusive) {
        ConstantNode[] constants = new ConstantNode[maxInclusive - minInclusive + 1];
        for (int n = minInclusive, i = 0; n <= maxInclusive; i++, n++) {
            constants[i] = new ConstantNode((double)n, doubleType());
        }
        return constants;
    }

    /**
     * Creates an array of the specified size and assigns the result of {@link NodeType#integerType()} to each element.
     */
    public static NodeType[] intArrayType(int size) {
        NodeType[] array = new NodeType[size];
        NodeType type = integerType();
        Arrays.fill(array, type);
        return array;
    }

    /**
     * Returns a map grouping the specified nodes by their {@code Type}.
     */
    public static <T extends Node> Map<NodeType, T[]> groupByType(T[] nodes) {
        return groupBy(nodes, Node::returnType);
    }

    /**
     * Returns a map grouping the specified values according to the specified classification function.
     *
     * @param values     the values to group
     * @param valueToKey the classification function used to group values
     */
    public static <K, V> Map<K, V[]> groupBy(V[] values, Function<V, K> valueToKey) {
        Map nodesByType = Arrays.stream(values).collect(groupingBy(valueToKey));
        for (Map.Entry e : (Iterable<Map.Entry<K, ?>>)nodesByType.entrySet()) {
            List<V> l = (List<V>) (e.getValue());
            e.setValue(l.toArray(Arrays.copyOfRange(values, 0, l.size())));
        }
        return Map.copyOf(nodesByType);
    }

    /**
     * Returns randomly selected index of a node from the specified tree.
     */
    public static int selectSubNodeIndex(Random random, Node tree) {
        int nodeCount = tree.size();
        if (nodeCount == 1) {
            
            return 0;
        } else {
            return selectSubNodeIndex(random, nodeCount);
        }
    }

    /**
     * Returns a int value between 0 (inclusive) and the specified {@code nodeCount} value minus 1 (exclusive).
     */
    public static int selectSubNodeIndex(Random random, int nodeCount) {
        
        return random.nextInt(nodeCount - 1);
    }

}
