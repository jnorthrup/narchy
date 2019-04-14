package spacegraph.video;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Off;
import jcog.math.v3;
import spacegraph.input.key.KeyXYZ;
import spacegraph.input.key.WindowKeyControls;
import spacegraph.space2d.ReSurface;
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
abstract public class JoglDisplay {

    /**
     * the hardware input/output implementation
     */
    public final JoglWindow video;

    public final v3 camPos, camFwd, camUp;
    private final float[] mat4f = new float[16];

    private final float cameraSpeed = 100f, cameraRotateSpeed = cameraSpeed;



    private final MetalConcurrentQueue<Runnable> pending = new MetalConcurrentQueue<>(1024);

    public float top;
    public float bottom;
    private float left;
    private float right;
    private float aspect;
    private float tanFovV;

    public float zNear = 0.5f, zFar = 1200;

    protected int debug;


    public JoglDisplay() {
        video = new MyJoglWindow();

        video.onUpdate((Animated) (camPos = new AnimVector3f(0, 0, 5, cameraSpeed)));
        video.onUpdate((Animated) (camFwd = new AnimVector3f(0, 0, -1, cameraRotateSpeed)));
        video.onUpdate((Animated) (camUp = new AnimVector3f(0, 1, 0, cameraRotateSpeed)));

    }

    public void later(Runnable r) { pending.add(r); }

    private void flush() {
        pending.clear(Runnable::run);
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

    protected void renderVolume(float dtS) {

    }


    /* render context */
    public final ReSurface rendering = new ReSurface();


    protected void renderOrthos(float dtS) {

    }


    private void clear() {
        //view.clearMotionBlur(0.5f);
        video.clearComplete();

    }


    protected void updateCamera(float dtS) {
        perspective();
    }

    private void perspective() {


        if (video.gl == null)
            return;

        video.gl.glMatrixMode(GL_PROJECTION);
        video.gl.glLoadIdentity();


        float aspect = ((float) video.getWidth()) / video.getHeight();

        JoglDisplay.this.aspect = aspect;

        tanFovV = (float) Math.tan(45 * FloatUtil.PI / 180.0f / 2f);

        top = tanFovV * zNear;
        right = aspect * top;
        bottom = -top;
        left = -right;


        video.gl.glMultMatrixf(FloatUtil.makePerspective(mat4f, 0, true, 45 * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0);


        Draw.glu.gluLookAt(camPos.x - camFwd.x, camPos.y - camFwd.y, camPos.z - camFwd.z,
                camPos.x, camPos.y, camPos.z,
                camUp.x, camUp.y, camUp.z);


        video.gl.glMatrixMode(GL_MODELVIEW);
        video.gl.glLoadIdentity();


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

            JoglDisplay.this.init();

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
//            int w = video.window.getWidth(), h = video.window.getHeight();

            rendering.restart(getWidth(), getHeight());
            JoglDisplay.this.update(rendering);
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
