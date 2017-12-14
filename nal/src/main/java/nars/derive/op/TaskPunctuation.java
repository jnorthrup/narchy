package nars.derive.op;

import nars.Op;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;

import static nars.Op.*;

/**
 * Created by me on 8/27/15.
 */
final public class TaskPunctuation extends AbstractPred<Derivation> {

    public final byte punc;

    TaskPunctuation(byte p) {
        this(p, "task:\"" + ((char) p) + '\"');
    }

    TaskPunctuation(byte p, String id) {
        super(id);
        this.punc = p;
    }


    @Override
    public final boolean test( Derivation m) {
        return m.taskPunct == punc;
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    public static final PrediTerm<Derivation> Belief = new TaskPunctuation(BELIEF);
    public static final PrediTerm<Derivation> Goal = new TaskPunctuation(GOAL);

    public static final PrediTerm<Derivation> BeliefOrGoal = new AbstractPred<Derivation>("task:\".!\"") {
        @Override
        public boolean test( Derivation o) {
            byte c = o.taskPunct;
            return c == BELIEF || c == GOAL;
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    };



    public static final PrediTerm<Derivation> Question = new TaskPunctuation(QUESTION);

    public static final PrediTerm<Derivation> Quest = new TaskPunctuation(QUEST);


}
