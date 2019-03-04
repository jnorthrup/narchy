package nars.task;

import nars.Task;
import nars.derive.Derivation;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;


/**
 * TODO extend an ImmutableTask class
 */
public class DerivedTask extends GenericNALTask {

    

    public DerivedTask(Term tc, byte punct, @Nullable Truth truth, long start, long end, Derivation d) {
        super(tc, punct, truth, d.time, start, end, d.concSingle ? d.evidenceSingle() : d.evidenceDouble());
    }

    @Override
    public final boolean isInput() {
        return false;
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











































































































































