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
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof Evolved) {
            Evolved r = (Evolved) o;
            return this.id.equals(r.id) && this.pri() == r.pri();
        } else {
            return false;
        }
    }


//    @Override
//    public int compareTo(Evolved o) {
//        if (this == o) return 0;
//
//        int result = Double.compare(pri(), o.pri());
//        if (result == 0) {
//
//            return Integer.compare(o.id.size(), id.size());
//        } else {
//            return result;
//        }
//    }

    @Override
    public String toString() {
        return "[" + id + ' ' + pri() + ']';
    }
}
