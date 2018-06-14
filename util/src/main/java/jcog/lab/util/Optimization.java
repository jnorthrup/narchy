package jcog.lab.util;

import jcog.io.arff.ARFF;
import jcog.lab.Goal;
import jcog.lab.Sensor;
import jcog.lab.Var;
import jcog.lab.var.FloatVar;
import jcog.list.FasterList;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.MultiDirectionalSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.eclipse.collections.api.list.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Optimization<E> extends ExperimentSeries<E> {

    private final static Logger logger = LoggerFactory.getLogger(Optimization.class);

    /** history of experiments. TODO use ranking like TopN etc */
    public final List<ARFF> data = new FasterList();

    private final Goal<E> goal;
    private final List<Sensor<E,?>> sensors;
    private final List<Var<E, ?>> vars;
    private final OptimizationStrategy strategy;
    private final Supplier<E> subj;

    private final Consumer<E> procedure;
    private final double[] inc, min, max, mid;
    private final FasterList<Sensor<E,?>> varsAndSensors;


    static final int timeColumn = 0;
    static final int ctxColumn = 1;
    static final int goalColumn = 2;

    public Optimization(Supplier<E> subj,
                        Consumer<E> procedure, Goal<E> goal,
                        List<Var<E, ?>> vars,
                        List<Sensor<E, ?>> sensors,
                        OptimizationStrategy strategy) {
        this.subj = subj;

        this.goal= goal;

        this.vars = vars;

        this.sensors = sensors;

        this.varsAndSensors = new FasterList<>();
        this.varsAndSensors.add(goal);
        for (Var v : vars) {
            varsAndSensors.add(v.sense());
        }
        this.varsAndSensors.addAll(sensors);


        this.procedure = procedure;
        assert !vars.isEmpty();
        this.strategy = strategy;

        {
            //initialize numeric or numeric-able variables
            final int numVars = vars.size();

            mid = new double[numVars];
            min = new double[numVars];
            max = new double[numVars];
            inc = new double[numVars];

            E example = subj.get();
            int i = 0;
            for (Var w: vars) {
                FloatVar s = (FloatVar) w;


                Object guess = s.get(example);
                mid[i] = guess != null ? (float) guess : (s.getMax() + s.getMin()) / 2f;

                min[i] = s.getMin();
                max[i] = s.getMax();
                inc[i] = s.getInc();

                i++;
            }
        }

    }

    @Override
    public void run() {
        strategy.run(this);
    }

    protected double run(double[] point) {
        ExperimentRun<E> ee = new ExperimentRun<>((e, r) -> {
            procedure.accept(e);
        }, Optimization.this.subject(point), varsAndSensors);

        ee.run();

        sense(ee);

        return goal.apply(ee.experiment);
    }


    /** collect results after an experiment has finished */
    protected void sense(ExperimentRun<E> next) {
        data.add(next.data);
    }

    /**
     * builds an experiment subject (input)
     * TODO handle non-numeric point entries
     */
    private E subject(double[] point) {
        E x = subj.get();

        for (int i = 0, dim = point.length; i < dim; i++) {
            point[i] = ((Var<E, Float>) vars.get(i)).set(x, (float) point[i]);
        }

        return x;
    }

    public ImmutableList best() {
        double bestScore = Double.NEGATIVE_INFINITY;
        ImmutableList best = null;
        for (ARFF ee : data) {
            ImmutableList e = ee.data.iterator().next(); //HACK
            double s = ((Number) e.get(goalColumn)).doubleValue();
            if (s > bestScore) {
                best = e;
                bestScore = s;
            }
        }
        return best;
    }

    public void print() {
        data.forEach(ARFF::print);
    }

//    public RealDecisionTree tree(int discretization, int maxDepth) {
//        return data.isEmpty() ? null :
//            new RealDecisionTree(data.toFloatTable(),
//                0 /* score */, maxDepth, discretization);
//    }


//    /** remove entries below a given percentile */
//    public void cull(float minPct, float maxPct) {
//
//        int n = data.data.size();
//        if (n < 6)
//            return;
//
//        Quantiler q = new Quantiler((int) Math.ceil((n-1)/2f));
//        data.forEach(r -> {
//            q.add( ((Number)r.get(0)).floatValue() );
//        });
//        float minValue = q.quantile(minPct);
//        float maxValue = q.quantile(maxPct);
//        data.data.removeIf(r -> {
//            float v = ((Number) r.get(0)).floatValue();
//            return v <= maxValue && v >= minValue;
//        });
//    }

//    public List<DecisionTree> forest(int discretization, int maxDepth) {
//        if (data.isEmpty())
//            return null;
//
//        List<DecisionTree> l = new FasterList();
//        int attrCount = data.attrCount();
//        for (int i = 1; i < attrCount; i++) {
//            l.add(
//                    new RealDecisionTree(data.toFloatTable(0, i),
//                            0 /* score */, maxDepth, discretization));
//        }
//        return l;
//    }


    abstract public static class OptimizationStrategy {

        abstract public void run(Optimization eOptimization);
    }

    abstract public static class ApacheCommonsMathOptimizationStrategy extends OptimizationStrategy {

        protected ObjectiveFunction func;
        protected Optimization o;

        @Override
        public void run(Optimization o) {
            this.func = new ObjectiveFunction(o::run);
            this.o = o;
            run();
        }

        abstract protected void run();
    }

    public static class SimplexOptimizationStrategy extends ApacheCommonsMathOptimizationStrategy {
        private final int maxIter;

        public SimplexOptimizationStrategy(int maxIter) {
            this.maxIter = maxIter;
        }

        @Override
        protected void run() {

            try {
                new SimplexOptimizer(1e-10, 1e-30).optimize(
                        new MaxEval(maxIter),
                        func,
                        GoalType.MAXIMIZE,
                        new InitialGuess(o.mid),
                        new MultiDirectionalSimplex(o.inc)
                );
            } catch (TooManyEvaluationsException e) {

            }
        }

    }

//    public static class CMAESOptimizationStrategy extends ApacheCommonsMathOptimizationStrategy {
//
//    }
//
//    public static class GPOptimizationStrategy extends OptimizationStrategy {
//        //TODO
//    }
}

//package jcog.lab;
//
//import jcog.io.arff.ARFF;
//import jcog.list.FasterList;
//import jcog.lab.var.VarFloat;
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
