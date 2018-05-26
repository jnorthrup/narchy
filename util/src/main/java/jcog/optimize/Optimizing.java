package jcog.optimize;

import com.google.common.base.Joiner;
import jcog.io.Schema;
import jcog.io.arff.ARFF;
import jcog.math.Quantiler;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

/** executes optimizations. repeated calls collect data to common results collector. */
public class Optimizing<X,Y> {
    final Supplier<X> subjects;

    final Tweaks<X> tweaks;

    public final ARFF data;

    private final Optimal<Y, ?>[] seeks;
    private final Function<X, Y> experiment;
    private final List<Tweak<X, ?>> tweaksReady;

    /** feature of an evaluation; evaluated after */
    abstract public static class Optimal<Y,V> {
        final String id; //must not conflict with any tweaks

        protected Optimal(String id) {
            this.id = id;
        }

        abstract ObjectFloatPair<V> eval(Y x); //observation + relative score

        public void defineIn(Schema data) {
            data.defineNumeric(id);
        }
    }

    /** simple scalar Optimal */
    public static class Score<Y> extends Optimal<Y,Float> {

        private final FloatFunction<Y> func;
        private final float pri;

        public Score(String name, float pri, FloatFunction<Y> f) {
            super(name);
            this.func = f;
            this.pri = pri;
        }

        public Score(String name, FloatFunction<Y> f) {
            this(name, 1f, f);
        }

        public Score(FloatFunction<Y> f) {
            this("score", 1f, f);
        }

        @Override
        ObjectFloatPair<Float> eval(Y y) {
            float v = func.floatValueOf(y);
            return PrimitiveTuples.pair((Float)v, v * pri);
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(Optimizing.class);


    Optimizing(Supplier<X> subjects, Tweaks<X> tweaks, Function<X,Y> experiment, Optimal<Y,?>... seeks) {
        this.subjects = subjects;
        this.tweaks = tweaks;
        this.experiment = experiment;
        this.seeks = seeks;

        final float autoInc_default = 5f;
        Map<String, Float> hints = Map.of("autoInc", autoInc_default);

        Pair<List<Tweak<X, ?>>, SortedSet<String>> uu = tweaks.get(hints);
        tweaksReady = uu.getOne();
        SortedSet<String> unknown = uu.getTwo();
        if (tweaksReady.isEmpty()) {
            throw new RuntimeException("tweaks not ready:\n" + Joiner.on('\n').join(unknown));
        }


        if (!unknown.isEmpty()) {
            for (String w : unknown) {
                logger.warn("unknown: {}", w);
            }
        }


        data = new ARFF();
        data.defineNumeric("score");
        tweaksReady.forEach(t -> t.defineIn(data));

        if (seeks.length > 1) {
            //dont include repeat score column if single objective
            for (Optimal o : seeks)
                o.defineIn(data);
        }
    }

    public Result run(int iterations) {
        return run(iterations, 1);
    }
    public Result run(int iterations, int repeats) {
        return run(iterations, repeats, Executors.newSingleThreadExecutor());
    }

    public Result run(int iterations, int repeats, ExecutorService exe) {
        return run(iterations, repeats, exe::submit);
    }

    public Result run(int iterations, int repeats, Function<Callable,Future> exe) {
        return new Optimize<>(subjects, tweaksReady).run(data, iterations, repeats, experiment, seeks, exe);
    }

    public void saveOnShutdown(String file) {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try {
                data.writeToFile(file);
                System.out.println("saved " + data.data.size() + " experiment results to: " + file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

    }

    /** result = t in tweaks(subject) { eval(subject + tweak(t)) } */
    public static class Result {

        public final ARFF data;

        public Result(ARFF data) {
            this.data = data;
        }

        public ImmutableList best() {
            double bestScore = Double.NEGATIVE_INFINITY;
            ImmutableList best = null;
            for (ImmutableList e : data.data) {
                double s = ((Number) e.get(0)).doubleValue();
                if (s > bestScore) {
                    best = e;
                    bestScore = s;
                }
            }
            return best;
        }

        public void print() {
            data.print();
        }

        public RealDecisionTree tree(int discretization, int maxDepth) {
            return data.isEmpty() ? null :
                new RealDecisionTree(data.toFloatTable(),
                    0 /* score */, maxDepth, discretization);
        }


        /** remove entries below a given percentile */
        public void cull(float minPct, float maxPct) {

            int n = data.data.size();
            if (n < 6)
                return;

            Quantiler q = new Quantiler((int) Math.ceil((n-1)/2f));
            data.forEach(r -> {
                q.add( ((Number)r.get(0)).floatValue() );
            });
            float minValue = q.quantile(minPct);
            float maxValue = q.quantile(maxPct);
            data.data.removeIf(r -> {
                float v = ((Number) r.get(0)).floatValue();
                return (v <= maxValue && v >= minValue);
            });
        }
    }
}
