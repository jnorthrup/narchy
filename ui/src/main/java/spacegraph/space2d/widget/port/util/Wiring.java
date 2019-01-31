package spacegraph.space2d.widget.port.util;

import jcog.Util;
import jcog.reflect.ExtendedCastGraph;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerDragging;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.shape.PathSurface;
import spacegraph.space2d.widget.windo.GraphEdit;
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

        CAST.set(Boolean.class, Integer.class, (Function<Boolean, Integer>) (i)->i ? 1 : 0);
        CAST.set(Integer.class, Float.class, (Function<Integer, Float>) Integer::floatValue);
        CAST.set(Integer.class, Boolean.class, (Function<Integer, Boolean>) (i)->i >= 0);
        CAST.set(Short.class, Boolean.class, (Function<Short, Boolean>) (i)->i >= 0);
        CAST.set(Byte.class, Boolean.class, (Function<Byte, Boolean>) (i)->i >= 0);
        CAST.set(Float.class, Double.class, (Function<Float,Double>)(Float::doubleValue)); //1-element
        CAST.set(Double.class, Float.class, (Function<Double,Float>)(Double::floatValue)); //1-element

        CAST.set(float[].class, double[].class, (Function<float[], double[]>) Util::toDouble);
        CAST.set(float[].class, Tensor.class, (Function<float[],Tensor>)(ArrayTensor::new));
        CAST.set(double[].class, float[].class, (Function<double[], float[]>)Util::toFloat);
        CAST.set(Float.class, float[].class, (Function<Float,float[]>)(v -> v!=null ? new float[] { v } : new float[] { Float.NaN } )); //1-element
//        CAST.setAt(Float.class, Tensor.class, (Function<Float,Tensor>)((f) -> new ArrayTensor(new float[] { f} ))); //1-element
        CAST.set(Tensor.class, ArrayTensor.class, (Function<Tensor,ArrayTensor>)(t -> {
            if (t instanceof ArrayTensor) {
                return (ArrayTensor) t; //does this happen
            }
            return new ArrayTensor(t.toFloatArrayShared());
        }));
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

    public Wiring(int button, Surface start) {
        super(button);
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

            GraphEdit g = graph();
            if (g!=null)
                g.addRaw(pathVis = new PathSurface(path = new Path2D(64)));
            else
                return false; //detached component no longer or never was in graph

        } else {
            updateEnd(f);
        }

        pathVis.add(f.posOrtho, 64);

        return true;
    }

    private GraphEdit graph() {
        return start.parent(GraphEdit.class);
    }

    @Override
    public void stop(Finger finger) {


        if (end != null) {
            if (!tryWire())
                return; //fail
        }

        if (this.start instanceof Wireable)
            ((Wireable) start).onWireOut(this, false);

        if (this.end instanceof Wireable)
            ((Wireable) end).onWireIn(this, false);

        if (pathVis != null) {
            pathVis.remove();
            pathVis = null;
        }

    }



    private Wire typeAdapt(Wire w, GraphEdit wall) {
        if (w.a instanceof TypedPort && w.b instanceof TypedPort) {

            //TODO lazy construct and/or cache these

            //apply type checking and auto-conversion if necessary
            Class aa = ((TypedPort) w.a).type;
            Class bb = ((TypedPort) w.b).type;
            if (aa.equals(bb)) {
                //ok
            } else {

                List<Function> ab = CAST.convertors(aa, bb);
                List<Function> ba = CAST.convertors(bb, aa);

                if (!ab.isEmpty() || !ba.isEmpty()) {
                    w = new AdaptingWire(w, ab, ba);
                }


            }

        }
        return w;
    }


    protected boolean tryWire() {
        GraphEdit wall = graph();

        Wire wire = typeAdapt(new Wire(start, end), wall);
        if (!wire.connectable())
            return false;

        if (wire == wall.addWire(wire)) {

            wire.connected();

            start.root().debug(start, 1, () -> "wire(" + wall + ",(" + start + ',' + end + "))");

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
