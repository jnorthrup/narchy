package nars.task.util.signal;


import nars.task.TemporalTask;
import nars.task.UnevaluatedTask;
import nars.term.Term;
import nars.truth.Truth;


public class SignalTask extends TemporalTask implements UnevaluatedTask  {

    public SignalTask(Term t, byte punct, Truth truth, long creation, long start, long end, long[] stamp) {
        super(t, punct, truth, creation, start, end,
                stamp /* TODO use an implementation which doenst need an array for this */);
        assert(start!=ETERNAL && end!=ETERNAL);
    }

    public SignalTask(Term t, byte punct, Truth truth, long start, long end, long stamp) {
        this(t, punct, truth, start, start, end, new long[] { stamp });
    }


}
