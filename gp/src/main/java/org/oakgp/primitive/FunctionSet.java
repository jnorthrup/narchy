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

import org.oakgp.Type;
import org.oakgp.function.Function;
import org.oakgp.util.Signature;

import java.util.List;
import java.util.Map;

import static org.oakgp.util.Utils.groupBy;

/**
 * Represents the set of possible {@code Function} implementations to use during a genetic programming run.
 */
public final class FunctionSet {
    private final Map<Type, List<Function>> functionsByType;
    private final Map<Signature, List<Function>> functionsBySignature;

    /**
     * Constructs a function set containing the specified functions.
     */
    public FunctionSet(Function... functions) {
        
        this.functionsByType = groupBy(functions, f -> f.sig().returnType());
        this.functionsBySignature = groupBy(functions, Function::sig);
    }

    /**
     * Returns a list of all functions in this set that have the specified return type.
     *
     * @param type the type to find matching functions of
     * @return a list of all functions in this set that have the specified return type, or {@code null} if there are no functions with the required return type
     * in this set
     */
    public List<Function> getByType(Type type) {
        
        return functionsByType.get(type);
    }

    /**
     * Returns a list of all functions in this set that have the specified signature.
     *
     * @param signature the signature to find matching functions of
     * @return a list of all functions in this set that have the specified signature, or {@code null} if there are no functions with the required signature in
     * this set
     */
    public List<Function> getBySignature(Signature signature) {
        
        return functionsBySignature.get(signature);
    }
}
