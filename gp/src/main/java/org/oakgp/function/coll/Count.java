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
package org.oakgp.function.coll;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.util.Signature;

import static org.oakgp.NodeType.arrayType;
import static org.oakgp.NodeType.integerType;

/**
 * Determines the number of elements contained in a collection.
 */
public final class Count implements Fn {
    private final Signature signature;

    /**
     * Constructs a function to return the number of items in collections of the specified type.
     */
    public Count(NodeType t) {
        signature = new Signature(integerType(), arrayType(t));
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        Arguments a = arguments.firstArg().eval(assignments);
        return a.length();
    }

    @Override
    public Signature sig() {
        return signature;
    }
}
