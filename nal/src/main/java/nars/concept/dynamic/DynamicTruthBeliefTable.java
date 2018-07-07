package nars.concept.dynamic;

import nars.NAR;
import nars.Task;
import nars.table.EternalTable;
import nars.table.TemporalBeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;


/**
 * computes dynamic truth according to implicit truth functions
 * determined by recursive evaluation of the compound's sub-component's truths
 */
public class DynamicTruthBeliefTable extends DynamicBeliefTable {

    private final DynamicTruthModel model;


    public DynamicTruthBeliefTable(Term c, EternalTable e, TemporalBeliefTable t, DynamicTruthModel model, boolean beliefOrGoal) {
        super(c, beliefOrGoal, e, t);
        this.model = model;
    }

    @Override
    public boolean isEmpty() {
        /** since this is a dynamic evaluation, we have to assume it is not empty */
        return false;
    }


    @Override
    protected Task taskDynamic(long start, long end, Term template, NAR nar) {

        if (template == null)
            template = term;

        DynTruth yy = model.eval(template, beliefOrGoal, start, end, true /* dont force projection */, nar);
        return yy != null ? yy.task(template, model, beliefOrGoal, nar) : null;
    }


    @Override
    protected @Nullable Truth truthDynamic(long start, long end, Term template, NAR nar) {

        if (template == null)
            template = term;

        DynTruth d = model.eval(template, beliefOrGoal, start, end,
                false /* force projection to the specific time */, nar);
        if (d != null)
            return d.truth(template, model, beliefOrGoal, nar);
        else
            return null;
    }


}














































