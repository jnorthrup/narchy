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
import org.oakgp.rank.Evolved;
import org.oakgp.rank.Ranking;
import org.oakgp.util.DummyRandom;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.oakgp.TestUtils.integerConstant;

public class FitnessSelectorTest {
    @Test
    public void test() {
        Evolved c1 = new Evolved(integerConstant(1), 4);
        Evolved c2 = new Evolved(integerConstant(2), 2);
        Evolved c3 = new Evolved(integerConstant(3), 1);
        Ranking candidates = new Ranking(c1, c2, c3);

        DummyRandom r = new DummyRandom(.0, .57, .58, .85, .86, .999, .25, .65, .93);
        NodeSelector s = createFitnessProportionateSelection(r, candidates);

        s.reset(candidates);
        assertEquals(c1.get(), s.get());
        assertEquals(c1.get(), s.get());
        assertEquals(c2.get(), s.get());
        assertEquals(c2.get(), s.get());
        assertEquals(c3.get(), s.get());
        assertEquals(c3.get(), s.get());
        assertEquals(c1.get(), s.get());
        assertEquals(c2.get(), s.get());
        assertEquals(c3.get(), s.get());

        r.assertEmpty();
    }

    private FitnessSelector createFitnessProportionateSelection(Random random, Ranking candidates) {
        return new FitnessSelector(random);
    }
}
