package nars.task.proxy;

import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * accepts replacement truth and occurrence time for a proxied task
 */
public class SpecialTruthAndOccurrenceTask extends SpecialOccurrenceTask {

    public static Task the(Task t, Truth tr, long start, long end) {
        if (equivalent(t, tr, start, end))
            return t; //unchanged
        else {
            if (t instanceof SpecialTruthAndOccurrenceTask) {
                t = ((SpecialTruthAndOccurrenceTask) t).task; //unwrap
                if (equivalent(t, tr, start, end))
                    return t; //check again
            }

            return new SpecialTruthAndOccurrenceTask(t, tr, start, end);
        }
    }

    public static boolean equivalent(Task t, Truth tr, long start, long end) {
        return t.start()==start && t.end() == end && t.truth().equals(tr);
    }

    private final boolean negatedContentTerm;

    /**
     * either Truth, Function<Task,Truth>, or null
     */
    private final Truth truth;

    private SpecialTruthAndOccurrenceTask(Task task, Truth truth, long start, long end) {
        this(task, truth, start, end, false);
    }

    private SpecialTruthAndOccurrenceTask(Task task, Truth truth, long start, long end, boolean negatedContentTerm) {
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
