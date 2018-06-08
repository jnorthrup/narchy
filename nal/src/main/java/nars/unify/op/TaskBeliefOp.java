package nars.unify.op;

import nars.$;
import nars.Op;
import nars.derive.premise.PreDerivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


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

    final static private Atomic OP = Atomic.the("op");

    public TaskBeliefOp(int structure, boolean testTask, boolean testBelief, boolean isOrIsNot) {
        super($.func(OP, $.the(structure), $.the(testTask ? 1 : 0), $.the(testBelief ? 1 : 0)).negIf(!isOrIsNot));
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

    public static void add(Collection<PrediTerm<PreDerivation>> pres, boolean isOrIsnt, Term x, int struct, byte[] pt, byte[] pb) {
        if (pt != null) {
            pres.add(add(isOrIsnt, x, struct, true, pt));
        }
        if (pb != null) {
            pres.add(add(isOrIsnt, x, struct, false, pb));
        }
    }

    static PrediTerm<PreDerivation> add(boolean isOrIsnt, Term x, int struct, boolean taskOrBelief, byte[] path) {
        if (path.length > 0) {
            return new OpInTaskOrBeliefIsOrIsnt(isOrIsnt, x, struct,
                    taskOrBelief ? path : null, taskOrBelief ? null : path);
        } else {
            return new TaskBeliefOp(struct, taskOrBelief, !taskOrBelief, isOrIsnt);
        }
    }

    public static class OpInTaskOrBeliefIsOrIsnt extends AbstractPred<PreDerivation> {

        private final int struct;

        @Nullable
        private final byte[] pathInTask;
        @Nullable
        private final byte[] pathInBelief;
        private final boolean isOrIsnt;

        private OpInTaskOrBeliefIsOrIsnt(boolean isOrIsnt, Term x, int struct, byte[] pathInTask, byte[] pathInBelief) {
            super($.func(isOrIsnt ? "opIs" : "opIsNot",
                    x, $.the(struct),
                    pathInTask != null ? $.func("inTask", $.the(pathInTask)) : Op.EmptyProduct,
                    pathInBelief != null ? $.func("inBelief", $.the(pathInBelief)) : Op.EmptyProduct
            ));
            this.isOrIsnt = isOrIsnt;
            this.struct = struct;
            this.pathInTask = pathInTask;
            this.pathInBelief = pathInBelief;
        }


        @Override
        public float cost() {
            return 0.2f;
        }

        @Override
        public boolean test(PreDerivation o) {
            if (pathInTask != null) {
                Term T = o.taskTerm;
                boolean Thas = T.hasAny(struct);
                if (isOrIsnt && !Thas)
                    return false;
                if (isOrIsnt || Thas) {
                    Term s = T.subPath(pathInTask);
                    if (isOrIsnt != s.isAny(struct))
                        return false;
                }
            }
            if (pathInBelief != null) {
                Term B = o.beliefTerm;
                boolean Bhas = B.hasAny(struct);
                if (isOrIsnt && !Bhas)
                    return false;
                if (isOrIsnt || Bhas) {
                    Term s = B.subPath(pathInBelief);
                    if (isOrIsnt != s.isAny(struct))
                        return false;
                }
            }
            return true;
        }
    }
}
