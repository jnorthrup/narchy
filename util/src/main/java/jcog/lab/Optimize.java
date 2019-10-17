package jcog.lab;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.lab.util.MyCMAESOptimizer;
import jcog.lab.var.FloatVar;
import jcog.learn.decision.RealDecisionTree;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.table.ARFF;
import jcog.table.DataTable;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.util.MathArrays;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Row;

import java.io.PrintStream;
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
 *
 *   goal (score) is in column 0 and it assumed to be maximized. for minimized, negate model's score function
 *
 * TODO - max timeout parameter w/ timer that kills if time exceeded
 *
 * https://ax.dev/docs/core.html
 */
public class Optimize<S, E> extends Lab<E>  {

    static final private Logger logger = LoggerFactory.getLogger(Optimize.class);


    //static final int goalColumn = 0;
    //private final static Logger logger = LoggerFactory.getLogger(Optimization.class);

    /**
     * history of experiments. TODO use ranking like TopN etc
     */
    public DataTable data;

    private final Supplier<S> subj;
    public final List<Var<S, ?>> var;
    private final Function<Supplier<S>, E> experiment;
    public final Goal<E> goal;
    private final FasterList<Sensor<E, ?>> sensors = new FasterList();

    private double[] inc;
    private double[] min;
    private double[] max;
    private double[] mid;
    private final List<Sensor<S, ?>> varSensors;

    public Optimize(Supplier<S> subj,
                    Function<Supplier<S>, E> experiment, Goal<E> goal,
                    List<Var<S, ?>> _vars /* unsorted */,
                    List<Sensor<E, ?>> sensors) {
        this.subj = subj;

        this.goal = goal;

        this.var = new FasterList(_vars).sortThis();

        this.varSensors = var/* sorted */.stream().map(Var::sense).collect(toList());

        this.sensors.addAll( sensors );
        this.sensors.sortThis();


        this.experiment = experiment;

        this.data = new ARFF();
    }

    public static <X> FloatFunction<X> repeat(FloatFunction<X> f, int repeats) {
        return (x) -> {
            double sum = 0;
            for (int i = 0; i < repeats; i++) {
                float y = f.floatValueOf(x);
                if (!Float.isFinite(y))
                    return y;
                sum += y;
            }
            return (float) (sum / repeats);
        };
    }

    @Override
    public Optimize<S,E> sense(Sensor sensor) {
        super.sense(sensor);
        sensors.add(sensor); //HACK
        return this;
    }

    public Optimize<S,E> runSync(int maxIters) {
        return runSync(newDefaultOptimizer(maxIters));
    }

    public Optimize<S,E> runSync(OptimizationStrategy strategy) {
        run(strategy);
        return this;
    }

    public void run(OptimizationStrategy strategy) {

        //initialize numeric or numeric-able variables
        final int numVars = var.size();

        mid = new double[numVars];
        min = new double[numVars];
        max = new double[numVars];
        inc = new double[numVars];

        S example = subj.get();
        int i = 0;
        for (Var w: var) {
            FloatVar s = (FloatVar) w;


            Object guess = s.get(example);


            double mi = min[i] = s.getMin();
            double ma = max[i] = s.getMax();
            double inc = this.inc[i] = s.getInc();

            if (guess!=null && (mi!=mi || ma!=ma || inc!=inc)) {
                float x = (float)guess;
                //HACK assumption
                mi = min[i] = x/2;
                ma = max[i] = x*2;
                inc = this.inc[i] = x/4;
            }

            mid[i] = guess != null ? Util.clamp((float) guess, mi, ma) : (mi + ma) / 2f;

            if (!(mid[i] >= min[i]))
                throw new WTF();
            if (!(max[i] >= mid[i]))
                throw new WTF();

            i++;
        }


        goal.addToSchema(data);
        varSensors.forEach(s -> s.addToSchema(data));
        sensors.forEach(s -> s.addToSchema(data));

        if (logger.isTraceEnabled()) {
            String s = data.columnNames().toString();
            logger.trace("{}", s.substring(1, s.length()-1));
        }

        strategy.run(this);

        finish();
    }

    protected void finish() {
        //sort data
        //((FasterList<ImmutableList>)((ArrayHashSet<ImmutableList>)data.data).list).sortThisByDouble(r -> -((Double)r.get(goalColumn)));
        data = new DataTable(data.sortDescendingOn(data.column(0).name()));
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
            E y = experiment.apply(() -> {
                S s = subject(subj.get(), point);
                copy[0] = s; //for measurement
                return s;
            });

            double score = goal.apply(y).doubleValue();

            Object[] row = row(copy[0], y, score);

            if (logger.isTraceEnabled()) {
                String rs = Arrays.toString(row);
                logger.trace("{}", rs.substring(1, rs.length()-1));
            }

            data.add(row);
            return score;

        } catch (Throwable t) {
            //System.err.println(t.getMessage());
            /** enable to print exceptions */
            boolean debug = false;
            if (debug)
                t.printStackTrace();
            return Double.NEGATIVE_INFINITY;
        }

    }

    private Object[] row(Object o, E y, double score) {
        Object[] row = new Object[1 + var.size() + sensors.size()];
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
            point[i] = ((Var<S, Float>) var.get(i)).set(x, (float) point[i]);
        }

        return x;
    }

    public Row best() {
        //assuming it's sorted
        return data.iterator().next();
    }

    public Optimize<S, E> print() {
        return print(System.out);
    }

    public Optimize<S, E> print(PrintStream out) {
        out.println(data.print());
        return this;
    }

    public RealDecisionTree tree(int discretization, int maxDepth) {
        return Optimize.tree(data, discretization, maxDepth);
    }

    public static RealDecisionTree tree(DataTable data, int discretization, int maxDepth) {
        return data.isEmpty() ? null :
                new RealDecisionTree(data.toFloatTable(),
                        0 /* score */, maxDepth, discretization);
    }



    /** string representing the variables manipulated in this experiment */
    public String varKey() {
        return var.toString();
    }



//    /**
//     * remove entries below a given percentile
//     */
//    public void cull(float minPct, float maxPct) {
//
//        int n = data.data.size();
//        if (n < 6)
//            return;
//
//        Quantiler q = new Quantiler((int) Math.ceil((n - 1) / 2f));
//        data.forEach(r -> {
//            q.addAt(((Number) r.get(0)).floatValue());
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
//            l.addAt(
//                    new RealDecisionTree(data.toFloatTable(0, i),
//                            0 /* score */, maxDepth, discretization));
//        }
//        return l;
//    }


    abstract public static class OptimizationStrategy {

        abstract public void run(Optimize eOptimize);
    }

    abstract public static class ApacheCommonsMathOptimizationStrategy extends OptimizationStrategy {


        @Override
        public void run(Optimize o) {
            run(o, new ObjectiveFunction(o::run));
        }

        abstract protected void run(Optimize o, ObjectiveFunction func);
    }

    public static class SimplexOptimizationStrategy extends ApacheCommonsMathOptimizationStrategy {
        private final int maxIter;

        public SimplexOptimizationStrategy(int maxIter) {
            this.maxIter = maxIter;
        }

        @Override
        protected void run(Optimize o, ObjectiveFunction func) {

            /** cache the points, becaues if integers are involved, it could confuse the simplex solver when it makes duplicate samples */
            //TODO discretization must be applied correctly here for this to work
//            func = new ObjectiveFunction( cache( func.getObjectiveFunction() ) );

            try {
                int dim = o.inc.length;
                //double[] range = new double[dim];
                double[] step = new double[dim];
                double[] init = new double[dim];
                for (int i = 0; i < dim; i++) {
                    double min = o.min[i];
                    double max = o.max[i];
                    double range = max - min;
                    step[i] = range / o.inc[i];
                    init[i] = (Math.random() * range) + min;
                }


                new SimplexOptimizer(new SimpleValueChecker(1e-10, 1e-30, maxIter))
                    .optimize(
                        new MaxEval(maxIter),
                        func,
                        GoalType.MAXIMIZE,
                        new InitialGuess(init),
                        //new MultiDirectionalSimplex(steps)
                        new NelderMeadSimplex(step)
                );
            } catch (TooManyEvaluationsException e) {
                e.printStackTrace();
            }
        }

//        private static MultivariateFunction cache(MultivariateFunction objectiveFunction) {
//            return new MultivariateFunction() {
//
//                final ObjectDoubleHashMap<ImmutableDoubleList> map = new ObjectDoubleHashMap<>();
//
//                @Override
//                public double value(double[] point) {
//                    return map.getIfAbsentPutWithKey(DoubleLists.immutable.of(point),
//                            p-> objectiveFunction.value(p.toArray()));
//                }
//            };
//
//        }

    }

    public static class CMAESOptimizationStrategy extends ApacheCommonsMathOptimizationStrategy {
        private final int maxIter;

        public CMAESOptimizationStrategy(int maxIter) {
            this.maxIter = maxIter;
        }

        @Override
        protected void run(Optimize o, ObjectiveFunction func) {

            int popSize =
                    (int) Math.ceil(4 + 3 * Math.log(o.var.size()));


            double[] sigma = MathArrays.scale(1f, o.inc);

            MyCMAESOptimizer m = new MyCMAESOptimizer(maxIter, Double.NaN,
                    true, 0,
                    1, new XoRoShiRo128PlusRandom(), //new MersenneTwister(System.nanoTime())
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
