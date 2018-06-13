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
package org.oakgp.examples.ant;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.Type;
import org.oakgp.function.ImpureFunction;
import org.oakgp.util.Signature;

/**
 * Returns {@code true} if the square the ant is facing contains food, else {@code false}.
 */
class IsFoodAhead implements ImpureFunction {
    @Override
    public Signature sig() {
        return new Signature(Type.booleanType(), MutableState.STATE_TYPE);
    }

    @Override
    public Boolean evaluate(Arguments arguments, Assignments assignments) {
        MutableState state = arguments.firstArg().eval(assignments);
        return state.isFoodAhead();
    }
}
