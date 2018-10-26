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

import org.oakgp.node.Node;
import org.oakgp.rank.Evolved;
import org.oakgp.rank.GenerationRanker;
import org.oakgp.rank.Ranking;

import java.util.stream.Stream;

/**
 * Ranks and sorts the fitness of {@code Node} instances using a {@code TwoPlayerGame} in a round-robin tournament.
 * this may be broken temporarily
 */
public final class RoundRobinTournamentRanker implements GenerationRanker {
    private final TwoPlayerGame game;
    //private final Cache<Node, Double> cache;

    /**
     * Creates a {@code RoundRobinTournament} for the given {@code TwoPlayerGame}.
     */
    public RoundRobinTournamentRanker(TwoPlayerGame game) {
        this.game = game;
        //this.cache = createCache(cacheSize);
    }

    private static void toRankedCandidates(Ranking r, Node[] input, double[] fitness) {
        int size = fitness.length;
        //Evolved[] output = new Evolved[size];
        for (int i = 0; i < size; i++) {
            /*output[i] =*/
            r.add(new Evolved(input[i], fitness[i] /* reverse porder */));
        }
        //return new Ranking(Stream.of(output), Collections.reverseOrder());

    }

    @Override
    public void accept(Stream<Node> input, Ranking r) {
        Node[] inputAsArray = input.toArray(Node[]::new);
        double[] fitness = evaluateFitness(inputAsArray);
        toRankedCandidates(r, inputAsArray, fitness);
    }

    private double[] evaluateFitness(Node[] input) {
        int size = input.length;
        double[] fitness = new double[size];
        for (int i1 = 0; i1 < size - 1; i1++) {
            Node player1 = input[i1];
            for (int i2 = i1 + 1; i2 < size; i2++) {
                Node player2 = input[i2];
                double result = game.evaluate(player1, player2);
                fitness[i1] += result;
                fitness[i2] -= result;
            }
        }
        return fitness;
    }
}
