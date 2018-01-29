package jcog.optimize;

import jcog.list.FasterList;
import jcog.meter.event.CSVOutput;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.intelligentjava.machinelearning.decisiontree.FloatTable;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Optimization solver wrapper w/ lambdas
 */
public class Optimize<X> {

    final Supplier<X> subject;

    public final List<Tweak<X>> tweaks = new FasterList();

    private final boolean trace = true;
    private final static Logger logger = LoggerFactory.getLogger(Optimize.class);
    private CSVOutput csv;

    public Optimize(Supplier<X> subject) {
        this.subject = subject;
    }


    public /*@NotNull*/ Optimize<X> tweak(int min, int max, ObjectFloatProcedure<X> apply) {
        return tweak(min, max, 1, apply);
    }

    public /*@NotNull*/ Optimize<X> tweak(String key, int min, int max, int inc, ObjectIntProcedure<X> apply) {
        return tweak(key, min, max, inc, (ObjectFloatProcedure<X>)(X x, float v)->{
            apply.accept(x, Math.round(v));
        });
    }

//    public /*@NotNull*/ Optimize<X> tweak(int min, int max, int inc, ObjectFloatProcedure<X> apply) {
//        return tweak(apply.toString(), min, max, inc, apply);
//    }

    public /*@NotNull*/ Optimize<X> tweak(String key, int min, int max, ObjectFloatProcedure<X> apply) {
        return tweak(key, min, max, 1f, apply);
    }

    public /*@NotNull*/ Optimize<X> tweak(float min, float max, float inc, ObjectFloatProcedure<X> apply) {
        return tweak(apply.toString(), min, max, inc, apply);
    }

    public Optimize<X> tweak(String parameter, float min, float max, float inc, ObjectFloatProcedure<X> apply) {
        tweaks.add(new FloatRange<>(parameter, min, max, inc, apply));
        return this;
    }

//    public /*@NotNull*/ Optimize<X> tweak(String parameter, float min, float max, float inc, @NotNull String invoker) {
//        Map m = new HashMap(4);
//
//        tweaks.add(new FloatRange<>(parameter, min, max, inc, (v,x) -> {
//            //accessible by both these:
//            m.put(parameter, v);
//            m.put("x", v);
//
//            //integer rounded:
//            m.put("i", Math.round(v));
//
//            try {
//                Ognl.getValue(invoker, m, x);
//            } catch (OgnlException e) {
//                e.printStackTrace();
//            }
//        }));
//        return this;
//    }


    /*@NotNull*/
    public Result run(int maxIterations, FloatFunction<X> eval) {

        return run(
                maxIterations, 1, eval);
    }

    /*@NotNull*/
    public Result run(int maxIterations, int repeats, @NotNull FloatFunction<X> eval) {

        int i = 0;
        int n = tweaks.size();

        double[] mid = new double[n];
        //double[] sigma = new double[n];
        double[] min = new double[n];
        double[] max = new double[n];
        double[] inc = new double[n];
        double[] range = new double[n];

        for (Tweak w : tweaks) {
            //w.apply.value((float) point[i++], x);
            FloatRange s = (FloatRange) w;
            mid[i] = (s.getMax() + s.getMin()) / 2f;
            min[i] = (s.getMin());
            max[i] = (s.getMax());
            inc[i] = s.getInc();
            range[i] = max[i] - min[i];
            //sigma[i] = Math.abs(max[i] - min[i]) * 0.75f; //(s.getInc());
            i++;
        }


        List<DoubleObjectPair<double[]>> experiments = new FasterList();


        ObjectiveFunction func = new ObjectiveFunction(point -> {


            float score;
            try {
                float sum = 0;
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

            experiments.add(pair((double) score, point));
            onExperiment(point, score);
            return score;
        });


        startExperiments();

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

        int dim = tweaks.size();
        final int numIterpolationPoints = 3 * dim; //2 * dim + 1 + 1;
        PointValuePair r = new BOBYQAOptimizer(numIterpolationPoints,
                dim * 2.0,
                1.0E-3D)
                    .optimize(
                        MaxEval.unlimited(), //new MaxEval(maxIterations),
                        new MaxIter(maxIterations),
                        func,
                        GoalType.MAXIMIZE,
                        new SimpleBounds(min, max),
                        new InitialGuess(mid));

//        PointValuePair r = new SimplexOptimizer(1e-10, 1e-30).optimize(
//                new MaxEval(maxIterations),
//                func,
//                GoalType.MAXIMIZE,
//                new InitialGuess(mid),
//                //new NelderMeadSimplex(inc)
//                new MultiDirectionalSimplex(inc)
//        );


        return new Result(experiments, r);


    }

    private void startExperiments() {
        if (trace) {
            csv = new CSVOutput(System.out, Stream.concat(tweaks.stream().map(t -> t.id), Stream.of("score")).toArray(String[]::new));
        }
    }

    protected void onExperiment(double[] point, double score) {
    }

    /**
     * builds an experiment subject (input)
     */
    private X subject(double[] point) {
        X x = subject.get();
        int i1 = 0;
        for (int i = 0, tweaksSize = tweaks.size(); i < tweaksSize; i++) {
            tweaks.get(i).apply.value(x, (float) point[i1++]);
        }
        return x;
    }

    public class Result {

        public final PointValuePair optimal;
        public final List<DoubleObjectPair<double[]>> experiments;


        public Result(List<DoubleObjectPair<double[]>> experiments, PointValuePair r) {
            this.optimal = r;
            this.experiments = experiments;
        }

        public void print() {
            //System.out.println("optimal: " + optimal);
            System.out.println("score=" + optimal.getSecond());
            double[] p = optimal.getPoint();
            for (int i = 0; i < p.length; i++) {
                System.out.println(tweaks.get(i).id + ' ' + p[i]);
            }

        }

        public RealDecisionTree predict(int discretization, int maxDepth) {
            if (experiments.isEmpty())
                return null;


            FloatTable<String> data = new FloatTable<>(
                    ArrayUtils.add(
                            tweaks.stream().map(Tweak::toString).toArray(String[]::new), "score")
            );

            int cols = data.cols.length;

            for (DoubleObjectPair<double[]> exp : experiments) {
                float[] r = new float[cols];
                int i = 0;
                for (double x : exp.getTwo()) {
                    r[i++] = (float) x;
                }
                r[i] = (float) exp.getOne();
                data.add(r);
            }

            RealDecisionTree rt = new RealDecisionTree(data, cols - 1 /* score */, maxDepth, discretization);

            return rt;

        }
    }

    /**
     * a knob but cooler
     */
    public static class Tweak<X> {
        public final ObjectFloatProcedure<X> apply;
        private final String id;

        public Tweak(String id, ObjectFloatProcedure<X> apply) {
            this.id = id;
            this.apply = apply;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private static class FloatRange<X> extends Tweak<X> {
        private final String parameter;
        private final float min, max;
        private final float inc;

        private FloatRange(String parameter, float min, float max, float inc, ObjectFloatProcedure<X> apply) {
            super(parameter, apply);
            this.parameter = parameter;
            this.min = min;
            this.max = max;
            this.inc = inc;
        }

        public String getParameter() {
            return parameter;
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }

        public float getInc() {
            return inc;
        }


    }

}
