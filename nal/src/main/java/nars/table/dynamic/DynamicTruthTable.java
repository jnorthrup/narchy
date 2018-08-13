package nars.table.dynamic;

import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.dynamic.DynTruth;
import nars.truth.dynamic.DynamicTruthModel;
import org.jetbrains.annotations.Nullable;


/**
 * computes dynamic truth according to implicit truth functions
 * determined by recursive evaluation of the compound's sub-component's truths
 */
public class DynamicTruthTable extends DynamicTaskTable {

    private final DynamicTruthModel model;


    public DynamicTruthTable(Term c, DynamicTruthModel model, boolean beliefOrGoal) {
        super(c, beliefOrGoal);
        this.model = model;
    }


    @Override
    @Nullable public Task taskDynamic(long start, long end, Term template, NAR nar) {
        if (template == null)
            template = term;

        DynTruth yy = model.eval(template, beliefOrGoal, start, end, true /* dont force projection */, nar);

        if (yy!=null) {
            Truth t = model.apply(yy, nar);
            if (t != null) {
                Term tmplate = template;
                return (Task)yy.eval(() -> model.reconstruct(tmplate, yy), t, true, beliefOrGoal, nar);
            }
        }
        return null;
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














































