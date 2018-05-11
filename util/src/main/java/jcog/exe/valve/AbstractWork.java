package jcog.exe.valve;

abstract public class AbstractWork<Who,What> extends Share<Who,What> implements Work {

    protected final Demand<Who, What> demand;

    /** construction ends with attempted "delivery" (insertion) to the provided Customer */
    public AbstractWork(Demand<Who, What> where, What what, float initialNeed) {
        super(where.id, what);
        need(initialNeed);
        this.demand = where;
    }
}
