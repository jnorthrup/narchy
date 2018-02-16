package nars.task.signal;


import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;


public class SignalTask extends NALTask {

    public SignalTask(Term t, byte punct, Truth truth, long creation, long start, long end, long stamp) {
        super(t, punct, truth, creation, start, end,
                new long[]{stamp} /* TODO use an implementation which doenst need an array for this */);
    }
    public SignalTask(Term t, byte punct, Truth truth, long start, long end, long stamp) {
        this(t, punct, truth, start, start, end, stamp);
    }

    @Override
    public float eternalizability() {
        //return punc == GOAL ? 0 : 1f; //dont eternalize goal
        return 1f;
        //return 0.5f;
        //return 0.1f;
    }




}
