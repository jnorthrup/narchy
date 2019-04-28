package nars.task;

import nars.Task;

/** task which bypasses the evaluation procedure on input.
 *  this is faster but also necessary when
 *  something is specified in the task that evaluation
 *  otherwise would un-do.
 */
public interface UnevaluatedTask extends Task {
//
//    public UnevaluatedTask(Term t, byte punct, Truth truth, long creation, long start, long end, long[] stamp) {
//        super(t, punct, truth, creation, start, end, stamp);
//    }
//
//    public UnevaluatedTask(Term c, Task parent, Truth t) throws TaskException {
//        this(c, parent.punc(), t, parent.creation(), parent.start(), parent.end(), parent.stamp());
//    }

}
