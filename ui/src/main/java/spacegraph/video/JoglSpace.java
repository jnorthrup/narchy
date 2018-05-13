package spacegraph.video;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.FloatUtil;
import jcog.list.FastCoWList;
import spacegraph.input.key.KeyXYZ;
import spacegraph.input.key.WindowKeyControls;
import spacegraph.space2d.Surface;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space3d.Spatial;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;
import spacegraph.util.math.v3;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT;
import static com.jogamp.opengl.GL2ES3.GL_STENCIL;
import static com.jogamp.opengl.GL2GL3.GL_POLYGON_SMOOTH_HINT;
import static com.jogamp.opengl.GLES2.GL_MAX;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.*;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static spacegraph.util.math.v3.v;

abstract public class JoglSpace<X> extends JoglWindow implements Iterable<Spatial<X>> {

    public final v3 camPos;
    public final v3 camFwd;
    public final v3 camUp;
    final float[] mat4f = new float[16];

    final List<Surface> layers = new FastCoWList(Surface[]::new);
    final Queue<Runnable> pending = new ConcurrentLinkedQueue();
    private final float cameraSpeed = 5f;
    private final float cameraRotateSpeed = 5f;
    public float top;
    public float bottom;
    public float zNear = 0.5f;
    public float zFar = 1200;
    public int windowX, windowY;

    protected int debug;
    protected float aspect;
    float tanFovV;
    float left;
    float right;


    public JoglSpace() {
        super();

        onUpdate((Animated) (camPos = new AnimVector3f(0, 0, 5, cameraSpeed)));
        onUpdate((Animated) (camFwd = new AnimVector3f(0, 0, -1, cameraRotateSpeed) {
            @Override
            protected float interp(float dt) {
                interpLERP(dt);
                return 0;
            }
        })); //new AnimVector3f(0,0,1,dyn, 10f);
        onUpdate((Animated) (camUp = new AnimVector3f(0, 1, 0, cameraRotateSpeed) {
            @Override
            protected float interp(float dt) {
                interpLERP(dt);
                return 0;
            }
        })); //new AnimVector3f(0f, 1f, 0f, dyn, 1f);

    }

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {
        super.windowDestroyed(windowEvent);
        layers.clear();
        onUpdate.clear();
    }

    public JoglSpace add(Surface layer) {
        if (layer instanceof Ortho) {
            pending.add(() -> {
                ((Ortho) layer).start(this);
                this.layers.add(layer);
            });
        } else {
            pending.add(() -> {
                layer.start(null);
                this.layers.add(layer);
            });
        }
        return this;
    }

    public boolean remove(Surface layer) {
        if (this.layers.remove(layer)) {
            layer.stop();
            return true;
        }
        return false;
    }

    @Override
    protected void init(GL2 gl) {

        gl.glEnable(GL_STENCIL);


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
        gl.glEnable(GL_NORMALIZE);

        //gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[] { 1, 1, 1, 1 }, 0);
        //gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, new float[] { 0, 0, 0, 0 }, 0);

        initDepth(gl);


        initBlend(gl);


        //loadGLTexture(gl);

//        gleem.start(Vec3f.Y_AXIS, window);
//        gleem.attach(new DefaultHandleBoxManip(gleem).translate(0, 0, 0));
        // JAU
//        gl.glEnable(gl.GL_CULL_FACE);
//        gl.glCullFace(gl.GL_BACK);

        initLighting(gl);

        updateWindowInfo();
        initInput();

        onUpdate((Consumer) (w -> pending.removeIf((x) -> {
            x.run();
            return true;
        })));
    }

    protected void initDepth(GL2 gl) {
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0f);
        gl.glClearDepth(1f); // Depth Buffer Setup
    }

    protected void initBlend(GL2 gl) {
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE);
        gl.glBlendEquationSeparate(GL_FUNC_ADD, GL_MAX);

        //        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        gl.glBlendEquation(GL2.GL_FUNC_ADD);

    }

    protected void initLighting(GL2 gl) {


        // Quick And Dirty Lighting (Assumes Light0 Is Set Up)
        //gl.glEnable(GL2.GL_LIGHT0);

        //gl.glEnable(GL2.GL_LIGHTING); // Enable Lighting


        //gl.glDisable(GL2.GL_SCISSOR_TEST);
    }

    protected void initInput() {

        addKeyListener(new WindowKeyControls(this));

        addKeyListener(new KeyXYZ(this));

    }

    public void camera(v3 target, float radius) {
        v3 fwd = v();

        fwd.sub(target, camPos);
        fwd.normalize();
        camFwd.set(fwd);

        fwd.scale(radius * 1.25f + zNear * 1.25f);
        camPos.sub(target, fwd);

    }

    @Override
    protected final void render(int dtMS) {

        clear();

        updateCamera(dtMS);

        renderVolume(dtMS);

        renderOrthos(dtMS);
    }

    protected void renderVolume(int dtMS) {

    }

    protected void renderOrthos(int dtMS) {

        int facialsSize = layers.size();
        if (facialsSize > 0) {

            GL2 gl = this.gl;

            // See http://www.lighthouse3d.com/opengl/glut/index.php?bmpfontortho
            int w = getWidth();
            int h = getHeight();
            gl.glViewport(0, 0, w, h);
            gl.glMatrixMode(GL_PROJECTION);
            gl.glLoadIdentity();

            //gl.glOrtho(-2.0, 2.0, -2.0, 2.0, -1.5, 1.5);
            gl.glOrtho(0, w, 0, h, -1.5, 1.5);

            //        // switch to projection mode
            //        gl.glMatrixMode(gl.GL_PROJECTION);
            //        // save previous matrix which contains the
            //        //settings for the perspective projection
            //        // gl.glPushMatrix();
            //        // reset matrix
            //        gl.glLoadIdentity();
            //        // set a 2D orthographic projection
            //        glu.gluOrtho2D(0f, screenWidth, 0f, screenHeight);
            //        // invert the y axis, down is positive
            //        //gl.glScalef(1f, -1f, 1f);
            //        // mover the origin from the bottom left corner
            //        // to the upper left corner
            //        //gl.glTranslatef(0f, -screenHeight, 0f);
            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            //gl.glLoadIdentity();


            gl.glDisable(GL2.GL_DEPTH_TEST);

            for (int i = 0; i < facialsSize; i++)
                layers.get(i).render(gl, dtMS);

            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }

    protected void clear() {
        clearMotionBlur(0.5f);
        //clearComplete();

    }

    protected void clearComplete() {
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    protected void clearMotionBlur(float rate /* TODO */) {
//        gl.glClearAccum(0.5f, 0.5f, 0.5f, 1f);
//        gl.glClearColor(0f, 0f, 0f, 1f);
//        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        //if(i == 0)
        gl.glAccum(GL2.GL_LOAD, 0.5f);
        //else
        gl.glAccum(GL2.GL_ACCUM, 0.5f);

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

    protected void updateCamera(int dtMS) {
        perspective();
    }

    public void perspective() {
        //        stack.vectors.push();
//        stack.matrices.push();
//        stack.quats.push();

        if (gl == null)
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

        this.aspect = aspect;

        tanFovV = (float) Math.tan(45 * FloatUtil.PI / 180.0f / 2f);

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


        //        final v3 camDir = new v3();
//        camDir.sub(camPosTarget, camPos);
//        camDir.normalize();

        //System.out.println(camPos + " -> " + camFwd + " x " + camUp);

//        glu.gluLookAt(camPos.x, camPos.y, camPos.z,
//                camPosTarget.x, camPosTarget.y, camPosTarget.z,
//                camUp.x, camUp.y, camUp.z);
        Draw.glu.gluLookAt(camPos.x - camFwd.x, camPos.y - camFwd.y, camPos.z - camFwd.z,
                camPos.x, camPos.y, camPos.z,
                camUp.x, camUp.y, camUp.z);


        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
//        stack.vectors.pop();
//        stack.matrices.pop();
//        stack.quats.pop();
    }


    @Override
    public void windowResized(WindowEvent windowEvent) {
        updateWindowInfo();
    }

    @Override
    public void windowMoved(WindowEvent windowEvent) {
        updateWindowInfo();
    }


    private void updateWindowInfo() {
        GLWindow rww = window;
        if (rww == null)
            return;
//        if (!rww.isRealized() || !rww.isVisible() || !rww.isNativeValid()) {
//            return;
//        }

//        if (gettingScreenPointer.compareAndSet(false, true)) {
//
//            window.getScreen().getDisplay().getEDTUtil().invoke(false, () -> {
//                try {
        Point p = rww.getLocationOnScreen(new Point());
        windowX = p.getX();
        windowY = p.getY();
//                } finally {
//                    gettingScreenPointer.set(false);
//                }
//            });
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

    @Override
    public final Iterator<Spatial<X>> iterator() {
        throw new UnsupportedOperationException("use forEach");
    }


}
