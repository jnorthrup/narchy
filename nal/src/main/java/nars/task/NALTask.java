package nars.task;

import jcog.math.LongInterval;
import nars.NAL;
import nars.Task;
import nars.task.util.TaskException;
import nars.term.Neg;
import nars.term.Term;
import nars.time.When;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * generic immutable Task implementation,
 * with mutable cause[] and initially empty meta table
 */
public abstract class NALTask extends AbstractTask {

    /*@Stable*/ protected final long[] stamp;

    protected final Term term;
    private final Truth truth;
    protected final byte punc;
    protected final int hash;

    public static Task the(Term c, byte punct, Truth tr, When<? extends NAL> when) {
        return the(c, punct, tr, when.x.time.now(), when.start, when.end, new long[]{when.x.time.nextStamp()});
    }

    public static Task the(Term c, byte punct, Truth tr, When<? extends NAL> when, long[] evidence) {
        return the(c, punct, tr, when.x.time.now(), when.start, when.end, evidence);
    }

    public static NALTask the(Term c, byte punct, Truth tr, long creation, long start, long end, long[] evidence) {

        if (c instanceof Neg) {
            if (tr != null)
                tr = tr.neg();

            c = c.unneg();
        }

        return start == ETERNAL ?
            new EternalTask(c, punct, tr, creation, evidence) :
            new TemporalTask(c, punct, tr, creation, start, end, evidence);
    }

    protected NALTask(Term term, byte punc, @Nullable Truth truth, long start, long end, long creation, long[] stamp) {
        super();

        if (start != LongInterval.ETERNAL && end - start > NAL.belief.TASK_RANGE_LIMIT)
            throw new TaskException("excessive range: " + (end - start), term);

        if (!term.op().taskable)
            throw new TaskException("invalid task term: " + term, term);

        if (truth == null ^ (!(((int) punc == (int) BELIEF) || ((int) punc == (int) GOAL))))
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

        this.stamp = stamp;
        this.term = term;
        this.truth = truth;
        this.punc = punc;
        this.creation = creation;
        this.hash = hashCalculate(start, end, stamp); //must be last
    }

    protected int hashCalculate(long start, long end, long[] stamp) {
        return Task.hash(
                term,
                truth,
                punc,
                start, end, stamp);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final long[] stamp() {
        return stamp;
    }

    @Override
    public boolean equals(Object that) {
        return Task.equal(this, that);
    }


    @Override
    public final @Nullable Truth truth() {
        return truth;
    }

    @Override
    public byte punc() {
        return punc;
    }

    @Override
    public long creation() {
        return creation;
    }

    @Override
    public Term term() {
        return term;
    }

    @Override
    public String toString() {
        return appendTo(null).toString();
    }

}
