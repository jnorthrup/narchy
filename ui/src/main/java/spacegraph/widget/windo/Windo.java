package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Scale;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.input.FingerMove;
import spacegraph.input.FingerResize;
import spacegraph.input.Fingering;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.button.PushButton;

import java.util.Map;

import static spacegraph.layout.Grid.grid;
import static spacegraph.widget.windo.Windo.DragEdit.MOVE;

/**
 * draggable panel
 */
public class Windo extends Widget {

    public enum DragEdit {
        MOVE,
        RESIZE_N, RESIZE_E, RESIZE_S, RESIZE_W,
        RESIZE_NW,
        RESIZE_SW,
        RESIZE_NE,
        RESIZE_SE
    }

    public Fingering dragMode = null;
    public DragEdit potentialDragMode = null;

    public final static float resizeBorder = 0.1f;


    protected boolean hover;

    Map<Object, Port> ports = null;

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


    @Deprecated
    RectFloat2D before = null;

    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {

        if (!editable())
            return super.onTouch(finger, hitPoint, buttons); //pass-through

        if (finger != null) {
            float fx = finger.hit.x;
            float fy = finger.hit.y;

            if (dragMode == null && !bounds.contains(fx, fy)) {
                hover = false;
                dragMode = null;
                return null;
            }

            Surface s = //dragMode == null ? super.onTouch(finger, hitPoint, buttons) : this;
                    super.onTouch(finger, hitPoint, buttons);

            if (s == this) {

                //if (moveable()) System.out.println(bounds + "\thit=" + finger.hit + "\thitGlobal=" + finger.hitGlobal);

                if (hitPoint != null) {

                    hover = true;


                    if (dragMode == null/* && hitPoint.x >= 0 && hitPoint.y >= 0 && hitPoint.x <= 1f && hitPoint.y <= 1f*/) {

                        potentialDragMode = null;

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


                    boolean bDrag = buttons != null && buttons.length > 0 && buttons[0] == 1;
                    if (bDrag) {
                        if (dragMode == null && potentialDragMode != null) {

                            if (editable(potentialDragMode)) {
                                //finger.lock(0, )..
                                before = bounds; //TODO store these in a shared Finger-context "posOnHit" field, not in this instance

                                Fingering d = fingering(potentialDragMode);
                                if (d != null && finger.tryFingering(d)) {
                                    dragMode = d;
                                }
                            } else {
                                dragMode = null;
                            }

                        }
                    } else {
                        dragMode = null;
                    }

                    return this;
                } else {
                    potentialDragMode = null;
                    dragMode = null;
                }
            }

            return s;
        }

        hover = false;
        potentialDragMode = null;
        dragMode = null;
        return null;
    }

    protected Fingering fingering(DragEdit mode) {

        switch (mode) {
            case MOVE:
                return new FingerMove(this);

            default:
                return new FingerResize(this, mode);
        }

    }

    public boolean editable() {
        return true;
    }

    public boolean editable(DragEdit d) {
        return true;
    }

    public boolean opaque() {
        return true;
    }


    protected void prepaint(GL2 gl) {

    }

    protected void postpaint(GL2 gl) {

    }

    /** gets main content */
    public Surface get() {
        return ((Scale)children.get(0)).the;
    }

    @Override
    protected void paintComponent(GL2 gl) {
        paintBack(gl);


        prepaint(gl);

        float W = w(); //window.getWidth();
        float H = h(); //window.getHeight();

//        gl.glColor4f(0.8f, 0.6f, 0f, 0.25f);

//        int borderThick = 8;
//        gl.glLineWidth(borderThick);
//        Draw.line(gl, 0, 0, W, 0);
//        Draw.line(gl, 0, 0, 0, H);
//        Draw.line(gl, W, 0, W, H);
//        Draw.line(gl, 0, H, W, H);

//        float resizeBorder = Math.max(W, H) * this.resizeBorder;

        DragEdit p;
        if ((p = potentialDragMode) != null) {
            switch (p) {
                case RESIZE_SE:
                    //gl.glColor4f(1f, 0.8f, 0f, 0.5f);
                    //Draw.quad2d(gl, pmx, pmy, W, resizeBorder, W, 0, W - resizeBorder, 0);
                    break;
                case RESIZE_SW:
                    //gl.glColor4f(1f, 0.8f, 0f, 0.5f);
                    //Draw.quad2d(gl, pmx, pmy, 0, resizeBorder, 0, 0, resizeBorder, 0);
                    break;
            }
        }


//            gl.glLineWidth(2);
//            gl.glColor3f(0.8f, 0.5f, 0);
//            Draw.text(gl, str(notifications.top().get()), 32, smx + cw, smy + ch, 0);
//            gl.glColor3f(0.4f, 0f, 0.8f);
//            Draw.text(gl, wmx + "," + wmy, 32, smx - cw, smy - ch, 0);

        postpaint(gl);


    }

    protected void paintBack(GL2 gl) {
        if (hover && opaque()) {
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
                    gl.glColor3f(0.1f, 0.1f, 0.1f);
                    break;
            }
            Draw.rect(gl, x(), y(), w(), h());
        }
    }


    public static class Port extends Windo {
        public final String id;

        public final v2 posRel;
        public final v2 sizeRel;
        private final Windo win;

        Port(String id, Windo win) {
            super();
            this.id = id;
            this.win = win;

            Wall.CSurface content = win.wall().newCurface(id);

            set(content);
            //set(new Scale(new PushButton("?"), 0.9f));
            this.posRel = new v2(Float.NEGATIVE_INFINITY, 0);
            this.sizeRel = new v2(0.1f, 0.2f);
        }

        @Override
        public void doLayout() {
            float W = win.w();
            float H = win.h();
            {
                float x1, y1, x2, y2;
                float w = sizeRel.x * W;
                float h = sizeRel.y * H;
                if (posRel.x == Float.NEGATIVE_INFINITY) {
                    //glued to left
                    float y = Util.lerp((posRel.y) / 2f + 0.5f, win.bounds.min.y, win.bounds.max.y);
                    x1 = win.x() - w;
                    y1 = y - h / 2;
                    x2 = win.x();
                    y2 = y + h / 2;
                    pos(x1, y1, x2, y2);
                } else if (posRel.x == Float.POSITIVE_INFINITY) {
                    //glued to right
                } else {
                    //TODO
                    //etc
                }
            }

            super.doLayout();
        }

        @Override
        protected void paintBack(GL2 gl) {
            gl.glColor3f(1f, 0, 1f);
            Draw.rect(gl, x(), y(), w(), h());
        }

//        @Override
//        public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
//            if (hitPoint != null && hitPoint.inUnit())
//                return super.onTouch(finger, hitPoint, buttons);
//            return null;
//        }

        @Override
        protected Fingering fingering(DragEdit mode) {
            if (mode == MOVE) {
                return new FingerMove(this) {
                    @Override
                    public boolean moveX() {
                        return false;
                    }

                    @Override
                    public boolean moveY() {
                        return true;
                    }
                };
            }
            return super.fingering(mode);
        }
    }

    protected Wall wall() {
        return ((Wall) parent);
    }

    public Port addPort(String x) {
        Wall w = wall();
        {
//            if (ports == null)
//                ports = new LinkedHashMap<>();
//            return ports.computeIfAbsent(x, i -> {
            Port p = new Port(x, this);
            w.children.add(0, p);
            return p;
//            });
        }
    }


    public static void main(String[] args) {
        Wall d = new Wall() {
//            boolean init = true;
//            int shaderprogram;
//            String vsrc;
//
//            {
//                try {
//                    vsrc = new StringBuilder(new String(GLSL.class.getClassLoader().getResourceAsStream(
//                            "glsl/grid.glsl"
//                            //"glsl/16seg.glsl"
//                            //"glsl/metablob.glsl"
//                    ).readAllBytes())).toString();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            protected void paint(GL2 gl) {
//                if (init) {
////                    int v = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
//                    int f = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
//
//                    {
//
//                        gl.glShaderSource(f, 1, new String[]{vsrc}, new int[]{vsrc.length()}, 0);
//                        gl.glCompileShader(f);
//                    }
////                        {
////                            String vsrc = new StringBuilder(new String(GLSL.class.getClassLoader().getResourceAsStream(
////                                    "glsl/16seg.glsl"
////                            ).readAllBytes())).toString();
////                            gl.glShaderSource(f, 1, new String[]{vsrc}, new int[]{vsrc.length()}, 0);
////                            gl.glCompileShader(f);
////                        }
//
//                    shaderprogram = gl.glCreateProgram();
//                    //gl.glAttachShader(shaderprogram, v);
//                    gl.glAttachShader(shaderprogram, f);
//                    gl.glLinkProgram(shaderprogram);
//                    gl.glValidateProgram(shaderprogram);
//
//                    init = false;
//                }
//
//                gl.glUseProgram(shaderprogram);
//                super.paint(gl);
//                gl.glUseProgram(0);
//            }

        };

        //d.children.add(new GridTex(16).pos(0,0,1000,1000));

        {
            Windo w = d.addWindo(Widget.widgetDemo());
            w.pos(80, 80, 550, 450);

            Port p = w.addPort("X");
        }

        d.addWindo(grid(new PushButton("x"), new PushButton("y"))).pos(10, 10, 50, 50);

        d.addWindo(new PushButton("w")).pos(-50, -50, 10, 10);

        //d.newWindo(grid(new PushButton("x"), new PushButton("y"))).pos(-100, -100, 0, 0);

        SpaceGraph.window(d, 800, 800);
    }

}
