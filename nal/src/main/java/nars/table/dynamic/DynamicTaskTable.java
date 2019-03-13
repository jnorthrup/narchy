package nars.table.dynamic;

import nars.NAR;
import nars.Op;
import nars.control.op.Remember;
import nars.table.EmptyBeliefTable;
import nars.term.Term;

/** does not store tasks but only generates them on query */
public abstract class DynamicTaskTable extends EmptyBeliefTable {

    public final boolean beliefOrGoal;

    protected final Term term;

    protected DynamicTaskTable(Term c, boolean beliefOrGoal) {
        this.beliefOrGoal = beliefOrGoal;
        this.term = c;
    }

    @Override
    public void add(Remember r, NAR nar) {

    }

    /** this is very important:  even if size==0 this must return false */
    @Override public final boolean isEmpty() {
        return false;
    }

    protected final byte punc() {
        return beliefOrGoal ? Op.BELIEF : Op.GOAL;
    }

}
