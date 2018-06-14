package jcog.lab;

import jcog.lab.util.ExperimentRun;
import jcog.lab.util.Optimization;
import jcog.list.FasterList;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
 */
public class Lab<E> {

    private final static Logger logger = LoggerFactory.getLogger(Lab.class);
    final Supplier<E> subj;
    final Map<String, Sensor<E, ?>> sensors = new ConcurrentHashMap<>();
    final Map<String, Var<E, ?>> vars = new ConcurrentHashMap<>();
    final Map<String, Object> hints = new HashMap();

    public Lab(Supplier<E> subj) {
        this.subj = subj;

        final float autoInc_default = 5f;
        hints.put("autoInc", autoInc_default);
    }

    protected void add(Var<E, ?> v) {
        if (vars.put(v.id, v) != null)
            throw new UnsupportedOperationException(v.id + " already present in variables");
    }

    public Lab<E> discover() {
        VarDiscovery<E> d = new VarDiscovery<>(subj).discover();
        d.vars.forEach(this::add);
        hints.putAll(d.hints);


        SortedSet<String> unknown = validate();

        if (!unknown.isEmpty()) {
            for (String w: unknown) {
                logger.warn("unknown: {}", w);
            }
        }

//        if (this.vars.isEmpty()) {
//            throw new RuntimeException("tweaks not ready:\n" + Joiner.on('\n').join(unknown));
//        }


        return this;
    }

    private TreeSet<String> validate() {

//        Map<String, Float> h;
//        if (!this.hints.isEmpty()) {
//            if (additionalHints.isEmpty()) {
//                h = this.hints;
//            } else {
//
//                h = new HashMap();
//                h.putAll(this.hints);
//                h.putAll(additionalHints);
//            }
//        } else {
//            h = additionalHints;
//        }
//
//
        TreeSet<String> unknowns = new TreeSet<>();
        for (Var<E, ?> t: vars.values()) {
            List<String> u = t.unknown(hints);
            if (!u.isEmpty())
                unknowns.addAll(u);
        }
        return unknowns;
    }

    public ExperimentRun<E> run(Iterable<Sensor<E, ?>> sensors, BiConsumer<E, ExperimentRun<E>> proc) {
        return new ExperimentRun<E>(subj.get(), sensors, proc);
    }

    /**
     * score is an objective function that the optimization process tries to
     * maximize.
     */
    public Optimization<E> optimize(Goal<E> goal, List<Var<E, ?>> vars, List<Sensor<E, ?>> sensors, Optimization.OptimizationStrategy strategy, Consumer<E> procedure) {

        if (vars.isEmpty())
            throw new UnsupportedOperationException("no Var's provided");

        return new Optimization<E>(subj, procedure, goal, vars, sensors, strategy);
    }

    /**
     * defaults:
     * optimizing using all ready variables
     * the default optimization strategy and its parameters
     *
     * @param score
     * @param goal
     * @param procedure
     */
    public Optimization<E> optimize(Goal<E> goal, Consumer<E> procedure) {
        return optimize(goal,
                vars.values().stream().filter(Var::ready).collect(toList()),
                new FasterList<>(sensors.values()), newDefaultOptimizer(), procedure
        );
    }

    /**
     * simple creation method
     */
    public Optimization<E> optimize(FloatFunction<E> goal, Consumer<E> procedure) {
        return optimize(new Goal(goal), procedure);
    }

    private Optimization.OptimizationStrategy newDefaultOptimizer() {
        return new Optimization.SimplexOptimizationStrategy(64);
    }

}
