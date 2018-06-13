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

import org.oakgp.Arguments;
import org.oakgp.Evolution;
import org.oakgp.function.Fn;
import org.oakgp.function.choice.If;
import org.oakgp.function.sequence.BiSequence;
import org.oakgp.function.sequence.TriSequence;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.rank.Ranking;

import static org.oakgp.examples.ant.AntMovement.*;
import static org.oakgp.examples.ant.MutableState.STATE_TYPE;
import static org.oakgp.util.Void.VOID_CONSTANT;
import static org.oakgp.util.Void.VOID_TYPE;

public class ArtificialAntExample {
    private static final int TARGET_FITNESS = 0;
    private static final int NUM_GENERATIONS = 500;
    private static final int INITIAL_POPULATION_SIZE = 90;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;


    static final BiSequence antBiSequence = new BiSequence() {
        @Override
        public String toString() {
            return "antBi";
        }

        @Override
        public boolean isMutex(Node firstArg, Node secondArg) {
            return isLeftAndRight(firstArg, secondArg);
        }
    };

    static final TriSequence antTriSequence = new TriSequence(antBiSequence) {
        @Override
        public String toString() {
            return "antTri";
        }

        @Override public Node simplify(Arguments arg) {
            Node n = super.simplify(arg);
            if (n == null) {
                Node first = arg.firstArg(), second = arg.secondArg(), third = arg.thirdArg();
                if (areAllSame(LEFT, first, second, third))
                    return new FnNode(RIGHT, ((FnNode) first).args());
                if (areAllSame(RIGHT, first, second, third))
                    return new FnNode(LEFT, ((FnNode) first).args());
            }
            return n;
        }
    };

    public static void main(String[] args) {
        Fn[] functions = {
            new If(VOID_TYPE),
            new IsFoodAhead(),
            FORWARD,
            LEFT, RIGHT,
            antBiSequence,
            antTriSequence
        };

        Ranking output = Evolution
                .of(VOID_TYPE)
                .constants(VOID_CONSTANT)
                .variables(STATE_TYPE)
                .functions(functions)
                .goal(new ArtificialAntFitFn())
                .populationSize(INITIAL_POPULATION_SIZE)
                .populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                .stopFitness(TARGET_FITNESS)
                .stopGenerations(NUM_GENERATIONS)
                .get();

        output.forEach(System.out::println);

        System.out.println();

        Node best = output.top().id;
        System.out.println(best);


    }
}
