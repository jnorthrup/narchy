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
            a.term(term); //use default concept term

        Task y = new DynTaskify(model, beliefOrGoal, a).eval(a.start, a.end);
        if (y!=null) {

//            if (a.storeDynamic)
//                a.test(y, DynamicTruthTable::cacheDynamic);
//            else
                a.test(y);

        }
    }

//    /** insertion resolver
//     *  if r.result!=y replace y in a with r.result to re-use cached Task */
//    private static Task cacheDynamic(Task y, Answer a) {
//        float pBefore = y.pri(); //HACK
//        Remember r = Remember.the(y, true, false, false, a.what);
//        if (r!=null && r.store(false)) {
//            Task z = r.result;
//            if (z != null && z != y)
//                return z;
//
//            if (y.isDeleted())
//                y.pri(pBefore); //HACK
//
//        }
//
//        return y;
//    }

}














































