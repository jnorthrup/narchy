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
    @Deprecated abstract public int experimentIterations();

    /** select a goal for the next experiment */
    abstract public Goal<E> goals();

    /** select the subset of variables for the next experiment */
    abstract public List<Var<S, ?>> vars();

    /** select the subset of sensors for the next experiment */
    abstract public List<Sensor<E, ?>> sensors();

    abstract public void analyze(Optimize<S,E> results);
}
