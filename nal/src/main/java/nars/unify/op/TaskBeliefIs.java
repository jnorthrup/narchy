package nars.unify.op;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;

import java.util.Collection;


/**
 * Created by me on 5/19/17.
 */
public final class TaskBeliefIs extends AbstractPred<Derivation> {
    public final int structure;
    public final boolean task;
    public final boolean belief;
    public final boolean isOrIsNot;

    public TaskBeliefIs(Op op, boolean testTask, boolean testBelief) {
        this(op.bit, testTask, testBelief, true);
    }

    final static private Atomic OP = Atomic.the("is");



    public TaskBeliefIs(int structure, boolean testTask, boolean testBelief, boolean isOrIsNot) {
        super($.func(OP, Op.strucTerm(structure), testTask ? Derivation.Task : Op.EmptyProduct, testBelief ? Derivation.Belief : Op.EmptyProduct)
                .negIf(!isOrIsNot));
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
    public boolean test(Derivation derivation) {

        return (!task || (isOrIsNot ?
                        (((1<<derivation._taskOp) & structure) > 0) :
                        (((1<<derivation._taskOp) & structure) == 0)))
               &&
               (!belief || (isOrIsNot ?
                       (((1<<derivation._beliefOp) & structure) > 0) :
                       (((1<<derivation._beliefOp) & structure) == 0)));


    }

    public static void add(Collection<PrediTerm> pres, boolean isOrIsnt, int struct, byte[] pt, byte[] pb) {
        if (pt != null) {
            pres.add(add(isOrIsnt, struct, true, pt));
        }
        if (pb != null) {
            pres.add(add(isOrIsnt, struct, false, pb));
        }
    }

    static PrediTerm<Derivation> add(boolean isOrIsnt, int struct, boolean taskOrBelief, byte[] path) {
        if (path.length > 0) {
            return new PathStructure(isOrIsnt, struct,
                    taskOrBelief ? path : null, taskOrBelief ? null : path);
        } else {
            return new TaskBeliefIs(struct, taskOrBelief, !taskOrBelief, isOrIsnt);
        }
    }

}
