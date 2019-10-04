package nars.task;

import jcog.util.ArrayUtil;
import nars.Task;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import static nars.Op.COMMAND;

/** base task suitable only for command tasks.
 * no truth, no stamp, no creation, no occurrence, no cause.  hardcoded ';' puntuation
 */
public class AbstractCommandTask implements CommandTask {
    public final Term term;
    private final int hash;

    public AbstractCommandTask(Term term) {
        this.hash = Task.hash(
                this.term = term,
                null,
                COMMAND,
                TIMELESS,
                TIMELESS,
                ArrayUtil.EMPTY_LONG_ARRAY);
    }

    @Override
    public Term why() {
        //throw new UnsupportedOperationException(); //?
        return null;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object that) {
        return Task.equal(this, that);
    }

    @Override
    public @Nullable Appendable toString(boolean showStamp) {
        return new StringBuilder(32).append(term).append(';');
    }

    @Override
    public String toString() {
        return term + ";";
    }

    @Override
    public Term term() {
        return term;
    }

}
