package nars.time.event;

import nars.NAR;
import nars.control.Part;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class AtCycle extends NAREvent {

    public AtCycle(Consumer<NAR> x) {
        super(x);
    }
    public AtCycle(Term id, Consumer<NAR> x) {
        super(id, x);
    }


}