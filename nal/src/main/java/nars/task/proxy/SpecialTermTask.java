package nars.task.proxy;

import nars.Task;
import nars.task.ProxyTask;
import nars.task.util.TaskException;
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
        super(task.getClass() == SpecialTermTask.class ? //but not subclasses!
                ((SpecialTermTask) task).task  //unwrap to core
                :
                task);



        @Nullable ObjectBooleanPair<Term> z = Task.tryContent(term, task.punc(), false);
        if (z.getTwo())
            throw new TaskException(term, "SpecialTermTask does not support NEG target"); //use Task.withContent it will unwrap neg
        this.term = z.getOne();

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
