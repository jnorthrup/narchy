package nars.table.dynamic;

import nars.NAR;
import nars.Task;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.dynamic.DynStampTruth;
import nars.truth.dynamic.DynamicTruthModel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;


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

        return yy.task(model.reconstruct(template, yy), t, (r)->yy.stamp(r),  beliefOrGoal, nar);
    }


    @Override
    protected Truth truthDynamic(long start, long end, Term template, Predicate<Task> filter, NAR nar) {

        DynStampTruth d = model.eval(template, beliefOrGoal, start, end, filter, true, nar);

        return d != null ? d.truth(template, model, beliefOrGoal, nar) : null;

    }


}














































