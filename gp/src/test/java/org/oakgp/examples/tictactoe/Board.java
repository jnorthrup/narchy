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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static java.util.EnumSet.*;
import static org.oakgp.examples.tictactoe.Move.*;
import static org.oakgp.examples.tictactoe.Symbol.X;

final class Board {
    private static final int MAX_MOVES = Move.values().length;
    private static final List<EnumSet<Move>> LINES = new ArrayList<>();
    private static final EnumSet<Move> ALL = allOf(Move.class);
    private static final EnumSet<Move> NONE = noneOf(Move.class);
    private static final EnumSet<Move> CORNERS = of(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT);
    private static final EnumSet<Move> SIDES = of(TOP_CENTRE, MIDDLE_RIGHT, BOTTOM_CENTRE, BOTTOM_LEFT);

    static {
        
        LINES.add(of(TOP_LEFT, TOP_CENTRE, TOP_RIGHT));
        LINES.add(of(MIDDLE_LEFT, CENTRE, MIDDLE_RIGHT));
        LINES.add(of(BOTTOM_LEFT, BOTTOM_CENTRE, BOTTOM_RIGHT));

        
        LINES.add(of(TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT));
        LINES.add(of(TOP_CENTRE, CENTRE, BOTTOM_CENTRE));
        LINES.add(of(TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT));

        
        LINES.add(of(TOP_LEFT, CENTRE, BOTTOM_RIGHT));
        LINES.add(of(TOP_RIGHT, CENTRE, BOTTOM_LEFT));
    }

    private final EnumSet<Move> oState;
    private final EnumSet<Move> xState;

    Board() {
        this(NONE, NONE);
    }

    private Board(EnumSet<Move> oState, EnumSet<Move> xState) {
        this.oState = oState;
        this.xState = xState;
    }

    Board update(Symbol symbol, Move move) {
        if (isInvalid(move)) {
            throw new IllegalArgumentException("Invalid move:" + move + " o: " + oState + " x: " + xState);
        } else if (symbol == X) {
            return new Board(oState, copyAndAdd(xState, move));
        } else {
            return new Board(copyAndAdd(oState, move), xState);
        }
    }

    private EnumSet<Move> copyAndAdd(EnumSet<Move> original, Move move) {
        EnumSet<Move> updatedMoves = copyOf(original);
        updatedMoves.add(move);
        return updatedMoves;
    }

    private boolean isInvalid(Move move) {
        return !isFree(move) || isWinner(xState) || isWinner(oState) || isDraw();
    }

    boolean isDraw() {
        return oState.size() + xState.size() == MAX_MOVES;
    }

    boolean isWinner(Symbol symbol) {
        return isWinner(getState(symbol));
    }

    boolean isWinner(EnumSet<Move> moves) {
        return LINES.stream().filter(l -> getIntersectionSize(l, moves) == 3).findFirst().isPresent();
    }

    Move getWinningMove(Symbol symbol) {
        EnumSet<Move> moves = getState(symbol);
        EnumSet<Move> opponents = getState(symbol.getOpponent());
        Optional<EnumSet<Move>> o = LINES.stream().filter(l -> getIntersectionSize(l, moves) == 2 && getIntersectionSize(l, opponents) == 0).findFirst();
        return o.map(l -> first(difference(l, moves))).orElse(null);
    }

    Move getFreeCorner() {
        return getFree(CORNERS);
    }

    Move getFreeSide() {
        return getFree(SIDES);
    }

    Move getFreeCentre() {
        return isFree(CENTRE) ? CENTRE : null;
    }

    Move getFreeMove() {
        return getFree(ALL);
    }

    boolean isOccupied(Move m, Symbol s) {
        return getState(s).contains(m);
    }

    boolean isFree(Move m) {
        return !oState.contains(m) && !xState.contains(m);
    }

    private Move getFree(EnumSet<Move> possibles) {
        return possibles.stream().filter(this::isFree).findFirst().orElse(null);
    }

    private int getIntersectionSize(EnumSet<Move> a, EnumSet<Move> b) {
        return intersection(a, b).size();
    }

    private EnumSet<Move> intersection(EnumSet<Move> a, EnumSet<Move> b) {
        EnumSet<Move> intersection = copyOf(a);
        intersection.retainAll(b);
        return intersection;
    }

    private EnumSet<Move> difference(EnumSet<Move> a, EnumSet<Move> b) {
        EnumSet<Move> intersection = copyOf(a);
        intersection.removeAll(b);
        return intersection;
    }

    private Move first(EnumSet<Move> s) {
        return s.iterator().next();
    }

    private EnumSet<Move> getState(Symbol s) {
        return s == X ? xState : oState;
    }
}
