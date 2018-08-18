package nars.table.dynamic;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.table.EmptyBeliefTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.Truth;

import java.util.function.Predicate;

/** does not store tasks but only generates them on query */
public abstract class DynamicTaskTable extends EmptyBeliefTable {

    final boolean beliefOrGoal;

    protected final Term term;

    protected DynamicTaskTable(Term c, boolean beliefOrGoal) {
        this.beliefOrGoal = beliefOrGoal;
        this.term = c;
    }

    /** this is very important:  even if size==0 this must return false */
    @Override public final boolean isEmpty() {
        return false;
    }

    @Override
    public final void match(Answer t) {
        if (t.template == null)
            t.template(term);
        t.accept(taskDynamic(t));
    }


    /**
     * generates a dynamic matching task
     */
    protected abstract Task taskDynamic(Answer a);

    /**
     * generates a dynamic matching truth
     */
    protected abstract Truth truthDynamic(long start, long end, Term template, Predicate<Task> filter, NAR nar);


    protected final byte punc() {
        return beliefOrGoal ? Op.BELIEF : Op.GOAL;
    }

}
