package nars.task.proxy;

import nars.NAL;
import nars.Task;
import nars.task.ProxyTask;
import nars.task.util.TaskException;
import nars.term.Neg;
import nars.term.Term;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;

/**
 * accepts a separate target as a facade to replace the apparent content target of
 * a proxied task
 */
public class SpecialTermTask extends ProxyTask {

    private final Term term;

    public SpecialTermTask(Term term, Task task) {
        super(/*task.getClass() == SpecialTermTask.class ? //but not subclasses!
                ((SpecialTermTask) task).task  //unwrap to core
                :*/
                task);

        if (NAL.DEBUG) {
            @Nullable ObjectBooleanPair<Term> z = Task.tryTaskTerm(term, task.punc(), false);
            if (z.getTwo())
                throw new TaskException(term, "SpecialTermTask does not support NEG target"); //use Task.withContent it will unwrap neg
            this.term = z.getOne();
        } else {
            assert(!(term instanceof Neg));
            this.term = term;
        }

    }


    public static Task the(Task task, Term t, boolean setCyclic) {
        if (task.term().equals(t)) return task;

        if (task.getClass() == SpecialTermTask.class /* but not subclasses! */) {
            SpecialTermTask et = (SpecialTermTask) task;
            task = et.task;
        }

        boolean negated = t instanceof Neg;
        if (negated) {
            t = t.unneg();
            if (task.isBeliefOrGoal())
                return new SpecialPuncTermAndTruthTask(t, task.punc(), task.truth().neg(), task).cyclicIf(setCyclic);
        }

        return new SpecialTermTask(t, task).cyclicIf(setCyclic);
    }

    @Override
    public final Term term() {
        return term;
    }

}
