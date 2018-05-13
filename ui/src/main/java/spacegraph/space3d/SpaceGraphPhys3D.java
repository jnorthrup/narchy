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

package spacegraph.space3d;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL2;
import jcog.list.FasterList;
import spacegraph.input.finger.FPSLook;
import spacegraph.input.finger.OrbMouse;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.collision.DefaultCollisionConfiguration;
import spacegraph.space3d.phys.collision.DefaultIntersecter;
import spacegraph.space3d.phys.collision.broad.Broadphase;
import spacegraph.space3d.phys.collision.broad.DbvtBroadphase;
import spacegraph.space3d.phys.collision.broad.Intersecter;
import spacegraph.space3d.phys.constraint.BroadConstraint;
import spacegraph.space3d.phys.math.DebugDrawModes;
import spacegraph.space3d.widget.DynamicListSpace;
import spacegraph.video.Draw;
import spacegraph.video.JoglSpace;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL2.*;

/**
 * @author jezek2
 */

public class SpaceGraphPhys3D<X> extends JoglSpace<X> {

    private final boolean simulating = true;

    /** 0 for variable timing */
    private final int maxSubsteps =
            0;
            //4;


    //protected final BulletStack stack = BulletStack.get();


    //protected final Clock clock = new Clock();

    // this is the most important class
    public final Dynamics3D<X> dyn;

    public SpaceGraphPhys3D<X> camPos(float x, float y, float z) {
        camPos.set(x, y, z);
        return this;
    }


    @Override
    public void windowDestroyed(WindowEvent windowEvent) {
        super.windowDestroyed(windowEvent);
        inputs.clear();
    }

    protected SpaceGraphPhys3D() {
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

        dyn = new Dynamics3D<>(dispatcher, broadphase, this);

        onUpdate((dt)->{
            update(dtMS);
            return true;
        });
    }

    public SpaceGraphPhys3D(AbstractSpace<X>... cc) {
        this();

        for (AbstractSpace c : cc)
            add(c);
    }

    public SpaceGraphPhys3D(Spatial<X>... cc) {
        this();

        add(cc);
    }

    @Override
    protected void initInput() {

        //default 3D input controls
        addMouseListenerPost(new FPSLook(this));
        addMouseListenerPost(new OrbMouse(this));

        super.initInput();
    }


    @Override protected void initLighting(GL2 gl) {
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

    final Queue<Spatial> toRemove =
            //new ArrayBlockingQueue(1024);
            new ConcurrentLinkedQueue<>();

    final List<AbstractSpace<X>> inputs = new FasterList<>(1);

    protected void update(long dtMS) {

        toRemove.forEach(x -> x.delete(dyn));
        toRemove.clear();


        List<AbstractSpace<X>> ii = this.inputs;
        for (int i = 0, inputs1Size = ii.size(); i < inputs1Size; i++)
            ii.get(i).update(this, dtMS);



        if (simulating) {
            // NOTE: SimpleDynamics world doesn't handle fixed-time-stepping
            dyn.update(
                    //dtMS/1000f
                    Math.max(dtMS/1000f, 1000000f / renderFPS)
                            / 1000000.f, maxSubsteps
                    //clock.getTimeThenReset()
            );
        }



    }

    protected void renderVolume(int dtMS) {
        forEach(s -> s.renderAbsolute(gl, dtMS));

        forEach(s -> s.forEachBody(body -> {

            gl.glPushMatrix();

            Draw.transform(gl, body.transform);

            s.renderRelative(gl, body, dtMS);

            gl.glPopMatrix();

        }));
    }

    public DynamicListSpace<X> add(Spatial<X>... s) {
        DynamicListSpace<X> l = new DynamicListSpace<X>() {

            final List<Spatial<X>> ls = new FasterList().with(s);

            @Override
            protected List<? extends Spatial<X>> get() {
                return ls;
            }
        };
        add(l);
        return l;
    }

    public SpaceGraphPhys3D<X> add(AbstractSpace<X> c) {
        if (inputs.add(c))
            c.start(this);
        return this;
    }

    public void removeSpace(AbstractSpace<X> c) {
        if (inputs.remove(c)) {
            c.stop();
        }
    }


    public void remove(Spatial<X> y) {
        toRemove.add(y);
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

    @Deprecated
    public SpaceGraphPhys3D<X> with(BroadConstraint b) {
        dyn.addBroadConstraint(b);
        return this;
    }

    @Override
    final public void forEach(Consumer<? super Spatial<X>> each) {

        for (int i = 0, inputsSize = inputs.size(); i < inputsSize; i++) {
            inputs.get(i).forEach(each);
        }
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

//    public static Dynamic newBody(float mass, Transform startTransform, CollisionShape shape) {
//
//        boolean isDynamic = (mass != 0f);
//        int collisionFilterGroup = isDynamic ? 1 : 2;
//        int collisionFilterMask = isDynamic ? -1 : -3;
//
//        return Dynamics3D.newBody(mass, shape, startTransform, collisionFilterGroup, collisionFilterMask);
//    }




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
