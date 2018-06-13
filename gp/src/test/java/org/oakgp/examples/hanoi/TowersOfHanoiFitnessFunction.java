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

import org.oakgp.Assignments;
import org.oakgp.node.Node;
import org.oakgp.rank.fitness.FitnessFunction;

import java.util.HashSet;
import java.util.Set;

/**
 * Determines the fitness of potential solutions to the Towers of Hanoi puzzle.
 */
class TowersOfHanoiFitnessFunction implements FitnessFunction {
    private static final TowersOfHanoi START_STATE = new TowersOfHanoi();

    private final boolean doLog;

    TowersOfHanoiFitnessFunction(boolean doLog) {
        this.doLog = doLog;
    }

    /**
     * @param n a potential solution to the Towers of Hanoi puzzle
     * @return the fitness of {@code n}
     */
    @Override
    public double doubleValueOf(Node n) {
        TowersOfHanoi towersOfHanoi = START_STATE;
        Set<TowersOfHanoi> previousStates = new HashSet<>();
        previousStates.add(towersOfHanoi);

        Move previousMove = null;
        int previousFitness = Integer.MAX_VALUE;
        while (true) {
            
            Assignments assignments = new Assignments(towersOfHanoi, previousMove);
            previousMove = n.eval(assignments);
            towersOfHanoi = towersOfHanoi.move(previousMove);
            if (doLog) {
                System.out.println(previousMove + " " + towersOfHanoi);
            }

            if (towersOfHanoi == null) {
                
                return previousFitness;
            }
            if (!previousStates.add(towersOfHanoi)) {
                
                return previousFitness;
            }
            previousFitness = Math.min(previousFitness, towersOfHanoi.getFitness());
            if (previousFitness == 0) {
                
                return previousFitness;
            }
        }
    }
}
