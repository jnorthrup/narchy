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

import static org.oakgp.NodeType.integerType;
import static org.oakgp.TestUtils.createArguments;

public class FilterTest extends AbstractFnTest {
    @Override
    protected Filter getFunction() {
        return new Filter(integerType());
    }

    @Override
    public void testEvaluate() {
        evaluate("(filter pos? [2 -12 8 -3 -7 6])").to(createArguments("2", "8", "6"));
        evaluate("(filter neg? [2 -12 8 -3 -7 6])").to(createArguments("-12", "-3", "-7"));
        evaluate("(filter zero? [2 -12 8 -3 -7 6])").to(createArguments());
    }

    @Override
    public void testCanSimplify() {
        simplify("(filter pos? [2 -12 8])").to("[2 8]");
    }

    @Override
    public void testCannotSimplify() {
    }

    @Override
    protected Fn[] getFunctionSet() {
        return new Fn[]{getFunction(), new IsPositive(), new IsNegative(), new IsZero()};
    }
}
