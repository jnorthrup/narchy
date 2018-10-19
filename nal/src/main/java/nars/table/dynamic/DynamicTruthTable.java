package nars.table.dynamic;

import jcog.WTF;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.dynamic.DynStampTruth;
import nars.truth.dynamic.DynamicTruthModel;
import org.jetbrains.annotations.Nullable;


/**
 * computes dynamic truth according to implicit truth functions
 * determined by recursive evaluation of the compound's sub-component's truths
 */
public final class DynamicTruthTable extends DynamicTaskTable {

    private final DynamicTruthModel model;

    public DynamicTruthTable(Term c, DynamicTruthModel model, boolean beliefOrGoal) {
        super(c, beliefOrGoal);
        this.model = model;
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
    @Nullable public Task taskDynamic(Answer a) {
        Term template = a.template;

        NAR nar = a.nar;

        //TODO allow use of time's specified intersect/contain mode
        DynStampTruth yy = model.eval(template, beliefOrGoal, a.time.start, a.time.end, a.filter, false , nar);
        if (yy == null)
            return null;

        Truth t = model.apply(yy, nar);
        if (t == null)
            return null;

        Term reconstruct = model.reconstruct(template, yy, nar);
        if (reconstruct == null) {
            if (Param.DEBUG)
                throw new WTF("could not reconstruct: " + template + ' ' +  yy);
            return null;
        }

        return yy.task(reconstruct, t, yy::stamp,  beliefOrGoal, nar);
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














































