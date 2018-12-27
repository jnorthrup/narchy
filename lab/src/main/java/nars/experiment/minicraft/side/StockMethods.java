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

import jcog.Util;

public final class StockMethods {
    public static Boolean onScreen = true;
    public static final Int2 pos = new Int2(0, 0);

    public static Int2 computeDrawLocationInPlace(float cameraX, float cameraY, int width,
                                                  int height, int tileSize, float positionX, float positionY) {
        StockMethods.pos.x = Math.round((positionX - cameraX) * tileSize);
        StockMethods.pos.y = Math.round((positionY - cameraY) * tileSize);
        onScreen = !(pos.x + tileSize < 0 || pos.x > width * tileSize || pos.y + tileSize < 0 || pos.y > height
                * tileSize);
        return StockMethods.pos;
    }


    /**
     * Smoothly interpolates between edge0 and edge1 by x
     * <p>
     * This function plays like a sigmoid but is easier to compute
     *
     * @param edge0
     * @param edge1
     * @param x
     */
    public static float smoothStep(float edge0, float edge1, float x) {
        float t = Util.unitize((x - edge0) / (edge1 - edge0));
        return t * t * (3f - 2f * t);
    }


}
