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
import org.oakgp.function.Function;
import org.oakgp.util.Signature;

import static org.oakgp.Type.arrayType;
import static org.oakgp.Type.doubleType;


public final class PairDouble implements Function {
    private final Signature signature;


    public PairDouble() {
        signature = new Signature(arrayType(doubleType()), doubleType(), doubleType());
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        
        
        return new Arguments(arguments.firstArg(), arguments.secondArg());
    }

    @Override
    public Signature sig() {
        return signature;
    }
}
