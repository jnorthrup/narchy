package spacegraph.video;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import jcog.event.Off;
import jcog.math.v3;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.SpaceGraph;
import spacegraph.input.key.KeyXYZ;
import spacegraph.input.key.WindowKeyControls;
import spacegraph.util.animate.Animated;
import spacegraph.util.animate.v3Anim;

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

    /** field of view, in degrees */
    public float fov = 45;

    /**
     * the hardware input/output implementation
     */
    public final JoglWindow video;

    public final v3Anim camPos, camFwd, camUp;

    private final float cameraSpeed = 100f, cameraRotateSpeed = cameraSpeed;


    public float zNear = 0.5f, zFar = 1000;

    protected int debug;
    public double top, bottom, left, right;

    public JoglDisplay() {
        video = new MyJoglWindow();

        video.onUpdate(camPos = new v3Anim(0, 0, 5, cameraSpeed));
        video.onUpdate(camFwd = new v3Anim(0, 0, -1, cameraRotateSpeed));
        video.onUpdate(camUp = new v3Anim(0, 1, 0, cameraRotateSpeed));
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

    @Deprecated public void camera(v3 target, float radius) {
        v3 fwd = v();
        fwd.sub(target, camPos);
        fwd.normalize();
        camFwd.set(fwd);

        fwd.scaled(radius * 1.25f + zNear * 1.25f);
        camPos.sub(target, fwd);

    }

    protected void renderVolume(float dtS, GL2 gl, float aspect) {

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


//    public void updateCamera(GL2 gl) {
//        int glutScreenWidth = video.getWidth(), glutScreenHeight = video.getHeight();
//
//        updateCamera(gl, ((float) glutScreenWidth)/glutScreenHeight);
//    }

    public void updateCamera(GL2 gl, float aspect) {
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();



        top = zNear * Math.tan(fov * Math.PI / 360.0);
        bottom = -top;

        left = bottom * aspect;
        right = top * aspect;

//            xmin += -(2 * gl_state.camera_separation) / zNear;
//            xmax += -(2 * gl_state.camera_separation) / zNear;

        gl.glFrustum(left, right, bottom, top, zNear, zFar);

        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
        Draw.glu.gluLookAt(
            camPos.x, camPos.y, camPos.z,
            camPos.x + camFwd.x, camPos.y + camFwd.y, camPos.z + camFwd.z,
            camUp.x, camUp.y, camUp.z);

    }



//    public v3 rayTo(float x, float y) {
//
////        float tanFov = (float) ((top - bottom) * 0.5f / zNear);
////        float fov = 2f * (float) Math.atan(tanFov);
//
//        v3 rayFrom = camPos.clone();
//        v3 rayForward = camFwd.clone().normalized(zFar);
//
//
////        v3 rightOffset = new v3();
//        v3 v = camUp.clone();
//
//        v3 h = new v3();
//        // TODO: check: hor = rayForward.cross(vertical);
//        h.cross(rayForward, v);
//        h.normalize();
//
//        // TODO: check: vertical = hor.cross(rayForward);
//        v.cross(h, rayForward);
//        v.normalize();
//
//        float tanfov = (float) Math.tan(0.5f * fov);
//
//        float ph = (float)(top-bottom);
//        float pw = (float)(right-left);
//        x = (float) Util.lerp(x, left, right);
//        y = (float) Util.lerp(y, bottom, top);
//
//        float aspect = ph / pw;
//
//        h.scale(2f * zFar * tanfov);
//        v.scale(2f * zFar * tanfov);
//
//        if (aspect < 1f) {
//            h.scale(1f / aspect);
//        } else {
//            v.scale(aspect);
//        }
//
//        v3 rayToCenter = new v3();
//        rayToCenter.add(rayFrom, rayForward);
////        v3 dHor = new v3(h);
////        dHor.scale(1f / pw);
////        v3 dVert = new v3(v);
////        dVert.scale(1f / ph);
//
//        v3 tmp1 = new v3();
//        v3 tmp2 = new v3();
//        tmp1.scale(0.5f, h);
//        tmp2.scale(0.5f, v);
//
//        v3 rayTo = new v3();
//        rayTo.sub(rayToCenter, tmp1);
//        rayTo.add(tmp2);
//
//        tmp1.scale(x, h);
//        tmp2.scale(y, v);
//
//        rayTo.add(tmp1);
//        rayTo.add(tmp2);
//        System.out.println(x + "," + y + " -> " + rayTo);
//        return rayTo;
//    }

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

    public void renderVolumeEmbedded(float dtS, GL2 gl, RectFloat bounds) {
        video.dtS = dtS;
        video.onUpdate.emit(video); //HACK
        renderVolume(dtS, gl, bounds.w/bounds.h);
    }


    private class MyJoglWindow extends JoglWindow {

        MyJoglWindow() {
            super();
        }

        @Override
        protected void init(GL2 gl) {

            JoglDisplay.this.enable(gl);

            initInput();
        }

        @Override
        protected final void render(float dtS) {

            JoglDisplay.this.render(dtS, gl);
        }


        @Override
        public final void reshape(GLAutoDrawable drawable,
                                  int xstart,
                                  int ystart,
                                  int width,
                                  int height) {

        }


    }

    public void enable(GL2 gl) {
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

        renderVolume(dtS, gl, ((float)video.getWidth())/video.getHeight());

        renderOrthos(dtS);
    }



}
