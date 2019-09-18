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

import org.oakgp.function.Fn;
import org.oakgp.util.Signature;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.oakgp.util.Utils.groupBy;

/**
 * Represents the set of possible {@code Function} implementations to use during a genetic programming run.
 */
public final class FnSet extends NodeSet<Fn> {
    private final Map<Signature, Fn[]> functionsBySignature;

    /**
     * Constructs a function set containing the specified functions.
     */
    public FnSet(Fn... functions) {
        super(groupBy(functions, f -> f.sig().returnType()));
        this.functionsBySignature = groupBy(functions, Fn::sig);
    }

//    /**
//     * Returns a list of all functions in this set that have the specified return type.
//     *
//     * @param type the type to find matching functions of
//     * @return a list of all functions in this set that have the specified return type, or {@code null} if there are no functions with the required return type
//     * in this setAt
//     */
//    public List<Fn> getByType(NodeType type) {
//
//        return byType.get(type);
//    }

    /**
     * Returns a list of all functions in this set that have the specified signature.
     *
     * @param signature the signature to find matching functions of
     * @return a list of all functions in this set that have the specified signature, or {@code null} if there are no functions with the required signature in
     * this setAt
     */
    public List<Fn> asList(Signature signature) {
        Fn[] elements = get(signature);
        return elements != null ? List.of(elements) : Collections.EMPTY_LIST;
    }

    public Fn[] get(Signature signature) {
        return functionsBySignature.get(signature);
    }

    public Fn randomAlternate(Fn current, Random rng) {
        return NodeSet.randomAlternate(current, get(current.sig()), rng);
    }
}
