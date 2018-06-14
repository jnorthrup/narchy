package jcog.lab;

import jcog.lab.util.ExperimentRun;
import jcog.lab.util.Optimization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

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

    final Supplier<E> subj;

    final Map<String,Sensor<E, ?>> sensors = new ConcurrentHashMap<>();

    final Map<String, Var<E, ?>> vars = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(Lab.class);

    protected void add(Var<E,?> v) {
        if (vars.put(v.id, v)!=null)
            throw new UnsupportedOperationException(v.id + " already present in variables");
    }

    public Lab<E> discover() {
        VarDiscovery<E> d = new VarDiscovery<>(subj).discover();
        d.vars.forEach(this::add);
        return this;
    }


    public Lab(Supplier<E> subj) {
        this.subj = subj;

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

    public ExperimentRun<E> run(BiConsumer<E, ExperimentRun<E>> proc, List<Sensor<E, ?>> sensors) {
        return new ExperimentRun<>(proc, subj.get(), sensors);
    }

    /** score is an objective function that the optimization process tries to
     *  maximize.
     */
    public Optimization<E> optimize(Consumer<E> procedure, Goal<E> goal, List<Var<E, ?>> vars, List<Sensor<E, ?>> sensors,
                                    Optimization.OptimizationStrategy strategy) {

        if (vars.isEmpty())
            throw new UnsupportedOperationException("no Var's provided");

        return new Optimization<E>(subj, procedure, goal, vars, sensors, strategy);
    }

    /** defaults:
     *      optimizing using all ready variables
     *      the default optimization strategy and its parameters
     * @param procedure
     * @param goal
     * @param score
     */
    public Optimization<E> optimize(Consumer<E> procedure, Goal<E> goal) {
        return optimize(procedure, goal,
                vars.values().stream().filter(Var::ready).collect(toList()),
                sensors.values().stream().collect(toList()),
                newDefaultOptimizer());
    }

    private Optimization.OptimizationStrategy newDefaultOptimizer() {
        return new Optimization.SimplexOptimizationStrategy(64);
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


}
