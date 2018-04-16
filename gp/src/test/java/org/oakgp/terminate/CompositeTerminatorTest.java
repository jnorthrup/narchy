/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.terminate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oakgp.rank.Candidates;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.oakgp.TestUtils.singletonRankedCandidates;

public class CompositeTerminatorTest {
    private final Candidates candidates = singletonRankedCandidates();

    private Predicate<Candidates> t1;
    private Predicate<Candidates> t2;
    private Predicate<Candidates> t3;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setup() {
        t1 = mock(Predicate.class);
        t2 = mock(Predicate.class);
        t3 = mock(Predicate.class);
    }

    @Test
    public void testContinue() {
        assertContinue();

        verifyExecuted(t1);
        verifyExecuted(t2);
        verifyExecuted(t3);
    }

    @Test
    public void testTerminateFirst() {
        returnTrue(t1);

        assertTerminate();

        verifyExecuted(t1);
        verifyNotExecuted(t2);
        verifyNotExecuted(t3);
    }

    @Test
    public void testTerminateSecond() {
        returnTrue(t2);

        assertTerminate();

        verifyExecuted(t1);
        verifyExecuted(t2);
        verifyNotExecuted(t3);
    }

    @Test
    public void testTerminateThird() {
        returnTrue(t3);

        assertTerminate();

        verifyExecuted(t1);
        verifyExecuted(t2);
        verifyExecuted(t3);
    }

    private void returnTrue(Predicate<Candidates> t) {
        when(t.test(candidates)).thenReturn(true);
    }

    private void assertTerminate() {
        assertTrue(getOutcome());
    }

    private void assertContinue() {
        assertFalse(getOutcome());
    }

    private boolean getOutcome() {
        CompositeTerminator composite = new CompositeTerminator(t1, t2, t3);
        return composite.test(candidates);
    }

    private void verifyExecuted(Predicate<Candidates> t) {
        verifyExecuted(t, 1);
    }

    private void verifyNotExecuted(Predicate<Candidates> t) {
        verifyExecuted(t, 0);
    }

    private void verifyExecuted(Predicate<Candidates> t, int times) {
        verify(t, times(times)).test(candidates);
    }

}
