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
package org.oakgp.terminate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.oakgp.TestUtils.singletonRankedCandidates;

public class TargetFitnessTerminatorTest {
    @Test
    public void test() {
        TargetFitnessTerminator t = new TargetFitnessTerminator(c -> c.pri() == 0);

        assertTrue(shouldTerminate(t, 0));

        assertFalse(shouldTerminate(t, 1));
        assertFalse(shouldTerminate(t, -1));
        assertFalse(shouldTerminate(t, 2));
        assertFalse(shouldTerminate(t, -2));
    }

    private boolean shouldTerminate(TargetFitnessTerminator t, double fitness) {
        return t.test(singletonRankedCandidates(fitness));
    }
}
