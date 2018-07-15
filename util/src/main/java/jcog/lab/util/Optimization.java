package jcog.lab.util;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.io.arff.ARFF;
import jcog.lab.Goal;
import jcog.lab.Lab;
import jcog.lab.Sensor;
import jcog.lab.Var;
import jcog.lab.var.FloatVar;
import jcog.math.Quantiler;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.MathArrays;
import org.eclipse.collections.api.list.ImmutableList;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * @param S subject of the experiment
 * @param E experiment containing the subject
 *          <p>
 *          in simple cases, S and E may be the same type
 */
public class Optimization<S, E> extends Lab<E> implements Runnable {

    static final int goalColumn = 0;
    private final static Logger logger = LoggerFactory.getLogger(Optimization.class);

    /**
     * history of experiments. TODO use ranking like TopN etc
     */
    public final ARFF data;

    private final Supplier<S> subj;
    private final List<Var<S, ?>> vars;
    private final Function<Supplier<S>, E> procedure;
    private final Goal<E> goal;
    private final List<Sensor<E, ?>> sensors = new FasterList();

    private final OptimizationStrategy strategy;
    private double[] inc;
    private double[] min;
    private double[] max;
    private double[] mid;
    private final List<Sensor<S, ?>> varSensors;

    public Optimization(Supplier<S> subj,
                        Function<Supplier<S>, E> procedure, Goal<E> goal,
                        List<Var<S, ?>> vars,
                        List<Sensor<E, ?>> sensors,
                        OptimizationStrategy strategy) {
        this.subj = subj;

        this.goal = goal;

        this.vars = vars;
        this.varSensors = vars.stream().map(Var::sense).collect(toList());

        this.sensors.addAll( sensors );


        this.procedure = procedure;

        this.strategy = strategy;

        this.data = new ARFF();
    }

    @Override
    public Optimization<S,E> sense(Sensor sensor) {
        super.sense(sensor);
        sensors.add(sensor); //HACK
        return this;
    }

    public Optimization<S,E> runSync() {
        run();
        return this;
    }

    @Override
    public void run() {

        //initialize numeric or numeric-able variables
        final int numVars = vars.size();

        mid = new double[numVars];
        min = new double[numVars];
        max = new double[numVars];
        inc = new double[numVars];

        S example = subj.get();
        int i = 0;
        for (Var w: vars) {
            FloatVar s = (FloatVar) w;


            Object guess = s.get(example);


            min[i] = s.getMin();
            max[i] = s.getMax();
            mid[i] = guess != null ? Util.clamp((float) guess, min[i], max[i]) : (max[i] + min[i]) / 2f;
            inc[i] = s.getInc();

            if (!(mid[i] >= min[i]))
                throw new WTF();
            if (!(max[i] >= mid[i]))
                throw new WTF();

            i++;
        }


        goal.addToSchema(data);
        varSensors.forEach(s -> s.addToSchema(data));
        sensors.forEach(s -> s.addToSchema(data));

        strategy.run(this);

        finish();
    }

    protected void finish() {
        //sort data
        ((FasterList<ImmutableList>)((ArrayHashSet<ImmutableList>)data.data).list).sortThisByDouble(r -> -((Double)r.get(goalColumn)));
    }

    protected double run(double[] point) {

        //logger.info("run: {}", Texts.n4(point));

        /**
         * the only or last produced copy of the experiment input.
         * since all generated subjects should be identical
         * it wont matter which one.
         */

        try {
            Object[] copy = new Object[1];
            E y = procedure.apply(() -> {
                S s = subject(subj.get(), point);
                copy[0] = s; //for measurement
                return s;
            });

            double score = goal.apply(y).doubleValue();

            Object[] row = row(copy[0], y, score);

            System.out.println(Arrays.toString(row));
            data.add(row);
            return score;

        } catch (Throwable t) {
            //System.err.println(t.getMessage());
            t.printStackTrace();
            return Double.NEGATIVE_INFINITY;
        }

    }

    private Object[] row(Object o, E y, double score) {
        Object[] row = new Object[1 + vars.size() + sensors.size()];
        int j = 0;
        row[j++] = score;
        S x = (S) o;
        for (Sensor v: varSensors)
            row[j++] = v.apply(x);
        for (Sensor s: sensors)
            row[j++] = s.apply(y);
        return row;
    }


    /**
     * builds an experiment subject (input)
     * TODO handle non-numeric point entries
     */
    private S subject(S x, double[] point) {


        for (int i = 0, dim = point.length; i < dim; i++) {
            point[i] = ((Var<S, Float>) vars.get(i)).set(x, (float) point[i]);
        }

        return x;
    }

    public ImmutableList best() {
        return data.maxBy(goalColumn);
    }

    public Optimization<S, E> print() {
        data.print();
        return this;
    }

    public RealDecisionTree tree(int discretization, int maxDepth) {
        return Optimization.tree(data, discretization, maxDepth);
    }

    public static RealDecisionTree tree(ARFF data, int discretization, int maxDepth) {
        return data.isEmpty() ? null :
                new RealDecisionTree(data.toFloatTable(),
                        0 /* score */, maxDepth, discretization);
    }


    /**
     * remove entries below a given percentile
     */
    public void cull(float minPct, float maxPct) {

        int n = data.data.size();
        if (n < 6)
            return;

        Quantiler q = new Quantiler((int) Math.ceil((n - 1) / 2f));
        data.forEach(r -> {
            q.add(((Number) r.get(0)).floatValue());
        });
        float minValue = q.quantile(minPct);
        float maxValue = q.quantile(maxPct);
        data.data.removeIf(r -> {
            float v = ((Number) r.get(0)).floatValue();
            return v <= maxValue && v >= minValue;
        });
    }

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


        @Override
        public void run(Optimization o) {
            run(o, new ObjectiveFunction(o::run));
        }

        abstract protected void run(Optimization o, ObjectiveFunction func);
    }

    public static class SimplexOptimizationStrategy extends ApacheCommonsMathOptimizationStrategy {
        private final int maxIter;

        public SimplexOptimizationStrategy(int maxIter) {
            this.maxIter = maxIter;
        }

        @Override
        protected void run(Optimization o, ObjectiveFunction func) {

            try {
                int dim = o.inc.length;
                double[] range = new double[dim];
                for (int i = 0; i < dim; i++)
                    range[i] = o.inc[i]; //(o.max[i] - o.min[i]);

                new SimplexOptimizer(1e-10, 1e-30).optimize(
                        new MaxEval(maxIter),
                        func,
                        GoalType.MAXIMIZE,
                        new InitialGuess(o.mid),
                        //new MultiDirectionalSimplex(steps)
                        new NelderMeadSimplex(range, 1.1f, 1.1f, 0.8f, 0.8f)
                );
            } catch (TooManyEvaluationsException e) {
                e.printStackTrace();
            }
        }

    }

    public static class CMAESOptimizationStrategy extends ApacheCommonsMathOptimizationStrategy {
        private final int maxIter;

        public CMAESOptimizationStrategy(int maxIter) {
            this.maxIter = maxIter;
        }

        @Override
        protected void run(Optimization o, ObjectiveFunction func) {

            int popSize =
                    (int) Math.ceil(4 + 3 * Math.log(o.vars.size()));


            double[] sigma = MathArrays.scale(1f, o.inc);

            MyCMAESOptimizer m = new MyCMAESOptimizer(maxIter, Double.NaN,
                    true, 0,
                    1, new MersenneTwister(System.nanoTime()),
                    true, null, popSize, sigma);
            m.optimize(
                    func,
                    GoalType.MAXIMIZE,
                    new MaxEval(maxIter),
                    new SimpleBounds(o.min, o.max),
                    new InitialGuess(o.mid)
            );


        }
    }


    //
//    public static class GPOptimizationStrategy extends OptimizationStrategy {
//        //TODO
//    }
}

//package jcog.lab;
//
//import jcog.io.arff.ARFF;
//import jcog.data.list.FasterList;
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
