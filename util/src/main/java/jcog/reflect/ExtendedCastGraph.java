/*
 * The MIT License
 *
 * Copyright 2015 Kamnev Georgiy (nt.gocha@gmail.com).
 *
 */

package jcog.reflect;

import jcog.TODO;
import jcog.Util;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.math.v2;
import jcog.math.v3;
import jcog.reflect.spi.GetTypeConvertor;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import jcog.sort.FloatRank;
import jcog.sort.RankedN;

import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;

/** @author Kamnev Georgiy (nt.gocha@gmail.com)
 * @see xyz.cofe.typeconv.spi.GetTypeConvertor
 */
public class ExtendedCastGraph extends CastGraph {

    abstract public static class Way<X> implements Supplier<X> {
        public String name;

    }

    /** supplies zero or more chocies from a set */
    public static class Some<X> implements Supplier<X[]> {
        final Way<X>[] way;
        final AtomicMetalBitSet enable = new AtomicMetalBitSet();

        public Some(Way<X>[] way) {
            this.way = way;
            assert(way.length > 1 && way.length <= 31 /* AtomicMetalBitSet limit */);
        }

        public Some<X> set(int which, boolean enable) {
            this.enable.set(which, enable);
            return this;
        }

        @Override
        public X[] get() {
            throw new TODO();
        }

        public int size() {
            return way.length;
        }
    }

    public static class Best<X> extends RankedN implements Supplier<X> {
        final Some<X> how;
        final FloatRank<X> rank;

        public Best(Some<X> how, FloatRank<X> rank) {
            super(new Object[how.size()], rank);
            this.how = how;
            this.rank = rank;
        }

        @Override
        public X get() {
            clear();
            X[] xx = how.get();
            if (xx.length == 0)
                return null;
            for (X x : xx)
                add(x);
            return (X) top();
        }
    }

    /** forces a one or none choice from a set */
    public static class Either<X> implements Supplier<X> {
        final Way<X>[] way;
        volatile int which = -1;

        public Either(Way<X>... way) {
            assert(way.length > 1);
            this.way = way;
        }

        public Either<X> set(int which) {
            this.which = which;
            return this;
        }

        public final Either<X> disable() {
            set(-1);
            return this;
        }

        @Override
        public X get() {
            int c = this.which;
            return c >=0 ? way[c].get() : null;
        }
    }

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

        //add: generic Supplier<X> -> X -- requires generic argument processing

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
        //addEdge(v2.class, (Function<v2, Either<Double>>)(v4 -> Math.sqrt(Util.sqr(v4.x)+Util.sqr(v4.y))), Double.class);

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
