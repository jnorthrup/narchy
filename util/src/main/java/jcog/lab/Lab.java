package jcog.lab;

import jcog.io.arff.ARFF;
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
import java.util.function.Function;
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
public class Lab<S> {

    private final static Logger logger = LoggerFactory.getLogger(Lab.class);
    final Supplier<S> subj;
    final Map<String, Sensor<S,?>> sensors = new ConcurrentHashMap<>();
    public final Map<String, Var<S, ?>> vars = new ConcurrentHashMap<>();
    final Map<String, Object> hints = new HashMap();

    public Lab(Supplier<S> subj) {
        this.subj = subj;

        final float autoInc_default = 5f;
        hints.put("autoInc", autoInc_default);
    }

    /**
     * records all sensors ()
     */
    public static <X> Object[] record(X x, ARFF data, List<Sensor<X, ?>> sensors) {
        synchronized (data) {
            Object[] row = row(x, sensors);
            data.add(row);
            return row;
        }
    }

    public static <X> Object[] rowVars(X x, List<Var<X, ?>> vars) {
        List<Sensor<X, ?>> sensors = new FasterList(vars.size());
        for (int i = 0; i < vars.size(); i++)
            sensors.add(vars.get(i).sense());
        return row(x, sensors);
    }

    public static <X> Object[] row(X x, List<Sensor<X, ?>> sensors) {
        Object row[] = new Object[sensors.size()];
        int c = 0;
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            row[c++] = sensors.get(i).apply(x);
        }
        return row;
    }

    protected void add(Var<S, ?> v) {
        if (vars.put(v.id, v) != null)
            throw new UnsupportedOperationException(v.id + " already present in variables");
    }

    public Lab<S> discover() {
        Variables<S> d = new Variables<>(subj, vars).discover();
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
        for (Var<S, ?> t: vars.values()) {
            List<String> u = t.unknown(hints);
            if (!u.isEmpty())
                unknowns.addAll(u);
        }
        return unknowns;
    }

    public ExperimentRun<S> run(Iterable<Sensor<?, ?>> sensors, BiConsumer<S, ExperimentRun<S>> proc) {
        return new ExperimentRun<S>(subj.get(), sensors, proc);
    }

    /**
     * score is an objective function that the optimization process tries to
     * maximize.
     */
    public static <S,E> Optimization<S,E> optimize(Supplier<S> subj, List<Var<S, ?>> vars, List<Sensor<E, ?>> sensors, Optimization.OptimizationStrategy strategy,
                                    Function<Supplier<S>,E>  procedure, Goal<E> goal) {

        if (vars.isEmpty())
            throw new UnsupportedOperationException("no Var's provided");

        return new Optimization<>(subj, procedure, goal, vars, sensors, strategy);
    }

    /**
     * defaults:
     * optimizing using all ready variables
     * the default optimization strategy and its parameters
     *  @param score
     * @param procedure
     * @param goal
     */
    public Optimization<S, S> optimize(Consumer<S> procedure, Goal<S> goal) {
        return optimize(subj, vars.values().stream().filter(Var::ready).collect(toList()),
                new FasterList<>(sensors.values()), newDefaultOptimizer(), (s -> {
                    S ss = s.get();
                    procedure.accept(ss);
                    return ss;
                }), goal);
    }

    public <X> Optimization<S,X> optimize(Function<Supplier<S>,X> procedure, Goal<X> goal) {
        return optimize(subj, vars.values().stream().filter(Var::ready).collect(toList()),
                List.of(), newDefaultOptimizer(), procedure,
                goal);
    }

    public Optimization<S, S> optimize(Goal<S> goal) {
        return optimize(e->{}, goal);
    }

    /**
     * simple creation method
     */
    public Optimization<S, S> optimize(Consumer<S> procedure, FloatFunction<S> goal) {
        return optimize(procedure, new Goal(goal));
    }

    public <E> Optimization<S, E> optimize(Function<Supplier<S>, E> procedure, FloatFunction<E> goal) {
        return optimize(procedure, new Goal(goal));
    }

    private Optimization.OptimizationStrategy newDefaultOptimizer() {
        return new Optimization.SimplexOptimizationStrategy(128);
    }

    public Variables<S> var() {
        return new Variables<>(subj, vars);
    }
}
