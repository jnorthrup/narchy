package spacegraph.space2d.widget.port;

import jcog.Util;
import jcog.math.v2;
import jcog.math.v3;
import jcog.reflect.ExtendedCastGraph;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import spacegraph.space2d.widget.port.util.AdaptingWire;
import spacegraph.space2d.widget.windo.GraphEdit;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class TypedPort<X> extends Port<X> {


    public final static ExtendedCastGraph CAST = new ExtendedCastGraph();
    static {

        CAST.set(Boolean.class, Integer.class, (Function<Boolean, Integer>) (i)->i ? 1 : 0);
        CAST.set(Integer.class, Float.class, (Function<Integer, Float>) Integer::floatValue);
        CAST.set(Integer.class, Boolean.class, (Function<Integer, Boolean>) (i)->i >= 0);
        CAST.set(Short.class, Boolean.class, (Function<Short, Boolean>) (i)->i >= 0);
        CAST.set(Byte.class, Boolean.class, (Function<Byte, Boolean>) (i)->i >= 0);
        CAST.set(Float.class, Double.class, (Function<Float,Double>)(Float::doubleValue)); //1-element
        CAST.set(Double.class, Float.class, (Function<Double,Float>)(Double::floatValue)); //1-element

        CAST.set(float[].class, double[].class, (Function<float[], double[]>) Util::toDouble);

        CAST.set(v2.class, Double.class, (Function<v2, Double>)(v -> Math.sqrt(Util.sqr(v.x)+Util.sqr(v.y))) ); //default scalar value projection

        CAST.set(v2.class, float[].class, (Function<v2, float[]>)(v -> new float[] { v.x, v.y }) );
        CAST.set(v3.class, float[].class, (Function<v3, float[]>)(v -> new float[] { v.x, v.y, v.z }) );
        CAST.set(v2.class, v3.class, (Function<v2, v3>)(v -> new v3( v.x, v.y, 0)) );

        CAST.set(float[].class, Tensor.class, (Function<float[],Tensor>)(ArrayTensor::new));
        CAST.set(double[].class, float[].class, (Function<double[], float[]>) Util::toFloat);
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

    public final Class<? super X> type;

    public TypedPort(Class<? super X> type) {
        super();
        this.type = type;
    }

    public TypedPort(Class<? super X> type, In<? super X> o) {
        super(o);
        this.type = type;
    }

    public TypedPort(Class<? super X> type, Consumer<? super X> o) {
        super(o);
        this.type = type;
    }


    public static Wire adapt(Wire w, GraphEdit g) {
        if (w.a instanceof TypedPort && w.b instanceof TypedPort) {

            //TODO lazy construct and/or cache these

            //apply type checking and auto-conversion if necessary
            Class aa = ((TypedPort) w.a).type, bb = ((TypedPort) w.b).type;
            if (aa.equals(bb)) {
                //ok
            } else {

                List<Function> ab = CAST.convertors(aa, bb), ba = CAST.convertors(bb, aa);

                if (!ab.isEmpty() || !ba.isEmpty())
                    w = new AdaptingWire(w, ab, ba);

            }

        }
        return w;
    }
}
