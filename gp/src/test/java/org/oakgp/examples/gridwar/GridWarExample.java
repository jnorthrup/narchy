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
import org.oakgp.Type;
import org.oakgp.function.Function;
import org.oakgp.function.choice.If;
import org.oakgp.function.compare.*;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.tournament.FirstPlayerAdvantageGame;
import org.oakgp.rank.tournament.TwoPlayerGame;
import org.oakgp.util.GPRandom;
import org.oakgp.util.StdRandom;
import org.oakgp.util.Utils;

import static org.oakgp.Type.integerType;
import static org.oakgp.util.Utils.intArrayType;

public class GridWarExample {
    private static final int NUM_VARIABLES = 5;
    private static final int NUM_GENERATIONS = 100;
    private static final int INITIAL_POPULATION_SIZE = 50;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;

    public static void main(String[] args) {
        Function[] functions = {IntFunc.the.add, IntFunc.the.subtract, IntFunc.the.getMultiply(),
                LessThan.create(integerType()), LessThanOrEqual.create(integerType()), new GreaterThan(integerType()), new GreaterThanOrEqual(integerType()),
                new Equal(integerType()), new NotEqual(integerType()), new If(integerType())};
        ConstantNode[] constants = Utils.intConsts(0, 4);
        Type[] variables = intArrayType(NUM_VARIABLES);
        GPRandom random = new StdRandom();
        
        TwoPlayerGame game = new FirstPlayerAdvantageGame(new GridWar(random));

        Candidates output = new Evolution().returns(integerType()).constants(constants).variables(variables).functions(functions)
                .setTwoPlayerGame(game).population(INITIAL_POPULATION_SIZE).depth(INITIAL_POPULATION_MAX_DEPTH)
                .setMaxGenerations(NUM_GENERATIONS).get();
        Node best = output.best().node;
        System.out.println(best);
    }
}
