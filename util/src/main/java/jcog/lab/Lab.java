package jcog.lab;

import jcog.io.arff.ARFF;
import jcog.list.FasterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * the Lab is essentially an ExperimentBuilder.
 * it is a mutable representation of the state necessary to
 * "compile" individual experiment instances, and such
 * is the entry point to performing them.
 *
 * @param X subject type
 * @param E experiment type. may be the same as X in some cases but other times
 *          there is a reason to separate the subject from the experiment
 * */
public class Lab<E> {

    final Supplier<E> subjectBuilder;

    final Map<String,Sensor<E, ?>> sensors = new ConcurrentHashMap<>();

    final Map<String, Tweak<E, ?>> vars = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(Lab.class);

    /**
     *  an instance of an experiment, conducting the experiment, collecting data as it runs;
     *  the integration of a subject, a repeatable procedure, and measurement schema
     *
     *  contains:
     *     -all or some of the Lab's sensors
     *     -executable procedure for applying the starting conditions to the subject via
     *      some or all of the variables
     *     -executable schedule for recording sensor measurements, with at least
     *      the start and ending state enabled by default. TODO
     */
    public static class Trial<E> implements Runnable {

        static final String blankContext = " ";

        final E experiment;
        private final BiConsumer<E, Trial<E>> procedure;

        /** data specific to this experiment; can be merged with multi-experiment
         * data collections later */
        public final ARFF data = new ARFF();

        /** enabled sensors */
        private final List<Sensor<E,?>> sensors = new FasterList();
        private long startTime;
        private long startNano;
        private long endTime;

        public Trial(BiConsumer<E,Trial<E>> procedure, E model, Iterable<Sensor<E,?>> sensors) {
            this.experiment = model;
            this.procedure = procedure;
            data.setComment(experiment + ": " + procedure);
            data.defineNumeric("time");
            data.defineText("context");

            sensors.forEach(s -> {
                this.sensors.add(s);
                s.addToSchema(data);
            });
        }

        @Override public void run() {
            startTime = System.currentTimeMillis();
            startNano = System.nanoTime();

            sense("start");

            try {
                procedure.accept(experiment, this);
            } catch (Throwable t) {
                sense(t.getMessage());
            }

            endTime = System.currentTimeMillis();

            sense("end");

        }

        /** records all sensors (blank context)*/
        public void sense() {

            sense(blankContext);
        }

        public void sense(String context) {
            synchronized (experiment) {
                long whenNano = System.nanoTime();

                Object row[] = new Object[sensors.size() + 2];
                int c = 0;
                row[c++] = whenNano - startNano;
                row[c++] = context;
                for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
                    row[c++] = sensors.get(i).apply(experiment);
                }
                data.add(row);
            }
        }
    }



    public Lab(Supplier<E> subjectBuilder) {
        this.subjectBuilder = subjectBuilder;

//        final float autoInc_default = 5f;
//        Map<String, Float> hints = Map.of("autoInc", autoInc_default);
//
//        Pair<List<Tweak<X, ?>>, SortedSet<String>> uu = vars.get(hints);
//        this.vars = uu.getOne();
//        SortedSet<String> unknown = uu.getTwo();
//        if (this.vars.isEmpty()) {
//            throw new RuntimeException("tweaks not ready:\n" + Joiner.on('\n').join(unknown));
//        }
//        if (!unknown.isEmpty()) {
//            for (String w : unknown) {
//                logger.warn("unknown: {}", w);
//            }
//        }
//
//
//        data.defineNumeric("score");
//        this.vars.forEach(t -> t.defineIn(data));
//
//        if (sensors.length > 1) {
//
//            for (Sensor o : sensors)
//                o.addToSchema(data);
//        }
    }

//    public Trial<E> get(Function<X, E> model, String... sensors) {
//        List<Sensor<E, ?>> s = Stream.of(sensors).map(this.sensors::get).collect(toList());
//        return get(model, s);
//    }

    public Lab.Trial<E> get(BiConsumer<E,Trial<E>> proc, List<Sensor<E, ?>> sensors) {
        return new Trial<>(proc, subjectBuilder.get(), sensors);
    }


//    public Result run(int iterations) {
//        return run(iterations, 1);
//    }
//    public Result run(int iterations, int repeats) {
//        return run(iterations, repeats, Executors.newSingleThreadExecutor());
//    }
//
//    public Result run(int iterations, int repeats, ExecutorService exe) {
//        return run(iterations, repeats, exe::submit);
//    }
//
//    public Result run(int iterations, int repeats, Function<Callable,Future> exe) {
//        return new Optimize<>(subjectBuilder, vars).run(data, iterations, repeats, experimentBuilder, sensors, exe);
//    }
//
//    public void saveOnShutdown(String file) {
//        Runtime.getRuntime().addShutdownHook(new Thread(()->{
//            try {
//                data.writeToFile(file);
//                System.out.println("saved " + data.data.size() + " experiment results to: " + file);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }));

//    }

//    /** result = t in tweaks(subject) { eval(subject + tweak(t)) } */
//    public static class Result implements Serializable {
//
//        public final ARFF data;
//
//        public Result(ARFF data) {
//            this.data = data;
//        }
//
//        public ImmutableList best() {
//            double bestScore = Double.NEGATIVE_INFINITY;
//            ImmutableList best = null;
//            for (ImmutableList e : data.data) {
//                double s = ((Number) e.get(0)).doubleValue();
//                if (s > bestScore) {
//                    best = e;
//                    bestScore = s;
//                }
//            }
//            return best;
//        }
//
//        public void print() {
//            data.print();
//        }
//
//        public RealDecisionTree tree(int discretization, int maxDepth) {
//            return data.isEmpty() ? null :
//                new RealDecisionTree(data.toFloatTable(),
//                    0 /* score */, maxDepth, discretization);
//        }
//
//
//        /** remove entries below a given percentile */
//        public void cull(float minPct, float maxPct) {
//
//            int n = data.data.size();
//            if (n < 6)
//                return;
//
//            Quantiler q = new Quantiler((int) Math.ceil((n-1)/2f));
//            data.forEach(r -> {
//                q.add( ((Number)r.get(0)).floatValue() );
//            });
//            float minValue = q.quantile(minPct);
//            float maxValue = q.quantile(maxPct);
//            data.data.removeIf(r -> {
//                float v = ((Number) r.get(0)).floatValue();
//                return (v <= maxValue && v >= minValue);
//            });
//        }
//
//        public List<DecisionTree> forest(int discretization, int maxDepth) {
//            if (data.isEmpty())
//                return null;
//
//            List<DecisionTree> l = new FasterList();
//            int attrCount = data.attrCount();
//            for (int i = 1; i < attrCount; i++) {
//                l.add(
//                        new RealDecisionTree(data.toFloatTable(0, i),
//                                0 /* score */, maxDepth, discretization));
//            }
//            return l;
//        }
//    }
}
