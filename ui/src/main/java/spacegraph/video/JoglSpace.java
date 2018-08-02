package spacegraph.video;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.FloatUtil;
import jcog.data.list.FastCoWList;
import spacegraph.input.key.KeyXYZ;
import spacegraph.input.key.WindowKeyControls;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
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
    private final float[] mat4f = new float[16];

    private final List<Surface> layers = new FastCoWList(Surface[]::new);
    private final Queue<Runnable> pending = new ConcurrentLinkedQueue();
    private final float cameraSpeed = 100f;
    private final float cameraRotateSpeed = cameraSpeed;
    public float top;
    public float bottom;
    public float zNear = 0.5f;
    public float zFar = 1200;

    protected int debug;
    private float aspect;
    private float tanFovV;
    private float left;
    private float right;


    protected JoglSpace() {
        super();

        onUpdate((Animated) (camPos = new AnimVector3f(0, 0, 5, cameraSpeed)));
        onUpdate((Animated) (camFwd = new AnimVector3f(0, 0, -1, cameraRotateSpeed)));
        onUpdate((Animated) (camUp = new AnimVector3f(0, 1, 0, cameraRotateSpeed)));

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

    private void renderOrthos(int dtMS) {

        int facialsSize = layers.size();
        if (facialsSize > 0) {

            GL2 gl = this.gl;


            int w = window.getWidth();
            int h = window.getHeight();
            gl.glViewport(0, 0, w, h);
            gl.glMatrixMode(GL_PROJECTION);
            gl.glLoadIdentity();


            gl.glOrtho(0, w, 0, h, -1.5, 1.5);


            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);


            gl.glDisable(GL2.GL_DEPTH_TEST);

            SurfaceRender r = new SurfaceRender(w, h, dtMS);
            for (Surface l : layers) {
                if (l.visible()) {
                    if (l instanceof Ortho) {
                        Ortho o = (Ortho) l;
                        r.setScale(o.cam, o.scale);
                    }


                    l.render(gl, r);
                }
            }

            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }


    private void clear() {
        //clearMotionBlur(0.5f);
        clearComplete();

    }

    protected void clearComplete() {
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void clearMotionBlur(float rate /* TODO */) {


        gl.glAccum(GL2.GL_LOAD, 0.5f);

        gl.glAccum(GL2.GL_ACCUM, 0.5f);


        gl.glAccum(GL2.GL_RETURN, rate);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);


    }

    private void updateCamera(int dtMS) {
        perspective();
    }

    private void perspective() {


        if (gl == null)
            return;

        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();


        float aspect = ((float) window.getWidth()) / window.getHeight();

        this.aspect = aspect;

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


    @Override
    public final void reshape(GLAutoDrawable drawable,
                              int xstart,
                              int ystart,
                              int width,
                              int height) {

    }

    @Override
    public final Iterator<Spatial<X>> iterator() {
        throw new UnsupportedOperationException("use forEach");
    }


}
