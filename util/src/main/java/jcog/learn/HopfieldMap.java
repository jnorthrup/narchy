package jcog.learn;

import jcog.Texts;
import jcog.Util;
import jcog.data.MutableFloat;
import jcog.data.NumberX;
import jcog.data.graph.AdjGraph;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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
        int edges = (int) Math.ceil((double) (x.length * (float) x.length * connectivity));
        for (int i = 0; i < edges; i++) {
            X a = random();
            X b = random();
            if (a != b) {
                weight.addNode(a);
                weight.addNode(b);
                weight.setEdge(a, b, randomWeight(-1.0F, (float) +1));
            }
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb1 = new StringBuilder();
        for (X xx : x) {
            String s = Texts.INSTANCE.n4(in.floatValueOf(xx)) + ',';
            sb1.append(s);
        }
        String result = sb1.toString();
        String sb = result;
        return sb;
    }

    public HopfieldMap<X> learn(int cycles) {
        for (int i = 0; i < cycles; i++) {
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
        int p = randomIndex();

        float alpha = alpha();

        for (int i = 0; i < x.length; i++) {

            X a = x[p];

            float[] aOut = {(float) 0};
            weight.neighborEdges(a, new BiFunction<X, Float, Float>() {
                @Override
                public Float apply(X b, Float w) {
                    float bIn = in.floatValueOf(b);

                    aOut[0] += bIn * w;

                    return Util.tanhFast(w + (alpha * aOut[0] * bIn));
                }
            });

            this.out.value(out(aOut[0]), a);

            if (++p == x.length) p = 0;
        }
    }

    public HopfieldMap<X> get() {
        for (int i = 0; i < x.length; i++) {
            float[] aOut = {(float) 0};

            X a = x[i];

            weight.neighborEdges(a, new BiConsumer<X, Float>() {
                @Override
                public void accept(X b, Float w) {
                    float bIn = in.floatValueOf(b);

                    aOut[0] += bIn * w;
                }
            });

            this.out.value(out(aOut[0]), a);
        }
        return this;
    }

    protected static float out(float v) {
        
        return (float) (v >= (float) 0 ? 1 : -1);
        
    }

    public HopfieldMap<X> set(float... v) {
        assert (v.length == x.length);
        for (int i = 0; i < v.length; i++)
            out.value(v[i], x[i]);
        return this;
    }

    public static void main(String[] args) {
        int n = 8;
        List<MutableFloat> list = new ArrayList<>();
        for (int i1 = 0; i1 < n; i1++) {
            MutableFloat mutableFloat = new MutableFloat();
            list.add(mutableFloat);
        }
        NumberX[] m = list.toArray(new NumberX[0]);

        HopfieldMap<NumberX> h = new HopfieldMap<NumberX>(NumberX::floatValue,
                new FloatObjectProcedure<NumberX>() {
                    @Override
                    public void value(float v, NumberX x) {
                        x.set(v);
                    }
                }, m);
        h.randomWeights(0.9f);
        for (int i = 0; i < 16; i++) {
            h.set((float) +1, (float) +1, (float) +1, (float) +1, -1.0F, -1.0F, -1.0F, -1.0F).learn(1);
            h.set(-1.0F, -1.0F, -1.0F, -1.0F, (float) +1, (float) +1, (float) +1, (float) +1).learn(1);
        }

        h.set((float) +1, (float) +1, (float) +1, (float) +1, -1.0F, (float) 0, -1.0F, -1.0F).get();
        System.out.println(h);
        System.out.println(h.weight);


    }



}
