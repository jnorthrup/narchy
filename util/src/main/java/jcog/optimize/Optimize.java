package jcog.optimize;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.list.FasterList;
import jcog.meter.event.CSVOutput;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.MultiDirectionalSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.MathArrays;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static jcog.Texts.n4;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Optimization solver wrapper w/ lambdas
 * instance of an experiment
 */
public class Optimize<X> {

    /**
     * if a tweak's 'inc' (increment) is not provided,
     * use the known max/min range divided by this value as 'inc'
     */
    static final float autoInc_default = 2f;

    final List<Tweak<X,?>> tweaks;
    final Supplier<X> subject;

    private final boolean trace = false;
    private final static Logger logger = LoggerFactory.getLogger(Optimize.class);

    private CSVOutput csv;

    protected Optimize(Supplier<X> subject, Tweaks<X> t) {
        this(subject, t, Map.of("autoInc", autoInc_default));
    }

    protected Optimize(Supplier<X> subject, Tweaks<X> t, Map<String, Float> hints) {
        Pair<List<Tweak<X,?>>, SortedSet<String>> uu = t.get(hints);
        List<Tweak<X,?>> ready = uu.getOne();
        SortedSet<String> unknown = uu.getTwo();
        if (ready.isEmpty()) {
            throw new RuntimeException("tweaks not ready:\n" + Joiner.on('\n').join(unknown));
        }


        if (!unknown.isEmpty()) {
            for (String w : unknown) {
                logger.warn("unknown: {}", w);
            }
        }

        this.subject = subject;
        this.tweaks = ready;
    }

    public final Result<X> run(int maxIterations, FloatFunction<Supplier<X>> eval) {
        return run(maxIterations, 1, eval);
    }

    public Result<X> run(int maxIterations, int repeats, FloatFunction<Supplier<X>> eval) {

        assert (repeats >= 1);


        final int dim = tweaks.size();

        double[] mid = new double[dim];
        //double[] sigma = new double[n];
        double[] min = new double[dim];
        double[] max = new double[dim];
        double[] inc = new double[dim];
//        double[] range = new double[dim];

        X example = subject.get();
        int i = 0;
        for (Tweak w : tweaks) {
            TweakFloat s = (TweakFloat) w;

            //initial guess: get from sample, otherwise midpoint of min/max range
            Object guess = s.get(example);
            mid[i] = guess!=null ? ((float) guess) : ((s.getMax() + s.getMin()) / 2f);

            min[i] = (s.getMin());
            max[i] = (s.getMax());
            inc[i] = s.getInc();
//            range[i] = max[i] - min[i];
            //sigma[i] = Math.abs(max[i] - min[i]) * 0.75f; //(s.getInc());
            i++;
        }


        FasterList<DoubleObjectPair<double[]>> experiments = new FasterList<>(maxIterations);


        final double[] maxScore = {Double.NEGATIVE_INFINITY};

        ObjectiveFunction func = new ObjectiveFunction(point -> {

            double score;
            try {

                double sum = 0;

                for (int r = 0; r < repeats; r++) {

                    Supplier<X> x = () -> subject(point);

                    sum += eval.floatValueOf(x);

                }

                score = sum / repeats;

            } catch (Exception e) {
                logger.error("{} {} {}", this, point, e);
                score = Float.NEGATIVE_INFINITY;
            }


            if (trace)
                csv.out(ArrayUtils.add(point, score));

            maxScore[0] = Math.max(maxScore[0], score);
            System.out.println(
                n4(score) + " / " + n4(maxScore[0]) + "\t" + n4(point)
            );

            experiments.add(pair(score, point));
            experimentIteration(point, score);
            return score;
        });


        if (trace)
            csv = new CSVOutput(System.out, Stream.concat(tweaks.stream().map(t -> t.id), Stream.of("score")).toArray(String[]::new));

        experimentStart();

        try {
            solve(dim, func, mid, min, max, inc, maxIterations);
        } catch (Throwable t) {
            logger.info("solve {} {}", func, t);
        }

        return new Result<X>(experiments, tweaks);


    }

    public void solve(int dim, ObjectiveFunction func, double[] mid, double[] min, double[] max, double[] inc, int maxIterations) {
        if (dim == 1) {
            //use a solver capable of 1 dim
            new SimplexOptimizer(1e-10, 1e-30).optimize(
                    new MaxEval(maxIterations),
                    func,
                    GoalType.MAXIMIZE,
                    new InitialGuess(mid),
                    //new NelderMeadSimplex(inc)
                    new MultiDirectionalSimplex(inc)
            );
        } else {

            int popSize = 4 * Math.round(Util.sqr(tweaks.size())); /* estimate */

            double[] sigma = MathArrays.scale(1f, inc);

            new MyCMAESOptimizer(maxIterations, Double.NEGATIVE_INFINITY,
                    true, 0,
                    1, new MersenneTwister(System.nanoTime()),
                    true, null, popSize, sigma).optimize(
                    new MaxEval(maxIterations), //<- ignored?
                    func,
                    GoalType.MAXIMIZE,
                    new SimpleBounds(min, max),
                    new InitialGuess(mid)
            );

//            final int numIterpolationPoints = 3 * dim; //2 * dim + 1 + 1;
//            new BOBYQAOptimizer(numIterpolationPoints,
//                    dim * 2.0,
//                    1.0E-4D /* ? */).optimize(
//                    MaxEval.unlimited(), //new MaxEval(maxIterations),
//                    new MaxIter(maxIterations),
//                    func,
//                    GoalType.MAXIMIZE,
//                    new SimpleBounds(min, max),
//                    new InitialGuess(mid));
        }

    }

    /**
     * called before experiment starts
     */
    protected void experimentStart() {
    }

    /**
     * called after each iteration
     */
    protected void experimentIteration(double[] point, double score) {
    }

    /**
     * builds an experiment subject (input)
     */
    private X subject(double[] point) {
        X x = subject.get();

        for (int i = 0, dim = point.length; i < dim; i++) {
            point[i] = ((Tweak<X,Float>)tweaks.get(i)).set(x, (float) point[i]);
        }

        return x;
    }

}

//public class MeshOptimize<X> extends Optimize<X> {
//
//    /** experiment id's */
//    private static final AtomicInteger serial = new AtomicInteger();
//
//    /** should get serialized compactly though by msgpack */
//    private final MeshMap<Integer, List<Float>> m;
//
//    public MeshOptimize(String id, Supplier<X> subject, Tweaks<X> tweaks) {
//        super(subject, tweaks);
//
//        m = MeshMap.get(id, (k,v)->{
//            System.out.println("optimize recv: " + v);
//        });
//
//    }
//
//    @Override
//    protected void experimentIteration(double[] point, double score) {
//        super.experimentIteration(point, score);
//        m.put(serial.incrementAndGet(), Floats.asList(ArrayUtils.add(Util.toFloat(point), (float)score)));
//    }
//}
