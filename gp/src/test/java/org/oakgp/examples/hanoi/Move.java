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

/**
 * Represents the possible moves that can be used to attempt to solve the puzzle.
 */
enum Move {
    LEFT_MIDDLE(TowersOfHanoi.Pole.LEFT, TowersOfHanoi.Pole.MIDDLE),
    LEFT_RIGHT(TowersOfHanoi.Pole.LEFT, TowersOfHanoi.Pole.RIGHT),
    MIDDLE_LEFT(TowersOfHanoi.Pole.MIDDLE, TowersOfHanoi.Pole.LEFT),
    MIDDLE_RIGHT(TowersOfHanoi.Pole.MIDDLE, TowersOfHanoi.Pole.RIGHT),
    RIGHT_LEFT(TowersOfHanoi.Pole.RIGHT, TowersOfHanoi.Pole.LEFT),
    RIGHT_MIDDLE(TowersOfHanoi.Pole.RIGHT, TowersOfHanoi.Pole.MIDDLE);

    /**
     * The pole to remove a disc from.
     */
    final TowersOfHanoi.Pole from;
    /**
     * The pole to add a disc to.
     */
    final TowersOfHanoi.Pole to;

    private Move(TowersOfHanoi.Pole from, TowersOfHanoi.Pole to) {
        this.from = from;
        this.to = to;
    }
}
