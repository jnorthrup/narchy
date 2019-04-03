package spacegraph.video;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import jcog.event.Off;
import jcog.math.v3;
import spacegraph.input.key.KeyXYZ;
import spacegraph.input.key.WindowKeyControls;
import spacegraph.space2d.ReSurface;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

abstract public class JoglSpace {

    /**
     * the hardware input/output implementation
     */
    public final JoglWindow display;

    public final v3 camPos, camFwd, camUp;
    private final float[] mat4f = new float[16];

    private final float cameraSpeed = 100f, cameraRotateSpeed = cameraSpeed;



    protected final Queue<Runnable> pending = new ConcurrentLinkedQueue();

    public float top, bottom, left, right, aspect, tanFovV;

    public float zNear = 0.5f, zFar = 1200;

    protected int debug;


    public JoglSpace() {
        display = new MyJoglWindow();

        display.onUpdate((Animated) (camPos = new AnimVector3f(0, 0, 5, cameraSpeed)));
        display.onUpdate((Animated) (camFwd = new AnimVector3f(0, 0, -1, cameraRotateSpeed)));
        display.onUpdate((Animated) (camUp = new AnimVector3f(0, 1, 0, cameraRotateSpeed)));

    }

    public void later(Runnable r) { pending.add(r); }

    private void flush() {
        pending.removeIf((x) -> {
            x.run();
            return true;
        });
    }

    protected void initDepth(GL2 gl) {
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

        display.addKeyListener(new WindowKeyControls(JoglSpace.this));

        display.addKeyListener(new KeyXYZ(JoglSpace.this));

    }

    public void camera(v3 target, float radius) {
        v3 fwd = v();
        fwd.sub(target, camPos);
        fwd.normalize();
        camFwd.set(fwd);

        fwd.scaled(radius * 1.25f + zNear * 1.25f);
        camPos.sub(target, fwd);

    }

    protected void renderVolume(float dtS) {

    }


    /* render context */
    public final ReSurface rendering = new ReSurface();


    protected void renderOrthos(float dtS) {

    }


    private void clear() {
        //view.clearMotionBlur(0.5f);
        display.clearComplete();

    }


    private void updateCamera(float dtS) {
        perspective();
    }

    private void perspective() {


        if (display.gl == null)
            return;

        display.gl.glMatrixMode(GL_PROJECTION);
        display.gl.glLoadIdentity();


        float aspect = ((float) display.window.getWidth()) / display.window.getHeight();

        JoglSpace.this.aspect = aspect;

        tanFovV = (float) Math.tan(45 * FloatUtil.PI / 180.0f / 2f);

        top = tanFovV * zNear;
        right = aspect * top;
        bottom = -top;
        left = -right;


        display.gl.glMultMatrixf(FloatUtil.makePerspective(mat4f, 0, true, 45 * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0);


        Draw.glu.gluLookAt(camPos.x - camFwd.x, camPos.y - camFwd.y, camPos.z - camFwd.z,
                camPos.x, camPos.y, camPos.z,
                camUp.x, camUp.y, camUp.z);


        display.gl.glMatrixMode(GL_MODELVIEW);
        display.gl.glLoadIdentity();


    }

    public final Off onUpdate(Consumer<JoglWindow> c) {
        return display.onUpdate(c);
    }

    public final Off onUpdate(Animated c) {
        return display.onUpdate(c);
    }

    public final Off onUpdate(Runnable c) {
        return display.onUpdate(c);
    }

    public final GL2 gl() {
        return display.gl;
    }



    private class MyJoglWindow extends JoglWindow {

        public MyJoglWindow() {
            super();
        }

        @Override
        protected void init(GL2 gl) {

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

            initInput();

            JoglSpace.this.init();

            flush();
            onUpdate((Consumer) (w -> flush()));
        }

        @Override
        protected final void render(float dtS) {

            clear();

            updateCamera(dtS);

            renderVolume(dtS);

            renderOrthos(dtS);
        }

        @Override
        protected void update() {
            int w = display.window.getWidth(), h = display.window.getHeight();

            rendering.restart(w, h);
            JoglSpace.this.update(rendering);
//            for (Surface/*Root*/ l : layers.children()) {
//                if (l instanceof Ortho) {
//                    ((Ortho) l).compile(rendering);
//                }
//            }
        }

        @Override
        public final void reshape(GLAutoDrawable drawable,
                                  int xstart,
                                  int ystart,
                                  int width,
                                  int height) {

        }


    }

    protected void update(ReSurface rendering) {

    }

    /** for misc init tasks */
    protected void init() {


    }
}
