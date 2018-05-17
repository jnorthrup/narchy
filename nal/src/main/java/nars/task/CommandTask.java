package nars.task;

import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import static nars.Op.COMMAND;

/** base task suitable only for command tasks.
 * no truth, no stamp, no creation, no occurrence, no cause.  hardcoded ';' puntuation
 */
public class CommandTask implements Task {
    public final Term term;
    private final int hash;

    public CommandTask(Term term) {
        this.hash = Task.hash(
                this.term = term,
                null,
                COMMAND,
                TIMELESS,
                TIMELESS,
                ArrayUtils.EMPTY_LONG_ARRAY);
    }

    @Override
    public short[] cause() {
        return null;
    }

    @Override
    public float coordF(boolean maxOrMin, int dimension) {
        return ETERNAL;
    }

    @Override
    public byte punc() {
        return COMMAND;
    }

    @Override
    public float freq(long start, long end) {
        return Float.NaN;
    }

    @Override
    public float priSet(float ignored) {
        return 0;
    }

    @Override
    public boolean delete() {
        //ignored
        return false;
    }

    @Override
    public float pri() {
        return 0;
    }

    @Override
    public Term term() {
        return term;
    }

    @Override
    public boolean isCyclic() {
        return false;
    }

    @Override
    public void setCyclic(boolean b) {
        //ignored
    }

    @Override
    public long creation() {
        return ETERNAL;
    }

    @Override
    public long start() {
        return ETERNAL;
    }

    @Override
    public long end() {
        return ETERNAL;
    }

    @Override
    public long[] stamp() {
        return ArrayUtils.EMPTY_LONG_ARRAY;
    }

    @Override
    public @Nullable Truth truth() {
        return null;
    }
}
