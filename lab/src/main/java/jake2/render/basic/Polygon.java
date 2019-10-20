/*
 * Polygon.java
 * Copyright (C) 2003
 *
 * $Id: Polygon.java,v 1.2 2006-11-21 00:50:46 cawe Exp $
 */
/*
 Copyright (C) 1997-2001 Id Software, Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */
package jake2.render.basic;

import jake2.render.glpoly_t;

/**
 * Polygon
 * 
 * @author cwei
 */
public final class Polygon extends glpoly_t {

    private static final int MAXPOLYS = 20000;

    private static final int MAX_BUFFER_VERTICES = 120000;

    private static final float[] buffer = new float[MAX_BUFFER_VERTICES * STRIDE];

    private static int bufferIndex;

    private static int polyCount;

    private static final Polygon[] polyCache = new Polygon[MAXPOLYS];
    static {
        for (var i = 0; i < polyCache.length; i++) {
            polyCache[i] = new Polygon();
        }
    }

    public static glpoly_t create(int numverts) {
        var poly = polyCache[polyCount++];
        poly.clear();
        poly.numverts = numverts;
        poly.pos = bufferIndex;
        bufferIndex += numverts;
        return poly;
    }

    public static void reset() {
        polyCount = 0;
        bufferIndex = 0;
    }

    private Polygon() {
    }

    private final void clear() {
        next = null;
        chain = null;
        numverts = 0;
        flags = 0;
    }

    @Override
    public final float x(int index) {
        return buffer[offset(index)];
    }

    @Override
    public final void x(int index, float value) {
        buffer[offset(index)] = value;
    }

    @Override
    public final float y(int index) {
        return buffer[offset(index) + 1];
    }

    @Override
    public final void y(int index, float value) {
        buffer[offset(index) + 1] = value;
    }

    @Override
    public final float z(int index) {
        return buffer[offset(index) + 2];
    }

    @Override
    public final void z(int index, float value) {
        buffer[offset(index) + 2] = value;
    }

    @Override
    public final float s1(int index) {
        return buffer[offset(index) + 3];
    }

    @Override
    public final void s1(int index, float value) {
        buffer[offset(index) + 3] = value;
    }

    @Override
    public final float t1(int index) {
        return buffer[offset(index) + 4];
    }

    @Override
    public final void t1(int index, float value) {
        buffer[offset(index) + 4] = value;
    }

    @Override
    public final float s2(int index) {
        return buffer[offset(index) + 5];
    }

    @Override
    public final void s2(int index, float value) {
        buffer[offset(index) + 5] = value;
    }

    @Override
    public final float t2(int index) {
        return buffer[offset(index) + 6];
    }

    @Override
    public final void t2(int index, float value) {
        buffer[offset(index) + 6] = value;
    }

    @Override
    public final void beginScrolling(float value) {
        
    }

    @Override
    public final void endScrolling() {
        
    }

    private final int offset(int index) {
        return (index + pos) * STRIDE;
    }
}
