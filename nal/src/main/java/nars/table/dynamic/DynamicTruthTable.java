package nars.table.dynamic;

import nars.Task;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.dynamic.AbstractDynamicTruth;
import nars.truth.dynamic.DynTaskify;
import org.jetbrains.annotations.Nullable;


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
    public final void match(Answer t) {

        if (t.template == null)
            t.template(term);

        Task tt = taskDynamic(t);
        if (tt != null)
            t.tryAccept(tt);
    }

    @Override
    public void sample(Answer m) {

    }

    /**
     * generates a dynamic matching task
     */
    @Nullable
    public Task taskDynamic(Answer a) {

        return DynTaskify.task(model, beliefOrGoal, a);

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














































