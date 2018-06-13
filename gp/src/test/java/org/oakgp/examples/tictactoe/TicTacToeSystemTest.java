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
package org.oakgp.examples.tictactoe;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.oakgp.Assignments;
import org.oakgp.Evolution;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.function.choice.If;
import org.oakgp.function.choice.OrElse;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.rank.fitness.FitFn;
import org.oakgp.rank.tournament.FirstPlayerAdvantageGame;
import org.oakgp.rank.tournament.TwoPlayerGame;
import org.oakgp.util.DummyNode;
import org.oakgp.util.Utils;

import static org.oakgp.NodeType.type;

@Disabled
public class TicTacToeSystemTest {
    private static final int NUM_GENERATIONS = 10;
    private static final int INITIAL_POPULATION_SIZE = 50;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;
    private static final NodeType MOVE_TYPE = type("move");
    private static final NodeType POSSIBLE_MOVE = type("possibleMove");
    private static final NodeType[] VARIABLE_TYPES = {
            type("board"),
            type("symbol"),
            type("symbol")
    };

    @Test
    public void testHighLevel() {
        Fn[] functions = {new GetPossibleMove("corner", Board::getFreeCorner), new GetPossibleMove("centre", Board::getFreeCentre),
                new GetPossibleMove("side", Board::getFreeSide), new GetWinningMove(), new GetAnyMove(), new OrElse(MOVE_TYPE)};
        TwoPlayerGame game = createTicTacToeGame();

        new Evolution().returns(MOVE_TYPE).constants().variables(VARIABLE_TYPES).functions(functions).setTwoPlayerGame(game)
                .populationSize(INITIAL_POPULATION_SIZE).populationDepth(INITIAL_POPULATION_MAX_DEPTH).stopGenerations(NUM_GENERATIONS).get();
    }

    @Test
    public void testLowLevelTournament() {
        Fn[] functions = {new IsFree(), new IsOccupied(), new GetAnyMove(), new IfValidMove(), new OrElse(MOVE_TYPE), new And(), new If(POSSIBLE_MOVE)};
        ConstantNode[] constants = getMoveConstants();
        TwoPlayerGame game = createTicTacToeGame();

        new Evolution().returns(MOVE_TYPE).constants(constants).variables(VARIABLE_TYPES).functions(functions).setTwoPlayerGame(game)
                .populationSize(INITIAL_POPULATION_SIZE).populationDepth(INITIAL_POPULATION_MAX_DEPTH).stopGenerations(NUM_GENERATIONS).get();
    }

    @Test
    public void testLowLevelFitnessFunction() {
        Fn[] functions = {new IsFree(), new IsOccupied(), new GetAnyMove(), new IfValidMove(), new OrElse(MOVE_TYPE), new And(), new If(POSSIBLE_MOVE)};
        ConstantNode[] constants = getMoveConstants();
        TicTacToeFitFn fitnessFunction = new TicTacToeFitFn();

        new Evolution().returns(MOVE_TYPE).constants(constants).variables(VARIABLE_TYPES).functions(functions)
                .goal(fitnessFunction).populationSize(INITIAL_POPULATION_SIZE).populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                .stopGenerations(NUM_GENERATIONS).get();
    }

    private ConstantNode[] getMoveConstants() {
        return Utils.enumConsts(Move.class, POSSIBLE_MOVE);
    }

    private TwoPlayerGame createTicTacToeGame() {
        return new FirstPlayerAdvantageGame(new TicTacToe());
    }

    private class TicTacToeFitFn implements FitFn {
        private TicTacToe ticTacToe = new TicTacToe();
        private Node[] ais = new Node[]{
                new DummyNode() {
                    @Override
                    public Move eval(Assignments assignments) {
                        Board board = (Board) assignments.get(0);
                        return board.getFreeMove();
                    }
                }, new DummyNode() {
            @Override
            public Move eval(Assignments assignments) {
                Board board = (Board) assignments.get(0);
                Move nextMove = board.getWinningMove(Symbol.X);
                if (nextMove == null) {
                    nextMove = board.getFreeMove();
                }
                return nextMove;
            }
        }, new DummyNode() {
            @Override
            public Move eval(Assignments assignments) {
                Board board = (Board) assignments.get(0);
                Move nextMove = board.getWinningMove(Symbol.O);
                if (nextMove == null) {
                    nextMove = board.getFreeMove();
                }
                return nextMove;
            }
        }, new DummyNode() {
            @Override
            public Move eval(Assignments assignments) {
                Board board = (Board) assignments.get(0);
                Move nextMove = board.getWinningMove(Symbol.X);
                if (nextMove == null) {
                    nextMove = board.getWinningMove(Symbol.O);
                }
                if (nextMove == null) {
                    nextMove = board.getFreeCorner();
                }
                if (nextMove == null) {
                    nextMove = board.getFreeCentre();
                }
                if (nextMove == null) {
                    nextMove = board.getFreeMove();
                }
                return nextMove;
            }
        }, new DummyNode() {
            @Override
            public Move eval(Assignments assignments) {
                Board board = (Board) assignments.get(0);
                Move nextMove = board.getWinningMove(Symbol.X);
                if (nextMove == null) {
                    nextMove = board.getWinningMove(Symbol.O);
                }
                if (nextMove == null) {
                    nextMove = board.getFreeCorner();
                }
                if (nextMove == null) {
                    nextMove = board.getFreeCentre();
                }
                if (nextMove == null) {
                    nextMove = board.getFreeMove();
                }
                return nextMove;
            }
        }};

        @Override
        public double doubleValueOf(Node candidate) {
            int result = 0;
            for (Node ai : ais) {
                result += ticTacToe.evaluate(ai, candidate);
            }
            return result;
        }
    }
}
