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
package org.oakgp.primitive;

import org.jetbrains.annotations.Nullable;
import org.oakgp.NodeType;
import org.oakgp.node.Node;
import org.oakgp.util.Utils;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Represents the range of possible constants to use during a genetic programming run.
 */
public class NodeSet<N> {
    protected final Map<NodeType, N[]> byType;

    /**
     * Constructs a constant set containing the specified constants.
     */
    public static <X extends Node> NodeSet<X> byType(X... x) {
        return x.length > 0 ? new NodeSet<>(Utils.groupByType(x)) : empty();
    }

    public static <X> NodeSet<X> empty() {
        return new NodeSet<>(Map.of());
    }

    public NodeSet(Map<NodeType, N[]> t) {
        byType = t;
    }

    /**
     * Returns a list of all constants in this set that are of the specified type.
     *
     * @param type the type to find matching constants of
     * @return a list of all constants in this set that are the specified type, or {@code null} if there are no constants of the required type in this setAt
     */
    public List<N> asList(NodeType type) {
        N[] t = get(type);
        return t!=null ? List.of(t) : List.of();
    }

    @Nullable
    public N[] get(NodeType type) {
        return byType.get(type);
    }

    public boolean hasType(NodeType type) {
        return byType.containsKey(type);
    }

    @Nullable public N random(NodeType type, Random random) {
        N[] t = get(type);
        if (t  == null) return null;
        if (t.length == 1) return t[0];
        return t[random.nextInt(t.length)];
    }

    public N randomAlternate(NodeType type, N current, Random random) {
        return randomAlternate(current, get(type), random);
    }
    public N randomAlternate(NodeType type, Random random) {
        return randomAlternate(null, get(type), random);
    }

    protected static <C, P extends C> C randomAlternate(C currentVersion, P[] possibilities, Random rng) {
        if (possibilities == null) {
            return currentVersion;
        }

        int possibilitiesSize = possibilities.length;
        int randomIndex = possibilitiesSize == 1 ? 0 : rng.nextInt(possibilitiesSize);
        C next = possibilities[randomIndex];
        if (next == currentVersion) {
            if (possibilitiesSize == 1) {
                return currentVersion;
            } else {
                int secondRandomIndex = rng.nextInt(possibilitiesSize - 1);
                return possibilities[secondRandomIndex + ((secondRandomIndex >= randomIndex) ? 1 : 0)];
            }
        } else {
            return next;
        }
    }


}
