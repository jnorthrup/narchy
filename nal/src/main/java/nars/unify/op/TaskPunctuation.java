package nars.unify.op;

import nars.$;
import nars.derive.premise.PreDerivation;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;

import static nars.Op.*;

/**
 * Created by me on 8/27/15.
 */
final public class TaskPunctuation extends AbstractPred<PreDerivation> {

    public final byte punc;

    TaskPunctuation(byte p) {
        this(p, INH.the($.quote((char) p), $.the("task")));
    }

    TaskPunctuation(byte p, Term id) {
        super(id);
        this.punc = p;
    }


    @Override
    public final boolean test(PreDerivation m) {
        return m.taskPunc == punc;
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    public static final PrediTerm<PreDerivation> Belief = new TaskPunctuation(BELIEF);
    public static final PrediTerm<PreDerivation> Goal = new TaskPunctuation(GOAL);

    public static final PrediTerm<PreDerivation> BeliefOrGoal = new AbstractPred<PreDerivation>($.inh($.quote(".!"), $.the("task"))) {
        @Override
        public boolean test(PreDerivation o) {
            byte c = o.taskPunc;
            return c == BELIEF || c == GOAL;
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    };


    public static final PrediTerm<PreDerivation> Question = new TaskPunctuation(QUESTION);

    public static final PrediTerm<PreDerivation> Quest = new TaskPunctuation(QUEST);


}
