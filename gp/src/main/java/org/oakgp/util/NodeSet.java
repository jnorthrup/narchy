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

import org.oakgp.node.Node;

import java.util.LinkedHashSet;

/**
 * A {@code java.util.Set} of the simplified versions of {@code Node} instances.
 *
 * @see NodeSimplifier
 */
public final class NodeSet extends LinkedHashSet<Node> {


    /**
     * Adds the simplified version of the specified {@code Node} to this set if it is not already present.
     *
     * @param n element that a simplified version of will be added to this set
     * @return {@code true} if this set did not already contain a simplified version of the specified {@code Node}
     * @see NodeSimplifier
     */
    @Override
    public boolean add(Node n) {
        return super.add(NodeSimplifier.simplify(n));
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    



}
