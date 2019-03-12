package nars.time.event;

import nars.NAR;
import nars.term.Term;

import java.util.function.Consumer;

public class AtClear extends NAREvent {

    public AtClear(Consumer<NAR> x) {
        super(x);
    }
    public AtClear(Consumer<NAR> x, Term id) {
        super(id, x);
    }
}
