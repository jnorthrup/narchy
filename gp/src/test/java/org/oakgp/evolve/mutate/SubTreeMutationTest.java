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
package org.oakgp.evolve.mutate;

import org.junit.jupiter.api.Test;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.select.DummyNodeSelector;

import static org.oakgp.TestUtils.*;
import static org.oakgp.util.DummyRandom.GetIntExpectation.nextInt;

public class SubTreeMutationTest {
    @Test
    public void test() {
        Node input = readNode("(+ (+ (if (zero? v0) 7 8) v1) (+ 9 v2))");
        DummyNodeSelector selector = new DummyNodeSelector(input);
        ConstantNode result = integerConstant(42);
        SubTreeMutation mutator = new SubTreeMutation(nextInt(10).returns(3), (t, d) -> result);
        assertNodeEquals("(+ (+ 9 v2) (+ 42 (if (zero? v0) 7 8)))", mutator.apply(selector));
    }
}
