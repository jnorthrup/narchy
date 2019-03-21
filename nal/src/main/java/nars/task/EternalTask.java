package nars.task;

import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class EternalTask extends ActualNALTask {

    public EternalTask(Term term, byte punc, @Nullable Truth truth, long creation, long[] stamp) {
        super(term, punc, truth, creation, ETERNAL, ETERNAL, stamp);
    }

    @Override
    public final boolean intersects(long rangeStart, long rangeEnd) {
        return true;
    }

    @Override
    public final boolean contains(long rangeStart, long rangeEnd) {
        return rangeStart!=ETERNAL;
    }

    @Override
    public final long start() {
        return ETERNAL;
    }

    @Override
    public final long end() {
        return ETERNAL;
    }

    @Override
    public final boolean isEternal() {
        return true;
    }

    @Override public final double evi(long when, int dur) {
        return truth().evi();
    }

    public final long minTimeTo(long when) {
        return 0;
    }

    @Override
    public final long minTimeTo(long a, long b) {
        return 0;
    }

    @Override
    public final long maxTimeTo(long x) {
        return 0;
    }

}
