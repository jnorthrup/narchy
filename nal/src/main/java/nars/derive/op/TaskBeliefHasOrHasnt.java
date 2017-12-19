package nars.derive.op;

import nars.$;
import nars.control.ProtoDerivation;
import nars.term.pred.AbstractPred;


/**
 * Created by me on 5/19/17.
 */
public final class TaskBeliefHasOrHasnt extends AbstractPred<ProtoDerivation> {
    private final int structure;
    private final boolean task;
    private final boolean belief;
    private final boolean includeOrExclude;

    public TaskBeliefHasOrHasnt(int structure, boolean testTask, boolean testBelief, boolean includeExclude) {
        super($.func((includeExclude ? "OpHas" : "opHasNot"), $.the(structure), $.the(testTask ? "1" : "0"), $.the(testBelief ? "1" : "0")));
        this.structure = structure;
        this.task = testTask;
        this.belief = testBelief;
        this.includeOrExclude = includeExclude;
    }

    @Override
    public boolean test(ProtoDerivation derivation) {
        return includeOrExclude ==
               ((!task || derivation.taskTerm.hasAny(structure))
                    &&
               (!belief || derivation.beliefTerm.hasAny(structure)));
    }

    @Override
    public float cost() {
        return 0.15f;
    }
}

