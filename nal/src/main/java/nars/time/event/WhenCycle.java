package nars.time.event;

import nars.NAR;
import nars.term.Term;

import java.util.function.Consumer;

/** per-cycle event */
public class WhenCycle extends WhenPeriodic {

    public WhenCycle(Consumer<NAR> x) {
        super(x);
    }
    public WhenCycle(Term id, Consumer<NAR> x) {
        super(id, x);
    }


}