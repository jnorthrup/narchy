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

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.function.Function;
import org.oakgp.util.Signature;

import static org.oakgp.Type.booleanType;
import static org.oakgp.examples.hanoi.TowersOfHanoiExample.MOVE_TYPE;
import static org.oakgp.examples.hanoi.TowersOfHanoiExample.STATE_TYPE;

/**
 * Determines if a move is a valid move for a particular game state.
 */
class IsValid implements Function {
    private static final Signature SIGNATURE = new Signature(booleanType(), STATE_TYPE, MOVE_TYPE);

    @Override
    public Signature sig() {
        return SIGNATURE;
    }

    /**
     * @param arguments   the first argument is a {@code TowersOfHanoi} representing a game state and the second argument is a {@code Move}
     * @param assignments the values assigned to each of member of the variable set
     * @return {@code true} if the specified move is a valid move for the specified game state, else {@code false}
     */
    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        TowersOfHanoi gameState = arguments.firstArg().eval(assignments);
        Move move = arguments.secondArg().eval(assignments);
        return gameState.move(move) != null;
    }
}
