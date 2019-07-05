package spacegraph.video;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import jcog.event.Off;
import jcog.math.v3;
import spacegraph.SpaceGraph;
import spacegraph.input.key.KeyXYZ;
import spacegraph.input.key.WindowKeyControls;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;

import java.util.function.Consumer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT;
import static com.jogamp.opengl.GL2ES3.GL_STENCIL;
import static com.jogamp.opengl.GL2GL3.GL_POLYGON_SMOOTH;
import static com.jogamp.opengl.GL2GL3.GL_POLYGON_SMOOTH_HINT;
import static com.jogamp.opengl.GLES2.GL_MAX;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.*;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static jcog.math.v3.v;

/** JOGL display implementation */
abstract public class JoglDisplay extends SpaceGraph {

    /**
     * the hardware input/output implementation
     */
    public final JoglWindow video;

    public final v3 camPos, camFwd, camUp;
    private final float[] mat4f = new float[16];

    private final float cameraSpeed = 100f, cameraRotateSpeed = cameraSpeed;

    public float top;
    public float bottom;
    private float left;
    private float right;

    private float tanFovV;

    public float zNear = 0.5f, zFar = 1200;

    protected int debug;

    public JoglDisplay() {
        video = new MyJoglWindow();

        video.onUpdate((Animated) (camPos = new AnimVector3f(0, 0, 5, cameraSpeed)));
        video.onUpdate((Animated) (camFwd = new AnimVector3f(0, 0, -1, cameraRotateSpeed)));
        video.onUpdate((Animated) (camUp = new AnimVector3f(0, 1, 0, cameraRotateSpeed)));

    }


    private void initDepth(GL2 gl) {
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);


        gl.glClearColor(0.0f, 0.0f, 0.0f, 0f);
        gl.glClearDepth(1f);
    }

    private void initBlend(GL gl) {
        gl.glEnable(GL_BLEND);
        gl.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE);
        gl.glBlendEquationSeparate(GL_FUNC_ADD, GL_MAX);
    }

    protected void initLighting(GL2 gl) {

    }

    protected void initInput() {

        video.addKeyListener(new WindowKeyControls(JoglDisplay.this));

        video.addKeyListener(new KeyXYZ(JoglDisplay.this));

    }

    public void camera(v3 target, float radius) {
        v3 fwd = v();
        fwd.sub(target, camPos);
        fwd.normalize();
        camFwd.set(fwd);

        fwd.scaled(radius * 1.25f + zNear * 1.25f);
        camPos.sub(target, fwd);

    }

    protected void renderVolume(float dtS, GL2 gl) {

    }

    protected void renderOrthos(float dtS) {

    }


    private void clear(GL2 gl) {
        //clearMotionBlur(0.5f, gl);
        clearComplete(gl);

    }

    protected void clearComplete(GL2 gl) {
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void clearMotionBlur(float rate /* TODO */, GL2 gl) {


        gl.glAccum(GL2.GL_LOAD, 0.5f);

        gl.glAccum(GL2.GL_ACCUM, 0.5f);


        gl.glAccum(GL2.GL_RETURN, rate);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);


    }



    protected void perspective(GL2 gl) {


        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();


        float aspect = ((float) video.getWidth()) / video.getHeight();


        tanFovV = (float) Math.tan(45 * FloatUtil.PI / 180.0f / 2f);

        top = tanFovV * zNear;
        right = aspect * top;
        bottom = -top;
        left = -right;


        gl.glMultMatrixf(FloatUtil.makePerspective(mat4f, 0, true, 45 * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0);


        Draw.glu.gluLookAt(camPos.x - camFwd.x, camPos.y - camFwd.y, camPos.z - camFwd.z,
                camPos.x, camPos.y, camPos.z,
                camUp.x, camUp.y, camUp.z);


        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();


    }

    public final Off onUpdate(Consumer<JoglWindow> c) {
        return video.onUpdate(c);
    }

    public final Off onUpdate(Animated c) {
        return video.onUpdate(c);
    }

    public final Off onUpdate(Runnable c) {
        return video.onUpdate(c);
    }

    public final GL2 gl() {
        return video.gl;
    }

    public void delete() {
        video.off();
    }


    private class MyJoglWindow extends JoglWindow {

        MyJoglWindow() {
            super();
        }

        @Override
        protected void init(GL2 gl) {

            initInput();

            JoglDisplay.this.init(gl);

            JoglDisplay.this.init();
        }

        @Override
        protected final void render(float dtS) {

            JoglDisplay.this.render(dtS, gl);
        }

        @Override
        protected void update() {

            JoglDisplay.this.update();

        }

        @Override
        public final void reshape(GLAutoDrawable drawable,
                                  int xstart,
                                  int ystart,
                                  int width,
                                  int height) {

        }


    }

    public void init(GL2 gl) {
        gl.glEnable(GL_STENCIL);

        gl.glEnable(GL_LINE_SMOOTH);
        gl.glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        gl.glEnable(GL_POLYGON_SMOOTH);
        gl.glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);

        gl.glEnable(GL_MULTISAMPLE);


        gl.glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);

        gl.glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);


        gl.glColorMaterial(GL_FRONT_AND_BACK,
                GL_AMBIENT_AND_DIFFUSE
        );
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glEnable(GL_NORMALIZE);


        initDepth(gl);


        initBlend(gl);


        initLighting(gl);




    }

    public void render(float dtS, GL2 gl) {
        clear(gl);

        renderVolume(dtS, gl);

        renderOrthos(dtS);
    }

    protected void update() {


    }


    /** for misc init tasks */
    protected void init() {


    }
}
