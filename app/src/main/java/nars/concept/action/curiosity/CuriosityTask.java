package nars.concept.action.curiosity;

import nars.NAR;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;

import static nars.Op.GOAL;

public class CuriosityTask extends SignalTask {
    public CuriosityTask(Term term, Truth goal, NAR n, long pStart, long pEnd) {
        super(term, GOAL, goal, n.time(), pStart, pEnd, n.evidence());
    }
}
