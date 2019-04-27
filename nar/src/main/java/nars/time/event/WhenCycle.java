package nars.time.event;

import nars.NAL;
import nars.NAR;
import nars.term.Term;

import java.util.function.Consumer;

/** per-cycle event */
public class WhenCycle extends WhenPeriodic {

    public WhenCycle(Consumer<NAL<NAL<NAR>>> x) {
        super(x);
    }
    public WhenCycle(Term id, Consumer<NAL<NAL<NAR>>> x) {
        super(id, x);
    }


}