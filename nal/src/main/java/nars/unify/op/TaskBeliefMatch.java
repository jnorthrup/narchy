package nars.unify.op;

import jcog.TODO;
import nars.$;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;

import java.util.Collection;


/**
 * Created by me on 5/19/17.
 */
public final class TaskBeliefMatch extends AbstractPred<Derivation> {
    public final boolean task;
    public final boolean belief;

    public final boolean trueOrFalse;
    public final boolean exactOrSuper;

    public final TermMatch match;

    public TaskBeliefMatch(TermMatch match, boolean testTask, boolean testBelief, boolean trueOrFalse, boolean exactOrSuper) {
        super($.func(Atomic.the(match.getClass().getSimpleName()), match.param(), testTask ? Derivation.Task : Derivation.Belief).negIf(!trueOrFalse));

        if (testTask == testBelief)
            throw new TODO("easy to impl");

        this.match = match;
        this.trueOrFalse = trueOrFalse;
        this.task = testTask;
        this.belief = testBelief;
        this.exactOrSuper = exactOrSuper;
    }


    @Override
    public float cost() {
        return 0.1f;
    }

    @Override
    public boolean test(Derivation d) {
        return (!task || (testTarget(d.taskTerm) ==trueOrFalse))
               &&
               (!belief || (testTarget(d.beliefTerm) ==trueOrFalse));
    }

    final boolean testTarget(Term target) {
        return exactOrSuper ? match.test(target) : match.testSuper(target);
    }

    public static void pre(Collection<PrediTerm> pre, byte[] pt, byte[] pb, TermMatch m, boolean isOrIsnt) {
        if (pt != null)
            pre.add(pre(true, pt, m, isOrIsnt));
        if (pb != null)
            pre.add(pre(false, pb, m, isOrIsnt));
    }

    public static void preSuper(Collection<PrediTerm> pre, boolean taskOrBelief, TermMatch m, boolean trueOrFalse) {
        pre.add(new TaskBeliefMatch(m, taskOrBelief, !taskOrBelief, trueOrFalse, false));
    }

    private static PrediTerm<Derivation> pre(boolean taskOrBelief, byte[] path, TermMatch m, boolean isOrIsnt) {
        if (path.length == 0) {
            //root
            return new TaskBeliefMatch(m, taskOrBelief, !taskOrBelief, isOrIsnt, true);
        } else {
            //subterm
            return new SubtermMatch(isOrIsnt, m,
                    taskOrBelief ? path : null, taskOrBelief ? null : path);
        }
    }

}
