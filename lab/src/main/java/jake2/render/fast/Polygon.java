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
package jake2.render.fast;

import jake2.render.glpoly_t;
import jake2.util.Lib;

import java.nio.FloatBuffer;


/**
 * Polygon
 * 
 * @author cwei
 */
public final class Polygon extends glpoly_t {

    private static final int MAX_POLYS = 20000;

    private static final int MAX_BUFFER_VERTICES = 120000;

    
    private static final float[] s1_old = new float[MAX_VERTICES];

    private static final FloatBuffer buffer = Lib.newFloatBuffer(MAX_BUFFER_VERTICES
            * STRIDE);

    private static int bufferIndex;

    private static int polyCount;

    private static final Polygon[] polyCache = new Polygon[MAX_POLYS];
    static {
        for (int i = 0; i < polyCache.length; i++) {
            polyCache[i] = new Polygon();
        }
    }

    static glpoly_t create(int numverts) {
        Polygon poly = polyCache[polyCount++];
        poly.clear();
        poly.numverts = numverts;
        poly.pos = bufferIndex;
        bufferIndex += numverts;
        return poly;
    }

    static void reset() {
        polyCount = 0;
        bufferIndex = 0;
    }

    static FloatBuffer getInterleavedBuffer() {
        buffer.rewind();
        return buffer;
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
    public final float s1(int index) {
        return buffer.get((index + pos) * STRIDE);
    }

    @Override
    public final void s1(int index, float value) {
        buffer.put((index + pos) * STRIDE, value);
    }

    @Override
    public final float t1(int index) {
        return buffer.get((index + pos) * STRIDE + 1);
    }

    @Override
    public final void t1(int index, float value) {
        buffer.put((index + pos) * STRIDE + 1, value);
    }

    @Override
    public final float x(int index) {
        return buffer.get((index + pos) * STRIDE + 2);
    }

    @Override
    public final void x(int index, float value) {
        buffer.put((index + pos) * STRIDE + 2, value);
    }

    @Override
    public final float y(int index) {
        return buffer.get((index + pos) * STRIDE + 3);
    }

    @Override
    public final void y(int index, float value) {
        buffer.put((index + pos) * STRIDE + 3, value);
    }

    @Override
    public final float z(int index) {
        return buffer.get((index + pos) * STRIDE + 4);
    }

    @Override
    public final void z(int index, float value) {
        buffer.put((index + pos) * STRIDE + 4, value);
    }

    @Override
    public final float s2(int index) {
        return buffer.get((index + pos) * STRIDE + 5);
    }

    @Override
    public final void s2(int index, float value) {
        buffer.put((index + pos) * STRIDE + 5, value);
    }

    @Override
    public final float t2(int index) {
        return buffer.get((index + pos) * STRIDE + 6);
    }

    @Override
    public final void t2(int index, float value) {
        buffer.put((index + pos) * STRIDE + 6, value);
    }

    @Override
    public final void beginScrolling(float scroll) {
        int index = pos * STRIDE;
        for (int i = 0; i < numverts; i++, index += STRIDE) {
            scroll += s1_old[i] = buffer.get(index);
            buffer.put(index, scroll);
        }
    }

    @Override
    public final void endScrolling() {
        int index = pos * STRIDE;
        for (int i = 0; i < numverts; i++, index += STRIDE) {
            buffer.put(index, s1_old[i]);
        }
    }
}