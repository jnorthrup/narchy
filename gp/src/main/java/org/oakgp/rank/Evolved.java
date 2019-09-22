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
package org.oakgp.rank;

import jcog.pri.NLink;
import org.oakgp.node.Node;

/**
 * Associates a {@code Node} with its fitness value.
 */
public final class Evolved extends NLink<Node> {


    /**
     * Creates a {@code RankedCandidate} which associates the given {@code Node} with the given fitness value.
     */
    public Evolved(Node node, double pri) {
        super(node, (float)pri);
    }

    @Override
    public boolean equals(Object that) {
        return this==that;
    }

    @Override
    public String toString() {
        return "[" + id + ' ' + pri() + ']';
    }
}
