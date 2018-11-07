package spacegraph.video;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.FloatUtil;
import jcog.data.list.FastCoWList;
import jcog.event.Off;
import spacegraph.input.key.KeyXYZ;
import spacegraph.input.key.WindowKeyControls;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.hud.Ortho;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;
import spacegraph.util.math.v3;

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

abstract public class JoglSpace {

    /**
     * the hardware input/output implementation
     */
    public final JoglWindow io;

    public final v3 camPos, camFwd, camUp;
    private final float cameraSpeed = 100f, cameraRotateSpeed = cameraSpeed;

    private final float[] mat4f = new float[16];

    private final FastCoWList<Surface> layers = new FastCoWList(Surface[]::new);
    private final Queue<Runnable> pending = new ConcurrentLinkedQueue();

    public float top, bottom, left, right, aspect, tanFovV;

    public float zNear = 0.5f, zFar = 1200;

    protected int debug;


    public JoglSpace() {
        io = new MyJoglWindow();

        io.onUpdate((Animated) (camPos = new AnimVector3f(0, 0, 5, cameraSpeed)));
        io.onUpdate((Animated) (camFwd = new AnimVector3f(0, 0, -1, cameraRotateSpeed)));
        io.onUpdate((Animated) (camUp = new AnimVector3f(0, 1, 0, cameraRotateSpeed)));
    }

    /**
     * dummy root node for layers to have a non-null parent
     */
    final Surface root = new EmptySurface();

    public JoglSpace add(Surface layer) {

        pending.add(() -> {
            JoglSpace.this.layers.add(layer);
            if (layer instanceof Ortho)
                ((Ortho) layer).start(JoglSpace.this);
            else
                layer.start(root);
        });
        return JoglSpace.this;
    }

    public boolean remove(Surface layer) {
        if (JoglSpace.this.layers.remove(layer)) {
            layer.stop();
            return true;
        }
        return false;
    }

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
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE);
        gl.glBlendEquationSeparate(GL_FUNC_ADD, GL_MAX);


    }

    protected void initLighting(GL2 gl) {


    }

    protected void initInput() {

        io.addKeyListener(new WindowKeyControls(JoglSpace.this));

        io.addKeyListener(new KeyXYZ(JoglSpace.this));

    }

    public void camera(v3 target, float radius) {
        v3 fwd = v();

        fwd.sub(target, camPos);
        fwd.normalize();
        camFwd.set(fwd);

        fwd.scale(radius * 1.25f + zNear * 1.25f);
        camPos.sub(target, fwd);

    }

    protected void renderVolume(int dtMS) {

    }

    /* render context */
    final SurfaceRender rendering = new SurfaceRender();

    private void renderOrthos(int dtMS) {

        int facialsSize = layers.size();
        if (facialsSize > 0) {

            GL2 gl = io.gl;


            int w = io.window.getWidth();
            int h = io.window.getHeight();
            gl.glViewport(0, 0, w, h);
            gl.glMatrixMode(GL_PROJECTION);
            gl.glLoadIdentity();


            gl.glOrtho(0, w, 0, h, -1.5, 1.5);


            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);


            gl.glDisable(GL2.GL_DEPTH_TEST);


            rendering.start(w, h, dtMS);
            for (Surface l : layers) {
                if (l.visible()) {
                    if (l instanceof Ortho) {
                        Ortho o = (Ortho) l;
                        rendering.set(o.cam, o.scale);
                    }

                    rendering.layer(l, gl);
                }
            }

            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }


    private void clear() {
        //view.clearMotionBlur(0.5f);
        io.clearComplete();

    }


    private void updateCamera(int dtMS) {
        perspective();
    }

    private void perspective() {


        if (io.gl == null)
            return;

        io.gl.glMatrixMode(GL_PROJECTION);
        io.gl.glLoadIdentity();


        float aspect = ((float) io.window.getWidth()) / io.window.getHeight();

        JoglSpace.this.aspect = aspect;

        tanFovV = (float) Math.tan(45 * FloatUtil.PI / 180.0f / 2f);

        top = tanFovV * zNear;
        right = aspect * top;
        bottom = -top;
        left = -right;


        io.gl.glMultMatrixf(FloatUtil.makePerspective(mat4f, 0, true, 45 * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0);


        Draw.glu.gluLookAt(camPos.x - camFwd.x, camPos.y - camFwd.y, camPos.z - camFwd.z,
                camPos.x, camPos.y, camPos.z,
                camUp.x, camUp.y, camUp.z);


        io.gl.glMatrixMode(GL_MODELVIEW);
        io.gl.glLoadIdentity();


    }

    public final Off onUpdate(Consumer<JoglWindow> c) {
        return io.onUpdate(c);
    }

    public final Off onUpdate(Animated c) {
        return io.onUpdate(c);
    }

    public final Off onUpdate(Runnable c) {
        return io.onUpdate(c);
    }


    private class MyJoglWindow extends JoglWindow {
        public MyJoglWindow() {
            super();
        }

//        @Override
//        public void windowDestroyed(WindowEvent windowEvent) {
//            super.windowDestroyed(windowEvent);
//            layers.clear();
//            onUpdate.clear();
//        }

        @Override
        protected void init(GL2 gl) {

            gl.glEnable(GL_STENCIL);


            gl.glEnable(GL_LINE_SMOOTH);

            gl.glEnable(GL2.GL_MULTISAMPLE);


            gl.glHint(GL_POLYGON_SMOOTH_HINT,
                    GL_NICEST);

            gl.glHint(GL_LINE_SMOOTH_HINT,
                    GL_NICEST);

            gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT,
                    GL_NICEST);


            gl.glColorMaterial(GL_FRONT_AND_BACK,
                    GL_AMBIENT_AND_DIFFUSE

            );
            gl.glEnable(GL_COLOR_MATERIAL);
            gl.glEnable(GL_NORMALIZE);


            initDepth(gl);


            initBlend(gl);


            initLighting(gl);

            initInput();

            flush();
            onUpdate((Consumer) (w -> flush()));
        }

        @Override
        protected final void render(int dtMS) {

            clear();

            updateCamera(dtMS);

            renderVolume(dtMS);

            renderOrthos(dtMS);
        }

        @Override
        public final void reshape(GLAutoDrawable drawable,
                                  int xstart,
                                  int ystart,
                                  int width,
                                  int height) {

        }


    }
}
