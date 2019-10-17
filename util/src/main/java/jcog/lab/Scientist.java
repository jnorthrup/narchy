package jcog.lab;

import jcog.Paper;

import java.util.List;

/** virtual scientist model: designs experiments and analyzes results */
@Paper
public abstract class Scientist<S,E> {
    protected List<Goal<E>> goals;
    protected List<Var<S, ?>> vars;
    protected List<Sensor<E, ?>> sensors;

    /** initialize the start of a set of experiments */
    public void start(List<Goal<E>> goals, List<Var<S, ?>> vars, List<Sensor<E, ?>> sensors) {
        this.goals = goals;
        this.vars = vars;
        this.sensors = sensors;
    }

    /** TODO generic BooleanSupplier stop condition which could be according to iterations, realtime, or other */
    @Deprecated
    public abstract int experimentIterations();

    /** select a goal for the next experiment */
    public abstract Goal<E> goals();

    /** select the subset of variables for the next experiment */
    public abstract List<Var<S, ?>> vars();

    /** select the subset of sensors for the next experiment */
    public abstract List<Sensor<E, ?>> sensors();

    public abstract void analyze(Optimize<S,E> results);
}
