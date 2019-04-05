package nars.time.event;

import nars.NAR;
import nars.term.Term;

import java.util.function.Consumer;

public class AtCycle extends NAREvent {

    public AtCycle(Consumer<NAR> x) {
        super(x);
    }
    public AtCycle(Term id, Consumer<NAR> x) {
        super(id, x);
    }


}