package nars.derive.op;

import nars.$;
import nars.derive.PreDerivation;
import nars.term.pred.AbstractPred;
import nars.term.pred.PrediTerm;

/**
 * task truth is postiive
 */
abstract public class TaskPolarity extends AbstractPred<PreDerivation> {


//    public static final PrediTerm<ProtoDerivation> taskContainsBelief = new TaskPolarity("TaskContainsBelief") {
//        @Override
//        public boolean test(ProtoDerivation m) {
//            return m.taskTerm.contains(m.beliefTerm) || (m.taskTerm.hasAny(Op.NEG) && m.taskTerm.contains(m.beliefTerm.neg()));
//        }
//
//        @Override
//        public float cost() {
//            return 0.5f;
//        }
//    };
    public static final PrediTerm<PreDerivation> taskContainsBeliefRecursively = new TaskPolarity("TaskContainsBelief") {
        @Override
        public boolean test(PreDerivation m) {
            return m.taskTerm.containsRecursively(m.beliefTerm);
        }

        @Override
        public float cost() {
            return 0.75f;
        }
    };
    public static final PrediTerm<PreDerivation> taskPos = new TaskPolarity("TaskPos") {
        @Override
        public boolean test(PreDerivation m) {
            return m.taskPolarity==+1;
        }

    };
    public static final PrediTerm<PreDerivation> taskNeg = new TaskPolarity("TaskNeg") {
        @Override
        public boolean test(PreDerivation m) {
            return m.taskPolarity==-1;
        }
    };
    public static final PrediTerm<PreDerivation> beliefPos = new TaskPolarity("BeliefPos") {
        @Override
        public boolean test(PreDerivation m) {
            return m.beliefPolarity==+1;
        }
    };
    public static final PrediTerm<PreDerivation> beliefNeg = new TaskPolarity("BeliefNeg") {
        @Override
        public boolean test(PreDerivation m) {
            return m.beliefPolarity==-1;
        }
    };

    /** requires a double premise, excluding single */
    public static final PrediTerm<PreDerivation> belief = new TaskPolarity("Belief") {
        @Override
        public boolean test(PreDerivation m) {
            return m.beliefPolarity!=0;
        }
    };

    @Override
    public float cost() {
        return 0.1f;
    }

    protected TaskPolarity(String x) {
        super($.the(x));
    }
}
