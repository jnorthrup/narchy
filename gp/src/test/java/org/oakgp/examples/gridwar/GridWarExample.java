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
package org.oakgp.examples.gridwar;

import org.oakgp.Evolution;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.function.choice.If;
import org.oakgp.function.compare.*;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.rank.Ranking;
import org.oakgp.rank.tournament.FirstPlayerAdvantageGame;
import org.oakgp.rank.tournament.TwoPlayerGame;
import org.oakgp.util.Utils;

import java.util.Random;

import static org.oakgp.NodeType.integerType;
import static org.oakgp.util.Utils.intArrayType;

public class GridWarExample {
    private static final int NUM_VARIABLES = 5;
    private static final int NUM_GENERATIONS = 100;
    private static final int INITIAL_POPULATION_SIZE = 50;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;

    public static void main(String[] args) {
        Fn[] functions = {IntFunc.the.add, IntFunc.the.subtract, IntFunc.the.multiply,
                LessThan.create(integerType()), LessThanOrEqual.create(integerType()), new GreaterThan(integerType()), new GreaterThanOrEqual(integerType()),
                new Equal(integerType()), new NotEqual(integerType()), new If(integerType())};
        ConstantNode[] constants = Utils.intConsts(0, 4);
        NodeType[] variables = intArrayType(NUM_VARIABLES);
        Random random = new Random();
        
        TwoPlayerGame game = new FirstPlayerAdvantageGame(new GridWar(random));

        Ranking output = new Evolution().returns(integerType()).constants(constants).variables(variables).functions(functions)
                .setTwoPlayerGame(game).populationSize(INITIAL_POPULATION_SIZE).populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                .stopGenerations(NUM_GENERATIONS).get();
        Node best = output.top().get();
        System.out.println(best);
    }
}
