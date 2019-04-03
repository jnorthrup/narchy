/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2007 Erwin Coumans  http:
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.video;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.v2;
import jcog.math.v3;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.common.MathUtils;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.particle.ParticleColor;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.shape.*;
import spacegraph.space3d.phys.util.BulletStack;
import spacegraph.space3d.widget.EDraw;
import spacegraph.util.math.Quat4f;

import java.util.function.Consumer;

import static com.jogamp.opengl.GL.*;
import static jcog.Util.sqr;
import static jcog.Util.unitize;
import static jcog.math.v3.v;

/**
 * @author jezek2
 */
public enum Draw {
    ;



	/*
    private static Map<CollisionShape,TriMeshKey> g_display_lists = new HashMap<CollisionShape,TriMeshKey>();
	
	private static int OGL_get_displaylist_for_shape(CollisionShape shape) {
		
		TriMeshKey trimesh = g_display_lists.get(shape);
		if (trimesh != null) {
			return trimesh.dlist;
		}

		return 0;
	}

	private static void OGL_displaylist_clean() {
		
		for (TriMeshKey trimesh : g_display_lists.values()) {
			glDeleteLists(trimesh.dlist, 1);
		}

		g_display_lists.clear();
	}
	*/

    public static final GLU glu = new GLU();
    private final static GLSRT glsrt = new GLSRT(glu);
    public static final GLUT glut = new GLUT();

    @Deprecated
    final static BulletStack stack = new BulletStack();
    private static final float[] glMat = new float[16];
    private static final v3 a = new v3(), b = new v3();


    public static void drawCoordSystem(GL gl) {
        ImmModeSink vbo = ImmModeSink.createFixed(3 * 4,
                3, GL.GL_FLOAT,
                4, GL.GL_FLOAT,
                0, GL.GL_FLOAT,
                0, GL.GL_FLOAT,
                GL.GL_STATIC_DRAW);
        vbo.glBegin(GL.GL_LINES);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 0f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(1f, 0f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 0f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 1f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 0f, 0f);
        vbo.glColor4f(1f, 1f, 1f, 1f);
        vbo.glVertex3f(0f, 0f, 1f);
        vbo.glEnd(gl);
    }

    public static void translate(GL2 gl, Transform trans) {
        v3 o = trans;
        gl.glTranslatef(o.x, o.y, o.z);
    }

    public static void transform(GL2 gl, Transform trans) {
        gl.glMultMatrixf(trans.getOpenGLMatrix(glMat), 0);
    }

    public static void draw(GL2 gl, CollisionShape shape) {


        if (shape.getShapeType() == BroadphaseNativeType.COMPOUND_SHAPE_PROXYTYPE) {
            CompoundShape compoundShape = (CompoundShape) shape;
            Transform childTrans = new Transform();
            for (int i = compoundShape.getNumChildShapes() - 1; i >= 0; i--) {
                stack.transforms.get(
                        compoundShape.getChildTransform(i, childTrans)
                );
                CollisionShape colShape = compoundShape.getChildShape(i);


                push(gl);
                stack.pushCommonMath();
                draw(gl, colShape);
                stack.popCommonMath();
                pop(gl);
            }
        } else {
            boolean useWireframeFallback = true;
            switch (shape.getShapeType()) {
                case BOX_SHAPE_PROXYTYPE:
                    SimpleBoxShape boxShape = (SimpleBoxShape) shape;
                    v3 a = boxShape.implicitShapeDimensions;

                    gl.glScalef(2f * a.x, 2f * a.y, 2f * a.z);

                    glut.glutSolidCube(1f);

                    useWireframeFallback = false;
                    break;
                case CONVEX_HULL_SHAPE_PROXYTYPE:
                case TRIANGLE_SHAPE_PROXYTYPE:
                case TETRAHEDRAL_SHAPE_PROXYTYPE:


                    if (shape.isConvex()) {
                        ConvexShape convexShape = (ConvexShape) shape;
                        if (shape.getUserPointer() == null) {

                            ShapeHull hull = new ShapeHull(convexShape);


                            float margin = shape.getMargin();
                            hull.buildHull(margin);
                            convexShape.setUserPointer(hull);


                        }

                        if (shape.getUserPointer() != null) {
                            ShapeHull hull = (ShapeHull) shape.getUserPointer();

                            int tris = hull.numTriangles();
                            if (tris > 0) {
                                int index = 0;
                                spacegraph.space3d.phys.util.IntArrayList idx = hull.getIndexPointer();
                                FasterList<v3> vtx = hull.getVertexPointer();

                                v3 normal = v();
                                v3 tmp1 = v();
                                v3 tmp2 = v();

                                gl.glBegin(GL.GL_TRIANGLES);

                                for (int i = 0; i < tris; i++) {

                                    v3 v1 = vtx.get(idx.get(index++));
                                    v3 v2 = vtx.get(idx.get(index++));
                                    v3 v3 = vtx.get(idx.get(index++));

                                    tmp1.sub(v3, v1);
                                    tmp2.sub(v2, v1);
                                    normal.cross(tmp1, tmp2);
                                    normal.normalize();

                                    gl.glNormal3f(normal.x, normal.y, normal.z);
                                    gl.glVertex3f(v1.x, v1.y, v1.z);
                                    gl.glVertex3f(v2.x, v2.y, v2.z);
                                    gl.glVertex3f(v3.x, v3.y, v3.z);

                                }

                                gl.glEnd();
                            }
                        }
                    }

                    useWireframeFallback = false;
                    break;
                case SPHERE_SHAPE_PROXYTYPE: {
                    SphereShape sphereShape = (SphereShape) shape;
                    float radius = sphereShape.getMargin();


                    glsrt.drawSphere(gl, radius);
                            /*
                            glPointSize(10f);
							glBegin(gl.GL_POINTS);
							glVertex3f(0f, 0f, 0f);
							glEnd();
							glPointSize(1f);
							*/
                    useWireframeFallback = false;
                    break;
                }
                case CAPSULE_SHAPE_PROXYTYPE: {
                    CapsuleShape capsuleShape = (CapsuleShape) shape;
                    float radius = capsuleShape.getRadius();
                    float halfHeight = capsuleShape.getHalfHeight();
                    int upAxis = 1;

                    glsrt.drawCylinder(gl, radius, halfHeight, upAxis);

                    gl.glTranslatef(0f, -halfHeight, 0f);


                    glsrt.drawSphere(gl, radius);
                    gl.glTranslatef(0f, 2f * halfHeight, 0f);


                    glsrt.drawSphere(gl, radius);
                    useWireframeFallback = false;
                    break;
                }
                case MULTI_SPHERE_SHAPE_PROXYTYPE:
                    break;


                case CONVEX_TRIANGLEMESH_SHAPE_PROXYTYPE:
                    useWireframeFallback = false;
                    break;

                case CONVEX_SHAPE_PROXYTYPE:
                case CYLINDER_SHAPE_PROXYTYPE:
                    CylinderShape cylinder = (CylinderShape) shape;
                    int upAxis = cylinder.getUpAxis();

                    float radius = cylinder.getRadius();
                    float halfHeight = VectorUtil.coord(cylinder.getHalfExtentsWithMargin(new v3()), upAxis);

                    glsrt.drawCylinder(gl, radius, halfHeight, upAxis);

                    break;
                default:

            }


            if (useWireframeFallback) {

                if (shape.isPolyhedral()) {
                    PolyhedralConvexShape polyshape = (PolyhedralConvexShape) shape;

                    ImmModeSink vbo = ImmModeSink.createFixed(polyshape.getNumEdges() + 3,
                            3, GL.GL_FLOAT,
                            0, GL.GL_FLOAT,
                            0, GL.GL_FLOAT,
                            0, GL.GL_FLOAT, GL.GL_STATIC_DRAW);

                    vbo.glBegin(GL.GL_LINES);


                    int i;
                    for (i = 0; i < polyshape.getNumEdges(); i++) {
                        polyshape.getEdge(i, a, b);

                        vbo.glVertex3f(a.x, a.y, a.z);
                        vbo.glVertex3f(b.x, b.y, b.z);
                    }
                    vbo.glEnd(gl);


                }
            }


            if (shape.isConcave()) {
                ConcaveShape concaveMesh = (ConcaveShape) shape;


                a.set(1e30f, 1e30f, 1e30f);
                b.set(-1e30f, -1e30f, -1e30f);

                GlDrawcallback drawCallback = new GlDrawcallback(gl);
                drawCallback.wireframe = false;

                concaveMesh.processAllTriangles(drawCallback, b, a);
            }
        }


    }

    @Deprecated
    public static void line(double x1, double y1, double x2, double y2, GL2 gl) {
        line((float) x1, (float) y1, (float) x2, (float) y2, gl);
    }

    public static void line(int x1, int y1, int x2, int y2, GL2 gl) {
        gl.glBegin(GL_LINES);
        gl.glVertex2i(x1, y1);
        gl.glVertex2i(x2, y2);
        gl.glEnd();
    }

    public static void line(float x1, float y1, float x2, float y2, GL2 gl) {
        gl.glBegin(GL_LINES);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x2, y2);
        gl.glEnd();
    }

    public static void tri2d(int x1, int y1, int x2, int y2, int x3, int y3, GL2 gl) {
        gl.glBegin(GL_TRIANGLES);
        gl.glVertex2i(x1, y1);
        gl.glVertex2i(x2, y2);
        gl.glVertex2i(x3, y3);
        gl.glEnd();
    }

    public static void tri2f(GL2 gl, float x1, float y1, float x2, float y2, float x3, float y3) {
        gl.glBegin(GL_TRIANGLES);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x2, y2);
        gl.glVertex2f(x3, y3);
        gl.glEnd();
    }

    public static void quad2d(GL2 gl, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        gl.glBegin(GL2ES3.GL_QUADS);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x2, y2);
        gl.glVertex2f(x3, y3);
        gl.glVertex2f(x4, y4);
        gl.glEnd();
    }

    public static void quad2d(GL2 gl, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        gl.glBegin(GL2ES3.GL_QUADS);
        gl.glVertex2i(x1, y1);
        gl.glVertex2i(x2, y2);
        gl.glVertex2i(x3, y3);
        gl.glVertex2i(x4, y4);
        gl.glEnd();
    }

    public static void line(GL2 gl, v3 a, v3 b) {
        gl.glBegin(GL_LINES);
        gl.glVertex3f(a.x, a.y, a.z);
        gl.glVertex3f(b.x, b.y, b.z);
        gl.glEnd();
    }

    public static void rectStroke(float left, float bottom, float w, float h, GL2 gl) {
        gl.glBegin(GL_LINE_STRIP);
        gl.glVertex2f(left, bottom);
        gl.glVertex2f(left + w, bottom);
        gl.glVertex2f(left + w, bottom + h);
        gl.glVertex2f(left, bottom + h);
        gl.glVertex2f(left, bottom);
        gl.glEnd();
    }

    /** note: (cx,cy,w,h) */
    public static void rectFrame(float cx, float cy, float wi, float hi, float thick, GL2 gl) {
        rectFrame(cx, cy, wi, hi, wi + thick, hi + thick, gl);
    }

    public static void rectFrame(RectFloat bounds, float thick, GL2 gl) {
        rectFrame(bounds.cx(), bounds.cy(), bounds.w, bounds.h, thick, gl);
    }
    /** wi,hi - inner width/height
     *  wo,ho - outer width/height
     *  */
    public static void rectFrame(float cx, float cy, float wi, float hi, float wo, float ho, GL2 gl) {
        //N
        float vthick = (ho - hi) / 2;
        Draw.rect(cx-wo/2, cy-ho/2, wo, vthick, gl );
        //S
        Draw.rect(cx-wo/2, cy+ho/2 - vthick, wo, vthick, gl );

        float hthick = (wo - wi) / 2;
        //W
        Draw.rect(cx-wo/2, cy - hi/2, hthick, hi, gl );
        //E
        Draw.rect(cx+wo/2 - hthick, cy - hi/2, hthick, hi, gl );
    }

    public static void circle(GL2 gl, v2 center, boolean solid, float radius, int NUM_CIRCLE_POINTS) {


        float theta = 2 * MathUtils.PI / NUM_CIRCLE_POINTS;
        float c = (float) Math.cos(theta);
        float s = (float) Math.sin(theta);
        float x = radius;
        float y = 0;
        float cx = center.x;
        float cy = center.y;
        gl.glBegin(solid ? GL_TRIANGLE_FAN : GL_LINE_LOOP);

        for (int i = 0; i < NUM_CIRCLE_POINTS; i++) {
            gl.glVertex3f(x + cx, y + cy, 0);

            float temp = x;
            x = c * x - s * y;
            y = s * temp + c * y;
        }
        gl.glEnd();

    }

    public static void particles(GL2 gl, v2[] centers, float radius, int NUM_CIRCLE_POINTS, ParticleColor[] colors, int count) {


        float theta = 2 * MathUtils.PI / NUM_CIRCLE_POINTS;
        float c = (float) Math.cos(theta);
        float s = (float) Math.sin(theta);

        float x = radius;
        float y = 0;

        for (int i = 0; i < count; i++) {
            v2 center = centers[i];
            float cx = center.x;
            float cy = center.y;
            gl.glBegin(GL_TRIANGLE_FAN);
            if (colors == null) {
                gl.glColor4f(1, 1, 1, .4f);
            } else {
                ParticleColor color = colors[i];
                gl.glColor4b(color.r, color.g, color.b, color.a);
            }
            for (int j = 0; j < NUM_CIRCLE_POINTS; j++) {
                gl.glVertex3f(x + cx, y + cy, 0);
                float temp = x;
                x = c * x - s * y;
                y = s * temp + c * y;
            }
            gl.glEnd();
        }

    }

    public static void rect(float left, float bottom, float w, float h, GL2 gl) {
        gl.glRectf(left, bottom, left + w, bottom + h);
    }

    public static void rectAlphaCorners(float left, float bottom, float x2, float y2, float[] color, float[] cornerAlphas, GL2 gl) {
        gl.glBegin(GL2ES3.GL_QUADS);
        gl.glColor4f(color[0], color[1], color[2], cornerAlphas[0]);
        gl.glVertex3f(left, bottom, 0);
        gl.glColor4f(color[0], color[1], color[2], cornerAlphas[1]);
        gl.glVertex3f(x2, bottom, 0);
        gl.glColor4f(color[0], color[1], color[2], cornerAlphas[2]);
        gl.glVertex3f(x2, y2, 0);
        gl.glColor4f(color[0], color[1], color[2], cornerAlphas[3]);
        gl.glVertex3f(left, y2, 0);
        gl.glEnd();

    }

    public static void rect(GL2 gl, int x1, int y1, int w, int h) {

        gl.glRecti(x1, y1, x1 + w, y1 + h);

    }

    public static void rect(GL2 gl, float x1, float y1, float w, float h, float z) {
        if (z == 0) {
            rect(x1, y1, w, h, gl);
        } else {

            gl.glBegin(GL2ES3.GL_QUADS);
            gl.glNormal3f(0, 0, 1);
            gl.glVertex3f(x1, y1, z);
            gl.glVertex3f(x1 + w, y1, z);
            gl.glVertex3f(x1 + w, y1 + h, z);
            gl.glVertex3f(x1, y1 + h, z);
            gl.glEnd();
        }
    }


    public static void rectTex(GL2 gl, Texture tt, float x, float y, float w, float h, float z, float repeatScale, boolean mipmap, float alpha) {
        rectTex(gl, tt, x, y, z, w, h, repeatScale, alpha, mipmap, false);
    }

    public static void rectTex(GL2 gl, Texture tt, float x, float y, float w, float h, float z, float repeatScale, float alpha, boolean mipmap, boolean inverted) {


        tt.enable(gl);
        tt.bind(gl);

        gl.glColor4f(1.0f, 1.0f, 1.0f, alpha);

        if (mipmap)  {
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, 16);
            gl.glGenerateMipmap(GL_TEXTURE_2D);
        } else {
            //gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }

        boolean repeat = repeatScale > 0;
        if (repeat) {
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        } else {
//            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            repeatScale = 1f;
        }

        gl.glBegin(GL2ES3.GL_QUADS);

        final float s = repeatScale;
        if (!inverted) {
            gl.glTexCoord2f(0.0f, s);
            gl.glVertex3f(x, y, z);
            gl.glTexCoord2f(s, s);
            gl.glVertex3f(x + w, y, z);
            gl.glTexCoord2f(s, 0.0f);
            gl.glVertex3f(x + w, y + h, z);
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex3f(x, y + h, z);
        } else {
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex3f(x, y, z);
            gl.glTexCoord2f(s, 0.0f);
            gl.glVertex3f(x + w, y, z);
            gl.glTexCoord2f(s, s);
            gl.glVertex3f(x + w, y + h, z);
            gl.glTexCoord2f(0.0f, s);
            gl.glVertex3f(x, y + h, z);
        }

        gl.glEnd();

        tt.disable(gl);


    }

    static public void renderHalfTriEdge(GL2 gl, SimpleSpatial src, EDraw<?> e, float width, float twist, Quat4f tmpQ) {


        Transform st = src.transform;

        tmpQ = st.getRotation(tmpQ);

        if (twist != 0)
            tmpQ.setAngle(0, 1, 0, twist);

        v3 ww = new v3(0, 0, 1);
        tmpQ.rotateVector(ww, ww);


        Transform tt = e.tgt().transform;

        float sx = st.x;
        float tx = tt.x;
        float dx = tx - sx;
        float sy = st.y;
        float ty = tt.y;
        float dy = ty - sy;
        float sz = st.z;
        float tz = tt.z;
        float dz = tz - sz;
        v3 vv = new v3(dx, dy, dz).cross(ww).normalized(width);


        gl.glBegin(GL_TRIANGLES);

        gl.glColor4f(e.r, e.g, e.b, e.a);
        gl.glNormal3f(ww.x, ww.y, ww.z);

        gl.glVertex3f(sx + vv.x, sy + vv.y, sz + vv.z);

        gl.glVertex3f(
                sx + -vv.x, sy + -vv.y, sz + -vv.z

        );

        gl.glColor4f(e.r / 2f, e.g / 2f, e.b / 2f, e.a * 2 / 3);
        gl.glVertex3f(tx, ty, tz);

        gl.glEnd();


    }

    public static void renderLineEdge(GL2 gl, SimpleSpatial src, SimpleSpatial tgt, float width) {
        gl.glLineWidth(width);
        gl.glBegin(GL.GL_LINES);
        v3 s = src.transform();
        gl.glVertex3f(s.x, s.y, s.z);
        v3 t = tgt.transform();
        gl.glVertex3f(t.x, t.y, t.z);
        gl.glEnd();
    }

    public static void hsb(GL2 gl, float hue, float saturation, float brightness, float a) {
        float[] f = new float[4];

        //hsb(f, hue, saturation, brightness, a);

        hsl(f, hue, saturation, brightness); f[3] = a;

        gl.glColor4fv(f, 0);

    }

    public static int hsb(float hue, float saturation, float brightness) {
        float[] f = new float[4];
        hsb(f, hue, saturation, brightness, 1);
        return rgbInt(f[0], f[1], f[2]);
    }

    public static float[] hsb(@Nullable float[] target, float hue, float saturation, float brightness, float a) {
        if (target == null || target.length < 4)
            target = new float[4];

        float r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (brightness);
                    g = (t);
                    b = (p);
                    break;
                case 1:
                    r = (q);
                    g = (brightness);
                    b = (p);
                    break;
                case 2:
                    r = (p);
                    g = (brightness);
                    b = (t);
                    break;
                case 3:
                    r = (p);
                    g = (q);
                    b = (brightness);
                    break;
                case 4:
                    r = (t);
                    g = (p);
                    b = (brightness);
                    break;
                case 5:
                    r = (brightness);
                    g = (p);
                    b = (q);
                    break;
            }
        }
        target[0] = r;
        target[1] = g;
        target[2] = b;
        target[3] = a;
        return target;
    }


    public static int colorHSB(float hue, float saturation, float brightness) {

        float r, g, b;
        if (saturation < Float.MIN_NORMAL) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else if (brightness < Float.MIN_NORMAL) {
            return 0;
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (brightness);
                    g = (t);
                    b = (p);
                    break;
                case 1:
                    r = (q);
                    g = (brightness);
                    b = (p);
                    break;
                case 2:
                    r = (p);
                    g = (brightness);
                    b = (t);
                    break;
                case 3:
                    r = (p);
                    g = (q);
                    b = (brightness);
                    break;
                case 4:
                    r = (t);
                    g = (p);
                    b = (brightness);
                    break;
                case 5:
                    r = (brightness);
                    g = (p);
                    b = (q);
                    break;
                default:
                    return 0; //should not happen
            }
        }
        return rgbInt(r, g, b);
    }
    /**
     * Converts an HSL color value to RGB. Conversion formula
     * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
     * Assumes h, s, and l are contained in the set [0, 1] and
     * returns r, g, and b in the set [0, 255].
     *
     * @param h       The hue
     * @param s       The saturation
     * @param l       The lightness
     * @return int array, the RGB representation
     */
    public static int colorHSL(float h, float s, float l){
        float r, g, b;

        if (s == 0f) {
            r = g = b = l; // achromatic
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hueToRgb(p, q, h + 1f/3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f/3f);
        }
        return rgbInt(r, g, b);
//        int[] rgb = {(int) (r * 255), (int) (g * 255), (int) (b * 255)};
//        return rgb;
    }
    public static void hsl(float[] target, float h, float s, float l){
        float r, g, b;

        if (s == 0f) {
            r = g = b = l; // achromatic
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hueToRgb(p, q, h + 1f/3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f/3f);
        }
        target[0] = r;
        target[1] = g;
        target[2] = b;
        //return rgbInt(r, g, b);
//        int[] rgb = {(int) (r * 255), (int) (g * 255), (int) (b * 255)};
//        return rgb;
    }
    /** Helper method that converts hue to rgb */
    static float hueToRgb(float p, float q, float t) {
        if (t < 0f)
            t += 1f;
        if (t > 1f)
            t -= 1f;
        if (t < 1f/6f)
            return p + (q - p) * 6f * t;
        if (t < 1f/2f)
            return q;
        if (t < 2f/3f)
            return p + (q - p) * (2f/3f - t) * 6f;
        return p;
    }
    /**
     * uses the built-in color scheme for displaying values in the range -1..+1
     */
    public static void colorBipolar(GL2 gl, float v) {
        float r, g, b;
        if (v < 0) {
            r = -v / 2f;
            g = 0f;
            b = -v;
        } else {
            r = v;
            g = v / 2;
            b = 0f;
        }
        gl.glColor3f(r, g, b);
    }

//    public static int colorBipolarHSB(float v) {
//        return hsb(v / 2f + 0.5f, 0.7f, 0.75f);
//    }

    public static int colorBipolar(float v) {

        float r, g, b;
        if (v != v) {
            r = g = b = 0.5f;
        } else if (v < 0) {
            v = unitize(-v);
            r = v;
            g = 0f;
            b = v / 2;
        } else {
            v = unitize(v);
            r = v / 2;
            g = v;
            b = 0f;
        }
        return rgbInt(r, g, b);
    }

    public static int rgbInt(float r, float g, float b) {
        return  ((int) (255 * r) << 16) | ((int) (255 * g) << 8) | ((int) (255 * b));
    }

    public static void colorUnipolarHue(GL2 gl, float v, float hueMin, float hueMax) {
        colorUnipolarHue(gl, v, hueMin, hueMax, 1f);
    }

    public static void colorUnipolarHue(GL2 gl, float v, float hueMin, float hueMax, float alpha) {
        hsb(gl, Util.lerp(v, hueMin, hueMax), 0.7f, 0.7f, alpha);
    }

    public static void colorUnipolarHue(float[] c, float v) {
        colorUnipolarHue(c, v, 0, 1);
    }

    public static void colorUnipolarHue(float[] c, float v, float hueMin, float hueMax) {
        colorUnipolarHue(c, v, hueMin, hueMax, 1f);
    }

    public static void colorUnipolarHue(float[] c, float v, float hueMin, float hueMax, float alpha) {
        hsb(c, Util.lerp(v, hueMin, hueMax), 0.7f, 0.7f, alpha);
    }

    public static void colorHash(Object x, float[] color) {
        colorHash(x.hashCode(), color, 1f);
    }

    public static void colorHash(int hash, float[] color, float sat, float bri, float alpha) {
        Draw.hsb(color, (Math.abs(hash) % 500) / 500f * 360.0f, sat, bri, alpha);
    }

    public static void colorHash(int hash, float[] color, float alpha) {
        colorHash(hash, color, 0.5f, 0.5f, alpha);
    }

    public static void colorHash(GL2 gl, int hash, float sat, float bri, float alpha) {
        float[] f = new float[4];
        colorHash(hash, f, sat, bri, alpha);
        gl.glColor4fv(f, 0);
    }

    public static void colorHash(GL2 gl, int hash, float alpha) {
        colorHash(gl, hash, 0.7f, 0.7f, alpha);
    }
    public static void colorHashRange(GL2 gl, int hash, float hueStart, float hueEnd, float alpha) {
        float h = Util.lerp( ((float)Math.abs(hash))/Integer.MAX_VALUE, hueStart, hueEnd);
        Draw.hsb(gl, h, 0.7f, 0.7f, alpha);
    }

    private static void colorHash(GL2 gl, Object o, float alpha) {
        colorHash(gl, o.hashCode(), alpha);
    }

    public static void colorHash(GL2 gl, Object o) {
        colorHash(gl, o, 1f);
    }

    public static void colorGrays(GL2 gl, float x) {
        gl.glColor3f(x, x, x);
    }
    public static void colorGrays(GL2 gl, float x, float a) {
        gl.glColor4f(x, x, x, a);
    }

    public static void bounds(GL2 gl, Surface s, Consumer<GL2> c) {
        bounds(s.bounds, gl, c);
    }

    public static void bounds(RectFloat s, GL2 gl, Consumer<GL2> c) {
        bounds(gl, s.x, s.y, s.w, s.h, c);
    }

    private static void bounds(GL2 gl, float x1, float y1, float w, float h, Consumer<GL2> c) {
        gl.glPushMatrix();
        gl.glTranslatef(x1, y1, 0);
        gl.glScalef(w, h, 1);
        c.accept(gl);
        gl.glPopMatrix();
    }

    public static void rect(RectFloat bounds, GL2 gl) {
        Draw.rect(bounds.x, bounds.y, bounds.w, bounds.h, gl);
    }

    public static void rectStroke(RectFloat bounds, GL2 gl) {
        Draw.rectStroke(bounds.x, bounds.y, bounds.w, bounds.h, gl);
    }

    public static void colorRGBA(float[] c, float r, float g, float b, float a) {
        c[0] = r;
        c[1] = g;
        c[2] = b;
        c[3] = a;
    }

    public static void poly(Body2D body, GL2 gl, float preScale, PolygonShape shape) {
        PolygonShape poly = shape;

        gl.glBegin(GL_TRIANGLE_FAN);
        int n = poly.vertices;
        v2[] pv = poly.vertex;

        for (int i = 0; i < n; ++i)
            body.getWorldPointToGL(pv[i], preScale, gl);

        body.getWorldPointToGL(pv[0], preScale, gl);

        gl.glEnd();
    }


    public static void poly(int n, float rad, boolean fill, GL2 gl) {
        poly(n, rad, 0, fill, gl);
    }

    /** TODO https://stackoverflow.com/questions/8779570/opengl-drawing-a-hexigon-with-vertices#8779622 */
    public static void poly(int n, float rad, float angle, boolean fill, GL2 gl) {

        assert(n>2);

        gl.glBegin(fill ? GL_TRIANGLE_FAN : GL_LINE_LOOP);

        for (int i = 0; i < n; ++i) {
            double theta = angle + (i/((float)n)) * (float)(2*Math.PI);
            gl.glVertex2f(rad * (float)Math.cos(theta), rad * (float)Math.sin(theta) );
        }

        gl.glEnd();
    }

    /**
     * utility for stencil painting
     * include = only draw inside the stencil
     * exclude = only draw outside the stencil
     * adapted from: https:
     */
    public static void stencilMask(GL2 gl, boolean includeOrExclude, Consumer<GL2> paintTheStencilRegion, Consumer<GL2> paintStenciled) {

        stencilStart(gl);

        paintTheStencilRegion.accept(gl);

        stencilUse(gl, includeOrExclude);

        paintStenciled.accept(gl);

        stencilEnd(gl);
    }

    public static void stencilStart(GL gl) {
        gl.glEnable(gl.GL_STENCIL_TEST);


        gl.glColorMask(false, false, false, false);
        gl.glDepthMask(false);

        /*
            gl.GL_NEVER: For every pixel, fail the stencil test (so we automatically overwrite the pixel's stencil buffer value)
            1: write a '1' to the stencil buffer for every drawn pixel because glStencilOp has 'gl.GL_REPLACE'
            0xFF: function mask, you'll usually use 0xFF
        */

        gl.glStencilFunc(gl.GL_NEVER, 1, 0xFF);
        gl.glStencilOp(gl.GL_REPLACE, gl.GL_KEEP, gl.GL_KEEP);


        gl.glStencilMask(0xFF);


        gl.glClear(gl.GL_STENCIL_BUFFER_BIT);


    }


    public static void stencilEnd(GL gl) {

        gl.glDisable(gl.GL_STENCIL_TEST);
    }





























    /*
     * http:
     * Hershey Fonts
     * http:
     *
     * Drawn in Processing.
     *
     */


    public static void stencilUse(GL gl, boolean includeOrExclude) {

        gl.glColorMask(true, true, true, true);
        gl.glDepthMask(true);


        gl.glStencilMask(0x00);


        gl.glStencilFunc(includeOrExclude ? GL_NOTEQUAL : GL_EQUAL, 0, 0xFF);


    }

    public static void push(GL2 gl) {
        gl.glPushMatrix();
    }

    public static void pop(GL2 gl) {
        gl.glPopMatrix();
    }


    public static void rectRGBA(RectFloat bounds, float r, float g, float b, float a, GL2 gl) {
        gl.glColor4f(r, g, b, a);
        Draw.rect(bounds, gl);
    }

    public static void halfTriEdge2D(float fx, float fy, float tx, float ty, float base, GL2 gl) {
        float len = (float) Math.sqrt(sqr(fx - tx) + sqr(fy - ty));
        float theta = (float) (Math.atan2(ty - fy, tx - fx) * 180 / Math.PI) + 270f;

        //isosceles triangle
        gl.glPushMatrix();
        gl.glTranslatef((tx + fx) / 2, (ty + fy) / 2, 0);
        gl.glRotatef(theta, 0, 0, 1);
        tri2f(gl, -base / 2, -len / 2, +base / 2, -len / 2, 0, +len / 2);
        gl.glPopMatrix();
    }

    /** draws unit rectangle */
    public static void rectUnit(GL2 g) {
        Draw.rect(g, 0, 0, 1, 1);
    }

    public enum TextAlignment {
        Left, Center, Right
    }

    private static class GlDrawcallback extends TriangleCallback {
        private final GL gl;
        boolean wireframe;

        GlDrawcallback(GL gl) {
            this.gl = gl;
        }

        @Override
        public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
            ImmModeSink vbo = ImmModeSink.createFixed(10,
                    3, GL.GL_FLOAT,
                    4, GL.GL_FLOAT,
                    0, GL.GL_FLOAT,
                    0, GL.GL_FLOAT, GL.GL_STATIC_DRAW);
            if (wireframe) {
                vbo.glBegin(GL.GL_LINES);
                vbo.glColor4f(1, 0, 0, 1);
                vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
                vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
                vbo.glColor4f(0, 1, 0, 1);
                vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
                vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
                vbo.glColor4f(0, 0, 1, 1);
                vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
                vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
                vbo.glEnd(gl);
            } else {
                vbo.glBegin(GL.GL_TRIANGLES);
                vbo.glColor4f(1, 0, 0, 1);
                vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
                vbo.glColor4f(0, 1, 0, 1);
                vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
                vbo.glColor4f(0, 0, 1, 1);
                vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
                vbo.glEnd(gl);
            }
        }
    }


}
