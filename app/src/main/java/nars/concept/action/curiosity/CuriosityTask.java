package nars.concept.action.curiosity;

import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;

import static nars.Op.GOAL;

public class CuriosityTask extends SignalTask {
    public CuriosityTask(Term term, Truth goal, long now, long pStart, long pEnd, long[] evi) {
        super(term, GOAL, goal, now, pStart, pEnd, evi);
    }
}
