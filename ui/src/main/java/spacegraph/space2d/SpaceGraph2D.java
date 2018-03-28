//package spacegraph.render;
//
//import com.jogamp.newt.event.MouseEvent;
//import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
//import jcog.Util;
//import spacegraph.AbstractSpace;
//import spacegraph.SimpleSpatial;
//import spacegraph.SpaceGraph;
//import spacegraph.input.KeyXYZ;
//import spacegraph.input.OrbMouse;
//import spacegraph.math.v3;
//
//import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
//
///**
// * 2D ortho view of physics space
// */
//public class SpaceGraph2D<X> extends SpaceGraph<X> {
//
//
//    private OrbMouse orb;
//
//
//    public SpaceGraph2D() {
//        super();
//    }
//
//    public SpaceGraph2D(AbstractSpace<X, ?>... cc) {
//        super(cc);
//    }
//
//    public SpaceGraph2D(SimpleSpatial... x) {
//        super(x);
//    }
//
//
//    float camWidth = 1, camHeight = 1;
//
//
//    protected void ortho(float cx, float cy, float scale) {
//        int w = getWidth();
//        int h = getHeight();
//        gl.glViewport(0, 0, w, h);
//        gl.glMatrixMode(GL_PROJECTION);
//        gl.glLoadIdentity();
//
//
//        float aspect = h / ((float) w);
//
//        this.zNear = -scale;
//        this.zFar = scale;
//
//        //gl.glOrtho(-2.0, 2.0, -2.0, 2.0, -1.5, 1.5);
//        camWidth = scale;
//        camHeight = aspect * scale;
//        gl.glOrtho(cx - camWidth / 2f, cx + camWidth / 2f, cy - camHeight / 2f, cy + camHeight / 2f,
//                zNear, zFar);
//
////        // switch to projection mode
////        gl.glMatrixMode(gl.GL_PROJECTION);
////        // save previous matrix which contains the
////        //settings for the perspective projection
////        // gl.glPushMatrix();
////        // reset matrix
////        gl.glLoadIdentity();
////        // set a 2D orthographic projection
////        glu.gluOrtho2D(0f, screenWidth, 0f, screenHeight);
////        // invert the y axis, down is positive
////        //gl.glScalef(1f, -1f, 1f);
////        // mover the origin from the bottom left corner
////        // to the upper left corner
////        //gl.glTranslatef(0f, -screenHeight, 0f);
//        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
//        //gl.glLoadIdentity();
//
//        //gl.glDisable(GL2.GL_DEPTH_TEST);
//
//
//        //gl.glTranslatef(cx + w/2f, cy + h/2f, 0);
//
////        float s = Math.min(w, h);
////        gl.glScalef(scale*s,scale*s,1f);
//    }
//
//    boolean ortho;
//
//    float minWidth = 1f;
//    float maxWidth = 1000;
//
//    @Override
//    public void updateCamera() {
//        float scale = camPos.z = Math.max(minWidth, camPos.z);
//
//        if (ortho) {
//            //tan(A) = opposite/adjacent
//            //tan(focus/2) = scale / Z
//            //scale = z * tan(focus/2)
//            ortho(camPos.x, camPos.y, scale);
//        } else {
//            super.updateCamera();
//
//            float aspect = getHeight() / ((float) getWidth());
//
//
//            //gl.glOrtho(-2.0, 2.0, -2.0, 2.0, -1.5, 1.5);
//            camWidth = scale * 2f;
//            camHeight = aspect * scale * 2f;
//        }
//    }
//
////    @Override
////    protected void initLighting() {
////        //none
////    }
//
//    @Override
//    protected void initInput() {
//
//        addKeyListener(new KeyXYZ(this));
//        addMouseListener(orb = new OrbMouse(this) {
//
//
//
//
//            @Override
//            public void mouseWheelMoved(MouseEvent e) {
//                float[] rotation = e.getRotation();
//                //System.out.println(Arrays.toString(rotation));
//                camera(camPos, Util.clamp(0.5f*camWidth *  (1f + -0.45f * rotation[1]), minWidth, maxWidth));
//            }
//
//        });
//
////
////        add(new AbstractSpatial(orb + " view" /* HACK */) {
////
////            @Override
////            public void forEachBody(Consumer c) {
////
////            }
////
////            @Override
////            public void renderAbsolute(GL2 gl) {
////                ClosestRay pickRay = orb.pickRay;
////                if (pickRay!=null) {
////                    gl.glLineWidth(10);
////                    gl.glColor4f(0f, 0.25f, 1f, 0.5f);
////                    Draw.line(gl, pickRay.rayFromWorld, pickRay.rayToWorld);
////                    //System.out.println(pickRay.rayFromWorld + " " + pickRay.rayToWorld);
////                }
////                if(orb.pickedSpatial!=null) {
////                    gl.glLineWidth(20);
////                    gl.glColor4f(1f, 0.5f, 0.5f, 0.5f);
////                    Draw.line(gl, orb.hitPoint, camPos);
////                }
////            }
////
////        });
//
//    }
//
//    @Override
//    public void camera(v3 target, float radius) {
//        camPos.set(target.x, target.y, radius);
//
//    }
//
////    public v3 rayTo(int px, int py) {
////        float height = getHeight();
////        return rayTo( px / ((float) getWidth()), (height - py) / height);
////    }
//
//
////    @Override
////    public v3 rayTo(float x, float y, float depth) {
////        return v(
////                camWidth * (-0.5f + x) * 2f,
////                camWidth / aspect * (-0.5f + y) * 2f,
////                0);
////    }
//
//
//
////    public void clear(float opacity) {
////
////        if (opacity < 1f) {
////            //TODO use gl.clear faster than rendering this quad
////            ortho();
////            gl.glColor4f(0, 0, 0, opacity);
////            gl.glRectf(0, 0, getWidth(), getHeight());
////        } else {
////            gl.glClearColor(0f, 0f, 0f, 1f);
////            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
////        }
////    }
//
//
//}
