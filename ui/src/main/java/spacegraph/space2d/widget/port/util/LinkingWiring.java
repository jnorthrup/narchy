package spacegraph.space2d.widget.port.util;

import jcog.Util;
import jcog.reflect.ExtendedCastGraph;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import spacegraph.input.finger.Wiring;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Windo;

import java.util.List;
import java.util.function.Function;

/** attempts to establish a default link if the wiring was successful */
public class LinkingWiring extends Wiring {

    final static ExtendedCastGraph CAST = new ExtendedCastGraph();
    static {
        CAST.set(float[].class, Tensor.class, (Function<float[],Tensor>)(ArrayTensor::new));

        CAST.set(float[].class, double[].class, (Function<float[], double[]>)Util::toDouble);
        CAST.set(double[].class, float[].class, (Function<double[], float[]>)Util::toFloat);

        CAST.set(Float.class, Tensor.class, (Function<Float,Tensor>)(ArrayTensor::new)); //1-element
        CAST.set(ArrayTensor.class, float[].class, (Function<ArrayTensor,float[]>)(a->a.data));

    }
    public LinkingWiring(Surface start) {
        super(start);
    }

    @Override
    protected Wire wire(Wire w, GraphEdit wall) {

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

        return wall.cable(w, new PushButton("x").click((r)->r.parent(Windo.class).remove()));

//        PathSurface p = new PathSurface(2);
//        p.set(0, start.cx(), start.cy());
//        p.set(1, end.cx(), end.cy());
//        wall.addRaw(p);
    }
}
