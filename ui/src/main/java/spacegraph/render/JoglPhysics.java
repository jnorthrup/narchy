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

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.math.FloatUtil;
import org.jetbrains.annotations.NotNull;
import spacegraph.Spatial;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.DefaultCollisionConfiguration;
import spacegraph.phys.collision.DefaultIntersecter;
import spacegraph.phys.collision.broad.Broadphase;
import spacegraph.phys.collision.broad.DbvtBroadphase;
import spacegraph.phys.collision.broad.Intersecter;
import spacegraph.phys.math.Clock;
import spacegraph.phys.math.DebugDrawModes;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.util.AnimVector3f;

import static com.jogamp.opengl.GL2.*;
import static spacegraph.math.v3.v;

/**
 * @author jezek2
 */

abstract public class JoglPhysics<X> extends JoglSpace implements KeyListener, Iterable<Spatial<X>> {




    private final float cameraSpeed = 5f;
    private final float cameraRotateSpeed = 5f;
    private boolean simulating = true;
    private float lastFrameTime;

    private final int maxSubsteps = 0; //set to zero for variable timing
    protected float aspect;





    public void camera(v3 target, float radius) {
        v3 fwd = v();

        fwd.sub(target, camPos);
        fwd.normalize();
        camFwd.set(fwd);

        fwd.scale(radius * 1.25f + zNear * 1.25f);
        camPos.sub(target, fwd);

    }





    /**
     * activate/deactivate the simulation; by default it is enabled
     */
    public void setSimulating(boolean simulating) {
        this.simulating = simulating;
    }

    //protected final BulletStack stack = BulletStack.get();


    protected final Clock clock = new Clock();

    // this is the most important class
    public final @NotNull Dynamics<X> dyn;


    protected int debug;


    public final v3 camPos;
    public final v3 camFwd;
    public final v3 camUp;
    //    public final MutableFloat cameraDistance;
//    public final MutableFloat ele;
//    public final MutableFloat azi;
    public float top;
    public float bottom;
    float tanFovV;
    //float fov;
    float left;
    float right;

    public float zNear = 0.5f;
    public float zFar = 1200;


    protected boolean stepping = true;
    protected int lastKey;



    protected JoglPhysics() {
        super();

        debug |= DebugDrawModes.NO_HELP_TEXT;


        // Setup the basic world

        Intersecter dispatcher = new DefaultIntersecter(new DefaultCollisionConfiguration());

        //btPoint3 worldAabbMin(-10000,-10000,-10000);
        //btPoint3 worldAabbMax(10000,10000,10000);
        //btBroadphaseInterface* overlappingPairCache = new btAxisSweep3 (worldAabbMin, worldAabbMax);

        Broadphase broadphase =
//                //new SimpleBroadphase();
                new DbvtBroadphase();

        dyn = new Dynamics<X>(dispatcher, broadphase, this);

//        cameraDistance = new AnimFloat(55f, dyn, 4f);
//        azi = new AnimFloatAngle(-180, dyn, 30f);
//        ele = new AnimFloatAngle(20, dyn, 30f);

        camPos = new AnimVector3f(0, 0, 5, dyn, cameraSpeed);
        camFwd = new AnimVector3f(0, 0, -1, dyn, cameraRotateSpeed); //new AnimVector3f(0,0,1,dyn, 10f);
        camUp = new AnimVector3f(0, 1, 0, dyn, cameraRotateSpeed); //new AnimVector3f(0f, 1f, 0f, dyn, 1f);


    }



    /**
     * return false to remove this object during the beginning of the physics frame
     */
    protected boolean valid(int nextID, Collidable<X> c) {
        return true;
    }

    @Override
    protected void init(GL2 gl2) {

        window.addKeyListener(this);

        //gl.glEnable(GL_POINT_SPRITE);
        //gl.glEnable(GL_POINT_SMOOTH);
        gl.glEnable(GL_LINE_SMOOTH);
        //gl.glEnable(GL_POLYGON_SMOOTH); //[Polygon smooth] is not a recommended method for anti-aliasing. Use Multisampling instead.
        gl.glEnable(GL2.GL_MULTISAMPLE);

//        gl.glShadeModel(
//            GL_SMOOTH
//            //GL_FLAT
//        );



        gl.glHint(GL_POLYGON_SMOOTH_HINT,
                GL_NICEST);
                //GL_FASTEST);
        gl.glHint(GL_LINE_SMOOTH_HINT,
                GL_NICEST);
                //GL_FASTEST);
        gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT,
                GL_NICEST);
                //GL_FASTEST);

        //https://www.sjbaker.org/steve/omniv/opengl_lighting.html
        gl.glColorMaterial(GL_FRONT_AND_BACK,
                GL_AMBIENT_AND_DIFFUSE
                //GL_DIFFUSE
        );
        gl.glEnable(GL_COLOR_MATERIAL);
        //gl.glEnable(GL_NORMALIZE);

        //gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[] { 1, 1, 1, 1 }, 0);
        //gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, new float[] { 0, 0, 0, 0 }, 0);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

//        gl.glEnable(GL2.GL_TEXTURE_2D); // Enable Texture Mapping

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0f); // Black Background
        gl.glClearDepth(1f); // Depth Buffer Setup

        // Quick And Dirty Lighting (Assumes Light0 Is Set Up)
        //gl.glEnable(GL2.GL_LIGHT0);

        //gl.glEnable(GL2.GL_LIGHTING); // Enable Lighting


        gl.glDisable(GL2.GL_SCISSOR_TEST);

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //gl.glBlendEquation(GL_FUNC_ADD);


        //gl.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        //gl.glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);

        //loadGLTexture(gl);

//        gleem.start(Vec3f.Y_AXIS, window);
//        gleem.attach(new DefaultHandleBoxManip(gleem).translate(0, 0, 0));
        // JAU
//        gl.glEnable(gl.GL_CULL_FACE);
//        gl.glCullFace(gl.GL_BACK);





        initLighting();
    }

    protected void initLighting() {
        gl.glLightModelf(GL_LIGHT_MODEL_AMBIENT, 0.6f);

        final float a = 0.7f;
        float[] light_ambient = {a, a, a, 1.0f};
        float[] light_diffuse = {0.5f, 0.5f, 0.5f, 0.5f};
        //float[] light_specular = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        float[] light_specular = {0.5f, 0.5f, 0.5f, 0.5f};
        /* light_position is NOT default value */

        float distance = 25f;
        float[] light_position0 = {0f, 0f, distance, 0.0f};

        //float[] light_position1 = new float[]{-1.0f, -10.0f, -1.0f, 0.0f};

//        if (gl.isGLES2()) {
//            //gl.enableFixedFunctionEmulationMode(GLES2.FIXED_EMULATION_VERTEXCOLORTEXTURE);
//
//        }

        gl.glLightfv(GL_LIGHT0, GL_AMBIENT, light_ambient, 0);
        gl.glLightfv(GL_LIGHT0, GL_DIFFUSE, light_diffuse, 0);
        gl.glLightfv(GL_LIGHT0, GL_SPECULAR, light_specular, 0);
        gl.glLightfv(GL_LIGHT0, GL_POSITION, light_position0, 0);
        gl.glEnable(GL_LIGHTING);
        gl.glEnable(GL_LIGHT0);


//        if (useLight1) {
//            gl.glLightfv(gl.GL_LIGHT1, gl.GL_AMBIENT, light_ambient, 0);
//            gl.glLightfv(gl.GL_LIGHT1, gl.GL_DIFFUSE, light_diffuse, 0);
//            gl.glLightfv(gl.GL_LIGHT1, gl.GL_SPECULAR, light_specular, 0);
//            gl.glLightfv(gl.GL_LIGHT1, gl.GL_POSITION, light_position1, 0);
//        }


    }


    @Override
    public final void reshape(GLAutoDrawable drawable,
                              int xstart,
                              int ystart,
                              int width,
                              int height) {

        //height = (height == 0) ? 1 : height;

        //updateCamera();
    }

    //final AtomicBoolean busy = new AtomicBoolean(false);

    @Override protected void update() {


        long dt = clock.getTimeThenReset();
        lastFrameTime = dt / 1000f;

        if (simulating) {
            // NOTE: SimpleDynamics world doesn't handle fixed-time-stepping
            dyn.update(
                    Math.max(dt, 1000000f / RENDER_FPS_IDEAL) / 1000000.f, maxSubsteps
                    //clock.getTimeThenReset()
            );
        }



    }



    @Override protected void render(int dtMS) {

        clear();

        updateCamera(dtMS);

        forEach(s -> s.renderAbsolute(gl, dtMS));

        forEach(s -> s.forEachBody(body -> {

            gl.glPushMatrix();

            Draw.transform(gl, body.transform);

            s.renderRelative(gl, body, dtMS);

            gl.glPopMatrix();

        }));
    }


    protected void clear() {
        //clearMotionBlur(0.35f);
        clearComplete();

    }

    protected void clearComplete() {
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    protected void clearMotionBlur(float rate /* TODO */) {
//        gl.glClearAccum(0.5f, 0.5f, 0.5f, 1f);
//        gl.glClearColor(0f, 0f, 0f, 1f);
//        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        //if(i == 0)
        gl.glAccum(GL2.GL_LOAD, rate);
        //else
        gl.glAccum(GL2.GL_ACCUM, 1f-rate);

//        i++;
//
//        if(i >= n) {
//            i = 0;
        gl.glAccum(GL2.GL_RETURN, rate);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        //gl.glSwapBuffers();
//            wait_until_next(timestep);
//        }
    }



    /**
     * in seconds
     */
    public float getLastFrameTime() {
        return lastFrameTime;
    }


    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

//    final Matrix3f tmpMat1 = new Matrix3f(); //stack.matrices.get();
//    final Matrix3f tmpMat2 = new Matrix3f(); //stack.matrices.get();
//    final Quat4f roll = new Quat4f(); //stack.quats.get();
//    final Quat4f rot = new Quat4f(); //stack.quats.get();

    protected void updateCamera(int dtMS) {
        perspective();
    }

    public void perspective() {
        //        stack.vectors.push();
//        stack.matrices.push();
//        stack.quats.push();

        if(gl == null)
            return;

        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();

//        System.out.println(camPos + " " + camUp + " " + camPosTarget);
//        float rele = ele.floatValue() * 0.01745329251994329547f; // rads per deg
//        float razi = azi.floatValue() * 0.01745329251994329547f; // rads per deg

//        QuaternionUtil.setRotation(rot, camUp, razi);
//        v3 eyePos = v();
//        VectorUtil.setCoord(eyePos, forwardAxis, -cameraDistance.floatValue());
//
//        v3 forward = v(eyePos.x, eyePos.y, eyePos.z);
//        if (forward.lengthSquared() < ExtraGlobals.FLT_EPSILON) {
//            forward.set(1f, 0f, 0f);
//        }
//
//        v3 camRight = v();
//        camRight.cross(camUp, forward);
//        camRight.normalize();
//        QuaternionUtil.setRotation(roll, camRight, -rele);
//
//
//        tmpMat1.set(rot);
//        tmpMat2.set(roll);
//        tmpMat1.mul(tmpMat2);
//        tmpMat1.transform(eyePos);
//
//        camPos.set(eyePos);

        //gl.glFrustumf(-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10000.0f);
        //glu.gluPerspective(45, (float) screenWidth / screenHeight, 4, 2000);
        float aspect = ((float) getWidth()) / getHeight();

        perspective(0, true, 45 * FloatUtil.PI / 180.0f, aspect);


//        final v3 camDir = new v3();
//        camDir.sub(camPosTarget, camPos);
//        camDir.normalize();

        //System.out.println(camPos + " -> " + camFwd + " x " + camUp);

//        glu.gluLookAt(camPos.x, camPos.y, camPos.z,
//                camPosTarget.x, camPosTarget.y, camPosTarget.z,
//                camUp.x, camUp.y, camUp.z);
        glu.gluLookAt(camPos.x - camFwd.x, camPos.y - camFwd.y, camPos.z - camFwd.z,
                camPos.x, camPos.y, camPos.z,
                camUp.x, camUp.y, camUp.z);


        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
//        stack.vectors.pop();
//        stack.matrices.pop();
//        stack.quats.pop();
    }


    public final float[] mat4f = new float[16];

    void perspective(final int m_off, final boolean initM,
                     final float fovy_rad, final float aspect) throws GLException {

        this.aspect = aspect;

        tanFovV = (float) Math.tan(fovy_rad / 2f);

        top = tanFovV * zNear; // use tangent of half-fov !
        right = aspect * top;    // aspect * fovhvTan.top * zNear
        bottom = -top;
        left = -right;

//        gl.glMultMatrixf(
//                makeFrustum(matTmp, m_off, initM, left, right, bottom, top, zNear, zFar),
//                0
//        );

        //glu.gluPerspective(45, aspect, zNear, zFar);
        gl.glMultMatrixf(FloatUtil.makePerspective(mat4f, 0, true, 45 * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0);


    }


//    public void keyboardCallback(char key) {
//        lastKey = 0;
//
//        if (key >= 0x31 && key < 0x37) {
//            int child = key - 0x31;
//            // TODO: m_profileIterator->Enter_Child(child);
//        }
//        if (key == 0x30) {
//            // TODO: m_profileIterator->Enter_Parent();
//        }
//
//        switch (key) {
//            case 'h':
//                if ((debug & DebugDrawModes.NO_HELP_TEXT) != 0) {
//                    debug = debug & (~DebugDrawModes.NO_HELP_TEXT);
//                } else {
//                    debug |= DebugDrawModes.NO_HELP_TEXT;
//                }
//                break;
//
//            case 'w':
//                if ((debug & DebugDrawModes.DRAW_WIREFRAME) != 0) {
//                    debug = debug & (~DebugDrawModes.DRAW_WIREFRAME);
//                } else {
//                    debug |= DebugDrawModes.DRAW_WIREFRAME;
//                }
//                break;
//
//            case 'p':
//                if ((debug & DebugDrawModes.PROFILE_TIMINGS) != 0) {
//                    debug = debug & (~DebugDrawModes.PROFILE_TIMINGS);
//                } else {
//                    debug |= DebugDrawModes.PROFILE_TIMINGS;
//                }
//                break;
//
//            case 'm':
//                if ((debug & DebugDrawModes.ENABLE_SAT_COMPARISON) != 0) {
//                    debug = debug & (~DebugDrawModes.ENABLE_SAT_COMPARISON);
//                } else {
//                    debug |= DebugDrawModes.ENABLE_SAT_COMPARISON;
//                }
//                break;
//
//            case 'n':
//                if ((debug & DebugDrawModes.DISABLE_BULLET_LCP) != 0) {
//                    debug = debug & (~DebugDrawModes.DISABLE_BULLET_LCP);
//                } else {
//                    debug |= DebugDrawModes.DISABLE_BULLET_LCP;
//                }
//                break;
//
//            case 't':
//                if ((debug & DebugDrawModes.DRAW_TEXT) != 0) {
//                    debug = debug & (~DebugDrawModes.DRAW_TEXT);
//                } else {
//                    debug |= DebugDrawModes.DRAW_TEXT;
//                }
//                break;
//            case 'y':
//                if ((debug & DebugDrawModes.DRAW_FEATURES_TEXT) != 0) {
//                    debug = debug & (~DebugDrawModes.DRAW_FEATURES_TEXT);
//                } else {
//                    debug |= DebugDrawModes.DRAW_FEATURES_TEXT;
//                }
//                break;
//            case 'a':
//                if ((debug & DebugDrawModes.DRAW_AABB) != 0) {
//                    debug = debug & (~DebugDrawModes.DRAW_AABB);
//                } else {
//                    debug |= DebugDrawModes.DRAW_AABB;
//                }
//                break;
//            case 'c':
//                if ((debug & DebugDrawModes.DRAW_CONTACT_POINTS) != 0) {
//                    debug = debug & (~DebugDrawModes.DRAW_CONTACT_POINTS);
//                } else {
//                    debug |= DebugDrawModes.DRAW_CONTACT_POINTS;
//                }
//                break;
//
//            case 'd':
//                if ((debug & DebugDrawModes.NO_DEACTIVATION) != 0) {
//                    debug = debug & (~DebugDrawModes.NO_DEACTIVATION);
//                } else {
//                    debug |= DebugDrawModes.NO_DEACTIVATION;
//                }
//                if ((debug & DebugDrawModes.NO_DEACTIVATION) != 0) {
//                    ExtraGlobals.gDisableDeactivation = true;
//                } else {
//                    ExtraGlobals.gDisableDeactivation = false;
//                }
//                break;
//
//            case 'o': {
//                stepping = !stepping;
//                break;
//            }
//            case 's':
//                break;
//            //    case ' ' : newRandom(); break;
//
//            case '1': {
//                if ((debug & DebugDrawModes.ENABLE_CCD) != 0) {
//                    debug = debug & (~DebugDrawModes.ENABLE_CCD);
//                } else {
//                    debug |= DebugDrawModes.ENABLE_CCD;
//                }
//                break;
//            }
//
//
//            default:
//                // std::cout << "unused key : " << key << std::endl;
//                break;
//        }
//
////        if (getDyn() != null && getDyn().debugDrawer != null) {
////            getDyn().debugDrawer.setDebugMode(debug);
////        }
//
//        //LWJGL.postRedisplay();
//
//    }

    public int getDebug() {
        return debug;
    }

    public void setDebug(int mode) {
        debug = mode;
//        if (getDyn() != null && getDyn().debugDrawer != null) {
//            getDyn().debugDrawer.setDebugMode(mode);
//        }
    }

//    public void specialKeyboard(int keycode) {
//        switch (keycode) {
//            case KeyEvent.VK_F1: {
//                break;
//            }
//            case KeyEvent.VK_F2: {
//                break;
//            }
//            case KeyEvent.VK_END: {
////                int numObj = getDyn().getNumCollisionObjects();
////                if (numObj != 0) {
////                    CollisionObject<X> obj = getDyn().objects().get(numObj - 1);
////
////                    getDyn().removeCollisionObject(obj);
////                    RigidBody body = RigidBody.upcast(obj);
////                    if (body != null && body.getMotionState() != null) {
////                        //delete body->getMotionState();
////                    }
////                    //delete obj;
////                }
//                break;
//            }
//            /*
//            case KeyEvent.VK_PRIOR:
//				zoomIn();
//				break;
//			case KeyEvent.VK_NEXT:
//				zoomOut();
//				break;
//            */
//
//            default:
//                // std::cout << "unused (special) key : " << key << std::endl;
//                break;
//        }
//    }

//    public void shootBox(Vector3f destination) {
//        if (dyn != null) {
//            float mass = 10f;
//            Transform startTransform = new Transform();
//            startTransform.setIdentity();
//            Vector3f camPos = v(getCamPos());
//            startTransform.origin.set(camPos);
//
//            if (shootBoxShape == null) {
//                //#define TEST_UNIFORM_SCALING_SHAPE 1
//                //#ifdef TEST_UNIFORM_SCALING_SHAPE
//                //btConvexShape* childShape = new btBoxShape(btVector3(1.f,1.f,1.f));
//                //m_shootBoxShape = new btUniformScalingShape(childShape,0.5f);
//                //#else
//                shootBoxShape = new BoxShape(v(1f, 1f, 1f));
//                //#endif//
//            }
//
//            RigidBody body = this.newBody(mass, startTransform, shootBoxShape);
//
//            Vector3f linVel = v(destination.x - camPos.x, destination.y - camPos.y, destination.z - camPos.z);
//            linVel.normalize();
//            linVel.scale(ShootBoxInitialSpeed);
//
//            Transform ct = new Transform();
//            ct.origin.set(camPos);
//            ct.setRotation(new Quat4f(0f, 0f, 0f, 1f));
//            body.setWorldTransform(ct);
//
//            body.setLinearVelocity(linVel);
//            body.setAngularVelocity(v(0f, 0f, 0f));
//        }
//    }



//    public v3 rayTo(int px, int py) {
//        float x = (2.0f * px) / getWidth() - 1.0f;
//        float y = 1.0f - (2.0f * py) / getHeight();
//        float z = 0.0f;
//        v3 ray_nds = v(x, y, z);
//        Vector4f ray_eye = new Vector4f( x, y, -1.0f, 1.0f );
//
//        //https://capnramses.github.io/opengl/raycasting.html
//        Matrix4f viewMatrixInv = new Matrix4f(mat4f);
//        viewMatrixInv.invert();
//        viewMatrixInv.transform(ray_eye);
//        ray_eye.setZ(-1f);
//        ray_eye.setW(1f);
//
//        viewMatrixInv.transform(ray_eye);
//        v3 ray_wor = v(ray_eye.x, ray_eye.y, ray_eye.z);
//        ray_wor.normalize();
//
//
//        return ray_wor;
//
//        //return rayTo(-1f + 2 * x / ((float) getWidth()), -1f + 2 * y / ((float) getHeight()));
//    }

//    public v3 rayTo(float x, float y) {
//        return rayTo(x, y, zFar);
//    }
//
//    public v3 rayTo(float x, float y, float depth) {
//
//        v3 hor = v().cross(camFwd, camUp);
//        v3 ver = v().cross(hor, camFwd);
//
//        v3 center = v(camPos);
//        center.addScaled(camFwd, depth);
//
//        return (v3)
//                v(center)
//                        .addScaled(hor, depth * tanFovV * aspect * x)
//                        .addScaled(ver, depth * tanFovV * -y);
//    }

    /**
     * Bullet's global variables and constants.
     *
     * @author jezek2
     */
    public static class ExtraGlobals {

        public static final boolean DEBUG = true;


        public static final float FLT_EPSILON = 1.19209290e-07f;
        public static final float SIMD_EPSILON = FLT_EPSILON;

        public static final float SIMD_2_PI = 6.283185307179586232f;
        public static final float SIMD_PI = SIMD_2_PI * 0.5f;
        public static final float SIMD_HALF_PI = SIMD_2_PI * 0.25f;


        public static boolean gDisableDeactivation;


//        static {
//            if (ENABLE_PROFILE) {
//                Runtime.getRuntime().addShutdownHook(new Thread() {
//                    @Override
//                    public void run() {
//                        printProfiles();
//                    }
//                });
//            }
//        }

//        private static class ProfileBlock {
//            public String name;
//            public long startTime;
//        }

    }

    public static Dynamic newBody(float mass, Transform startTransform, CollisionShape shape) {

        boolean isDynamic = (mass != 0f);
        int collisionFilterGroup = isDynamic ? 1 : 2;
        int collisionFilterMask = isDynamic ? -1 : -3;

        return Dynamics.newBody(mass, shape, startTransform, collisionFilterGroup, collisionFilterMask);
    }




    //    public void clientResetScene() {
//        //#ifdef SHOW_NUM_DEEP_PENETRATIONS
////		BulletGlobals.gNumDeepPenetrationChecks = 0;
////		BulletGlobals.gNumGjkChecks = 0;
//        //#endif //SHOW_NUM_DEEP_PENETRATIONS
//
//        int numObjects = 0;
//        if (dyn != null) {
//            dyn.stepSimulation(1f / 60f, 0);
//            numObjects = dyn.getNumCollisionObjects();
//        }
//
//        for (int i = 0; i < numObjects; i++) {
//            CollisionObject colObj = dyn.getCollisionObjectArray().get(i);
//            RigidBody body = RigidBody.upcast(colObj);
//            if (body != null) {
//                if (body.getMotionState() != null) {
//                    Motion myMotionState = (Motion) body.getMotionState();
//                    myMotionState.graphicsWorldTrans.set(myMotionState.startWorldTrans);
//                    colObj.setWorldTransform(myMotionState.graphicsWorldTrans);
//                    colObj.setInterpolationWorldTransform(myMotionState.startWorldTrans);
//                    colObj.activate();
//                }
//                // removed cached contact points
//                dyn.getBroadphase().getOverlappingPairCache().cleanProxyFromPairs(colObj.getBroadphaseHandle(), getDyn().getDispatcher());
//
//                body = RigidBody.upcast(colObj);
//                if (body != null && !body.isStaticObject()) {
//                    RigidBody.upcast(colObj).setLinearVelocity(v(0f, 0f, 0f));
//                    RigidBody.upcast(colObj).setAngularVelocity(v(0f, 0f, 0f));
//                }
//            }
//
//			/*
//            //quickly search some issue at a certain simulation frame, pressing space to reset
//			int fixed=18;
//			for (int i=0;i<fixed;i++)
//			{
//			getDynamicsWorld()->stepSimulation(1./60.f,1);
//			}
//			*/
//        }
//    }

}


//GLShapeDrawer.drawCoordSystem(gl);

//            if (false) {
//                System.err.println("++++++++++++++++++++++++++++++++");
//                System.err.println("++++++++++++++++++++++++++++++++");
//                try {
//                    Thread.sleep(2000);
//                } catch (Exception e) {
//                }
//            }

//            float xOffset = 10f;
//            float yStart = 20f;
//            float yIncr = 20f;

// gl.glDisable(gl.GL_LIGHTING);
// JAU gl.glColor4f(0f, 0f, 0f, 0f);

/*
            if ((debugMode & DebugDrawModes.NO_HELP_TEXT) == 0) {
				setOrthographicProjection();

				// TODO: showProfileInfo(xOffset,yStart,yIncr);

//					#ifdef USE_QUICKPROF
//					if ( getDebugMode() & btIDebugDraw::DBG_ProfileTimings)
//					{
//						static int counter = 0;
//						counter++;
//						std::map<std::string, hidden::ProfileBlock*>::iterator iter;
//						for (iter = btProfiler::mProfileBlocks.begin(); iter != btProfiler::mProfileBlocks.end(); ++iter)
//						{
//							char blockTime[128];
//							sprintf(blockTime, "%s: %lf",&((*iter).first[0]),btProfiler::getBlockTime((*iter).first, btProfiler::BLOCK_CYCLE_SECONDS));//BLOCK_TOTAL_PERCENT));
//							glRasterPos3f(xOffset,yStart,0);
//							BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),blockTime);
//							yStart += yIncr;
//
//						}
//					}
//					#endif //USE_QUICKPROF


				String s = "mouse to interact";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// JAVA NOTE: added
				s = "LMB=shoot, RMB=drag, MIDDLE=apply impulse";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "space to reset";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "cursor keys and z,x to navigate";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "i to toggle simulation, s single step";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "q to quit";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = ". to shoot box";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// not yet hooked up again after refactoring...

				s = "d to toggle deactivation";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "g to toggle mesh animation (ConcaveDemo)";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// JAVA NOTE: added
				s = "e to spawn new body (GenericJointDemo)";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "h to toggle help text";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//buf = "p to toggle profiling (+results to file)";
				//drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//bool useBulletLCP = !(getDebugMode() & btIDebugDraw::DBG_DisableBulletLCP);
				//bool useCCD = (getDebugMode() & btIDebugDraw::DBG_EnableCCD);
				//glRasterPos3f(xOffset,yStart,0);
				//sprintf(buf,"1 CCD mode (adhoc) = %i",useCCD);
				//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//glRasterPos3f(xOffset, yStart, 0);
				//buf = String.format(%10.2f", ShootBoxInitialSpeed);
				buf.setLength(0);
				buf.append("+- shooting speed = ");
				FastFormat.append(buf, ShootBoxInitialSpeed);
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//#ifdef SHOW_NUM_DEEP_PENETRATIONS
				buf.setLength(0);
				buf.append("gNumDeepPenetrationChecks = ");
				FastFormat.append(buf, BulletGlobals.gNumDeepPenetrationChecks);
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				buf.setLength(0);
				buf.append("gNumGjkChecks = ");
				FastFormat.append(buf, BulletGlobals.gNumGjkChecks);
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//buf = String.format("gNumAlignedAllocs = %d", BulletGlobals.gNumAlignedAllocs);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//buf = String.format("gNumAlignedFree= %d", BulletGlobals.gNumAlignedFree);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//buf = String.format("# alloc-free = %d", BulletGlobals.gNumAlignedAllocs - BulletGlobals.gNumAlignedFree);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//enable BT_DEBUG_MEMORY_ALLOCATIONS define in Bullet/src/LinearMath/btAlignedAllocator.h for memory leak detection
				//#ifdef BT_DEBUG_MEMORY_ALLOCATIONS
				//glRasterPos3f(xOffset,yStart,0);
				//sprintf(buf,"gTotalBytesAlignedAllocs = %d",gTotalBytesAlignedAllocs);
				//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;
				//#endif //BT_DEBUG_MEMORY_ALLOCATIONS

				if (getDynamicsWorld() != null) {
					buf.setLength(0);
					buf.append("# objects = ");
					FastFormat.append(buf, getDynamicsWorld().getNumCollisionObjects());
					drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
					yStart += yIncr;

					buf.setLength(0);
					buf.append("# pairs = ");
					FastFormat.append(buf, getDynamicsWorld().getBroadphase().getOverlappingPairCache().getNumOverlappingPairs());
					drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
					yStart += yIncr;

				}
				//#endif //SHOW_NUM_DEEP_PENETRATIONS

				// JAVA NOTE: added
				int free = (int)Runtime.getRuntime().freeMemory();
				int total = (int)Runtime.getRuntime().totalMemory();
				buf.setLength(0);
				buf.append("heap = ");
				FastFormat.append(buf, (float)(total - free) / (1024*1024));
				buf.append(" / ");
				FastFormat.append(buf, (float)(total) / (1024*1024));
				buf.append(" MB");
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				resetPerspectiveProjection();
			} */

// gl.glEnable(gl.GL_LIGHTING);


//    public void renderObject(CollisionObject colObj) {

//        if (0 == i) {
//            wireColor.set(0.5f, 1f, 0.5f); // wants deactivation
//        } else {
//            wireColor.set(1f, 1f, 0.5f); // wants deactivation
//        }
//        if ((i & 1) != 0) {
//            wireColor.set(0f, 0f, 1f);
//        }

//        // color differently for active, sleeping, wantsdeactivation states
//        if (colObj.getActivationState() == 1) // active
//        {
//            if ((i & 1) != 0) {
//                //wireColor.add(new Vector3f(1f, 0f, 0f));
//                wireColor.x += 1f;
//            } else {
//                //wireColor.add(new Vector3f(0.5f, 0f, 0f));
//                wireColor.x += 0.5f;
//            }
//        }
//        if (colObj.getActivationState() == 2) // ISLAND_SLEEPING
//        {
//            if ((i & 1) != 0) {
//                //wireColor.add(new Vector3f(0f, 1f, 0f));
//                wireColor.y += 1f;
//            } else {
//                //wireColor.add(new Vector3f(0f, 0.5f, 0f));
//                wireColor.y += 0.5f;
//            }
//        }

//    }
