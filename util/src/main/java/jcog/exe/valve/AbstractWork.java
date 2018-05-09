package jcog.exe.valve;

abstract public class AbstractWork<Who,What> extends Share<Who,What> implements Work {

    /** construction ends with attempted "delivery" (insertion) to the provided Customer */
    public AbstractWork(Valve.Customer<Who, What> where, What what, float initialNeed) {
        super(where.id, what);
        need(initialNeed);

        where.need(this);
    }
}
