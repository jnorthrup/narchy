package nars.task;

import jcog.math.LongInterval;
import nars.NAL;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class EternalTask extends NALTask {

	public EternalTask(Term term, byte punc, @Nullable Truth truth, NAL n) {
        this(term, punc, truth, n.time.now(), n.evidence());
    }

    public EternalTask(Term term, byte punc, @Nullable Truth truth, long creation, long[] stamp) {
		super(term, punc, truth, LongInterval.ETERNAL, LongInterval.ETERNAL, creation, stamp);
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
        return LongInterval.ETERNAL;
    }

    @Override
    public final long end() {
        return LongInterval.ETERNAL;
    }

}
