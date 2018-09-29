package spacegraph.input.finger;

import jcog.Util;
import jcog.reflect.ExtendedCastGraph;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.port.util.Wire;
import spacegraph.space2d.widget.shape.PathSurface;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Link;
import spacegraph.util.Path2D;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * the process of drawing a wire between two surfaces
 */
public class Wiring extends FingerDragging {

    final static ExtendedCastGraph CAST = new ExtendedCastGraph();
    static {
        CAST.set(float[].class, Tensor.class, (Function<float[],Tensor>)(ArrayTensor::new));

        CAST.set(float[].class, double[].class, (Function<float[], double[]>) Util::toDouble);
        CAST.set(double[].class, float[].class, (Function<double[], float[]>)Util::toFloat);

        CAST.set(Float.class, Tensor.class, (Function<Float,Tensor>)(ArrayTensor::new)); //1-element
        CAST.set(ArrayTensor.class, float[].class, (Function<ArrayTensor,float[]>)(a->a.data));

    }

    public interface Wireable {
        boolean onWireIn(@Nullable Wiring w, boolean active);

        void onWireOut(@Nullable Wiring w, boolean active);

    }

    private Path2D path;

    public final Surface start;
    private PathSurface pathVis;
    protected Surface end = null;

//    protected Windo source() {
//        return start.parent(Windo.class);
//    }
//    public Windo target() {
//        return end!=null ? end.parent(Windo.class) : null;
//    }

    public Wiring(Surface start) {
        super(2);
        this.start = start;
    }

    @Override
    protected boolean startDrag(Finger f) {
        if (super.startDrag(f)) {
            if (this.start instanceof Wireable)
                ((Wireable) start).onWireOut(this, true);
            return true;
        }
        return false;
    }


    @Override
    public boolean escapes() {
        return true;
    }

    @Override
    protected boolean drag(Finger f) {
        if (path == null) {
            path = new Path2D(64);

            GraphEdit g = graph();
            g.addRaw(pathVis = new PathSurface(path));
        } else {
            updateEnd(f);
        }

        pathVis.add(f.pos, 64);

        return true;
    }

    private GraphEdit graph() {
        return start.parent(GraphEdit.class);
    }

    @Override
    public void stop(Finger finger) {
        if (pathVis != null) {
            graph().removeRaw(pathVis);
            pathVis = null;
        }

        if (end != null) {


            if (!tryWire())
                return; //fail
        }

        if (this.start instanceof Wireable)
            ((Wireable) start).onWireOut(this, false);

        if (this.end instanceof Wireable)
            ((Wireable) end).onWireIn(this, false);

    }

    /**
     * called when wire has been established
     *
     * @param start
     * @param end
     * @param y
     * @param wall
     */
    protected Link wire(Wire w, GraphEdit wall) {

        if (w.a instanceof TypedPort && w.b instanceof TypedPort) {
            //apply type checking and auto-conversion if necessary
            Class aa = ((TypedPort) w.a).type;
            Class bb = ((TypedPort) w.b).type;
            if (aa.equals(bb)) {
                //ok
            } else {
                List<Function> ab = CAST.convertors(aa, bb);
                List<Function> ba = CAST.convertors(bb, aa);

//                System.out.println(aa + " -> " + bb + ": " + ab);
//                System.out.println(bb + " -> " + aa + ": " + ba);
            }
        }

        return wall.wire(w);

//        PathSurface p = new PathSurface(2);
//        p.set(0, start.cx(), start.cy());
//        p.set(1, end.cx(), end.cy());
//        wall.addRaw(p);
    }


    protected boolean tryWire() {
        GraphEdit wall = graph();
        Wire x = new Wire(start, end);
        Wire y = wall.addWire(x);
        if (y == x) {
            Link w = //new Wire(start, end);
                    wire(y, wall);


            start.root().debug(start, 1, () -> "wire(" + wall + ",(" + start + ',' + end + "))");
            //wire(start, end, y, wall);
            return true;
        }

        return false;
    }

    private void updateEnd(Finger finger) {
        Surface nextEnd = finger.touching.get();
        if (nextEnd != end) {

            if (nextEnd == start) {
                end = null;
            } else {

                if (end instanceof Wireable) {
                    ((Wireable) end).onWireIn(this, false);
                }

                if (nextEnd instanceof Wireable) {

                    if (((Wireable) nextEnd).onWireIn(this, true)) {
                        this.end = nextEnd;
                    } else {
                        this.end = null;
                    }

                }
            }
        }
    }
}
