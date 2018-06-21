package nars.task.signal;


import nars.NAR;
import nars.task.ITask;
import nars.task.UnevaluatedTask;
import nars.term.Term;
import nars.truth.Truth;


public class SignalTask extends UnevaluatedTask {

    public SignalTask(Term t, byte punct, Truth truth, long creation, long start, long end, long[] stamp) {
        super(t, punct, truth, creation, start, end,
                stamp /* TODO use an implementation which doenst need an array for this */);
    }

    public SignalTask(Term t, byte punct, Truth truth, long creation, long start, long end, long stamp) {
        this(t, punct, truth, creation, start, end,new long[]{stamp});
    }

    public SignalTask(Term t, byte punct, Truth truth, long start, long end, long stamp) {
        this(t, punct, truth, start, start, end, stamp);
    }


    @Override
    public final boolean isInput() {
        return true;
    }

    @Override
    public final boolean isEternal() {
        return false;
    }

    @Override
    public final boolean isCyclic() {
        return false;
    }

    @Override
    public ITask next(NAR n) {
        return inputStrategy(this, n);
    }
}
