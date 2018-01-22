package spacegraph.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Util;
import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.render.Draw;

import static spacegraph.layout.Gridding.col;
import static spacegraph.layout.Gridding.grid;

/**
 * abstract 1D slider/scrollbar
 */
public class BaseSlider extends Surface {


    /** dead-zone at the edges to latch min/max values */
    private static final float margin = 0.02f;

    private static final float EPSILON = Float.MIN_NORMAL;

    @Nullable ObjectFloatProcedure<BaseSlider> change;
    private float p;



    public BaseSlider(float p) {
        this.p = p;
    }


    public BaseSlider on(ObjectFloatProcedure<BaseSlider> c) {
        this.change = c;
        return this;
    }

    @Override
    protected void paint(GL2 gl, int dtMS) {
        Draw.bounds(gl, bounds, (g)->{
            draw.value(this.p, g);
        });
    }


    FloatObjectProcedure<GL2> draw = SolidLeft;

    public BaseSlider type(FloatObjectProcedure<GL2> draw) {
        this.draw = draw;
        return this;
    }

    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
        if (hitPoint!=null && leftButton(buttons)) {
            //System.out.println(this + " touched " + hitPoint + " " + Arrays.toString(buttons));

            _set(p(hitPoint));

            return this;
        }

        return super.onTouch(finger, hitPoint, buttons);
    }


    public void _set(float p) {
        changed(p);
    }

    protected void changed(float p) {
        if (Util.equals(this.p, p, Float.MIN_NORMAL))
            return;

        this.p = p;
        if (change!=null)
            change.value(this, value());
    }

    public float value() {
        return v(p);
    }

    public void value(float v) {
        this.p = p(v);
    }

    /** normalize: gets the output value given the proportion (0..1.0) */
    protected float v(float p) {
        return p;
    }

    /**
     * unnormalize: gets proportion from external value
     */
    protected float p(float v) {

        return v;
    }

    //    public static void main(String[] args) {
//        new GraphSpace<Surface>(
//
//                (Surface vt) -> new SurfaceMount(null, vt),
//
//                new XYPadSurface()
//
//        ).show(800,800);
//    }
    public static void main(String[] args) {

        SpaceGraph.window(
                grid(
                    new XYSlider(), new XYSlider(), new XYSlider(),
                    col(
                        new BaseSlider(0.75f),
                        new BaseSlider(0.25f),
                        new BaseSlider(0.5f)
                    )
                )
        , 800, 800 );
    }

    private static float p(v2 hitPoint) {

        //TODO interpret point coordinates according to the current drawn model, which could be a knob etc

        float x = hitPoint.x;
        if (x <= margin)
            return 0;
        else if (x >= (1f-margin))
            return 1f;
        else
            return hitPoint.x;
    }

    public static final FloatObjectProcedure<GL2> SolidLeft = (p, gl) -> {
        float W = 1; //TODO use correct w() and h()
        float H = 1;
        float barSize = W * p;

        //inset
        gl.glColor4f(0f, 0f, 0f, 0.5f);
        Draw.rect(gl, barSize, 0, W-barSize, H);

        //growing knob from left
        gl.glColor4f(0.75f * 1f - p, 0.75f * p, 0f, 0.8f);
        Draw.rect(gl, 0, 0, barSize, H);
    };

    public static final FloatObjectProcedure<GL2> Knob = (p, gl) -> {

        float knobWidth = 0.05f;
        float W = 1;
        float H = 1;
        float x = W * p;

        gl.glColor4f(0f, 0f, 0f, 0.5f);
        Draw.rect(gl, 0, 0, W, H);

        gl.glColor4f(1f - p, p, 0f, 0.75f);
        Draw.rect(gl, x-knobWidth/2f, 0, knobWidth, H);

    };


//    public void denormalized(double... n) {
//        double mn = min.get();
//        double mx = max.get();
//
//        for (int i = 0; i < n.length; i++) {
//            double nn = n[i];
//            double v = (nn) * (mx - mn) + mn;
//            if (v < mn) v = mn; //clip to bounds
//            if (v > mx) v = mx;
//            value(i, v);
//        }
//
//    }
//
//    public double[] normalized() {
//        double[] n = normalized;
//        if (n == null) {
//            n = normalized = new double[dimensions];
//        }
//
//        //TODO only compute this if invalidated
//        for (int i = 0; i < dimensions; i++) {
//            n[i] = p(value[i].get());
//        }
//
//        return n;
//    }


//
//    public interface Vis {
//
//        /**
//         * @param vector vector of the normalized proportional positions, between 0 and 1.0
//         * @param canvas target canvas
//         * @param w      parent width
//         * @param h      parent height
//         * @param g      re-usable graphics context
//         */
//        void redraw(double[] vector, Canvas canvas, double w, double h, GraphicsContext g) throws RuntimeException;
//
//        static double getFirstAndOnlyDimension(double[] vector) {
//            if (vector.length != 1) throw new RuntimeException("invalid dimension");
//            return vector[0];
//        }
//    }
//
//
//    public static final Vis BarSlider = (vector, canvas1, W, H, g1) -> {
//
//
//        double p = Vis.getFirstAndOnlyDimension(vector);
//
//        double margin = 4;
//        double mh = margin / 2.0;
//
//
//        g1.setLineWidth(mh * 2);
//        g1.setStroke(Color.GRAY);
//        g1.strokeRect(0, 0, W, H);
//
//        g1.setLineWidth(0);
//        double hp = 0.5 + 0.5 * p;
//        g1.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//        double barSize = W * p;
//        g1.fillRect(mh, mh, barSize - mh * 2, H - mh * 2);
//    };
//
//    public static final Vis NotchSlider = (v, canvas, W, H, g) -> {
//
//        double p = Vis.getFirstAndOnlyDimension(v);
//
//        double margin = 4;
//
//
//        g.setLineWidth(0);
//
//        //TODO use a x,w calculation that keeps the notch within bounds that none if it goes invisible at the extremes
//
//        double hp = 0.5 + 0.5 * p;
//        g.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//        double notchRadius = W * 0.1;
//        double mh = margin / 2.0;
//        double barSize = W * p;
//        g.fillRect(mh + barSize - notchRadius, mh, notchRadius * 2, H - mh * 2);
//    };
//
//    public static final Vis CircleKnob = (v, canvas, W, H, g) -> {
//        double p = Vis.getFirstAndOnlyDimension(v);
//
//        g.clearRect(0, 0, W, H);
//        //g.setFill(Color.BLACK);
//        //g.fillRect(0, 0, W, H);
//
//        double angleStart = 0;
//        double circumferenceActive = 0.5; //how much of the circumference of the interior circle is active as a dial track
//
//
////        double x = W/2 + (W/2-margin) * Math.cos(theta);
////        double y = H/2 + (H/2-margin) * Math.sin(theta);
////        double t = 4;
//        /*g.setFill(Color.WHITE);
//        g.fillOval(x-t, y-t, t*2,t*2);*/
//
//        double ew = W;
//        double eh = H;
//
//        double np = 0.75 + (p * 0.25);
//
//        //double ews = ew * np, ehs = eh * np; //scale by prop
//
//        g.setFill(Color.DARKGRAY);
//        double ut = (H - eh) / 2.0;
//        double ul = (W - ew) / 2.0;
//        g.fillOval(ul, ut, ew, eh);
//
//        double hp = 0.5 + 0.5 * p;
//        g.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//
//
//        double theta = angleStart + (1 - p) * circumferenceActive * (2 * Math.PI);
//        double atheta = theta * 180.0 / Math.PI; //radian to degree
//        double knobArc = 60;
//        g.fillArc(ul, ut, ew, eh,
//                atheta - knobArc / 2, knobArc, ArcType.ROUND);
//
//
//    };
//
//


//    public NSlider bind(DoubleProperty... p) {
//        ensureDimension(p.length);
//
//        for (int i = 0; i < p.length; i++)
//            p[i].bindBidirectional(value[i]);
//
//        return this;
//    }


//    public static void makeDraggable(final Stage stage, final Node byNode) {
//        final Delta dragDelta = new Delta();
//        byNode.setOnMouseEntered(new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent mouseEvent) {
//                if (!mouseEvent.isPrimaryButtonDown()) {
//                    byNode.setCursor(Cursor.HAND);
//                }
//            }
//        });
//        byNode.setOnMouseExited(new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent mouseEvent) {
//                if (!mouseEvent.isPrimaryButtonDown()) {
//                    byNode.setCursor(Cursor.DEFAULT);
//                }
//            }
//        });
//    }


//    public static void main(String[] args) {
//        FX.run((a, b) -> {
//
//            FlowPane p = new FlowPane(16, 16);
//
//            p.getChildren().setAll(
//                    new NSlider("Bar", 256, 96, NSlider.BarSlider, 0.5),
//                    new NSlider("Notch", 128, 45, NSlider.NotchSlider, 0.25),
//                    new NSlider("Notch--", 64, 25, NSlider.NotchSlider, 0.75),
//                    new NSlider("Knob", 256, 256, NSlider.CircleKnob, 0.5),
//                    new NSlider("Ranged", 256, 256, NSlider.BarSlider, 75)
//                            .range(0, 100).on(0, c -> System.out.println(Arrays.toString(c.normalized())))
//            );
//
//
//            b.setScene(new Scene(p, 800, 800));
//            b.show();
//        });
//    }
//
//    private NSlider range(double min, double max) {
//        this.min.set(min);
//        this.max.set(max);
//        return this;
//    }
//    private NSlider on(int dimension, Consumer<NSlider> callback) {
//
//        //TODO save listener so it can be de-registered
//        value[0].addListener(c -> callback.accept(NSlider.this));
//
//        return this;
//    }
//
//    public static class LeftRightDrag implements Control, EventHandler<MouseEvent> {
//
//        private Canvas canvas;
//        private NSlider n;
//
//        @Override
//        public void start(NSlider n) {
//            canvas = n.canvas;
//            this.n = n;
//
//            canvas.setOnMouseDragged(this);
//            canvas.setOnMousePressed(this); //could also work as released
//            canvas.setOnMouseReleased(this);
//
//            canvas.setCursor(Cursor.CROSSHAIR);
//
//        }
//
//        @Override
//        public void stop() {
//            throw new RuntimeException("unimpl");
//        }
//
//        @Override
//        public void handle(MouseEvent e) {
//
//            canvas.setCursor(
//                    (e.getEventType()==MouseEvent.MOUSE_RELEASED) ? Cursor.CROSSHAIR : Cursor.MOVE
//            );
//
//            n.denormalized(e.getX() / canvas.getWidth());
//
//            //System.out.println(dx + " " + dy + " " + value.get());
//
//            e.consume();
//        }
//
//        double p(double dx) {
//            return dx / canvas.getWidth();
//        }
//
//    }
//
}
