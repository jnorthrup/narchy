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

import org.oakgp.Assignments;
import org.oakgp.node.Node;
import org.oakgp.rank.fitness.FitFn;

/**
 * Calculates the fitness of candidate solutions to navigate an artificial ant around a two-dimensional grid.
 */
class ArtificialAntFitFn implements FitFn {
    private static final int MAX_MOVES = 200;

    @Override
    public double doubleValueOf(Node candidate) {
        MutableState state = new MutableState(GridReader.copyGrid());
        evaluate(candidate, state);
        return calculateFitness(state);
    }

    /**
     * Keeps evaluating the given candidate using the given state until either the maximum number of moves have been taken or all the food has been eaten.
     */
    private void evaluate(Node candidate, MutableState state) {
        Assignments assignments = new Assignments(state);
        int movesTaken = 0;
        while (movesTaken < MAX_MOVES && isRemainingFood(state)) {
            candidate.eval(assignments);

            
            
            if (movesTaken == state.getMovesTaken()) {
                return;
            }

            movesTaken = state.getMovesTaken();
        }
    }

    /**
     * Calculate fitness of the given state.
     * <p>
     * Any state where all the food has been eaten is better than any state where there is food remaining. The best solution is the one where all the food has
     * been eaten in the quickest time.
     */
    private double calculateFitness(MutableState state) {
        if (isRemainingFood(state)) {
            return MAX_MOVES*2 - state.getFoodEaten();
        } else {
            return state.getMovesTaken();
        }
    }

    /**
     * Returns {@code true} if the given state contains cells with food that have not yet been visited.
     */
    private boolean isRemainingFood(MutableState state) {
        return state.getFoodEaten() < GridReader.getNumberOfFoodCells();
    }
}
