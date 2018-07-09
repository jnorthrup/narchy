package nars.task.proxy;

import nars.Task;
import nars.task.TaskProxy;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** accepts replacement truth and occurrence time for a proxied task */
public class TaskWithTruthAndOccurrence extends TaskProxy {

    private final long start;
    private final long end;

    private final boolean negatedContentTerm;

    /** either Truth, Function<Task,Truth>, or null */
    private final Truth truth;

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
















    }

}
