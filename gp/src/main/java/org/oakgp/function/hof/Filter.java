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
package org.oakgp.function.hof;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.Type;
import org.oakgp.function.Function;
import org.oakgp.function.Signature;
import org.oakgp.node.Node;

import java.util.ArrayList;
import java.util.List;

import static org.oakgp.Type.*;

/**
 * Uses a function to filter the elements of a collection.
 * <p>
 * Returns a new collection that exists of all the elements in the collection (specified by the second argument) for which the function (specified by the first
 * argument) returns {@code true}.
 *
 * @see <a href="http:
 */
public final class Filter implements Function {
    private final Signature signature;

    /**
     * Creates a higher order functions that filters the elements of a collection.
     *
     * @param type the type of the elements contained in the collection
     */
    public Filter(Type type) {
        signature = new Signature(arrayType(type), functionType(booleanType(), type), arrayType(type));
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        Function f = arguments.firstArg().eval(assignments);
        Arguments candidates = arguments.secondArg().eval(assignments);
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < candidates.length(); i++) {
            Node candidate = candidates.get(i);
            if ((Boolean) f.evaluate(new Arguments(candidate), assignments)) {
                result.add(candidate);
            }
        }
        Node[] args = result.toArray(new Node[0]);
        return new Arguments(args);
    }

    @Override
    public Signature sig() {
        return signature;
    }
}
