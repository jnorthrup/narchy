package nars.task.proxy;

import nars.Param;
import nars.Task;
import nars.task.TaskProxy;
import nars.task.util.TaskException;
import nars.term.Term;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;

/** accepts a separate target as a facade to replace the apparent content target of
 * a proxied task
  */
public class SpecialTermTask extends TaskProxy {

    private final Term term;

    public SpecialTermTask(Term term, Task task) {
        super(task.getClass() == SpecialTermTask.class ? //but not subclasses!
                ((SpecialTermTask)task).task  //unwrap to core
                :
                task);

        if (Param.DEBUG) {
            @Nullable ObjectBooleanPair<Term> z = Task.tryContent(term, task.punc(), false);
            this.term = z.getOne();
            if (z.getTwo())
                throw new TaskException(term, this + " can not support negated content target");
        } else {
            this.term = term;
        }

    }

    @Override
    protected boolean inheritCyclic() {
        return false;
    }

    @Override
    public Term term() {
        return term;
    }

}
