package jcog.optimize;

import com.google.common.base.Joiner;
import jcog.list.FasterList;
import jcog.meter.event.CSVOutput;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Optimization solver wrapper w/ lambdas
 */
public class Optimize<X> {

    public final Tweaks<X> tweaks;
    final Supplier<X> subject;

    private final boolean trace = true;
    private final static Logger logger = LoggerFactory.getLogger(Optimize.class);
    private CSVOutput csv;

    public Optimize(Supplier<X> subject, Tweaks<X> t) {
        this(subject, t, Map.of());
    }

    public Optimize(Supplier<X> subject, Tweaks<X> t, Map<String, Float> hints) {
        SortedSet<String> u = t.unknown(hints);
        if (t.ready.isEmpty()) {
            throw new RuntimeException("tweaks not ready:\n" + Joiner.on('\n').join(u));
        }
        if (!u.isEmpty()) {
            for (String w : u) {
                logger.warn("unknown: {}", w);
            }
        }

        this.subject = subject;
        this.tweaks = t;
    }

    public final Result<X> run(int maxIterations, FloatFunction<X> eval) {
        return run(maxIterations, 1, eval);
    }

    public Result<X> run(int maxIterations, int repeats, FloatFunction<X> eval) {

        int dim = 0;
        int n = tweaks.size();

        double[] mid = new double[n];
        //double[] sigma = new double[n];
        double[] min = new double[n];
        double[] max = new double[n];
        double[] inc = new double[n];
        double[] range = new double[n];


        for (Tweak w : tweaks) {
            //w.apply.value((float) point[i++], x);
            TweakFloat s = (TweakFloat) w;
            mid[dim] = (s.getMax() + s.getMin()) / 2f;
            min[dim] = (s.getMin());
            max[dim] = (s.getMax());
            inc[dim] = s.getInc();
            range[dim] = max[dim] - min[dim];
            //sigma[i] = Math.abs(max[i] - min[i]) * 0.75f; //(s.getInc());
            dim++;
        }


        FasterList<DoubleObjectPair<double[]>> experiments = new FasterList();


        ObjectiveFunction func = new ObjectiveFunction(point -> {


            double score;
            try {
                double sum = 0;
                for (int r = 0; r < repeats; r++) {

                    X x = subject(point);

                    sum += eval.floatValueOf(x);

                }
                score = sum / repeats;
            } catch (Exception e) {
                logger.error("{} {} {}", this, point, e);
                score = Float.NEGATIVE_INFINITY;
            }


            if (trace)
                csv.out(ArrayUtils.add(point, score));
            //System.out.println(Joiner.on(",").join(Doubles.asList(point)) + ",\t" + score);

            experiments.add(pair(score, point));
            experimentIteration(point, score);
            return score;
        });


        if (trace) {
            csv = new CSVOutput(System.out, Stream.concat(tweaks.stream().map(t -> t.id), Stream.of("score")).toArray(String[]::new));
        }

        experimentStart();

//                pop size = (int) (16 * Math.round(Util.sqr(tweaks.size()))) /* estimate */,
//        CMAESOptimizer optim = new CMAESOptimizer(maxIterations, Double.NEGATIVE_INFINITY, true, 0,
//                1, new MersenneTwister(3), false, null);
//        PointValuePair r = optim.optimize(
//                new MaxEval(maxIterations), //<- ignored?
//                func,
//                GoalType.MAXIMIZE,
//                new SimpleBounds(min, max),
//                new InitialGuess(mid),
//                new CMAESOptimizer.Sigma(MathArrays.scale(1f, inc)),
//                new CMAESOptimizer.PopulationSize(populationSize)
//            );


        final int numIterpolationPoints = 3 * dim; //2 * dim + 1 + 1;
        BOBYQAOptimizer opt = new BOBYQAOptimizer(numIterpolationPoints,
                dim * 2.0,
                1.0E-3D);
        try {
            PointValuePair r = opt
                    .optimize(
                            MaxEval.unlimited(), //new MaxEval(maxIterations),
                            new MaxIter(maxIterations),
                            func,
                            GoalType.MAXIMIZE,
                            new SimpleBounds(min, max),
                            new InitialGuess(mid));
        } catch (Throwable t) {
            logger.info("{} {}", opt, t);
        }

//        PointValuePair r = new SimplexOptimizer(1e-10, 1e-30).optimize(
//                new MaxEval(maxIterations),
//                func,
//                GoalType.MAXIMIZE,
//                new InitialGuess(mid),
//                //new NelderMeadSimplex(inc)
//                new MultiDirectionalSimplex(inc)
//        );


        return new Result<>(experiments, eval, tweaks);


    }

    /** called before experiment starts */
    protected void experimentStart() {
    }

    /** called after each iteration */
    protected void experimentIteration(double[] point, double score) {
    }

    /**
     * builds an experiment subject (input)
     */
    private X subject(double[] point) {
        X x = subject.get();

        for (int i = 0, dim = point.length; i < dim; i++)
            tweaks.get(i).apply.value(x, (float) point[i]);

        return x;
    }

}
