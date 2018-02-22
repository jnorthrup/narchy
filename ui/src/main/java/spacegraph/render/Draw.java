/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2007 Erwin Coumans  http://continuousphysics.com/Bullet/
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

package spacegraph.render;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import jcog.Util;
import jcog.list.FasterList;
import jcog.tree.rtree.rect.RectFloat2D;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.MathUtils;
import org.jbox2d.dynamics.Body2D;
import org.jbox2d.particle.ParticleColor;
import org.jetbrains.annotations.Nullable;
import spacegraph.SimpleSpatial;
import spacegraph.Surface;
import spacegraph.math.*;
import spacegraph.phys.collision.broad.BroadphaseNativeType;
import spacegraph.phys.math.Transform;
import spacegraph.phys.math.VectorUtil;
import spacegraph.phys.shape.*;
import spacegraph.phys.util.BulletStack;
import spacegraph.space.EDraw;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.*;
import static jcog.Util.unitize;
import static spacegraph.math.v3.v;

/**
 * @author jezek2
 */
public enum Draw {
    ;



	/*
    private static Map<CollisionShape,TriMeshKey> g_display_lists = new HashMap<CollisionShape,TriMeshKey>();
	
	private static int OGL_get_displaylist_for_shape(CollisionShape shape) {
		// JAVA NOTE: rewritten
		TriMeshKey trimesh = g_display_lists.get(shape);
		if (trimesh != null) {
			return trimesh.dlist;
		}

		return 0;
	}

	private static void OGL_displaylist_clean() {
		// JAVA NOTE: rewritten
		for (TriMeshKey trimesh : g_display_lists.values()) {
			glDeleteLists(trimesh.dlist, 1);
		}

		g_display_lists.clear();
	}
	*/

    public static final GLU glu = new GLU();
    public final static GLSRT glsrt = new GLSRT(glu);
    public static final GLUT glut = new GLUT();

    public static void drawCoordSystem(GL gl) {
        ImmModeSink vbo = ImmModeSink.createFixed(3 * 4,
                3, GL.GL_FLOAT, // vertex
                4, GL.GL_FLOAT, // color
                0, GL.GL_FLOAT, // normal
                0, GL.GL_FLOAT, // texCoords
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

    private static final float[] glMat = new float[16];

    @Deprecated
    final static BulletStack stack = new BulletStack();
    private static final v3 a = new v3(), b = new v3();

    public static void translate(GL2 gl, Transform trans) {
        v3 o = trans;
        gl.glTranslatef(o.x, o.y, o.z);
    }

    public static void transform(GL2 gl, Transform trans) {
        gl.glMultMatrixf(trans.getOpenGLMatrix(glMat), 0);
    }


    public static void draw(GL2 gl, CollisionShape shape) {


        //System.out.println("shape="+shape+" type="+BroadphaseNativeTypes.forValue(shape.getShapeType()));


        //		if (shape.getShapeType() == BroadphaseNativeTypes.UNIFORM_SCALING_SHAPE_PROXYTYPE.getValue())
        //		{
        //			const btUniformScalingShape* scalingShape = static_cast<const btUniformScalingShape*>(shape);
        //			const btConvexShape* convexShape = scalingShape->getChildShape();
        //			float	scalingFactor = (float)scalingShape->getUniformScalingFactor();
        //			{
        //				btScalar tmpScaling[4][4]={{scalingFactor,0,0,0},
        //					{0,scalingFactor,0,0},
        //					{0,0,scalingFactor,0},
        //					{0,0,0,1}};
        //
        //				drawOpenGL( (btScalar*)tmpScaling,convexShape,color,debugMode);
        //			}
        //			return;
        //		}


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
                    //Vector3f halfExtent = stack.vectors.get();
                    gl.glScalef(2f * a.x, 2f * a.y, 2f * a.z);
                    //glsrt.drawCube(gl, 1f);
                    glut.glutSolidCube(1f);

                    useWireframeFallback = false;
                    break;
                case CONVEX_HULL_SHAPE_PROXYTYPE:
                case TRIANGLE_SHAPE_PROXYTYPE:
                case TETRAHEDRAL_SHAPE_PROXYTYPE:
                    //glutSolidCube(1.0);


                    if (shape.isConvex()) {
                        ConvexShape convexShape = (ConvexShape) shape;
                        if (shape.getUserPointer() == null) {
                            // create a hull approximation
                            ShapeHull hull = new ShapeHull(convexShape);

                            // JAVA NOTE: not needed
                            ///// cleanup memory
                            //m_shapeHulls.push_back(hull);

                            float margin = shape.getMargin();
                            hull.buildHull(margin);
                            convexShape.setUserPointer(hull);

                            //printf("numTriangles = %d\n", hull->numTriangles ());
                            //printf("numIndices = %d\n", hull->numIndices ());
                            //printf("numVertices = %d\n", hull->numVertices ());
                        }

                        if (shape.getUserPointer() != null) {
                            ShapeHull hull = (ShapeHull) shape.getUserPointer();

                            int tris = hull.numTriangles();
                            if (tris > 0) {
                                int index = 0;
                                spacegraph.phys.util.IntArrayList idx = hull.getIndexPointer();
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
                    float radius = sphereShape.getMargin(); // radius doesn't include the margin, so draw with margin
                    // TODO: glutSolidSphere(radius,10,10);
                    //sphere.draw(radius, 8, 8);
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
                    //glutSolidSphere(radius,10,10);
                    //sphere.draw(radius, 10, 10);
                    glsrt.drawSphere(gl, radius);
                    gl.glTranslatef(0f, 2f * halfHeight, 0f);
                    //glutSolidSphere(radius,10,10);
                    //sphere.draw(radius, 10, 10);
                    glsrt.drawSphere(gl, radius);
                    useWireframeFallback = false;
                    break;
                }
                case MULTI_SPHERE_SHAPE_PROXYTYPE:
                    break;
                //				case CONE_SHAPE_PROXYTYPE:
                //					{
                //						const btConeShape* coneShape = static_cast<const btConeShape*>(shape);
                //						int upIndex = coneShape->getConeUpIndex();
                //						float radius = coneShape->getRadius();//+coneShape->getMargin();
                //						float height = coneShape->getHeight();//+coneShape->getMargin();
                //						switch (upIndex)
                //						{
                //						case 0:
                //							glRotatef(90.0, 0.0, 1.0, 0.0);
                //							break;
                //						case 1:
                //							glRotatef(-90.0, 1.0, 0.0, 0.0);
                //							break;
                //						case 2:
                //							break;
                //						default:
                //							{
                //							}
                //						};
                //
                //						glTranslatef(0.0, 0.0, -0.5*height);
                //						glutSolidCone(radius,height,10,10);
                //						useWireframeFallback = false;
                //						break;
                //
                //					}
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
                // for polyhedral shapes
                if (shape.isPolyhedral()) {
                    PolyhedralConvexShape polyshape = (PolyhedralConvexShape) shape;

                    ImmModeSink vbo = ImmModeSink.createFixed(polyshape.getNumEdges() + 3,
                            3, GL.GL_FLOAT,  // vertex
                            0, GL.GL_FLOAT,  // color
                            0, GL.GL_FLOAT,  // normal
                            0, GL.GL_FLOAT, GL.GL_STATIC_DRAW); // texture

                    vbo.glBegin(GL.GL_LINES);

                    //Vector3f a = stack.vectors.get(), b = stack.vectors.get();
                    int i;
                    for (i = 0; i < polyshape.getNumEdges(); i++) {
                        polyshape.getEdge(i, a, b);

                        vbo.glVertex3f(a.x, a.y, a.z);
                        vbo.glVertex3f(b.x, b.y, b.z);
                    }
                    vbo.glEnd(gl);

                    //					if (debugMode==btIDebugDraw::DBG_DrawFeaturesText)
                    //					{
                    //						glRasterPos3f(0.0,  0.0,  0.0);
                    //						//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),polyshape->getExtraDebugInfo());
                    //
                    //						glColor3f(1.f, 1.f, 1.f);
                    //						for (i=0;i<polyshape->getNumVertices();i++)
                    //						{
                    //							btPoint3 vtx;
                    //							polyshape->getVertex(i,vtx);
                    //							glRasterPos3f(vtx.x(),  vtx.y(),  vtx.z());
                    //							char buf[12];
                    //							sprintf(buf," %d",i);
                    //							BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
                    //						}
                    //
                    //						for (i=0;i<polyshape->getNumPlanes();i++)
                    //						{
                    //							btVector3 normal;
                    //							btPoint3 vtx;
                    //							polyshape->getPlane(normal,vtx,i);
                    //							btScalar d = vtx.dot(normal);
                    //
                    //							glRasterPos3f(normal.x()*d,  normal.y()*d, normal.z()*d);
                    //							char buf[12];
                    //							sprintf(buf," plane %d",i);
                    //							BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
                    //
                    //						}
                    //					}


                }
            }

            //		#ifdef USE_DISPLAY_LISTS
            //
            //		if (shape->getShapeType() == TRIANGLE_MESH_SHAPE_PROXYTYPE||shape->getShapeType() == GIMPACT_SHAPE_PROXYTYPE)
            //			{
            //				GLuint dlist =   OGL_get_displaylist_for_shape((btCollisionShape * )shape);
            //				if (dlist)
            //				{
            //					glCallList(dlist);
            //				}
            //				else
            //				{
            //		#else
            if (shape.isConcave())//>getShapeType() == TRIANGLE_MESH_SHAPE_PROXYTYPE||shape->getShapeType() == GIMPACT_SHAPE_PROXYTYPE)
            //		if (shape->getShapeType() == TRIANGLE_MESH_SHAPE_PROXYTYPE)
            {
                ConcaveShape concaveMesh = (ConcaveShape) shape;
                //btVector3 aabbMax(btScalar(1e30),btScalar(1e30),btScalar(1e30));
                //btVector3 aabbMax(100,100,100);//btScalar(1e30),btScalar(1e30),btScalar(1e30));

                //todo pass camera, for some culling
//                    Vector3f aabbMax = stack.vectors.get(1e30f, 1e30f, 1e30f);
//                    Vector3f aabbMin = stack.vectors.get(-1e30f, -1e30f, -1e30f);
                a.set(1e30f, 1e30f, 1e30f);
                b.set(-1e30f, -1e30f, -1e30f);

                GlDrawcallback drawCallback = new GlDrawcallback(gl);
                drawCallback.wireframe = false; //(debugMode & DebugDrawModes.DRAW_WIREFRAME) != 0;

                concaveMesh.processAllTriangles(drawCallback, b, a);
            }
        }
        //#endif

        //#ifdef USE_DISPLAY_LISTS
        //		}
        //	}
        //#endif

        //			if (shape->getShapeType() == CONVEX_TRIANGLEMESH_SHAPE_PROXYTYPE)
        //			{
        //				btConvexTriangleMeshShape* convexMesh = (btConvexTriangleMeshShape*) shape;
        //
        //				//todo: pass camera for some culling
        //				btVector3 aabbMax(btScalar(1e30),btScalar(1e30),btScalar(1e30));
        //				btVector3 aabbMin(-btScalar(1e30),-btScalar(1e30),-btScalar(1e30));
        //				TriangleGlDrawcallback drawCallback;
        //				convexMesh->getMeshInterface()->InternalProcessAllTriangles(&drawCallback,aabbMin,aabbMax);
        //
        //			}

        // TODO: error in original sources GL_DEPTH_BUFFER_BIT instead of GL_DEPTH_TEST
        //gl.glDisable(GL_DEPTH_TEST);
        //glRasterPos3f(0, 0, 0);//mvtx.x(),  vtx.y(),  vtx.z());
//				if ((debugMode & DebugDrawModes.DRAW_TEXT) != 0) {
//					// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),shape->getName());
//				}
//
//				if ((debugMode & DebugDrawModes.DRAW_FEATURES_TEXT) != 0) {
//					//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),shape->getExtraDebugInfo());
//				}
        //gl.glEnable(GL_DEPTH_TEST);


    }

    @Deprecated
    public static void line(GL2 gl, double x1, double y1, double x2, double y2) {
        line(gl, (float) x1, (float) y1, (float) x2, (float) y2);
    }

    public static void line(GL2 gl, int x1, int y1, int x2, int y2) {
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2i(x1, y1);
        gl.glVertex2i(x2, y2);
        gl.glEnd();
    }

    public static void line(GL2 gl, float x1, float y1, float x2, float y2) {
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x2, y2);
        gl.glEnd();
    }

    public static void tri2d(GL2 gl, int x1, int y1, int x2, int y2, int x3, int y3) {
        gl.glBegin(GL2.GL_TRIANGLES);
        gl.glVertex2i(x1, y1);
        gl.glVertex2i(x2, y2);
        gl.glVertex2i(x3, y3);
        gl.glEnd();
    }

    public static void quad2d(GL2 gl, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x2, y2);
        gl.glVertex2f(x3, y3);
        gl.glVertex2f(x4, y4);
        gl.glEnd();
    }

    public static void quad2d(GL2 gl, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2i(x1, y1);
        gl.glVertex2i(x2, y2);
        gl.glVertex2i(x3, y3);
        gl.glVertex2i(x4, y4);
        gl.glEnd();
    }

    public static void line(GL2 gl, v3 a, v3 b) {
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(a.x, a.y, a.z);
        gl.glVertex3f(b.x, b.y, b.z);
        gl.glEnd();
    }


    public static void rectStroke(GL2 gl, float x1, float y1, float w, float h) {
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex2f(x1, y1);
        gl.glVertex2f(x1 + w, y1);
        gl.glVertex2f(x1 + w, y1 + h);
        gl.glVertex2f(x1, y1 + h);
        gl.glVertex2f(x1, y1);
        gl.glEnd();
//        line(gl, x1, y1, x1 + w, y1);
//        line(gl, x1, y1, x1, y1 + h);
//        line(gl, x1, y1 + h, x1 + w, y1 + h);
//        line(gl, x1 + w, y1, x1 + w, y1 + h);

    }

    public static void circle(GL2 gl, v2 center, boolean solid, float radius, int NUM_CIRCLE_POINTS) {
        //gl.glPushMatrix();
        //transformViewport(gl, zero);
        float theta = 2 * MathUtils.PI / NUM_CIRCLE_POINTS;
        float c = (float) Math.cos(theta);
        float s = (float) Math.sin(theta);
        float x = radius;
        float y = 0;
        float cx = center.x;
        float cy = center.y;
        gl.glBegin(solid ? GL2.GL_TRIANGLE_FAN : GL2.GL_LINE_LOOP);

        for (int i = 0; i < NUM_CIRCLE_POINTS; i++) {
            gl.glVertex3f(x + cx, y + cy, 0);
            // apply the rotation matrix
            float temp = x;
            x = c * x - s * y;
            y = s * temp + c * y;
        }
        gl.glEnd();
        //gl.glPopMatrix();
    }

    public static void particles(GL2 gl, v2[] centers, float radius, int NUM_CIRCLE_POINTS,  ParticleColor[] colors, int count) {

        //gl.glPushMatrix();
        //transformViewport(gl, zero);

        float theta = 2 * MathUtils.PI / NUM_CIRCLE_POINTS;
        float c = (float) Math.cos(theta);
        float s = (float) Math.sin(theta);

        float x = radius;
        float y = 0;

        for (int i = 0; i < count; i++) {
            v2 center = centers[i];
            float cx = center.x;
            float cy = center.y;
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
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
        //gl.glPopMatrix();
    }

    public static void rect(GL2 gl, float x1, float y1, float w, float h) {

        gl.glRectf(x1, y1, x1 + w, y1 + h);
//        gl.glBegin(GL2.GL_QUADS);
//        gl.glVertex3f(x1, y1, 0);
//        gl.glVertex3f(x1 + w, y1, 0);
//        gl.glVertex3f(x1 + w, y1 + h, 0);
//        gl.glVertex3f(x1, y1 + h, 0);
//        gl.glEnd();
    }

    public static void rectAlphaCorners(GL2 gl, float x1, float y1, float x2, float y2, float[] color, float[] cornerAlphas) {
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(color[0], color[1], color[2], cornerAlphas[0]);
        gl.glVertex3f(x1, y1, 0);
        gl.glColor4f(color[0], color[1], color[2], cornerAlphas[1]);
        gl.glVertex3f(x2, y1, 0);
        gl.glColor4f(color[0], color[1], color[2], cornerAlphas[2]);
        gl.glVertex3f(x2, y2, 0);
        gl.glColor4f(color[0], color[1], color[2], cornerAlphas[3]);
        gl.glVertex3f(x1, y2, 0);
        gl.glEnd();

    }
    public static void rect(GL2 gl, int x1, int y1, int w, int h) {

        gl.glRecti(x1, y1, x1 + w, y1 + h);

    }

    public static void rect(GL2 gl, float x1, float y1, float w, float h, float z) {
        if (z == 0) {
            rect(gl, x1, y1, w, h);
        } else {
            //TODO maybe translate z then call above method?
            gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0, 0, 1);
            gl.glVertex3f(x1, y1, z);
            gl.glVertex3f(x1 + w, y1, z);
            gl.glVertex3f(x1 + w, y1 + h, z);
            gl.glVertex3f(x1, y1 + h, z);
            gl.glEnd();
        }
    }

    public static void rectTex(GL2 gl, Texture tt, float x1, float y1, float w, float h, float z, float repeatScale, float alpha) {

//        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
//        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);


        //tt.bind(gl);
//        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, width, height, 0, GL_BGR, GL_UNSIGNED_BYTE, data);
//
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        //gl.glPushMatrix();

        tt.enable(gl);
        tt.bind(gl);


        //gl.glTexEnvi( GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE );
        gl.glColor4f(1.0f, 1.0f, 1.0f, alpha);


        boolean repeat = repeatScale > 0;
        if (repeat) {
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, 16);
            gl.glGenerateMipmap(GL_TEXTURE_2D);
        } else {
            //sharp pixels on magnification
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            repeatScale = 1f;
        }

        gl.glBegin(GL2.GL_QUADS);

//        gl.glNormal3f(0, 0, 1);
        gl.glTexCoord2f(0.0f, repeatScale);
        gl.glVertex3f(x1, y1, z);
        gl.glTexCoord2f(repeatScale, repeatScale);
        gl.glVertex3f(x1 + w, y1, z);
        gl.glTexCoord2f(repeatScale, 0.0f);
        gl.glVertex3f(x1 + w, y1 + h, z);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(x1, y1 + h, z);
        gl.glEnd();

        tt.disable(gl);


    }


//    @Deprecated
//    static public void text(GL2 gl, float scaleX, float scaleY, String label, float dx, float dy, float dz) {
//        text(gl, scaleX, scaleY, label, dx, dy, dz, null);
//    }

//    @Deprecated
//    static public void text(GL2 gl, float scaleX, float scaleY, String label, float dx, float dy, float dz, float[] color) {
//        push(gl);
//        //gl.glNormal3f(0, 0, 1f);
//        gl.glTranslatef(dx, dy, dz + zStep);
//
//        if (color != null)
//            gl.glColor3fv(color, 0);
//
//        float fontThick = 1f;
//        gl.glLineWidth(fontThick);
//
//        /*
//         GLUT_STROKE_ROMAN
//            A proportionally spaced Roman Simplex font for ASCII characters 32 through 127. The maximum top character in the font is 119.05 units; the bottom descends 33.33 units.
//
//        GLUT_STROKE_MONO_ROMAN
//            A mono-spaced spaced Roman Simplex font (same characters as GLUT_STROKE_ROMAN) for ASCII characters 32 through 127. The maximum top character in the font is 119.05 units; the bottom descends 33.33 units. Each character is 104.76 units wide.
//         */
//        //float r = v.radius;
//        renderString(gl, /*GLUT.STROKE_ROMAN*/ STROKE_MONO_ROMAN, label,
//                scaleX, scaleY,
//                0, 0, dz); // Print GL Text To The Screen
//        pop(gl);
//    }

    //    static final float[] tmpV = new float[3];

    //    static final Matrix4f tmpM4 = new Matrix4f();
//    static final Matrix3f tmpM3 = new Matrix3f();
    static final AxisAngle4f tmpA = new AxisAngle4f();

    static public void renderHalfTriEdge(GL2 gl, SimpleSpatial src, EDraw<?> e, float width, float twist, Quat4f tmpQ) {


        Transform st = src.transform;

        tmpQ = st.getRotation(tmpQ);

        if (twist != 0)
            tmpQ.setAngle(0, 1, 0, twist);

        v3 ww = new v3(0, 0, 1);
        tmpQ.rotateVector(ww, ww);

        //ww.normalize();


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


        gl.glBegin(GL2.GL_TRIANGLES);

        gl.glColor4f(e.r, e.g, e.b, e.a);
        gl.glNormal3f(ww.x, ww.y, ww.z);

        gl.glVertex3f(sx + vv.x, sy + vv.y, sz + vv.z); //right base

        gl.glVertex3f( //right base
                sx + -vv.x, sy + -vv.y, sz + -vv.z //full triangle
                //sx, sy, sz  //half triangle
        );

        gl.glColor4f(e.r / 2f, e.g / 2f, e.b / 2f, e.a * 2 / 3);
        gl.glVertex3f(tx, ty, tz); //tip

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
        hsb(f, hue, saturation, brightness, a);
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

    public static int colorBipolarHSB(float v) {
        return hsb(v / 2f + 0.5f, 0.7f, 0.75f);
    }

    public static int colorBipolar(float v) {


        float r, g, b;
        if (v!=v) {
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
        return ((int) (255 * r) << 16) | ((int) (255 * g) << 8) | ((int) (255 * b));
    }

    public static void colorUnipolarHue(GL2 gl, float v, float hueMin, float hueMax) {
        float hue = Util.lerp(v, hueMin, hueMax);
        hsb(gl, hue, 0.7f, 0.7f, 1f);
    }

    public static void colorUnipolarHue(float[] c, float v) {
        colorUnipolarHue(c, v, 0, 1);
    }

    public static void colorUnipolarHue(float[] c, float v, float hueMin, float hueMax) {
        colorUnipolarHue(c, v, hueMin, hueMax, 1f);
    }

    public static void colorUnipolarHue(float[] c, float v, float hueMin, float hueMax, float alpha) {
        float hue = Util.lerp(v, hueMin, hueMax);
        hsb(c, hue, 0.7f, 0.7f, alpha);
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

    public static void colorHash(GL2 gl, Object o, float alpha) {
        colorHash(gl, o.hashCode(), alpha);
    }

    public static void colorHash(GL2 gl, Object o) {
        colorHash(gl, o, 1f);
    }

    public static void colorGrays(GL2 gl, float x) {
        gl.glColor3f(x, x, x);
    }


    public static void bounds(GL2 gl, Surface s, Consumer<GL2> c) {
        bounds(gl, s.bounds, c);
    }

    public static void bounds(GL2 gl, RectFloat2D s, Consumer<GL2> c) {
        bounds(gl, s.x, s.y, s.w, s.h, c);
    }

    public static void bounds(GL2 gl, float x1, float y1, float w, float h, Consumer<GL2> c) {
        gl.glPushMatrix();
        gl.glTranslatef(x1, y1, 0);
        gl.glScalef(w, h, 1);
        c.accept(gl);
        gl.glPopMatrix();
    }

    public static void rect(GL2 gl, RectFloat2D bounds) {
        Draw.rect(gl, bounds.x, bounds.y, bounds.w, bounds.h);
    }


    public static void colorRGBA(float[] c, float r, float g, float b, float a) {
        c[0] = r; c[1] = g; c[2] = b; c[3] = a;
    }

    public static void drawPoly(Body2D body, GL2 gl, PolygonShape shape) {
        PolygonShape poly = shape;

        gl.glBegin(GL_TRIANGLE_FAN);
        int n = poly.vertices;
        Tuple2f[] pv = poly.vertex;
        float preScale = 1.1f;

        for (int i = 0; i < n; ++i)
            body.getWorldPointToGL(pv[i], preScale, gl);
        body.getWorldPointToGL(pv[0], preScale, gl); //close

        gl.glEnd();
    }


//
//    public static void hsb(float h, float s, float b, float a, float[] target) {
//        //TODO use a LUT matrix instaed of this shitty Color function
//        Color c = Color.hsb(360*h, s, b);
//        target[0] = (float)c.getRed();
//        target[1] = (float)c.getGreen();
//        target[2] = (float)c.getBlue();
//        target[3] = a;
//    }

    ////////////////////////////////////////////////////////////////////////////

    private static class TriMeshKey {
        public CollisionShape shape;
        public int dlist; // OpenGL display list
    }

    private static class GlDisplaylistDrawcallback extends TriangleCallback {
        private final GL gl;

        private final v3 diff1 = new v3();
        private final v3 diff2 = new v3();
        private final v3 normal = new v3();

        public GlDisplaylistDrawcallback(GL gl) {
            this.gl = gl;
        }

        @Override
        public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
            diff1.sub(triangle[1], triangle[0]);
            diff2.sub(triangle[2], triangle[0]);
            normal.cross(diff1, diff2);

            normal.normalize();

            ImmModeSink vbo = ImmModeSink.createFixed(GL.GL_STATIC_DRAW, 3,
                    3, GL.GL_FLOAT,  // vertex
                    4, GL.GL_FLOAT,  // color
                    3, GL.GL_FLOAT,  // normal
                    0, GL.GL_FLOAT); // texture

            vbo.glBegin(GL.GL_TRIANGLES);
            vbo.glColor4f(0, 1f, 0, 1f);
            vbo.glNormal3f(normal.x, normal.y, normal.z);
            vbo.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);

            vbo.glColor4f(0, 1f, 0, 1f);
            vbo.glNormal3f(normal.x, normal.y, normal.z);
            vbo.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);

            vbo.glColor4f(0, 1f, 0, 1f);
            vbo.glNormal3f(normal.x, normal.y, normal.z);
            vbo.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
            vbo.glEnd(gl);

			/*glBegin(gl.GL_LINES);
            glColor3f(1, 1, 0);
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[0].getX(), triangle[0].getY(), triangle[0].getZ());
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[1].getX(), triangle[1].getY(), triangle[1].getZ());
			glColor3f(1, 1, 0);
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[2].getX(), triangle[2].getY(), triangle[2].getZ());
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[1].getX(), triangle[1].getY(), triangle[1].getZ());
			glColor3f(1, 1, 0);
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[2].getX(), triangle[2].getY(), triangle[2].getZ());
			glNormal3d(normal.getX(),normal.getY(),normal.getZ());
			glVertex3d(triangle[0].getX(), triangle[0].getY(), triangle[0].getZ());
			glEnd();*/
        }
    }

    private static class GlDrawcallback extends TriangleCallback {
        private final GL gl;
        public boolean wireframe;

        public GlDrawcallback(GL gl) {
            this.gl = gl;
        }

        @Override
        public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
            ImmModeSink vbo = ImmModeSink.createFixed(10,
                    3, GL.GL_FLOAT,  // vertex
                    4, GL.GL_FLOAT,  // color
                    0, GL.GL_FLOAT,  // normal
                    0, GL.GL_FLOAT, GL.GL_STATIC_DRAW); // texture
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

    private static class TriangleGlDrawcallback extends InternalTriangleIndexCallback {
        private final GL gl;

        public TriangleGlDrawcallback(GL gl) {
            this.gl = gl;
        }

        @Override
        public void internalProcessTriangleIndex(v3[] triangle, int partId, int triangleIndex) {
            ImmModeSink vbo = ImmModeSink.createFixed(GL.GL_STATIC_DRAW, 10,
                    3, GL.GL_FLOAT,  // vertex
                    4, GL.GL_FLOAT,  // color
                    0, GL.GL_FLOAT,  // normal
                    0, GL.GL_FLOAT); // texture
            vbo.glBegin(GL.GL_TRIANGLES);//LINES);
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
        }
    }

    /*
     * Hershey Fonts
     * http://paulbourke.net/dataformats/hershey/
     *
     * Drawn in Processing.
     *
     */

//    // a curly brace from the Hershey characters
//    String exampleCurly = " 2226 40KYPBRCSDTFTHSJRKQMQOSQ R" +
//            "RCSESGRIQJPLPNQPURQTPVPXQZR[S]S_Ra R" +
//            "SSQUQWRYSZT\\T^S`RaPb";
//
//    static boolean isGlyphInteger(String str) {
//        return str.matches("-?\\d+");
//    }


//    // Point wrapper -- no tuples in Processing
//// (Bonus: can tell you the ascii character pair that
//// corresponds to it in the original file)
//    static class GlyphPoint {
//        int x, y;
//
//        GlyphPoint(int ix, int iy) {
//            x = ix;
//            y = iy;
//        }
//
//        String toString() {
//            return "<Point: (" + x + "," + y + ") [" + repr() + "]>";
//        }
//
//        String repr() {
//            return "" + char(x + offsetR) + char(y + offsetR); // empty string necessary for append
//        }
//    }

    public static final class HGlyph {
        /*int idx, verts, */
        final int leftPos;
        final int rightPos;
        //String spec;
        final byte[][] segments;

        // Hershey fonts use coordinates represented by characters'
        // integer values relative to ascii 'R'
        static final int offsetR = (int) ('R');
        private int id;


        HGlyph(String hspec) {
            FasterList<byte[]> segments = new FasterList();

            //idx      = Integer.valueOf(hspec.substring(0, 5));
            //verts    = Integer.valueOf(hspec.substring(5, 8));
            String spec = (hspec.substring(10));

            // TODO: is this needed?
            leftPos = (int) (hspec.charAt(8)) - offsetR;
            rightPos = (int) (hspec.charAt(9)) - offsetR;

            int curX, curY;
            boolean penUp = true;
            ByteArrayList currentSeg = new ByteArrayList();

            for (int i = 0; i < spec.length() - 1; i += 2) {
                if (spec.charAt(i + 1) == 'R' && spec.charAt(i) == ' ') {
                    penUp = true;
                    segments.add(currentSeg.toArray());
                    currentSeg = new ByteArrayList();
                    continue;
                }

                curX = (int) (spec.charAt(i)) - offsetR; //0..20
                currentSeg.add((byte) curX);
                curY = (int) (spec.charAt(i + 1)) - offsetR; //0..20
                currentSeg.add((byte) (10 - curY)); //half above zero half below zero
            }
            if (!currentSeg.isEmpty())
                segments.add(currentSeg.toArray());

            this.segments = segments.toArray(new byte[segments.size()][]);


        }

        public void draw(GL2 gl, float x) {
            //int pLastX = 0, pLastY = 0;

//            GLint firstA[4] = {0, 250, 500, 750};
//            GLint countA[4] = {250, 250, 250, 250};
//            glMultiDrawArrays(GL_LINE_STRIP, firstA, countA, 4);

            if (x != 0)
                gl.glTranslatef(x, 0, 0);

            gl.glCallList(id);

            if (x != 0)
                gl.glTranslatef(-x, 0, 0); //HACK un-translate, cheaper than pushMatrix
        }

        public void init(GL2 gl) {
            id = gl.glGenLists(1);
            gl.glNewList(id, GL2.GL_COMPILE);

            for (byte[] seg : segments) {

                int ss = seg.length;

                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int j = 0; j < ss; ) {
                    //gl.glVertex2i(seg[j++], seg[j++]);
                    gl.glVertex2i(seg[j++], seg[j++]);
                }
                gl.glEnd();
            }

            gl.glEndList();
        }
    }

    public enum TextAlignment {
        Left, Center, Right
    }

    public static void text(GL2 gl, CharSequence s, float scale, float x, float y, float z) {
        text(gl, s, scale, scale, x, y, z, TextAlignment.Center);
    }

    public static void text(GL2 gl, CharSequence s, float scale, float x, float y, float z, TextAlignment a) {
        text(gl, s, scale, scale, x, y, z, a);
    }

    public static void text(GL2 gl, CharSequence s, float scaleX, float scaleY, float x, float y, float z, TextAlignment a) {
//        int l = s.length();
//
//
//        float letterWidth = scale;
//        float letterHeight = scale;
//        float width = letterWidth * s.length();
//
//
//        y -= letterHeight / 2;
//
//        int N = fontMono.length;
//
//        gl.glPushMatrix();
//        gl.glTranslatef(x, y, z);
//        gl.glScalef(scale, scale, scale);
//
//        float dx = 0;
//        for (int i = 0; i < l; i++) {
//            int ci = s.charAt(i) - 32; //ASCII to index
//            if (ci >= 0 && (ci < N)) {
//                fontMono[ci].draw(gl, dx);
//            }
//            dx += letterWidth;
//        }
//        gl.glPopMatrix();


        int sl = s.length();
        if (sl == 0)
            return;

        float totalWidth = sl * scaleX;
        switch (a) {
            case Left:
                //nothing
                x += scaleX / 2f; //half letter towrads the right
                break;
            case Right:
                x -= totalWidth; //TODO check this
                break;
            case Center:
                x -= totalWidth / 2f; //TODO check this
                break;
        }

        push(gl);

        textStart(gl, scaleX, scaleY, x, y, z);

        for (int i = 0; i < sl; i++) {
            textNext(gl, s.charAt(i), i);
        }

        pop(gl);
    }

    public static void push(GL2 gl) {
        gl.glPushMatrix();
    }

    public static void text(GL2 gl, char c, float scale, float x, float y, float z) {
        text(gl, c, scale, scale, x, y, z);
    }

    public static void text(GL2 gl, char c, float scaleX, float scaleY, float x, float y, float z) {

        int ci = c - 32; //ASCII to index
        if (ci >= 0 && (ci < fontMono.length)) {

            push(gl);

            float sx = scaleX / 20f;
            float sy = scaleY / 20f;
            gl.glScalef(sx, sy, 1f);

            gl.glTranslatef(x / sx, y / sy, z);

            fontMono[ci].draw(gl, 0);
            pop(gl);
        }
    }

    public static void pop(GL2 gl) {
        gl.glPopMatrix();
    }

    /**
     * call glPush before this, and after all textNext's. returns the character width to translate by to display the next character (left to right direction)
     */
    public static void textStart(GL2 gl, float scaleX, float scaleY, float x, float y, float z) {

        gl.glTranslatef(x, y, z);
        gl.glScalef(scaleX / 20f, scaleY / 20f, 1f);
    }

    public static void textNext(GL2 gl, char c, float x) {

        int ci = c - 32; //ASCII to index
        if (ci >= 0 && (ci < fontMono.length)) {
            fontMono[ci].draw(gl, x * 20);
        }

    }

    public final static HGlyph[] fontMono;

    static {

        List<HGlyph> glyphs = new FasterList();
        String[] lines = null;

        //HACK this started happening after switching back to gradle which at the present seems to cause malfunctioned class loading on jdk9
        for (int tries = 0; tries < 2 && lines == null; tries++) {
            try {
                String font =

                        //"meteorology";
                        //"rowmans";
                        "futural";
                //                lines = Files.readAllLines(
                //                    Paths.get(
                //                        Draw.class.getClassLoader().getResourceAsStream("spacegraph/font/hershey/" + font + ".jhf").toURI()
                //                        //Draw.class.getClassLoader().getResource("spacegraph/font/hershey/" + font + ".jhf").toURI()
                //                    )
                //                );

                lines = new String(Draw.class.getClassLoader().getResourceAsStream("spacegraph/font/hershey/" + font + ".jhf").readAllBytes()).split("\n");
                break;
            } catch (IOException e) {

            }
            Util.sleep(50);
        }
        if (lines == null) {
            lines = ArrayUtils.EMPTY_STRING_ARRAY;
        }

        String scratch = "";
        HGlyph nextGlyph;
        for (int i = 0; i < lines.length; i++) {
            String c = lines[i];
            if (c.endsWith("\n"))
                c = c.substring(0, c.length() - 1);
//                if (c.length() < 5) {
//                    continue;
//                }
            //    if (lines[i].charAt(0) == ' ') {
            if (Character.isDigit(c.charAt(4))) {
                nextGlyph = new HGlyph(c + scratch);
                //      println("Instantiated glyph " + nextGlyph.idx);
                glyphs.add(nextGlyph);
                scratch = "";
            } else {
                scratch += c;
            }
        }


        fontMono = glyphs.toArray(new HGlyph[glyphs.size()]);
    }

    static void init(GL2 gl) {
        for (HGlyph x : fontMono) {
            x.init(gl);
        }
    }


//    https://raw.githubusercontent.com/osresearch/vst/master/teensyv/asteroids_font.c
//     \file
//     Super simple font from Asteroids.
//         http://www.edge-online.com/wp-content/uploads/edgeonline/oldfiles/images/feature_article/2009/05/asteroids2.jpg

    /*



#include <stdint.h>
#include "asteroids_font.h"

            #define P(x,y)	((((x) & 0xF) << 4) | (((y) & 0xF) << 0))

            const asteroids_char_t asteroids_font[] = {
            ['0' - 0x20] = { P(0,0), P(8,0), P(8,12), P(0,12), P(0,0), P(8,12), FONT_LAST },
	['1' - 0x20] = { P(4,0), P(4,12), P(3,10), FONT_LAST },
            ['2' - 0x20] = { P(0,12), P(8,12), P(8,7), P(0,5), P(0,0), P(8,0), FONT_LAST },
            ['3' - 0x20] = { P(0,12), P(8,12), P(8,0), P(0,0), FONT_UP, P(0,6), P(8,6), FONT_LAST },
            ['4' - 0x20] = { P(0,12), P(0,6), P(8,6), FONT_UP, P(8,12), P(8,0), FONT_LAST },
            ['5' - 0x20] = { P(0,0), P(8,0), P(8,6), P(0,7), P(0,12), P(8,12), FONT_LAST },
            ['6' - 0x20] = { P(0,12), P(0,0), P(8,0), P(8,5), P(0,7), FONT_LAST },
            ['7' - 0x20] = { P(0,12), P(8,12), P(8,6), P(4,0), FONT_LAST },
            ['8' - 0x20] = { P(0,0), P(8,0), P(8,12), P(0,12), P(0,0), FONT_UP, P(0,6), P(8,6), },
            ['9' - 0x20] = { P(8,0), P(8,12), P(0,12), P(0,7), P(8,5), FONT_LAST },
            [' ' - 0x20] = { FONT_LAST },
            ['.' - 0x20] = { P(3,0), P(4,0), FONT_LAST },
            [',' - 0x20] = { P(2,0), P(4,2), FONT_LAST },
            ['-' - 0x20] = { P(2,6), P(6,6), FONT_LAST },
            ['+' - 0x20] = { P(1,6), P(7,6), FONT_UP, P(4,9), P(4,3), FONT_LAST },
            ['!' - 0x20] = { P(4,0), P(3,2), P(5,2), P(4,0), FONT_UP, P(4,4), P(4,12), FONT_LAST },
            ['#' - 0x20] = { P(0,4), P(8,4), P(6,2), P(6,10), P(8,8), P(0,8), P(2,10), P(2,2) },
            ['^' - 0x20] = { P(2,6), P(4,12), P(6,6), FONT_LAST },
            ['=' - 0x20] = { P(1,4), P(7,4), FONT_UP, P(1,8), P(7,8), FONT_LAST },
            ['*' - 0x20] = { P(0,0), P(4,12), P(8,0), P(0,8), P(8,8), P(0,0), FONT_LAST },
            ['_' - 0x20] = { P(0,0), P(8,0), FONT_LAST },
            ['/' - 0x20] = { P(0,0), P(8,12), FONT_LAST },
            ['\\' - 0x20] = { P(0,12), P(8,0), FONT_LAST },
            ['@' - 0x20] = { P(8,4), P(4,0), P(0,4), P(0,8), P(4,12), P(8,8), P(4,4), P(3,6) },
            ['$' - 0x20] = { P(6,2), P(2,6), P(6,10), FONT_UP, P(4,12), P(4,0), FONT_LAST },
            ['&' - 0x20] = { P(8,0), P(4,12), P(8,8), P(0,4), P(4,0), P(8,4), FONT_LAST },
            ['[' - 0x20] = { P(6,0), P(2,0), P(2,12), P(6,12), FONT_LAST },
            [']' - 0x20] = { P(2,0), P(6,0), P(6,12), P(2,12), FONT_LAST },
            ['(' - 0x20] = { P(6,0), P(2,4), P(2,8), P(6,12), FONT_LAST },
            [')' - 0x20] = { P(2,0), P(6,4), P(6,8), P(2,12), FONT_LAST },
            ['{' - 0x20] = { P(6,0), P(4,2), P(4,10), P(6,12), FONT_UP, P(2,6), P(4,6), FONT_LAST },
            ['}' - 0x20] = { P(4,0), P(6,2), P(6,10), P(4,12), FONT_UP, P(6,6), P(8,6), FONT_LAST },
            ['%' - 0x20] = { P(0,0), P(8,12), FONT_UP, P(2,10), P(2,8), FONT_UP, P(6,4), P(6,2) },
            ['<' - 0x20] = { P(6,0), P(2,6), P(6,12), FONT_LAST },
            ['>' - 0x20] = { P(2,0), P(6,6), P(2,12), FONT_LAST },
            ['|' - 0x20] = { P(4,0), P(4,5), FONT_UP, P(4,6), P(4,12), FONT_LAST },
            [':' - 0x20] = { P(4,9), P(4,7), FONT_UP, P(4,5), P(4,3), FONT_LAST },
            [';' - 0x20] = { P(4,9), P(4,7), FONT_UP, P(4,5), P(1,2), FONT_LAST },
            ['"' - 0x20] = { P(2,10), P(2,6), FONT_UP, P(6,10), P(6,6), FONT_LAST },
            ['\'' - 0x20] = { P(2,6), P(6,10), FONT_LAST },
            ['`' - 0x20] = { P(2,10), P(6,6), FONT_LAST },
            ['~' - 0x20] = { P(0,4), P(2,8), P(6,4), P(8,8), FONT_LAST },
            ['?' - 0x20] = { P(0,8), P(4,12), P(8,8), P(4,4), FONT_UP, P(4,1), P(4,0), FONT_LAST },
            ['A' - 0x20] = { P(0,0), P(0,8), P(4,12), P(8,8), P(8,0), FONT_UP, P(0,4), P(8,4) },
            ['B' - 0x20] = { P(0,0), P(0,12), P(4,12), P(8,10), P(4,6), P(8,2), P(4,0), P(0,0) },
            ['C' - 0x20] = { P(8,0), P(0,0), P(0,12), P(8,12), FONT_LAST },
            ['D' - 0x20] = { P(0,0), P(0,12), P(4,12), P(8,8), P(8,4), P(4,0), P(0,0), FONT_LAST },
            ['E' - 0x20] = { P(8,0), P(0,0), P(0,12), P(8,12), FONT_UP, P(0,6), P(6,6), FONT_LAST },
            ['F' - 0x20] = { P(0,0), P(0,12), P(8,12), FONT_UP, P(0,6), P(6,6), FONT_LAST },
            ['G' - 0x20] = { P(6,6), P(8,4), P(8,0), P(0,0), P(0,12), P(8,12), FONT_LAST },
            ['H' - 0x20] = { P(0,0), P(0,12), FONT_UP, P(0,6), P(8,6), FONT_UP, P(8,12), P(8,0) },
            ['I' - 0x20] = { P(0,0), P(8,0), FONT_UP, P(4,0), P(4,12), FONT_UP, P(0,12), P(8,12) },
            ['J' - 0x20] = { P(0,4), P(4,0), P(8,0), P(8,12), FONT_LAST },
            ['K' - 0x20] = { P(0,0), P(0,12), FONT_UP, P(8,12), P(0,6), P(6,0), FONT_LAST },
            ['L' - 0x20] = { P(8,0), P(0,0), P(0,12), FONT_LAST },
            ['M' - 0x20] = { P(0,0), P(0,12), P(4,8), P(8,12), P(8,0), FONT_LAST },
            ['N' - 0x20] = { P(0,0), P(0,12), P(8,0), P(8,12), FONT_LAST },
            ['O' - 0x20] = { P(0,0), P(0,12), P(8,12), P(8,0), P(0,0), FONT_LAST },
            ['P' - 0x20] = { P(0,0), P(0,12), P(8,12), P(8,6), P(0,5), FONT_LAST },
            ['Q' - 0x20] = { P(0,0), P(0,12), P(8,12), P(8,4), P(0,0), FONT_UP, P(4,4), P(8,0) },
            ['R' - 0x20] = { P(0,0), P(0,12), P(8,12), P(8,6), P(0,5), FONT_UP, P(4,5), P(8,0) },
            ['S' - 0x20] = { P(0,2), P(2,0), P(8,0), P(8,5), P(0,7), P(0,12), P(6,12), P(8,10) },
            ['T' - 0x20] = { P(0,12), P(8,12), FONT_UP, P(4,12), P(4,0), FONT_LAST },
            ['U' - 0x20] = { P(0,12), P(0,2), P(4,0), P(8,2), P(8,12), FONT_LAST },
            ['V' - 0x20] = { P(0,12), P(4,0), P(8,12), FONT_LAST },
            ['W' - 0x20] = { P(0,12), P(2,0), P(4,4), P(6,0), P(8,12), FONT_LAST },
            ['X' - 0x20] = { P(0,0), P(8,12), FONT_UP, P(0,12), P(8,0), FONT_LAST },
            ['Y' - 0x20] = { P(0,12), P(4,6), P(8,12), FONT_UP, P(4,6), P(4,0), FONT_LAST },
            ['Z' - 0x20] = { P(0,12), P(8,12), P(0,0), P(8,0), FONT_UP, P(2,6), P(6,6), FONT_LAST },
};


     */


    /**
     * https://github.com/sojamo/controlp5/blob/master/src/controlP5/BitFont.java
     * TODO render Glyphs, this currently only decodes the base64 font present in the strings
     */
    static class BitFont {

        static public final String standard58base64 = "AakACQBgACAEAgQGBggGAgMDBAYDBAIGBQMFBQUFBQUFBQICBAUEBQgFBQUFBQUFBQIFBQQGBQUFBQUFBAUGCAUGBQMFAwYGAwQEBAQEBAQEAgQEAgYEBAQEAwQEBAQGBAQEBAIEBQKgUgghIaUAAIiRMeiZZwwAAANgjjnvmRRKESVzzDGXoqQUvYURQCCAQCCSCAAAAAgAAABEqECleCVFkRAAiLSUWEgoJQAAiSOllEJIKVRiSymllCRFSSlCEVIAQQBBQAARAAAAEAAAACQpgeALJASiIwAQSQipE1BKRS+QSEohhRBSqES1UkopSIqSkkIiFAGwEZOwSaplZGx2VVXVSQIAgeIgSETy4RCSCEnoEONAgJCkd0I6p73QiKilk46RpCQZQoQIAFBVVVOVVFVVVUKqqiqKCACCDyKpiIoAICQJ9FAiCUE8ElUphRRCSqESUUohJSRJSUpECBEAoCrqoiqZqqqqiFRVUiIJAADKI5UQASEgSAoJpSRSCgECUlJKKYSUSiWilEJKSRKRlIgQJABAVVVEVVJVVVUhqaqqQhIACBQixEIBQFBg9AwyRhhDBEIIpGPOCyZl0kXJBJOMGMImEW9owAcbMQmrpKpKxjJiopQdFQAAAAAAAABAAAAAAAAAAIAAAOAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAQIAAAEAQAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAgAAAgCAAAAAgAA";
        static public final String standard56base64 = "AeYACQBgACAEAgQGBggHAgMDBgYDBQIFBgMGBgYGBgYGBgIDBAYEBggGBgYGBgYGBgIGBgUIBgYGBgYGBgYGCAYGBgMFAwYHAwUFBQUFAwUFAgMFAggFBQUFBAQEBQUIBQUFBAMEBQKgUgghRwoBAIAcOQ7yOZ/jAADAAXAe5/k+JwqKQlDkPM7jfFGUFEXfwghAQAAICIQUAgAAAAABAAAAQAkVqBSvJFJUEQCQaFHEBBEURQAAiDiiKIqCIIqCkjAWRVEURUQUJUURFCEFIBAAAgEBhAAAAABAAAAAAEikBIIvkFAQOQQAJBIEKU8ARVGiLyCRKAqiIAiioCJUTVEURQERRUmKgkQoAsAd40zcSambY447u5SSUnoSAYBAcRBMRNWHh4iEMAn0II4HBBAk6XuC6HmyL2gISVX0RI9DREoSQRAhAgBIKaW0lFIpKaWUIiSlpJRQhAAg+CCSFBFBACAiEdAHRUgEgfiIqIqiIAqCKAoqQlAWBVEBEZGSpBBCiAAAUgrpJaU0SkoppRBJKckkIxEAAJRHKkIEEACESEKERBERRUEAAVKiKIqCIIqKkhAURUGUREREJEVEECQAgJRSCkkplZJSSilIUkpKKUgEAAKFCHGhAIBAwdHnII5DOA4iIAiB6HGeL3CinOgFRU7gRA7hEDYR8QUJ+MEd40xcSqmkZI6LEWdsknsSAQAAAAAAAAAgAAAAAAAAAACAAACAAwAAAAAAAAAAAAAAQAAAAAAAAAADAwAAAAAABBAAAICAAAAAAIAAJQAAAAAAAAAABAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwAACAAAgIAAAAAAYAAA=";
        static public final String grixelbase64 = "AnoADABgACAFAgQICAoIAgQEBgYDBQIKCQMICAgICAcICAIDBQYFBwkICAgIBwcICAYHCAcJCAgICAgICAgICggICAQKBAQHBAcHBwcHBQcHAgUHBAoHBwcHBgcGBwcKBwcHBQIFCAJAJeIjkENBAAAAQHzk4wPz5/Pz8QEAAB4ePj8+Pz6fX9AHCgoECvL58fnx+QsKiigo6C8CIAEIIAAAARwgEAoEAAAAAAAABAAAAAAAICIAAZVIUiERBQEAAIAIWlAQSkAQKCgIICCEhAQFBQUFAgFBBCgoMGwoKCgoKAghKCiioCCgEIAKQIAAAAQIgAAgEAAAAAAAABAAAAAAAICIsAUEfwlCRBCkEAAAIUhAQCQBAaCgIEAAAcoUFBQQFAgEBBGgoECpoqCgoKAAhKCgiEREQIIAAgAAAgAQIAACgEAAAAAAAABAAAAAAAAAIrIBEIgkgBBBEEEAAIIgAQGJ/ARAgoKS+AioVFBQQFAgEBBEgEICmZKCgoKCAhCCgiKioIAIBAgA4Pl4fJ7n+YRC8c7H8/F5ni8UiigU+okIAEAg4gOBA0HfhwcEguTDEwL0g/DxAwFAoFJ/PwFBv1/eHwH6CASKCgoKCvJBCAqKCAEBISAgAAAoFAqFQigUikREoVAoFISEUCgiSQgSQgAAgQgSAlEEEQQACAhSANAfUBAhCAiIj2BKBQUFBAUCQUEEKCQQKCzoJ+gHCCEoKCIKBIIAgQAAvlAg9AuhUOgREYVCoVBgEEKhiBghhIgAAAB/SITEEKQQABAgSAFAIEBBhCAgQABByBMUFBAUCAQFEaGgQKCgoICgECCEIJGIRBAEAggCAIRCgVAghEKhSEQUCoVCAUYIhSJihAgiAgAAiCQJFUMQAAgggCAFBIEEBRGCghACAkBAUFBQUCAQFESEggKBgoICkoKCEIIoIgpCCAhACAAQCoVCoRAKhUIRUSgUCgUhISSJSBISiAgAQCDiE4gTQQAgUAB89OcD4uND8PFJAAAEfkE/Pj++gF/Q5wn6BQryCfAJ8kHwQXAnCOEvACIAgM/j8XiCLxQKWUQhz8cXeDgPw52Q7yciAAAAAAIAANgAQAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAgAPg4AcAAAAAACAACAAAAAABEAAAAAAAACAAawAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB4ABgAAAAABEAAAAAAAAB4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        private final float[] texture;
        private final int[] ascii;
        private final int size;
        private final int ascent;
        private final int descent;
        private final int glyphCount;
        private final boolean lazy;
        private final Glyph[] glyphs;
        protected int characters;
        protected final int[] charWidth = new int[255];
        protected int charHeight;
        protected int[][] chars;
        protected int lineHeight = 10;
        protected int kerning;
        protected int wh;
        public static final int defaultChar = 32;
        private int textureHeight;
        private int textureWidth;

        static class Glyph {

            public int value;
            public int index;
            public float[] image;
            public int height;
            public int width;
            public int setWidth;
            public int topExtent;
            public int leftExtent;

            public void draw(float x, float y, float w, float h) {
                //TODO
            }
        }

        public BitFont(byte[] theBytes) {
            super();

            texture = decodeBitFont(theBytes);
            make();

            size = lineHeight;
            glyphs = new Glyph[256];
            ascii = new int[128];
            Arrays.fill(ascii, -1);
            lazy = false;
            ascent = 4;
            descent = 4;
            glyphCount = 128;
            for (int i = 0; i < 128; i++) {

                // unfortunately the PFont.Glyph constructor in
                // the android source code is for whatever
                // reason protected and not public like in the
                // java application source, therefore the
                // bitfont will only be used in the java
                // application mode until changes to the
                // processing core code have been made. see
                // issue
                // http://code.google.com/p/processing/issues/detail?id=1293

//                try {
//                    Constructor< PFont.Glyph >[] constructors = ( Constructor< PFont.Glyph >[] ) PFont.Glyph.class.getDeclaredConstructors( );
//                    Constructor< PFont.Glyph > constructor = ( Constructor< PFont.Glyph > ) PFont.Glyph.class.getDeclaredConstructors( )[ 0 ];
//                    constructor.setAccessible( true );
//                    for ( Constructor< PFont.Glyph > c : constructors ) {
//                        c.setAccessible( true );
//                        if ( c.getParameterTypes( ).length == 1 ) {
//                            glyphs[ i ] = c.newInstance( this );
//                            break;
//                        }
//                    }
//                } catch ( Exception e ) {
//                    System.out.println( e );
//                }

                // as soon as the constructor is public, the
                // line below will replace the hack above
                glyphs[i] = new Glyph();


                glyphs[i].value = i;

                if (glyphs[i].value < 128) {
                    ascii[glyphs[i].value] = i;
                }

                glyphs[i].index = i;
                int id = i - 32;
                if (id >= 0) {
                    glyphs[i].image = new float[charWidth[id] * 9];
                    for (int n = 0; n < chars[id].length; n++) {
                        glyphs[i].image[n] = (chars[id][n] == 1) ? 0xffffffff : 0x00000000;
                    }
                    glyphs[i].height = 9;
                    glyphs[i].width = charWidth[id];
                    glyphs[i].index = i;
                    glyphs[i].value = i;
                    glyphs[i].setWidth = charWidth[id];
                    glyphs[i].topExtent = 4;
                    glyphs[i].leftExtent = 0;
                } else {
                    glyphs[i].image = new float[1];
                }
            }
        }

        public Glyph getGlyph(char c) {
            int n = (int) c;
            /* if c is out of the BitFont-glyph bounds, return
             * the defaultChar glyph (the space char by
             * default). */
            n = (n >= 128) ? defaultChar : n;
            return glyphs[n];
        }


        static public int byteArrayToInt(byte[] b) {
            int value = 0;
            for (int i = 0; i < 2; i++) {
                int shift = (2 - 1 - i) * 8;
                value += (b[i] & 0x00FF) << shift;
            }
            return value;
        }


        static public int getBit(int theByte, int theIndex) {
            int bitmask = 1 << theIndex;
            return ((theByte & bitmask) > 0) ? 1 : 0;
        }

        float[] decodeBitFont(byte[] bytes) {

            float[] tex;

            // read width
            int w = byteArrayToInt(new byte[]{bytes[0], bytes[1]});

            // read height
            int h = byteArrayToInt(new byte[]{bytes[2], bytes[3]});

            // read size of chars
            int s = byteArrayToInt(new byte[]{bytes[4], bytes[5]});

            // read first ascii char
            int c = byteArrayToInt(new byte[]{bytes[6], bytes[7]});

            tex = new float[w * h];
            textureWidth = w;
            textureHeight = h;

            // read bytes and write pixels into image
            int off = 8 + s;
            for (int i = off; i < bytes.length; i++) {
                for (int j = 0; j < 8; j++) {
                    tex[(i - off) * 8 + j] = getBit(bytes[i], j) == 1 ? 0xff000000 : 0xffffffff;
                }
            }

            int cnt = 0, n = 0, i = 0;

            // add character seperators on top of the texture
            for (i = 0; i < s; i++) {
                while (++cnt != bytes[i + 8]) {
                }
                n += cnt;
                tex[n] = 0xffff0000;
                cnt = 0;
            }

            return tex;
        }

        int getHeight() {
            return textureHeight;
        }

        BitFont make() {

            charHeight = textureHeight;

            lineHeight = charHeight;

            int currWidth = 0;

            for (int i = 0; i < textureWidth; i++) {
                currWidth++;
                if (texture[i] == 0xffff0000) {
                    charWidth[characters++] = currWidth;
                    currWidth = 0;
                }
            }

            chars = new int[characters][];

            int indent = 0;

            for (int i = 0; i < characters; i++) {
                chars[i] = new int[charWidth[i] * charHeight];
                for (int u = 0; u < charWidth[i] * charHeight; u++) {
                    chars[i][u] = texture[indent + (u / charWidth[i]) * textureWidth + (u % charWidth[i])] == 0xff000000 ? 1 : 0;
                }
                indent += charWidth[i];
            }
            return this;
        }
    }
}
