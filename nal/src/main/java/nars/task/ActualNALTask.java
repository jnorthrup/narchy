package nars.task;

import jcog.math.LongInterval;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Task;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/** contains concrete references to stamp and cause */
abstract class ActualNALTask extends NALTask {
    /*@Stable*/ protected final long[] stamp;
    private /*volatile*/ short[] cause = ArrayUtil.EMPTY_SHORT_ARRAY;

    ActualNALTask(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) {
        super(term, punc, truth, start, end, creation, stamp);
        this.stamp = stamp;

        if (start!= LongInterval.ETERNAL && end-start > NAL.belief.TASK_RANGE_LIMIT)
            throw new TaskException(term, "excessive range: " + (end-start));

        if (!term.op().taskable)
            throw new TaskException(term, "invalid task term: " + term);

        if (truth == null ^ (!((punc == BELIEF) || (punc == GOAL))))
            throw new TaskException(term, "null truth");

        if ((start == LongInterval.ETERNAL && end != LongInterval.ETERNAL) ||
                (start > end) ||
                (start == LongInterval.TIMELESS) || (end == LongInterval.TIMELESS)
        ) {
            throw new RuntimeException("start=" + start + ", end=" + end + " is invalid task occurrence time");
        }

//        if (truth!=null && truth.conf() < NAL.truth.TRUTH_EPSILON)
//            throw new Truth.TruthException("evidence underflow: conf=", truth.conf());

        if (NAL.test.DEBUG_EXTRA) {
            if (!Stamp.validStamp(stamp))
                throw new TaskException(term, "invalid stamp: " + Arrays.toString(stamp));

            Task.validTaskTerm(term, punc, false);
        }

    }

    @Override
    public long[] stamp() {
        return stamp;
    }

    @Override
    public short[] why() {
        return cause;
    }

    @Override public NALTask cause(short[] cause) {
        this.cause = cause;
        return this;
    }

}
