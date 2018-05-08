package nars.task.proxy;

import nars.Task;
import nars.task.TaskProxy;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** accepts replacement truth and occurrence time for a proxied task */
public class TaskWithTruthAndOccurrence extends TaskProxy {

    public final long start, end;

    private final boolean negatedContentTerm;

    /** either Truth, Function<Task,Truth>, or null */
    final Truth truth;

    public TaskWithTruthAndOccurrence(Task task, long start, long end, boolean negatedContentTerm, Truth truth) {
        super(task);
        this.start = start;
        this.end = end;
        this.negatedContentTerm = negatedContentTerm;

        this.truth = truth;
    }

    @Override
    public Term term() {
        return super.term().negIf(negatedContentTerm);
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }


    @Override
    public @Nullable Truth truth() {
        return truth;
//            Object tt = this.truthCached;
//
//            if (tt instanceof Function) {
//                Truth computed = ((Function<Task,Truth>) tt).apply(task);
//                if (computed != null) {
//                    if (negated) {
//                        computed = computed.neg();
//                    }
//                    this.truthCached = computed;
//                    return computed;
//                } else {
//                    this.truthCached = null;
//                    return null;
//                }
//            }
//            return (Truth) tt;
    }

}
