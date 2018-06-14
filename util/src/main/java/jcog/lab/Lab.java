package jcog.lab;

import jcog.TODO;
import jcog.lab.util.ExperimentRun;
import jcog.lab.util.Optimization;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
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

    final Supplier<E> subjectBuilder;

    final Map<String,Sensor<E, ?>> sensors = new ConcurrentHashMap<>();

    final Map<String, Var<E, ?>> vars = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(Lab.class);

    public Lab discover() {

        return this;
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

    public ExperimentRun<E> run(BiConsumer<E, ExperimentRun<E>> proc, List<Sensor<E, ?>> sensors) {
        return new ExperimentRun<>(proc, subjectBuilder.get(), sensors);
    }

    /** score is an objective function that the optimization process tries to
     *  maximize.
     */
    public Optimization<E> optimize(FloatFunction<E> score, Optimization.OptimizationStrategy strategy, List<Var<E,?>> vars) {

        if (vars.isEmpty())
            throw new UnsupportedOperationException("no Var's provided");

        throw new TODO();
    }

    /** defaults:
     *      optimizing using all ready variables
     *      the default optimization strategy and its parameters
     */
    public Optimization<E> optimize(FloatFunction<E> score) {
        return optimize(score, newDefaultOptimizer(), vars.values().stream().filter(Var::ready).collect(toList()));
    }

    private Optimization.OptimizationStrategy newDefaultOptimizer() {
        throw new TODO();
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
