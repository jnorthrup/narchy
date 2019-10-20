package jcog.learn;

import jcog.Texts;
import jcog.Util;
import jcog.data.MutableFloat;
import jcog.data.NumberX;
import jcog.data.graph.AdjGraph;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** untested */
public class HopfieldMap<X> {

    private final X[] x;
    private final FloatFunction<X> in;
    private final FloatObjectProcedure<X> out;

    
    private final AdjGraph<X, Float> weight = new AdjGraph(false);

    private final Random rng = new Random();

    /**
     * TODO generalize to Iterable
     */
    @SafeVarargs
    public HopfieldMap(FloatFunction<X> in, FloatObjectProcedure<X> out, X... x) {
        assert (x.length > 1);
        this.x = x;
        this.in = in;
        this.out = out;
    }

    public X random() {
        return x[randomIndex()];
    }

    public int randomIndex() {
        return rng.nextInt(x.length);
    }

    public float randomWeight(float min, float max) {
        return Util.lerp(rng.nextFloat(), min, max);
    }

    public HopfieldMap<X> randomWeights(float connectivity) {
        var edges = (int) Math.ceil(x.length * x.length * connectivity);
        for (var i = 0; i < edges; i++) {
            var a = random();
            var b = random();
            if (a != b) {
                weight.addNode(a);
                weight.addNode(b);
                weight.setEdge(a, b, randomWeight(-1, +1));
            }
        }
        return this;
    }

    @Override
    public String toString() {
        var result = Arrays.stream(x).map(xx -> Texts.n4(in.floatValueOf(xx)) + ',').collect(Collectors.joining());
        var sb = result;
        return sb;
    }

    public HopfieldMap<X> learn(int cycles) {
        for (var i = 0; i < cycles; i++) {
            learn();
        }
        return this;
    }

    public static float alpha() {
        return 0.2f;
    }

    /**
     * https:
     */
    public void learn() {
        var p = randomIndex();

        var alpha = alpha();

        for (var i = 0; i < x.length; i++) {

            var a = x[p];

            float[] aOut = {0};
            weight.neighborEdges(a, (b, w) -> {
                var bIn = in.floatValueOf(b);

                aOut[0] += bIn * w;

                return Util.tanhFast(w + (alpha * aOut[0] * bIn)); 
            });

            this.out.value(out(aOut[0]), a);

            if (++p == x.length) p = 0;
        }
    }

    public HopfieldMap<X> get() {
        for (var i = 0; i < x.length; i++) {
            float[] aOut = {0};

            var a = x[i];

            weight.neighborEdges(a, (b, w) -> {
                var bIn = in.floatValueOf(b);

                aOut[0] += bIn * w;
            });

            this.out.value(out(aOut[0]), a);
        }
        return this;
    }

    protected static float out(float v) {
        
        return v >= 0 ? 1 : -1;
        
    }

    public HopfieldMap<X> set(float... v) {
        assert (v.length == x.length);
        for (var i = 0; i < v.length; i++)
            out.value(v[i], x[i]);
        return this;
    }

    public static void main(String[] args) {
        var n = 8;
        var m = IntStream.range(0, n).mapToObj(i1 -> new MutableFloat()).toArray(NumberX[]::new);

        var h = new HopfieldMap<NumberX>(NumberX::floatValue,
                (v, x) -> x.set(v), m);
        h.randomWeights(0.9f);
        for (var i = 0; i < 16; i++) {
            h.set(+1, +1, +1, +1, -1, -1, -1, -1).learn(1);
            h.set(-1, -1, -1, -1, +1, +1, +1, +1).learn(1);
        }

        h.set(+1, +1, +1, +1, -1, 0, -1, -1).get();
        System.out.println(h);
        System.out.println(h.weight);


    }



}
