package nars.task;

import jcog.WTF;
import nars.Task;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;

/** task which bypasses the evaluation procedure on input.
 *  this is faster but also necessary when
 *  something is specified in the task that evaluation
 *  otherwise would un-do.
 */
public class UnevaluatedTask extends TemporalTask {

    public UnevaluatedTask(Term t, byte punct, Truth truth, long creation, long start, long end, long[] stamp) {
        super(t, punct, truth, creation, start, end,
                stamp /* TODO use an implementation which doenst need an array for this */);
    }

    public UnevaluatedTask(Term c, Task parent, Truth t) throws TaskException {
        super(parent, c, t);
    }

    @Override
    @Deprecated public Task next(Object n) {
        /* no evaluation */
        //return Remember.the(this, n);
        throw new WTF();
    }
}
