/**
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * Created at 4:35:29 AM Jul 15, 2010
 */
/**
 * Created at 4:35:29 AM Jul 15, 2010
 */
package spacegraph.space2d.phys.callbacks;

import spacegraph.space2d.phys.common.Color3f;
import spacegraph.space2d.phys.common.IViewportTransform;
import spacegraph.space2d.phys.common.Transform;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.particle.ParticleColor;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

/**
 * Implement this abstract class to allow JBox2d to automatically draw your physics for debugging
 * purposes. Not intended to replace your own custom rendering routines!
 *
 * @author Daniel Murphy
 */
public abstract class DebugDraw {

    /** Draw shapes */
    public static final int e_shapeBit = 1 << 1;
    /** Draw joint connections */
    public static final int e_jointBit = 1 << 2;
    /** Draw axis aligned bounding boxes */
    public static final int e_aabbBit = 1 << 3;
    /** Draw pairs of connected objects */
    public static final int e_pairBit = 1 << 4;
    /** Draw center of mass frame */
    public static final int e_centerOfMassBit = 1 << 5;
    /** Draw dynamic tree */
    public static final int e_dynamicTreeBit = 1 << 6;
    /** Draw only the wireframe for drawing performance */
    public static final int e_wireframeDrawingBit = 1 << 7;


    private int m_drawFlags;
    private IViewportTransform viewportTransform;

    public DebugDraw() {
        this(null);
    }

    private DebugDraw(IViewportTransform viewport) {
        m_drawFlags = 0;
        viewportTransform = viewport;
    }

    public void setViewportTransform(IViewportTransform viewportTransform) {
        this.viewportTransform = viewportTransform;
    }

    public void setFlags(int flags) {
        m_drawFlags = flags;
    }

    public int getFlags() {
        return m_drawFlags;
    }

    public void appendFlags(int flags) {
        m_drawFlags |= flags;
    }

    public void clearFlags(int flags) {
        m_drawFlags &= ~flags;
    }

    /**
     * Draw a closed polygon provided in CCW order. This implementation uses
     * {@link #drawSegment(Tuple2f, Tuple2f, Color3f)} to draw each side of the polygon.
     *
     * @param vertices
     * @param vertexCount
     * @param color
     */
    public void drawPolygon(Tuple2f[] vertices, int vertexCount, Color3f color) {
        if (vertexCount == 1) {
            drawSegment(vertices[0], vertices[0], color);
            return;
        }

        for (int i = 0; i < vertexCount - 1; i += 1) {
            drawSegment(vertices[i], vertices[i + 1], color);
        }

        if (vertexCount > 2) {
            drawSegment(vertices[vertexCount - 1], vertices[0], color);
        }
    }

    public abstract void drawPoint(Tuple2f argPoint, float argRadiusOnScreen, Color3f argColor);

    /**
     * Draw a solid closed polygon provided in CCW order.
     *
     * @param vertices
     * @param vertexCount
     * @param color
     */
    public abstract void drawSolidPolygon(Tuple2f[] vertices, int vertexCount, Color3f color);

    /**
     * Draw a circle.
     *
     * @param center
     * @param radius
     * @param color
     */
    public abstract void drawCircle(Tuple2f center, float radius, Color3f color);

    /** Draws a circle with an axis */
    public void drawCircle(Tuple2f center, float radius, Tuple2f axis, Color3f color) {
        drawCircle(center, radius, color);
    }

    /**
     * Draw a solid circle.
     *
     * @param center
     * @param radius
     * @param axis
     * @param color
     */
    public abstract void drawSolidCircle(Tuple2f center, float radius, Tuple2f axis, Color3f color);

    /**
     * Draw a line segment.
     *
     * @param p1
     * @param p2
     * @param color
     */
    public abstract void drawSegment(Tuple2f p1, Tuple2f p2, Color3f color);

    /**
     * Draw a transform. Choose your own length scale
     *
     * @param xf
     */
    public abstract void drawTransform(Transform xf);

    /**
     * Draw a string.
     *
     * @param x
     * @param y
     * @param s
     * @param color
     */
    public abstract void drawString(float x, float y, String s, Color3f color);

    /**
     * Draw a particle array
     *
     * @param colors can be null
     */
    public abstract void drawParticles(Tuple2f[] centers, float radius, ParticleColor[] colors, int count);

    /**
     * Draw a particle array
     *
     * @param colors can be null
     */
    public abstract void drawParticlesWireframe(Tuple2f[] centers, float radius, ParticleColor[] colors,
                                                int count);

    /** Called at the end of drawing a world */
    public void flush() {
    }

    public void drawString(Tuple2f pos, String s, Color3f color) {
        drawString(pos.x, pos.y, s, color);
    }

    public IViewportTransform getViewportTranform() {
        return viewportTransform;
    }

    /**
     * @param x
     * @param y
     * @param scale
     * @deprecated use the viewport transform in {@link #getViewportTranform()}
     */
    public void setCamera(float x, float y, float scale) {
        viewportTransform.setCamera(x, y, scale);
    }


    /**
     * @param argScreen
     * @param argWorld
     */
    public void getScreenToWorldToOut(Tuple2f argScreen, Tuple2f argWorld) {
        viewportTransform.getScreenToWorld(argScreen, argWorld);
    }

    /**
     * @param argWorld
     * @param argScreen
     */
    public void getWorldToScreenToOut(Tuple2f argWorld, Tuple2f argScreen) {
        viewportTransform.getWorldToScreen(argWorld, argScreen);
    }

    /**
     * Takes the world coordinates and puts the corresponding screen coordinates in argScreen.
     *
     * @param worldX
     * @param worldY
     * @param argScreen
     */
    public void getWorldToScreenToOut(float worldX, float worldY, Tuple2f argScreen) {
        argScreen.set(worldX, worldY);
        viewportTransform.getWorldToScreen(argScreen, argScreen);
    }

    /**
     * takes the world coordinate (argWorld) and returns the screen coordinates.
     *
     * @param argWorld
     */
    public Tuple2f getWorldToScreen(Tuple2f argWorld) {
        Tuple2f screen = new Vec2();
        viewportTransform.getWorldToScreen(argWorld, screen);
        return screen;
    }

    /**
     * Takes the world coordinates and returns the screen coordinates.
     *
     * @param worldX
     * @param worldY
     */
    public Tuple2f getWorldToScreen(float worldX, float worldY) {
        Tuple2f argScreen = new v2(worldX, worldY);
        viewportTransform.getWorldToScreen(argScreen, argScreen);
        return argScreen;
    }

    /**
     * takes the screen coordinates and puts the corresponding world coordinates in argWorld.
     *
     * @param screenX
     * @param screenY
     * @param argWorld
     */
    public void getScreenToWorldToOut(float screenX, float screenY, Tuple2f argWorld) {
        argWorld.set(screenX, screenY);
        viewportTransform.getScreenToWorld(argWorld, argWorld);
    }

    /**
     * takes the screen coordinates (argScreen) and returns the world coordinates
     *
     * @param argScreen
     */
    public Tuple2f getScreenToWorld(Tuple2f argScreen) {
        Tuple2f world = new Vec2();
        viewportTransform.getScreenToWorld(argScreen, world);
        return world;
    }

    /**
     * takes the screen coordinates and returns the world coordinates.
     *
     * @param screenX
     * @param screenY
     */
    public Tuple2f getScreenToWorld(float screenX, float screenY) {
        Tuple2f screen = new v2(screenX, screenY);
        viewportTransform.getScreenToWorld(screen, screen);
        return screen;
    }
}
