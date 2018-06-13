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
package org.oakgp.function.math;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.node.Node;

/**
 * Performs division.
 */
public final class Divide extends ArithmeticOperator {
    private final NumFunc<?> numberUtils;


    /**
     * @see NumFunc#getDivide()
     */
    Divide(NumFunc<?> numberUtils) {
        super(numberUtils.type);
        this.numberUtils = numberUtils;
    }

    @Override
    protected Object evaluate(Node arg1, Node arg2, Assignments assignments) {
        if (numberUtils.one.equals(arg2))
            return arg1.eval(assignments);

        if (numberUtils.zero.equals(arg2)) {
            return numberUtils.one.eval(null);
        } else {
            return numberUtils.divide(arg1, arg2, assignments).eval(null);
        }
    }

    @Override
    public Node simplify(Arguments arguments) {
        Node arg2 = arguments.secondArg();
        if (numberUtils.zero.equals(arg2)) {
            return numberUtils.one;
        } else if (numberUtils.one.equals(arg2)) {
            return arguments.firstArg();
        } else if (numberUtils.minusOne.equals(arg2)) {
            return numberUtils.negate(arguments.firstArg());
        } else if (arguments.firstArg().equals(arg2)) {
            return numberUtils.one;
        } else {
            return null;
        }
    }

    @Override
    public String name() {
        return "/";
    }
}
