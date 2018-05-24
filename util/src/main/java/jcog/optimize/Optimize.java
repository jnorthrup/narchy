package jcog.optimize;

import com.google.common.base.Joiner;
import jcog.io.arff.ARFF;
import jcog.list.FasterList;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.MultiDirectionalSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.MathArrays;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Optimization solver wrapper w/ lambdas
 * instance of an experiment
 */
public class Optimize<X> {

    /**
     * if a tweak's 'inc' (increment) is not provided,
     * use the known max/min range divided by this value as 'inc'
     * <p>
     * this controls the exploration rate
     */
    static final float autoInc_default = 5f;
    private final static Logger logger = LoggerFactory.getLogger(Optimize.class);
    final List<Tweak<X, ?>> tweaks;
    final Supplier<X> subject;

    protected Optimize(Supplier<X> subject, Tweaks<X> t) {
        this(subject, t, Map.of("autoInc", autoInc_default));
    }

    protected Optimize(Supplier<X> subject, Tweaks<X> t, Map<String, Float> hints) {
        Pair<List<Tweak<X, ?>>, SortedSet<String>> uu = t.get(hints);
        List<Tweak<X, ?>> ready = uu.getOne();
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


    /**
     * TODO support evaluator that collects data during execution, and return score as one data field
     *
     * @param data
     * @param maxIterations
     * @param repeats
     * @param seeks
     * @param exe
     * @return
     */
    public Result<X> run(final ARFF data, int maxIterations, int repeats,
                         //FloatFunction<Supplier<X>> eval,
                         Optimizing.Optimal<X, ?>[] seeks,
                         ExecutorService exe) {


        assert (repeats >= 1);


        final int numTweaks = tweaks.size();

        double[] mid = new double[numTweaks];
        //double[] sigma = new double[n];
        double[] min = new double[numTweaks];
        double[] max = new double[numTweaks];
        double[] inc = new double[numTweaks];
//        double[] range = new double[dim];

        X example = subject.get();
        int i = 0;
        for (Tweak w : tweaks) {
            TweakFloat s = (TweakFloat) w;

            //initial guess: get from sample, otherwise midpoint of min/max range
            Object guess = s.get(example);
            mid[i] = guess != null ? ((float) guess) : ((s.getMax() + s.getMin()) / 2f);

            min[i] = (s.getMin());
            max[i] = (s.getMax());
            inc[i] = s.getInc();
//            range[i] = max[i] - min[i];
            //sigma[i] = Math.abs(max[i] - min[i]) * 0.75f; //(s.getInc());
            i++;
        }


        ObjectiveFunction func = new ObjectiveFunction(point -> {

            double score;

            double sum = 0;

            Supplier<X> x = () -> Optimize.this.subject(point);


            CountDownLatch c = new CountDownLatch(repeats);
            List<Future<Map<String, Object>>> each = new FasterList(repeats);
            for (int r = 0; r < repeats; r++) {
                Future<Map<String, Object>> ee = exe.submit(() -> {
                    try {
                        X y = x.get();

                        float subScore = 0;
                        Map<String, Object> e = new LinkedHashMap(seeks.length); //lhm to maintain seek order
                        for (Optimizing.Optimal<X, ?> o : seeks) {
                            ObjectFloatPair<?> xy = o.eval(y);
                            e.put(o.id, xy.getOne());
                            subScore += xy.getTwo();
                        }

                        e.put("_", subScore); //HACK

                        return e;

                    } catch (Exception e) {
                        logger.error("{} {} {}", Optimize.this, point, e);
                        return null;
                    } finally {
                        c.countDown();
                    }
                });
                each.add(ee);
            }

            try {
                c.await();
            } catch (InterruptedException e) {
                logger.error("interrupted waiting {}", e);
                return Float.NEGATIVE_INFINITY;
            }
            int numSeeks = seeks.length;

            DoubleArrayList seekMean = new DoubleArrayList(numSeeks);
            for (int si = 0; si < numSeeks; si++)
                seekMean.add(0);


            int numResults = 0;
            //TODO variance? etc

            for (Future<Map<String, Object>> y : each) {

                try {
                    Map<String, Object> ee = y.get();
                    if (ee != null) {
                        //iterated in LHM-presered order
                        int j = 0;
                        for (Map.Entry<String, Object> entry : ee.entrySet()) {
                            String k = entry.getKey();
                            Object v = entry.getValue();
                            if (k.equals("_")) {
                                sum += (float) v;
                            } else {
                                seekMean.addAtIndex(j++, (float) v);
                            }
                        }
                        assert (j == numSeeks);

                        numResults++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (numResults == 0)
                return Float.NEGATIVE_INFINITY;


            //TODO interplate and store the detected features

            score = sum / numResults;


//            if (trace)
//                csv.out(ArrayUtils.add(point, 0, score));
            FasterList p = new FasterList(numTweaks + numSeeks + 1);

            p.add(score);

            for (double pp : point)
                p.add(pp);

            if (numSeeks > 1) {
                //dont repeat a score column other than the master score if single objective

                for (int si = 0; si < numSeeks; si++) {
                    p.add(seekMean.get(si) / numResults);
                }
            }

            data.add(p.toImmutable());

            return score;
        });


//        if (trace)
//            csv = new CSVOutput(System.out, Stream.concat(
//                    Stream.of("score"), tweaks.stream().map(t -> t.id)
//            ).toArray(String[]::new));


        experimentStart();

        try {
            solve(numTweaks, func, mid, min, max, inc, maxIterations);
        } catch (Throwable t) {
            logger.info("solve {} {}", func, t);
        }

        return new Result<>(data, tweaks);
    }

    void solve(int dim, ObjectiveFunction func, double[] mid, double[] min, double[] max, double[] inc, int maxIterations) {
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

            int popSize =
                    //4 + 3 ln(n)
                    (int) Math.ceil(4 + 3 * Math.log(tweaks.size()));


            double[] sigma = MathArrays.scale(1f, inc);

            MyCMAESOptimizer m = new MyCMAESOptimizer(maxIterations, Double.NaN,
                    true, 0,
                    1, new MersenneTwister(System.nanoTime()),
                    true, null, popSize, sigma);
            m.optimize(
                    func,
                    GoalType.MAXIMIZE,
                    new MaxEval(maxIterations),
                    new SimpleBounds(min, max),
                    new InitialGuess(mid)
            );
            //m.print(System.out);

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
     * builds an experiment subject (input)
     * TODO handle non-numeric point entries
     */
    private X subject(double[] point) {
        X x = subject.get();

        for (int i = 0, dim = point.length; i < dim; i++) {
            point[i] = ((Tweak<X, Float>) tweaks.get(i)).set(x, (float) point[i]);
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
