package nars.task;

import jcog.math.LongInterval;
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

    ActualNALTask(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) {
        super(term, punc, truth, start, end, creation, stamp);
        this.stamp = stamp;

        if (start!= LongInterval.ETERNAL && end-start > NAL.belief.TASK_RANGE_LIMIT)
            throw new TaskException("excessive range: " + (end-start), term);

        if (!term.op().taskable)
            throw new TaskException("invalid task term: " + term, term);

        if (truth == null ^ (!((punc == BELIEF) || (punc == GOAL))))
            throw new TaskException("null truth", term);

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
                throw new TaskException("invalid stamp: " + Arrays.toString(stamp), term);

            Task.validTaskTerm(term, punc, false);
        }

    }

    @Override
    public long[] stamp() {
        return stamp;
    }



}
