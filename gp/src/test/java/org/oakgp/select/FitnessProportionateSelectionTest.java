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
package org.oakgp.select;

import org.junit.jupiter.api.Test;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.RankedCandidate;
import org.oakgp.util.DummyRandom;
import org.oakgp.util.GPRandom;

import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.oakgp.TestUtils.integerConstant;

public class FitnessProportionateSelectionTest {
    @Test
    public void test() {
        RankedCandidate c1 = new RankedCandidate(integerConstant(1), 4);
        RankedCandidate c2 = new RankedCandidate(integerConstant(2), 2);
        RankedCandidate c3 = new RankedCandidate(integerConstant(3), 1);
        Candidates candidates = new Candidates(Stream.of(c1, c2, c3), Collections.reverseOrder());

        DummyRandom r = new DummyRandom(.0, .57, .58, .85, .86, .999, .25, .65, .93);
        NodeSelector s = createFitnessProportionateSelection(r, candidates);

        assertEquals(c1.node, s.next());
        assertEquals(c1.node, s.next());
        assertEquals(c2.node, s.next());
        assertEquals(c2.node, s.next());
        assertEquals(c3.node, s.next());
        assertEquals(c3.node, s.next());
        assertEquals(c1.node, s.next());
        assertEquals(c2.node, s.next());
        assertEquals(c3.node, s.next());

        r.assertEmpty();
    }

    private FitnessProportionateSelection createFitnessProportionateSelection(GPRandom random, Candidates candidates) {
        return new FitnessProportionateSelectionFactory(random).getSelector(candidates);
    }
}
