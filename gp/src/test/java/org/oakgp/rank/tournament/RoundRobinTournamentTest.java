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
package org.oakgp.rank.tournament;

import org.junit.jupiter.api.Test;
import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.GenerationRanker;

import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.oakgp.TestUtils.assertRankedCandidate;
import static org.oakgp.TestUtils.integerConstant;

public class RoundRobinTournamentTest {
    @Test
    public void test() {
        
        Node a = integerConstant(1);
        Node b = integerConstant(2);
        Node c = integerConstant(3);

        
        TwoPlayerGame mockGame = mock(TwoPlayerGame.class);
        given(mockGame.evaluate(a, b)).willReturn(5d);
        given(mockGame.evaluate(a, c)).willReturn(-3d);
        given(mockGame.evaluate(b, c)).willReturn(2d);

        
        GenerationRanker tournament = new RoundRobinTournament(mockGame);
        Candidates output = tournament.apply(Stream.of(a,b,c));

        
        assertRankedCandidate(output.get(0), a, 2);
        assertRankedCandidate(output.get(1), c, 1);
        assertRankedCandidate(output.get(2), b, -3);
    }
}
