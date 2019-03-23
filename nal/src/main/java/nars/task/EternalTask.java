package nars.task;

import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class EternalTask extends ActualNALTask {

    public EternalTask(Term term, byte punc, @Nullable Truth truth, long creation, long[] stamp) {
        super(term, punc, truth, creation, ETERNAL, ETERNAL, stamp);
    }

//    @Override
//    public final boolean intersects(long rangeStart, long rangeEnd) {
//        return true;
//    }
//
//    @Override
//    public final boolean contains(long rangeStart, long rangeEnd) {
//        return rangeStart!=ETERNAL;
//    }

    @Override
    public final long start() {
        return ETERNAL;
    }

    @Override
    public final long end() {
        return ETERNAL;
    }

}
