package jcog.lab.util;

import jcog.TODO;
import jcog.io.arff.ARFF;
import jcog.list.FasterList;
import jcog.math.Quantiler;
import org.eclipse.collections.api.list.ImmutableList;
import org.intelligentjava.machinelearning.decisiontree.DecisionTree;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

import java.util.List;

public class Optimization<E> extends ExperimentSeries<E> {

    public final ARFF data = new ARFF();

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

    public List<DecisionTree> forest(int discretization, int maxDepth) {
        if (data.isEmpty())
            return null;

        List<DecisionTree> l = new FasterList();
        int attrCount = data.attrCount();
        for (int i = 1; i < attrCount; i++) {
            l.add(
                    new RealDecisionTree(data.toFloatTable(0, i),
                            0 /* score */, maxDepth, discretization));
        }
        return l;
    }

    @Override
    protected ExperimentRun<E> next() {
        throw new TODO();
    }

    abstract public static class OptimizationStrategy {

    }

    public static class SimplexOptimizationStrategy extends OptimizationStrategy {
        //TODO
    }

    public static class CMAESOptimizationStrategy extends OptimizationStrategy {
        //TODO
    }

    public static class GPOptimizationStrategy extends OptimizationStrategy {
        //TODO
    }
}

//package jcog.lab;
//
//import jcog.io.arff.ARFF;
//import jcog.list.FasterList;
//import jcog.lab.var.TweakFloat;
//import jcog.lab.util.MyCMAESOptimizer;
//import org.apache.commons.math3.exception.TooManyEvaluationsException;
//import org.apache.commons.math3.optim.InitialGuess;
//import org.apache.commons.math3.optim.MaxEval;
//import org.apache.commons.math3.optim.SimpleBounds;
//import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
//import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
//import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.MultiDirectionalSimplex;
//import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
//import org.apache.commons.math3.random.MersenneTwister;
//import org.apache.commons.math3.util.MathArrays;
//import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
//import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Callable;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.Future;
//import java.util.function.Function;
//import java.util.function.Supplier;
//
///**
// * Optimization solver wrapper w/ lambdas
// * instance of an experiment
// */
//public class Optimize<X> {
//
//    /**
//     * if a tweak's 'inc' (increment) is not provided,
//     * use the known max/min range divided by this value as 'inc'
//     * <p>
//     * this controls the exploration rate
//     */
//    private final static Logger logger = LoggerFactory.getLogger(Optimize.class);
//    final List<Tweak<X, ?>> tweaks;
//    final Supplier<X> subject;
//
//    protected Optimize(Supplier<X> subject,  List<Tweak<X, ?>> t) {
//        this.subject = subject;
//        this.tweaks = t;
//    }
//
//
//    /**
//     * TODO support evaluator that collects data during execution, and return score as one data field
//     *
//     * @param data
//     * @param maxIterations
//     * @param repeats
//     * @param seeks
//     * @param exe
//     * @return
//     */
//    public <Y> Lab.Result run(final ARFF data, int maxIterations, int repeats,
//
//                              Function<X,Y> experiment,
//                              Sensor<Y, ?>[] seeks,
//                              Function<Callable,Future> exe) {
//
//
//        assert (repeats >= 1);
//
//
//        final int numTweaks = tweaks.size();
//
//        double[] mid = new double[numTweaks];
//
//        double[] min = new double[numTweaks];
//        double[] max = new double[numTweaks];
//        double[] inc = new double[numTweaks];
//
//
//        X example = subject.get();
//        int i = 0;
//        for (Tweak w : tweaks) {
//            TweakFloat s = (TweakFloat) w;
//
//
//            Object guess = s.get(example);
//            mid[i] = guess != null ? ((float) guess) : ((s.getMax() + s.getMin()) / 2f);
//
//            min[i] = (s.getMin());
//            max[i] = (s.getMax());
//            inc[i] = s.getInc();
//
//
//            i++;
//        }
//
//
//        ObjectiveFunction func = new ObjectiveFunction(point -> {
//
//            double score;
//
//            double sum = 0;
//
//            Supplier<X> x = () -> Optimize.this.subject(point);
//
//
//            CountDownLatch c = new CountDownLatch(repeats);
//            List<Future<Map<String, Object>>> each = new FasterList(repeats);
//            for (int r = 0; r < repeats; r++) {
//                Future<Map<String, Object>> ee = exe.apply(() -> {
//                    try {
//                        X xx = x.get();
//                        Y y = experiment.apply(xx);
//
//                        float subScore = 0;
//                        Map<String, Object> e = new LinkedHashMap(seeks.length);
//                        for (Sensor<Y, ?> o : seeks) {
//                            ObjectFloatPair<?> xy = o.apply(y);
//                            e.put(o.id, xy.getOne());
//                            subScore += xy.getTwo();
//                        }
//
//                        e.put("_", subScore);
//
//                        return e;
//
//                    } catch (Exception e) {
//                        logger.error("{} {} {}", Optimize.this, point, e);
//                        return null;
//                    } finally {
//                        c.countDown();
//                    }
//                });
//                each.add(ee);
//            }
//
//            try {
//                c.await();
//            } catch (InterruptedException e) {
//                logger.error("interrupted waiting {}", e);
//                return Float.NEGATIVE_INFINITY;
//            }
//            int numSeeks = seeks.length;
//
//            DoubleArrayList seekMean = new DoubleArrayList(numSeeks);
//            for (int si = 0; si < numSeeks; si++)
//                seekMean.add(0);
//
//
//            int numResults = 0;
//
//
//            for (Future<Map<String, Object>> y : each) {
//
//                try {
//                    Map<String, Object> ee = y.get();
//                    if (ee != null) {
//
//                        int j = 0;
//                        for (Map.Entry<String, Object> entry : ee.entrySet()) {
//                            String k = entry.getKey();
//                            Object v = entry.getValue();
//                            if (k.equals("_")) {
//                                sum += (float) v;
//                            } else {
//                                seekMean.addAtIndex(j++, (float) v);
//                            }
//                        }
//                        assert (j == numSeeks);
//
//                        numResults++;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//
//            if (numResults == 0)
//                return Float.NEGATIVE_INFINITY;
//
//
//
//
//            score = sum / numResults;
//
//
//
//
//            FasterList p = new FasterList(numTweaks + numSeeks + 1);
//
//            p.add(score);
//
//            for (double pp : point)
//                p.add(pp);
//
//            if (numSeeks > 1) {
//
//
//                for (int si = 0; si < numSeeks; si++) {
//                    p.add(seekMean.get(si) / numResults);
//                }
//            }
//
//            data.add(p.toImmutable());
//
//            return score;
//        });
//
//
//
//
//
//
//
//
//        experimentStart();
//
//        try {
//            solve(numTweaks, func, mid, min, max, inc, maxIterations);
//        } catch (Throwable t) {
//            logger.info("solve {} {}", func, t);
//        }
//
//        return new Lab.Result(data);
//    }
//
//    void solve(int dim, ObjectiveFunction func, double[] mid, double[] min, double[] max, double[] inc, int maxIterations) {
//        if (dim == 1) {
//
//            try {
//                new SimplexOptimizer(1e-10, 1e-30).optimize(
//                        new MaxEval(maxIterations),
//                        func,
//                        GoalType.MAXIMIZE,
//                        new InitialGuess(mid),
//
//                        new MultiDirectionalSimplex(inc)
//                );
//            } catch (TooManyEvaluationsException e) {
//
//            }
//        } else {
//
//            int popSize =
//
//                    (int) Math.ceil(4 + 3 * Math.log(tweaks.size()));
//
//
//            double[] sigma = MathArrays.scale(1f, inc);
//
//            MyCMAESOptimizer m = new MyCMAESOptimizer(maxIterations, Double.NaN,
//                    true, 0,
//                    1, new MersenneTwister(System.nanoTime()),
//                    true, null, popSize, sigma);
//            m.optimize(
//                    func,
//                    GoalType.MAXIMIZE,
//                    new MaxEval(maxIterations),
//                    new SimpleBounds(min, max),
//                    new InitialGuess(mid)
//            );
//
//
//
//
//
//
//
//
//
//
//
//
//        }
//
//    }
//
//    /**
//     * called before experiment starts
//     */
//    protected void experimentStart() {
//    }
//
//
//    /**
//     * builds an experiment subject (input)
//     * TODO handle non-numeric point entries
//     */
//    private X subject(double[] point) {
//        X x = subject.get();
//
//        for (int i = 0, dim = point.length; i < dim; i++) {
//            point[i] = ((Tweak<X, Float>) tweaks.get(i)).set(x, (float) point[i]);
//        }
//
//        return x;
//    }
//
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
