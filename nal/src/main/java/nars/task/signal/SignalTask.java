package nars.task.signal;


import nars.NAR;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Collection;


public class SignalTask extends NALTask {

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
    public void preProcess(NAR n, Term y, Collection<ITask> queue) {
        queue.add(inputStrategy(this)); //direct
    }
}
