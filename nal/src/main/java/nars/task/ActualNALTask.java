package nars.task;

import jcog.util.ArrayUtils;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** contains concrete references to stamp and cause */
abstract public class ActualNALTask extends NALTask {
    /*@Stable*/ protected final long[] stamp;
    private /*volatile*/ short[] cause = ArrayUtils.EMPTY_SHORT_ARRAY;

    protected ActualNALTask(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) {
        super(term, punc, truth, start, end, creation, stamp);
        this.stamp = stamp;
    }


    @Override
    public long[] stamp() {
        return stamp;
    }

    @Override
    public short[] cause() {
        return cause;
    }

    @Override public NALTask cause(short[] cause) {
        this.cause = cause;
        return this;
    }

}
