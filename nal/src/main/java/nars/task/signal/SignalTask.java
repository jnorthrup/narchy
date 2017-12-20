package nars.task.signal;


import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;


public class SignalTask extends NALTask {


    //TODO add slidingStart ability

    /**
     * because this is an input task, its hash and equality will not depend on this value so it is free to change to represent a growing duration
     */
    public long slidingEnd;

    /**
     * true passed to update the task with a new futur endpoint,
     * false parameter signals the stretching end which the table may need to do any finalization
     */
    public LongObjectProcedure<SignalTask> stretch = null;

    public SignalTask(Term t, byte punct, Truth truth, long start, long end, long stamp) {
        super(t, punct, truth, start, start, end,
                new long[]{stamp} /* TODO use an implementation which doenst need an array for this */);
        slidingEnd = end;
    }

    @Override
    public float eternalizable() {
        return 0;
        //return 0.5f;
        //return 0.1f;
    }


    @Override
    public long end() {
        return slidingEnd;
    }

    /**
     * should be called only from the stretch procedure
     */
    public void end(long now) {
        grow(now);
        stretch = Frozen;
    }

    public void grow(long now) {
        if (now <= start() || now <= slidingEnd)
            return;

        LongObjectProcedure<SignalTask> stretch = this.stretch;
        if (stretch != null) {
            stretch.value(now, this);
        }
    }


    protected static final LongObjectProcedure<SignalTask> Frozen = (l, w) -> { /* nothing */ };

}
