package jcog.optimize;

import jcog.io.arff.ARFF;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/** executes optimizations. repeated calls collect data to common results collector. */
public class Optimizing<X> {
    final Supplier<X> subjects;

    final Tweaks<X> tweaks;

    //final List<Optimal<X>> seeks; TODO

    public final ARFF data;

    private final Optimal<X, ?>[] seeks;

    /** feature of an evaluation; evaluated after */
    abstract public static class Optimal<X,Y> {
        final String id; //must not conflict with any tweaks

        protected Optimal(String id) {
            this.id = id;
        }

        abstract ObjectFloatPair<Y> eval(X x); //observation + relative score
    }

    /** simple scalar Optimal */
    public static class Score<X> extends Optimal<X,Float> {

        private final FloatFunction<X> func;

        public Score(FloatFunction<X> f) {
            super("score");
            this.func = f;
        }

        @Override
        ObjectFloatPair<Float> eval(X x) {
            float v = func.floatValueOf(x);
            return PrimitiveTuples.pair((Float)v, v);
        }
    }


    Optimizing(Supplier<X> subjects, Tweaks<X> tweaks, Optimal<X,?>... seeks) {
        this.subjects = subjects;
        this.tweaks = tweaks;
        this.seeks = seeks;

        data = new ARFF();
        data.defineNumeric("score");
        tweaks.tweaks.forEach(t -> t.defineIn(data));
    }

    public Result<X> run(int iterations) {
        return run(iterations, 1);
    }
    public Result<X> run(int iterations, int repeats) {
        return run(iterations, repeats, Executors.newSingleThreadExecutor());
    }

    public Result<X> run(int iterations, int repeats, ExecutorService exe) {
        return new Optimize<>(subjects, tweaks).run(data, iterations, repeats, seeks, exe);
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
}
