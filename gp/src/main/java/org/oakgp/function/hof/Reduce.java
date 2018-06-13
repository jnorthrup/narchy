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
import org.oakgp.util.Signature;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;

import static org.oakgp.Type.arrayType;
import static org.oakgp.Type.functionType;

/**
 * Combines the elements of a collection by recursively applying a function.
 * <p>
 * Expects three arguments:
 * <ol>
 * <li>A function.</li>
 * <li>An initial value.</li>
 * <li>A collection.</li>
 * </ol>
 *
 * @see <a href="http:
 */
public final class Reduce implements Function {
    private final Signature signature;

    /**
     * Creates a higher order functions that recursively applies a function to the elements of a collection.
     *
     * @param type the type of the elements contained in the collection - this will also be the type associated with the value produced by evaluating this function
     */
    public Reduce(Type type) {
        signature = new Signature(type, functionType(type, type, type), type, arrayType(type));
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        Function f = arguments.firstArg().eval(assignments);
        Node result = arguments.secondArg();
        Arguments candidates = arguments.thirdArg().eval(assignments);
        for (int i = 0; i < candidates.length(); i++) {
            result = new ConstantNode(f.evaluate(new Arguments(result, candidates.get(i)), assignments), f.sig().returnType());
        }
        return result.eval(assignments);
    }

    @Override
    public Signature sig() {
        return signature;
    }
}
