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

import org.oakgp.function.AbstractFnTest;
import org.oakgp.function.Fn;
import org.oakgp.function.classify.IsNegative;
import org.oakgp.function.classify.IsPositive;
import org.oakgp.function.classify.IsZero;

import static org.oakgp.NodeType.booleanType;
import static org.oakgp.NodeType.integerType;
import static org.oakgp.TestUtils.createArguments;

public class MapTest extends AbstractFnTest {
    @Override
    protected Map getFunction() {
        return new Map(integerType(), booleanType());
    }

    @Override
    public void testEvaluate() {
        evaluate("(map pos? [2 -12 8 -3 -7 6])").to(createArguments("true", "false", "true", "false", "false", "true"));
        evaluate("(map neg? [2 -12 8 -3 -7 6])").to(createArguments("false", "true", "false", "true", "true", "false"));
        evaluate("(map zero? [2 -12 8 -3 -7 6])").to(createArguments("false", "false", "false", "false", "false", "false"));
    }

    @Override
    public void testCanSimplify() {
        simplify("(map pos? [2 -12 8])").to("[true false true]");
    }

    @Override
    public void testCannotSimplify() {
    }

    @Override
    protected Fn[] getFunctionSet() {
        return new Fn[]{getFunction(), new IsPositive(), new IsNegative(), new IsZero()};
    }
}
