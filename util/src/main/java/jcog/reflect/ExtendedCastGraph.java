/*
 * The MIT License
 *
 * Copyright 2015 Kamnev Georgiy (nt.gocha@gmail.com).
 *
 */

package jcog.reflect;

import jcog.Util;
import jcog.math.v2;
import jcog.math.v3;
import jcog.reflect.spi.GetTypeConvertor;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;

import java.util.ServiceLoader;
import java.util.function.Function;

/** @author Kamnev Georgiy (nt.gocha@gmail.com)
 * @see xyz.cofe.typeconv.spi.GetTypeConvertor
 */
public class ExtendedCastGraph extends CastGraph {

    public ExtendedCastGraph() {

        super();
        for (GetTypeConvertor gtc : ServiceLoader.load(GetTypeConvertor.class)) {
            if (gtc == null) continue;
            Function conv = gtc.getConvertor();
            if (conv == null) continue;
            Class srcType = gtc.getSourceType();
            if (srcType == null) continue;
            Class trgType = gtc.getTargetType();
            if (trgType == null) continue;
            addEdge(srcType, conv, trgType);
        }

        addEdge(Boolean.class, (Function<Boolean, Integer>) (i3)-> i3 ? 1 : 0, Integer.class);
        addEdge(Integer.class, (Function<Integer, Float>) Integer::floatValue, Float.class);
        addEdge(Integer.class, (Function<Integer, Boolean>) (i2)-> i2 >= 0, Boolean.class);
        addEdge(Short.class, (Function<Short, Boolean>) (i1)-> i1 >= 0, Boolean.class);
        addEdge(Byte.class, (Function<Byte, Boolean>) (i)->i >= 0, Boolean.class);
        //1-element
        addEdge(Float.class, (Function<Float,Double>)(Float::doubleValue), Double.class);
        //1-element
        addEdge(Double.class, (Function<Double,Float>)(Double::floatValue), Float.class);

        addEdge(float[].class, (Function<float[], double[]>) Util::toDouble, double[].class);

        //default scalar value projection
        addEdge(v2.class, (Function<v2, Double>)(v4 -> Math.sqrt(Util.sqr(v4.x)+Util.sqr(v4.y))), Double.class);

        addEdge(v2.class, (Function<v2, float[]>)(v3 -> new float[] { v3.x, v3.y }), float[].class);
        addEdge(v3.class, (Function<v3, float[]>)(v2 -> new float[] { v2.x, v2.y, v2.z }), float[].class);
        addEdge(v2.class, (Function<v2, v3>)(v1 -> new v3( v1.x, v1.y, 0)), v3.class);

        addEdge(float[].class, (Function<float[], Tensor>)(ArrayTensor::new), Tensor.class);
        addEdge(double[].class, (Function<double[], float[]>) Util::toFloat, float[].class);
        //1-element
        addEdge(Float.class, (Function<Float,float[]>)(v -> v!=null ? new float[] { v } : new float[] { Float.NaN } ), float[].class);
        //        setAt(Float.class, Tensor.class, (Function<Float,Tensor>)((f) -> new ArrayTensor(new float[] { f} ))); //1-element
        //does this happen
        addEdge(Tensor.class, (Function<Tensor, ArrayTensor>)(t -> {
            if (t instanceof ArrayTensor) {
                return (ArrayTensor) t; //does this happen
            }
            return new ArrayTensor(t.toFloatArrayShared());
        }), ArrayTensor.class);
        addEdge(ArrayTensor.class, (Function<ArrayTensor,float[]>)(a->a.data), float[].class);

    }

}
