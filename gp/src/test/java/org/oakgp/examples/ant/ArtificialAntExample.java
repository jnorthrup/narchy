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
package org.oakgp.examples.ant;

import org.oakgp.Evolution;
import org.oakgp.function.Function;
import org.oakgp.function.choice.If;
import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.fitness.FitnessFunction;

import static org.oakgp.examples.ant.AntMovement.*;
import static org.oakgp.examples.ant.BiSequence.BISEQUENCE;
import static org.oakgp.examples.ant.MutableState.STATE_TYPE;
import static org.oakgp.examples.ant.TriSequence.TRISEQUENCE;
import static org.oakgp.util.Void.VOID_CONSTANT;
import static org.oakgp.util.Void.VOID_TYPE;

public class ArtificialAntExample {
    private static final int TARGET_FITNESS = 0;
    private static final int NUM_GENERATIONS = 1000;
    private static final int INITIAL_POPULATION_SIZE = 100;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;

    public static void main(String[] args) {
        Function[] functions = {new If(VOID_TYPE), new IsFoodAhead(), FORWARD, LEFT, RIGHT, BISEQUENCE, TRISEQUENCE};
        FitnessFunction fitnessFunction = new ArtificialAntFitnessFunction();

        Candidates output = new Evolution().returns(VOID_TYPE).constants(VOID_CONSTANT).variables(STATE_TYPE).functions(functions)
                .goal(fitnessFunction).population(INITIAL_POPULATION_SIZE).depth(INITIAL_POPULATION_MAX_DEPTH)
                .goalTarget(TARGET_FITNESS).setMaxGenerations(NUM_GENERATIONS).get();
        Node best = output.best().node;
        System.out.println(best);
    }
}
