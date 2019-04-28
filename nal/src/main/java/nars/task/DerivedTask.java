package nars.task;

import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;


/**
 * TODO extend an ImmutableTask class
 */
public class DerivedTask extends TemporalTask {

    public DerivedTask(Term tc, byte punct, @Nullable Truth truth, long now, long start, long end, long[] evi) {
        super(tc, punct, truth, now, start, end, evi);
    }


    /**
     * provided in DebugDerivedTask subclass
     */
    @Nullable
    public Task getParentTask() {
        return null;
    }

    /**
     * provided in DebugDerivedTask subclass
     */
    @Nullable
    public Task getParentBelief() {
        return null;
    }


}











































































































































