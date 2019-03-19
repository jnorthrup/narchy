package nars.task;

import nars.Param;
import nars.Task;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


/** generic NAL Task with stored start,end time */
public class GenericNALTask extends ActualNALTask {

    private final long start, end;

    protected GenericNALTask(Task copied, Term newContent, @Nullable Truth newTruth) throws TaskException {
        this(newContent, copied.punc(), newTruth, copied.creation(), copied.start(), copied.end(), copied.stamp());
    }

    protected GenericNALTask(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) throws TaskException {
        super(term, punc, truth, creation, start, end, stamp);

        if (start!=ETERNAL && end-start > Param.TASK_RANGE_LIMIT)
            throw new TaskException(term, "excessive range: " + (end-start));

        if (!term.op().taskable)
            throw new TaskException(term, "invalid task term: " + term);

        if (truth == null ^ (!((punc == BELIEF) || (punc == GOAL))))
            throw new TaskException(term, "null truth");

        if ((start == ETERNAL && end != ETERNAL) ||
                (start > end) ||
                (start == TIMELESS) || (end == TIMELESS)
        ) {
            throw new RuntimeException("start=" + start + ", end=" + end + " is invalid task occurrence time");
        }

//        if (truth!=null && truth.conf() < Param.TRUTH_EPSILON)
//            throw new Truth.TruthException("evidence underflow: conf=", truth.conf());

        if (Param.DEBUG_EXTRA) {
            if (!Stamp.validStamp(stamp))
                throw new TaskException(term, "invalid stamp: " + Arrays.toString(stamp));

            Task.validTaskTerm(term, punc, false);
        }


        this.start = start;
        this.end = end;

    }


    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }


}
