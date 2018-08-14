package nars.task.proxy;

import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * accepts replacement truth and occurrence time for a proxied task
 */
public class SpecialTruthAndOccurrenceTask extends SpecialOccurrenceTask {

    private final boolean negatedContentTerm;

    /**
     * either Truth, Function<Task,Truth>, or null
     */
    private final Truth truth;

    public SpecialTruthAndOccurrenceTask(Task task, long start, long end, boolean negatedContentTerm, Truth truth) {
        super(task, start, end);
        this.negatedContentTerm = negatedContentTerm;

        this.truth = truth;
    }

    @Override
    public Term term() {
        return super.term().negIf(negatedContentTerm);
    }


    @Override
    public @Nullable Truth truth() {
        return truth;
    }

}
