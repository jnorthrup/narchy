package nars.task.proxy;

import nars.Task;
import nars.task.ProxyTask;
import nars.task.util.TaskException;
import nars.term.Neg;
import nars.term.Term;

/**
 * accepts a separate target as a facade to replace the apparent content target of
 * a proxied task
 */
public class SpecialTermTask extends ProxyTask {

    private final Term term;

    SpecialTermTask(Term term, Task task) {
        super(/*task.getClass() == SpecialTermTask.class ? //but not subclasses!
                ((SpecialTermTask) task).task  //unwrap to core
                :*/
                task);

        this.term = term;
    }


    public static Task the(Task x, Term t, boolean copyCyclic) {
        if (!t.unneg().op().taskable)
            throw new TaskException("new content not taskable", t);

        if (x.term().equals(t)) return x;

        if (x.getClass() == SpecialTermTask.class /* but not subclasses! */) {
            SpecialTermTask et = (SpecialTermTask) x;
            x = et.task;
        }

        ProxyTask y;
        if (x.isBeliefOrGoal() && t instanceof Neg) {
            t = t.unneg();
            y = new SpecialPuncTermAndTruthTask(t, x.punc(), x.truth().neg(), x);
        } else {
            if (t instanceof Neg)
                throw new TaskException("SpecialTermTask does not support NEG target", t);
            y = new SpecialTermTask(t, x);
        }

        if (copyCyclic && x.isCyclic())
            y.setCyclic(true);

        return y;
    }

    @Override
    public final Term term() {
        return term;
    }

}
