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

package spacegraph.space3d;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
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

    /**
     * 0 for variable timing
     */
    private final int maxSubsteps =
            0;


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

    private SpaceGraphPhys3D() {
        super();

        debug |= DebugDrawModes.NO_HELP_TEXT;


        Intersecter dispatcher = new DefaultIntersecter(new DefaultCollisionConfiguration());


        Broadphase broadphase =

                new DbvtBroadphase();

        dyn = new Dynamics3D<>(dispatcher, broadphase, this);

        onUpdate((dt) -> {
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


        addMouseListenerPost(new FPSLook(this));
        addMouseListenerPost(new OrbMouse(this));

        super.initInput();
    }


    @Override
    protected void initLighting(GL2 gl) {
        gl.glLightModelf(GL_LIGHT_MODEL_AMBIENT, 0.6f);

        final float a = 0.7f;
        float[] light_ambient = {a, a, a, 1.0f};
        float[] light_diffuse = {0.5f, 0.5f, 0.5f, 0.5f};

        float[] light_specular = {0.5f, 0.5f, 0.5f, 0.5f};
        /* light_position is NOT default value */

        float distance = 25f;
        float[] light_position0 = {0f, 0f, distance, 0.0f};


        gl.glLightfv(GL_LIGHT0, GL_AMBIENT, light_ambient, 0);
        gl.glLightfv(GL_LIGHT0, GL_DIFFUSE, light_diffuse, 0);
        gl.glLightfv(GL_LIGHT0, GL_SPECULAR, light_specular, 0);
        gl.glLightfv(GL_LIGHT0, GL_POSITION, light_position0, 0);
        gl.glEnable(GL_LIGHTING);
        gl.glEnable(GL_LIGHT0);


    }

    private final Queue<Spatial> toRemove =

            new ConcurrentLinkedQueue<>();

    private final List<AbstractSpace<X>> inputs = new FasterList<>(1);

    private void update(long dtMS) {

        toRemove.forEach(x -> x.delete(dyn));
        toRemove.clear();

        inputs.forEach((anIi) -> {
            anIi.update(this, dtMS);
        });


        if (simulating) {

            dyn.update(

                    Math.max(dtMS / 1000f, 1000000f / renderFPS)
                            / 1000000.f, maxSubsteps

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

    private DynamicListSpace<X> add(Spatial<X>... s) {
        DynamicListSpace<X> l = new DynamicListSpace<>() {

            final List<Spatial<X>> ls = new FasterList<Spatial<X>>().with(s);

            @Override
            protected List<? extends Spatial<X>> get() {
                return ls;
            }
        };
        add(l);
        return l;
    }

    private SpaceGraphPhys3D<X> add(AbstractSpace<X> c) {
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


    public int getDebug() {
        return debug;
    }

    public void setDebug(int mode) {
        debug = mode;


    }

    @Deprecated
    public SpaceGraphPhys3D<X> with(BroadConstraint b) {
        dyn.addBroadConstraint(b);
        return this;
    }

    @Override
    final public void forEach(Consumer<? super Spatial<X>> each) {

        for (AbstractSpace<X> input : inputs) {
            input.forEach(each);
        }
    }


    /**
     * Bullet's global variables and constants.
     *
     * @author jezek2
     */
    public static class ExtraGlobals {

        public static final boolean DEBUG = true;


        public static final float FLT_EPSILON = 1.19209290e-07f;
        public static final float SIMD_EPSILON = FLT_EPSILON;

        static final float SIMD_2_PI = 6.283185307179586232f;
        public static final float SIMD_PI = SIMD_2_PI * 0.5f;
        public static final float SIMD_HALF_PI = SIMD_2_PI * 0.25f;


        public static boolean gDisableDeactivation;


    }


}




















/*
            if ((debugMode & DebugDrawModes.NO_HELP_TEXT) == 0) {
				setOrthographicProjection();

				




















				String s = "mouse to interact";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				
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

				

				s = "d to toggle deactivation";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "g to toggle mesh animation (ConcaveDemo)";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				
				s = "e to spawn new body (GenericJointDemo)";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "h to toggle help text";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				
				
				yStart += yIncr;

				
				
				
				
				
				

				
				
				buf.setLength(0);
				buf.append("+- shooting speed = ");
				FastFormat.append(buf, ShootBoxInitialSpeed);
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				
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






































