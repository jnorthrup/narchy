package jcog.learn.decision.feature;

import jcog.Texts;
import jcog.learn.Discretize1D;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DiscretizedScalarFeature {

    final String name;
    final Discretize1D discretizer;
    public final int num;
    private final int arity;

    public DiscretizedScalarFeature(int x, String name, int arity, Discretize1D d) {
        this.arity = arity;
        this.num = x;
        this.name = name;
        this.discretizer = d;
        d.reset(arity);
    }

    public void learn(float x) {
        discretizer.put(x);
    }

    public Stream<Predicate<Function<Integer,Float>>> classifiers(@Nullable String... labels) {
        assert (labels == null || labels.length == 0 || labels.length == levels());
        return IntStream.range(0, levels()).mapToObj(
                labels != null && labels.length == levels() ?
                        i -> new CentroidMatch(i, labels[i]) :
                        i -> new CentroidMatch(i, null)
        );
    }





    protected int levels() {
        return arity;
    }

    public class CentroidMatch implements Predicate<Function<Integer,Float>> {

        private final int v;
        private final String label;

        CentroidMatch(int v, String label) {
            this.v = v;
            this.label = label;
        }

        @Override
        public String toString() {
            double[] interval = value();
            return name + '=' + ((label != null ? (label + '[') : "") + Texts.n4(interval[0]) + ".." + Texts.n4(interval[1]) + ']');
        }

        /** computes a bounded estimate interval */
        public double[] value() {
            return discretizer.value(v);
        }

        @Override
        public boolean test(Function<Integer, Float> rr) {
            return discretizer.index(rr.apply(num)) == v;
        }

        public String condition(boolean isTrue) {

            String var = name;
            double[] range = value();
            if (isTrue) {
                if (range[0] == Double.NEGATIVE_INFINITY && range[1] == Double.POSITIVE_INFINITY)
                    return "true";
                if (range[0] == Double.NEGATIVE_INFINITY)
                    return var + " <= " + range[1];
                else if (range[1] == Double.POSITIVE_INFINITY)
                    return var + " >= " + range[0];
                else
                    return var + " >= " + range[0] + " && " + var + " <= " + range[1];

            } else {
                //return var + " < " + range[0] + " || " + var + " > " + range[1];

                if (range[0] == Double.NEGATIVE_INFINITY && range[1] == Double.POSITIVE_INFINITY)
                    return "false";
                if (range[0] == Double.NEGATIVE_INFINITY)
                    return "false"; //var + " <= " + range[1];
                else if (range[1] == Double.POSITIVE_INFINITY)
                    return "false"; //var + " >= " + range[0];
                else
                    return var + " < " + range[0] + " || " + var + " > " + range[1];
            }

        }
    }

}
