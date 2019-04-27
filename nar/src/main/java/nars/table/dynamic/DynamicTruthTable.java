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
        if (a.term() == null)
            a.template(term); //use default concept term

        Task y = new DynTaskify(model, beliefOrGoal, a).result;
        if (y!=null)
            a.tryAccept(y);
    }



}














































