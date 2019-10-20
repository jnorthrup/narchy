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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.texture.Texture;
import jcog.data.list.FasterList;
import jcog.signal.wave2d.Bitmap2D;
import org.jctools.queues.MpscArrayQueue;
import spacegraph.input.finger.util.FPSLook;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.collision.DefaultCollisionConfiguration;
import spacegraph.space3d.phys.collision.DefaultIntersecter;
import spacegraph.space3d.phys.collision.broad.Broadphase;
import spacegraph.space3d.phys.collision.broad.DbvtBroadphase;
import spacegraph.space3d.phys.collision.broad.Intersecter;
import spacegraph.space3d.phys.constraint.BroadConstraint;
import spacegraph.space3d.widget.DynamicListSpace;
import spacegraph.video.Draw;
import spacegraph.video.JoglDisplay;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL2.*;

/**
 * @author jezek2
 */
public class SpaceGraph3D<X> extends JoglDisplay implements Iterable<Spatial<X>> {

    public final Dynamics3D<X> dyn;

    @Deprecated
    private final MpscArrayQueue<Spatial> toRemove = new MpscArrayQueue<>(1024);

    private final FasterList<AbstractSpace<X>> inputs = new FasterList<>(1);

    public SpaceGraph3D<X> camPos(float x, float y, float z) {
        camPos.set(x, y, z);
        return this;
    }
    public SpaceGraph3D<X> camFwd(float x, float y, float z) {
        camFwd.set(x, y, z);
        return this;
    }


//    @Override
//    public void windowDestroyed(WindowEvent windowEvent) {
//        super.windowDestroyed(windowEvent);
//        inputs.clear();
//    }

    public SpaceGraph3D() {
        super();


        Intersecter dispatcher = new DefaultIntersecter(new DefaultCollisionConfiguration());


        Broadphase broadphase =

                new DbvtBroadphase();

        dyn = new Dynamics3D<>(dispatcher, broadphase, this);

//        video.onUpdate((dt) -> {
//            return true;
//        });
    }

    @SafeVarargs
    public SpaceGraph3D(AbstractSpace<X>... cc) {
        this();

        for (AbstractSpace c : cc)
            add(c);
    }

    @SafeVarargs
    public SpaceGraph3D(Spatial<X>... cc) {
        this();

        add(cc);
    }

    @Override
    protected void initInput() {


        video.addMouseListenerPost(new FPSLook(this));
//        display.addMouseListenerPost(new OrbSpaceMouse(this, new Finger(3) {
//            /* TODO */
//        }));

        super.initInput();
    }


    @Override
    protected void initLighting(GL2 gl) {
        gl.glLightModelf(GL_LIGHT_MODEL_AMBIENT, 0.6f);

        final float a = 0.7f;
        float[] light_ambient = {a, a, a, 1.0f};

        /* light_position is NOT default value */


        gl.glLightfv(GL_LIGHT0, GL_AMBIENT, light_ambient, 0);
        float[] light_diffuse = {0.5f, 0.5f, 0.5f, 0.5f};
        gl.glLightfv(GL_LIGHT0, GL_DIFFUSE, light_diffuse, 0);
        float[] light_specular = {0.5f, 0.5f, 0.5f, 0.5f};
        gl.glLightfv(GL_LIGHT0, GL_SPECULAR, light_specular, 0);
        float distance = 25f;
        float[] light_position0 = {0f, 0f, distance, 0.0f};
        gl.glLightfv(GL_LIGHT0, GL_POSITION, light_position0, 0);
        gl.glEnable(GL_LIGHTING);
        gl.glEnable(GL_LIGHT0);


    }


    private void update(long dtMS) {

        toRemove.drain(x-> x.delete(dyn));

        inputs.forEach((anIi) -> anIi.update(this, dtMS));

        boolean simulating = true;
        if (simulating) {

            /**
             * 0 for variable timing
             */
            int maxSubsteps = 0;
            dyn.update(

                    Math.max(dtMS / 1000f, 1000000f / video.renderFPS)
                            / 1000000.f, maxSubsteps

            );
        }


    }

    public void renderVolume(float dtS, GL2 gl, float aspect) {

        updateCamera(gl, aspect);
        //perspective(gl, aspect);


        int dtMS = Math.max(1, Math.round(1000 * dtS));

        update(dtMS);

        for (Spatial<X> xSpatial : this) {
            xSpatial.renderAbsolute(gl, dtMS);
        }

        for (Spatial<X> s : this) {
            s.forEachBody(body -> {

                gl.glPushMatrix();

                Draw.transform(gl, body.transform);

                s.renderRelative(gl, body, dtS);

                gl.glPopMatrix();

            });
        }

        //renderSky();

    }

    private Texture tHv;
    private GLUquadric quad;

    private void renderSky() {


        if (quad == null) {
            quad = Draw.glu.gluNewQuadric();
            Draw.glu.gluQuadricDrawStyle(quad, GLU.GLU_FILL);
            Draw.glu.gluQuadricNormals(quad,
                    //GLU.GLU_SMOOTH
                    //GLU.GLU_FLAT
                    GLU.GLU_NONE
            );
            Draw.glu.gluQuadricTexture(quad, true);
        }
        video.gl.glColor3f(1f, 1f, 1f);
        video.gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_AMBIENT_AND_DIFFUSE,
                //new float[] { 10.0f , 10.0f , 10.0f , 1.0f },
                new float[]{1.0f, 1.0f, 1.0f, 1.0f},
                0);
        if (tHv == null) {
            Tex t = new Tex();

            int w = 33, h = 33;
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = (Graphics2D) bi.getGraphics();
            int on = Bitmap2D.encodeRGB8b(0.3f, 0.3f, 0.3f);
            int off = Bitmap2D.encodeRGB8b(0f, 0f, 0f);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    bi.setRGB(x, y, (x ^ y) % 2 == 0 ? on : off);
                }
            }
            g.dispose();

            t.commit(video.gl);  //HACK
            t.set(bi);  //HACK
            t.commit(video.gl);  //HACK

            tHv = t.texture;
        }
        tHv.enable(video.gl);
        tHv.bind(video.gl);

        video.gl.glPushMatrix();
//        gl.glLoadIdentity();
//        gl.glTranslatef(camPos.x, camPos.y, camPos.z);


        Draw.glu.gluSphere(quad, 1000.0f, 9, 9);
        video.gl.glPopMatrix();

        tHv.disable(video.gl);    ////TTTTTTTTTTTTTTT
    }

    @SafeVarargs
    public final DynamicListSpace<X> add(Spatial<X>... s) {
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

    public SpaceGraph3D<X> add(AbstractSpace<X> c) {
        if (inputs.add(c))
            c.start(this);
        return this;
    }

    private void removeSpace(AbstractSpace<X> c) {
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
    public SpaceGraph3D<X> with(BroadConstraint b) {
        dyn.addBroadConstraint(b);
        return this;
    }

    @Override
    public final void forEach(Consumer<? super Spatial<X>> each) {
        inputs.forEachWith(AbstractSpace::forEach, each);
    }


    /**
     * Bullet's global variables and constants.
     *
     * @author jezek2
     */
    public static class ExtraGlobals {


        public static final float FLT_EPSILON = 1.19209290e-07f;
        public static final float SIMD_EPSILON = FLT_EPSILON;

        static final float SIMD_2_PI = 6.283185307179586232f;
        public static final float SIMD_PI = SIMD_2_PI * 0.5f;
        public static final float SIMD_HALF_PI = SIMD_2_PI * 0.25f;


    }

    @Override
    public final Iterator<Spatial<X>> iterator() {
        throw new UnsupportedOperationException("use forEach");
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

				s = "q to stop";
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






































