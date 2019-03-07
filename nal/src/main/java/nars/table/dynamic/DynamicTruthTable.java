package nars.table.dynamic;

import nars.Task;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.dynamic.AbstractDynamicTruth;
import nars.truth.dynamic.DynTaskify;


/**
 * computes dynamic truth according to implicit truth functions
 * determined by recursive evaluation of the compound's sub-component's truths
 */
public final class DynamicTruthTable extends DynamicTaskTable {

    private final AbstractDynamicTruth model;

    public DynamicTruthTable(Term c, AbstractDynamicTruth model, boolean beliefOrGoal) {
        super(c, beliefOrGoal);
        this.model = model;
    }


    @Override
    public final void match(Answer a) {

        if (a.term == null)
            a.term = term;

        Task tt = DynTaskify.task(model, beliefOrGoal, a);
        if (tt != null)
            a.tryAccept(tt);
    }





//    @Override
//    protected Truth truthDynamic(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
//
//        DynStampTruth d = model.eval(template, beliefOrGoal, start, end, filter, true, nar);
//
//        return d != null ? d.truth(template, model, beliefOrGoal, nar) : null;
//
//    }


}














































