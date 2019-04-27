package nars.time.event;

import nars.NAL;
import nars.NAR;
import nars.term.Term;

import java.util.function.Consumer;

public class WhenClear extends WhenPeriodic {

    public WhenClear(Consumer<NAL<NAL<NAR>>> x) {
        super(x);
    }
    public WhenClear(Consumer<NAL<NAL<NAR>>> x, Term id) {
        super(id, x);
    }
}
