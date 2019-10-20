/*
 * Copyright 2012 Jonathan Leahey
 *
 * This file is part of Minicraft
 *
 * Minicraft is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Minicraft is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Minicraft. If not, see http:
 */

package nars.experiment.minicraft.side;

public class Template implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public final int outCount;
    public final Int2 position = new Int2(0, 0);

    private char[][] matrix;

    public Template(int[][] matrix, int outCount) {
        if (matrix != null) {

            this.matrix = new char[matrix.length][matrix[0].length];
            for (var i = 0; i < matrix.length; i++) {
                for (var j = 0; j < matrix[0].length; j++) {
                    this.matrix[i][j] = (char) matrix[i][j];
                }
            }
        }
        this.outCount = outCount;
    }

    public boolean compare(char[][] input) {
        if (matrix == null) {
            return false;
        }

        for (var x = 0; x <= (input.length - matrix.length); x++) {
            for (var y = 0; y <= (input[0].length - matrix[0].length); y++) {
                var isGood = false;
                var isBad = false;

                for (var i = 0; i < matrix.length; i++) {
                    for (var j = 0; j < matrix[0].length; j++) {
                        if (matrix[i][j] != input[x + i][y + j]) {
                            if (input[x + i][y + j] != 0) {
                                return false;
                            }
                            if (matrix[i][j] != 0 && input[x + i][y + j] == 0) {
                                isBad = true;
                            }
                        } else if (input[x + i][y + j] != 0 && matrix[i][j] != 0) {
                            isGood = true;
                        }
                    }
                }

                if (isGood && !isBad) {
                    position.x = x;
                    position.y = y;
                    return true;
                }

            }
        }

        return false;
    }
}
