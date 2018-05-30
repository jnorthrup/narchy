package nars.unify.op;

import nars.$;
import nars.Op;
import nars.derive.premise.PreDerivation;
import nars.term.control.AbstractPred;


/**
 * Created by me on 5/19/17.
 */
public final class TaskBeliefOp extends AbstractPred<PreDerivation> {
    public final int structure;
    public final boolean task;
    public final boolean belief;
    public final boolean isOrIsNot;

    public TaskBeliefOp(Op op, boolean testTask, boolean testBelief) {
        this(op.bit, testTask, testBelief, true);
    }

    public TaskBeliefOp(Op op, boolean testTask, boolean testBelief, boolean isOrIsNot) {
        this(op.bit, testTask, testBelief, isOrIsNot);
    }

    public TaskBeliefOp(int structure, boolean testTask, boolean testBelief, boolean isOrIsNot) {
        super($.func("op", $.the(structure), $.the(testTask ? 1 : 0), $.the(testBelief ? 1 : 0)).negIf(!isOrIsNot));
        this.isOrIsNot = isOrIsNot;
        this.structure = structure;
        this.task = testTask;
        this.belief = testBelief;
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    @Override
    public boolean test(PreDerivation derivation) {

        return (!task || (isOrIsNot ?
                        (((1<<derivation._taskOp) & structure) > 0) :
                        (((1<<derivation._taskOp) & structure) == 0)))
               &&
               (!belief || (isOrIsNot ?
                       (((1<<derivation._beliefOp) & structure) > 0) :
                       (((1<<derivation._beliefOp) & structure) == 0)));


    }

























































}
