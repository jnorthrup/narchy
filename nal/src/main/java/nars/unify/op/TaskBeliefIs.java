package nars.unify.op;

import jcog.TODO;
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
    public final int struct;
    public final boolean task;
    public final boolean belief;
    public final boolean isOrIsnt;

    public TaskBeliefIs(Op op, boolean testTask, boolean testBelief) {
        this(op.bit, testTask, testBelief, true);
    }

    final static private Atomic is = Atomic.the("is");



    public TaskBeliefIs(int struct, boolean testTask, boolean testBelief, boolean isOrIsnt) {
        super($.func(is, Op.strucTerm(struct), testTask ? Derivation.Task : Derivation.Belief).negIf(!isOrIsnt));
        if (testTask == testBelief) throw new TODO("easy to impl");
        this.isOrIsnt = isOrIsnt;
        this.struct = struct;
        this.task = testTask;
        this.belief = testBelief;
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    @Override
    public boolean test(Derivation d) {
        return (!task || (isOrIsnt == (((1 << d._taskOp) & struct) != 0)))
               &&
               (!belief || (isOrIsnt == (((1 << d._beliefOp) & struct) != 0)));
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
        if (path.length == 0) {
            //root
            return new TaskBeliefIs(struct, taskOrBelief, !taskOrBelief, isOrIsnt);
        } else {
            //subterm
            return new SubtermIs(isOrIsnt, struct,
                    taskOrBelief ? path : null, taskOrBelief ? null : path);
        }
    }

}
