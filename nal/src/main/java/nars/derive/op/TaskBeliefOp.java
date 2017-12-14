package nars.derive.op;

import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.derive.AbstractPred;


/**
 * Created by me on 5/19/17.
 */
public final class TaskBeliefOp extends AbstractPred<Derivation> {
    private final byte op;
    private final boolean task;
    private final boolean belief;

    public TaskBeliefOp(Op op, boolean testTask, boolean testBelief) {
        super($.func("op", $.quote(op.str), $.the(testTask ? 1 : 0), $.the(testBelief ? 1 : 0)));
        this.op = op.id;
        this.task = testTask;
        this.belief = testBelief;
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    @Override
    public boolean test(Derivation derivation) {
        return (!task || derivation.termSub0op == op)
                &&
                (!belief || derivation.termSub1op == op);
    }

//    static boolean isSequence(int dt) {
//            return dt!=0 && dt!=DTERNAL && dt!=XTERNAL;
//        }

//    public static class TaskBeliefConjSeq extends AbstractPred<Derivation> {
//
//        private final boolean task;
//        private final boolean belief;
//
//        public TaskBeliefConjSeq(boolean testTask, boolean testBelief) {
//            super($.func("conjSeq", $.the(testTask ? 1 : 0), $.the(testBelief ? 1 : 0)));
//            this.task = testTask;
//            this.belief = testBelief;
//        }
//
//        @Override
//        public boolean test(Derivation derivation) {
//            if (task) {
//                if (!(derivation.termSub0op == CONJ.id && isSequence(derivation.taskTerm.dt())))
//                    return false;
//            }
//            if (belief) {
//                return derivation.belief != null && derivation.termSub1op == CONJ.id && isSequence(derivation.belief.term().dt());
//            }
//            return true;
//        }
//
//
//
//    }

//    public static class TaskBeliefConjComm extends AbstractPred<Derivation> {
//
//        private final boolean task;
//        private final boolean belief;
//
//        public TaskBeliefConjComm(boolean testTask, boolean testBelief) {
//            super($.func("conjComm", $.the(testTask ? 1 : 0), $.the(testBelief ? 1 : 0)));
//            this.task = testTask;
//            this.belief = testBelief;
//        }
//
//        @Override
//        public boolean test(Derivation derivation) {
//            if (task) {
//                if (!(derivation.termSub0op == CONJ.id && !isSequence(derivation.taskTerm.dt())))
//                    return false;
//            }
//            if (belief) {
//                return derivation.belief != null && derivation.termSub1op == CONJ.id && !isSequence(derivation.belief.term().dt());
//            }
//            return true;
//        }
//
//    }

}
