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
package org.oakgp.examples.hanoi;

import org.oakgp.Evolution;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.function.choice.If;
import org.oakgp.function.choice.SwitchEnum;
import org.oakgp.function.compare.Equal;
import org.oakgp.function.compare.GreaterThan;
import org.oakgp.function.compare.LessThan;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.rank.Ranking;
import org.oakgp.rank.fitness.FitFn;
import org.oakgp.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;
import static org.oakgp.NodeType.*;
import static org.oakgp.util.Utils.enumConsts;

public class TowersOfHanoiExample {
    static final NodeType STATE_TYPE = type("gameState");
    static final NodeType MOVE_TYPE = type("move");
    static final NodeType POLE_TYPE = type("pole");

    private static final int TARGET_FITNESS = 0;
    private static final int NUM_GENERATIONS = 1000;
    private static final int INITIAL_POPULATION_SIZE = 100;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;

    public static void main(String[] args) {
        Fn[] functions = {new If(MOVE_TYPE), new Equal(MOVE_TYPE), new IsValid(), new SwitchEnum(Move.class, nullableType(MOVE_TYPE), MOVE_TYPE),
                new GreaterThan(integerType()), LessThan.create(integerType()), new Equal(integerType()), new Next()};
        List<ConstantNode> constants = createConstants();
        NodeType[] variables = {STATE_TYPE, nullableType(MOVE_TYPE)};
        FitFn fitnessFunction = new TowersOfHanoiFitFn(false);

        Ranking output = new Evolution().returns(MOVE_TYPE).constants(constants).variables(variables).functions(functions)
                .goal(fitnessFunction).populationSize(INITIAL_POPULATION_SIZE).populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                .stopFitness(TARGET_FITNESS).stopGenerations(NUM_GENERATIONS).get();

        output.forEach(System.out::println);

        Node best = output.top().get();
        System.out.println(best);
        new TowersOfHanoiFitFn(true).doubleValueOf(best);
    }

    private static List<ConstantNode> createConstants() {
        List<ConstantNode> constants = new ArrayList<>();
        constants.add(IntFunc.the.zero);
        constants.add(Utils.TRUE_NODE);
        addAll(constants, enumConsts(Move.class, MOVE_TYPE));
        addAll(constants, enumConsts(TowersOfHanoi.Pole.class, POLE_TYPE));
        return constants;
    }
}
