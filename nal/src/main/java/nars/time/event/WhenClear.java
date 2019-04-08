package nars.time.event;

import nars.NAR;
import nars.term.Term;

import java.util.function.Consumer;

public class WhenClear extends WhenPeriodic {

    public WhenClear(Consumer<NAR> x) {
        super(x);
    }
    public WhenClear(Consumer<NAR> x, Term id) {
        super(id, x);
    }
}
