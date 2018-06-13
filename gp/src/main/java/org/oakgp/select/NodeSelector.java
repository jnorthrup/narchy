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
package org.oakgp.select;

import org.oakgp.node.Node;
import org.oakgp.rank.Ranking;

import java.util.function.Supplier;

/**
 * Used to obtain {@code Node} instances.
 * <p>
 * The strategy to determine what is returned, and in what order, will depend on the specific implementation of {@code NodeSelector} that is being used.
 */
public interface NodeSelector extends Supplier<Node> {

    /** called before the selector is utilized on a new population distribution */
    void reset(Ranking living);


//    /**
//     * Returns a {@code Node}.
//     *
//     * @return a {@code Node}
//     */
//    Node apply(Ranking pop);
}
