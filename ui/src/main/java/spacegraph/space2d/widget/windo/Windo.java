package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMove;
import spacegraph.input.finger.FingerResize;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.Surface;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

import static spacegraph.space2d.widget.windo.Windo.DragEdit.MOVE;

/**
 * draggable panel
 */
public class Windo extends Widget {

    public final static float resizeBorder = 0.1f;
    public Fingering dragMode = null;
    public DragEdit potentialDragMode = null;
    protected boolean hover;

    public Windo() {
        super();


//        clipTouchBounds = false;
//        Surface menubar = //row(new PushButton("x"),
//                new Label(title)
//        ;
//
//        PushButton upButton = new PushButton("^");
//        upButton.scale(0.25f,0.25f).move(0.5f,0.75f);
//
//        PushButton leftButton = new PushButton("<", (p) -> Windo.this.move(-1f, 0f));
//        leftButton.scale(0.25f,0.25f).move(0f, 0.5f);
//
//        PushButton rightButton = new PushButton(">", (p) -> Windo.this.move(1f, 0));
//        rightButton.scale(0.25f,0.25f).move(0.75f, 0.5f);
//
//        set(
//                upButton, leftButton, rightButton,
//                new VSplit(menubar, content, 0.1f)
//                );
    }

    @Override
    public Surface onTouch(Finger finger, short[] buttons) {

        Surface other = null;
        if (dragMode==null) {
            Surface c = super.onTouch(finger, buttons);
            if (!fingerable() || fingeringWindow(c)) {
                //            this.dragMode = null;
                //            this.potentialDragMode = null;
                other = c; //something else or a child inside of the content
            }

        } else {
            other = this;
        }

//        if (finger == null)
//            return null;

        v2 f;
        if (other!=null && other!=this) {
            this.dragMode = null;
            this.potentialDragMode = null;
            this.hover = false;
            return other;
        } else if (finger == null || !fingeringBounds(finger)) {

            if (finger == null)
                return (dragMode!=null ? this : null);

            this.dragMode = null;
            this.potentialDragMode = null;
            this.hover = false;
            return null;
        } else {

            DragEdit potentialDragMode = null;

            //if (moveable()) System.out.println(bounds + "\thit=" + finger.hit + "\thitGlobal=" + finger.hitGlobal);
            v2 hitPoint = windowHitPointRel(finger);

            this.hover = true;

            //if (dragMode == null && !finger.prevButtonDown[ZoomOrtho.PAN_BUTTON] /* && hitPoint.x >= 0 && hitPoint.y >= 0 && hitPoint.x <= 1f && hitPoint.y <= 1f*/) {
            {

                if (potentialDragMode == null && hitPoint.x >= 0.5f - resizeBorder / 2f && hitPoint.x <= 0.5f + resizeBorder / 2) {
                    if (potentialDragMode == null && hitPoint.y <= resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_S;
                    }
                    if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_N;
                    }
                }

                if (potentialDragMode == null && hitPoint.y >= 0.5f - resizeBorder / 2f && hitPoint.y <= 0.5f + resizeBorder / 2) {
                    if (potentialDragMode == null && hitPoint.x <= resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_W;
                    }
                    if (potentialDragMode == null && hitPoint.x >= 1f - resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_E;
                    }
                }

                if (potentialDragMode == null && hitPoint.x <= resizeBorder) {
                    if (potentialDragMode == null && hitPoint.y <= resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_SW;
                    }
                    if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_NW;
                    }
                }

                if (potentialDragMode == null && hitPoint.x >= 1f - resizeBorder) {

                    if (potentialDragMode == null && hitPoint.y <= resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_SE;
                    }
                    if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_NE;
                    }
                }


                if (potentialDragMode == null)
                    potentialDragMode = MOVE;
            }


            if (!fingerable(potentialDragMode))
                potentialDragMode = null;

            this.potentialDragMode = potentialDragMode;

            if (buttons != null && buttons.length > 0 && buttons[ZoomOrtho.PAN_BUTTON] == 1) {
                //actual drag mode enabled
                Fingering d = fingering(potentialDragMode);
                if (d != null && finger.tryFingering(d)) {
                    this.dragMode = d;
                } else {
                    this.dragMode = null;
                }
            }


            return this;
        }


    }

    public boolean fingeringBounds(Finger finger) {
        v2 f;
        return (f = finger.posGlobal)!= null && bounds.contains(f.x, f.y);
    }

    public v2 windowHitPointRel(Finger finger) {
        return finger.relativePos(this);
    }

    public boolean fingeringWindow(Surface childFingered) {
        return childFingered != this && childFingered != null && childFingered != this.get();
    }

    protected Fingering fingering(DragEdit mode) {

        switch (mode) {
            case MOVE:
                return fingeringMove();

            default:
                return new FingerResize(this, mode);
        }

    }

    protected Fingering fingeringMove() {
        return new FingerMove(this);
    }

    public boolean fingerable() {
        return true;
    }

    public boolean fingerable(DragEdit d) {
        return true;
    }

    public boolean opaque() {
        return true;
    }

    protected void prepaint(GL2 gl) {

    }

    protected void postpaint(GL2 gl) {

    }

    /**
     * gets main content
     */
    public Surface get() {
        return content.children()[0];
        //return ((UnitContainer) content.children()[0]).the;
        //return content;
    }

    @Override
    protected void paintWidget(GL2 gl, RectFloat2D bounds) {
        paintBack(gl);


        prepaint(gl);


//        gl.glColor4f(0.8f, 0.6f, 0f, 0.25f);

//        int borderThick = 8;
//        gl.glLineWidth(borderThick);
//        Draw.line(gl, 0, 0, W, 0);
//        Draw.line(gl, 0, 0, 0, H);
//        Draw.line(gl, W, 0, W, H);
//        Draw.line(gl, 0, H, W, H);


//            gl.glLineWidth(2);
//            gl.glColor3f(0.8f, 0.5f, 0);
//            Draw.text(gl, str(notifications.top().get()), 32, smx + cw, smy + ch, 0);
//            gl.glColor3f(0.4f, 0f, 0.8f);
//            Draw.text(gl, wmx + "," + wmy, 32, smx - cw, smy - ch, 0);

        postpaint(gl);


    }

    protected void paintBack(GL2 gl) {
        if (hover && opaque() && potentialDragMode != null) {
            switch (potentialDragMode) {
                case RESIZE_N:
                case RESIZE_S:
                case RESIZE_W:
                case RESIZE_E:
                    gl.glColor3f(0.15f, 0f, 0.2f);
                    break;
                case RESIZE_NE:
                case RESIZE_SW:
                case RESIZE_NW:
                case RESIZE_SE:
                    gl.glColor3f(0.15f, 0.2f, 0f);
                    break;
                default:
                    //gl.glColor3f(0.3f, 0.3f, 0.3f);
                    //break;
                    return;

            }
            Draw.rect(gl, x(), y(), w(), h());
        }
    }

    public enum DragEdit {
        MOVE,
        RESIZE_N, RESIZE_E, RESIZE_S, RESIZE_W,
        RESIZE_NW,
        RESIZE_SW,
        RESIZE_NE,
        RESIZE_SE
    }


//    public static class Port extends Windo {
//        public final String id;
//
//        public final v2 posRel;
//        public final v2 sizeRel;
//        private final Windo win;
//
//        Port(String id, Windo win) {
//            super();
//            this.id = id;
//            this.win = win;
//
//            //Surface content = win.wall().newCurface(id);
//            //children(content);
//
//            //set(new Scale(new PushButton("?"), 0.9f));
//            this.posRel = new v2(Float.NEGATIVE_INFINITY, 0);
//            this.sizeRel = new v2(0.1f, 0.2f);
//        }
//
//        @Override
//        public void doLayout(int dtMS) {
//            float W = win.w();
//            float H = win.h();
//            {
//                float x1, y1, x2, y2;
//                float w = sizeRel.x * W;
//                float h = sizeRel.y * H;
//                if (posRel.x == Float.NEGATIVE_INFINITY) {
//                    //glued to left
//                    float y = Util.lerp((posRel.y) / 2f + 0.5f, win.bounds.y, win.bounds.bottom());
//                    x1 = win.x() - w;
//                    y1 = y - h / 2;
//                    x2 = win.x();
//                    y2 = y + h / 2;
//                    pos(x1, y1, x2, y2);
//                } else if (posRel.x == Float.POSITIVE_INFINITY) {
//                    //glued to right
//                } else {
//                    //TODO
//                    //etc
//                }
//            }
//
//            super.doLayout(dtMS);
//        }
//
//        @Override
//        protected void paintBack(GL2 gl) {
//            gl.glColor3f(1f, 0, 1f);
//            Draw.rect(gl, x(), y(), w(), h());
//        }
//
////        @Override
////        public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
////            if (hitPoint != null && hitPoint.inUnit())
////                return super.onTouch(finger, hitPoint, buttons);
////            return null;
////        }
//
//        @Override
//        protected Fingering fingering(DragEdit mode) {
//            if (mode == MOVE) {
//                return new FingerMove(this, false, true);
//            }
//            return super.fingering(mode);
//        }
//    }

//    protected Wall wall() {
//        return ((Wall) parent);
//    }

//    public Port addPort(String x) {
//        Wall w = wall();
//        {
////            if (ports == null)
////                ports = new LinkedHashMap<>();
////            return ports.computeIfAbsent(x, i -> {
//            Port p = new Port(x, this);
//            w.add(/*0, */ p);
//            return p;
////            });
//        }
//    }

//
//    public static void main(String[] args) {
////        Wall d =
////                //new Wall();
////                new PhyWall();
//        {
////            boolean init = true;
////            int shaderprogram;
////            String vsrc;
////
////            {
////                try {
////                    vsrc = new StringBuilder(new String(GLSL.class.getClassLoader().getResourceAsStream(
////                            "glsl/grid.glsl"
////                            //"glsl/16seg.glsl"
////                            //"glsl/metablob.glsl"
////                    ).readAllBytes())).toString();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
////            }
////
////            @Override
////            protected void paint(GL2 gl) {
////                if (init) {
//////                    int v = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
////                    int f = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
////
////                    {
////
////                        gl.glShaderSource(f, 1, new String[]{vsrc}, new int[]{vsrc.length()}, 0);
////                        gl.glCompileShader(f);
////                    }
//////                        {
//////                            String vsrc = new StringBuilder(new String(GLSL.class.getClassLoader().getResourceAsStream(
//////                                    "glsl/16seg.glsl"
//////                            ).readAllBytes())).toString();
//////                            gl.glShaderSource(f, 1, new String[]{vsrc}, new int[]{vsrc.length()}, 0);
//////                            gl.glCompileShader(f);
//////                        }
////
////                    shaderprogram = gl.glCreateProgram();
////                    //gl.glAttachShader(shaderprogram, v);
////                    gl.glAttachShader(shaderprogram, f);
////                    gl.glLinkProgram(shaderprogram);
////                    gl.glValidateProgram(shaderprogram);
////
////                    init = false;
////                }
////
////                gl.glUseProgram(shaderprogram);
////                super.paint(gl);
////                gl.glUseProgram(0);
////            }
//
//        }
////        JoglSpace.window(d, 800, 800);
////
////        //d.children.add(new GridTex(16).pos(0,0,1000,1000));
////
////        {
////            Windo w = d.addWindo(WidgetTest.widgetDemo());
////            w.pos(80, 80, 550, 450);
////
//////            Port p = w.addPort("X");
////        }
////
////        d.addWindo(grid(new PushButton("x"), new PushButton("y"))).pos(10, 10, 50, 50);
////
////        d.addWindo(new PushButton("w")).pos(-50, -50, 10, 10);
////
////        //d.newWindo(grid(new PushButton("x"), new PushButton("y"))).pos(-100, -100, 0, 0);
////
//    }

}
