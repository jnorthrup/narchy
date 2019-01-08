package nars.task;

import jcog.util.ArrayUtils;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
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
    public final short[] cause() {
        return ArrayUtils.EMPTY_SHORT_ARRAY;
    }

    @Override
    public double coord(int dimension, boolean maxOrMin) {
        throw new UnsupportedOperationException();
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
    public float pri(float ignored) {
        return 0;
    }

    @Override
    public boolean delete() {
        
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
        
    }

    @Override
    public long creation() {
        return ETERNAL;
    }

    @Override
    public void setCreation(long creation) {
        //ignored
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
