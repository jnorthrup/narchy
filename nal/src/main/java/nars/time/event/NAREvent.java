package nars.time.event;

import jcog.event.ConsumerAdapter;
import nars.$;
import nars.NAR;
import nars.term.Term;

import java.util.function.Consumer;

abstract public class NAREvent extends InternalEvent {

    protected final Consumer<NAR> run;
    protected final Term id;

    public NAREvent(Term id, Consumer<NAR> x) {
        this.run = x;
        this.id = id;
    }

    public NAREvent(Consumer<NAR> x) {
        this.run = x;

        Object y;
        if (x instanceof ConsumerAdapter)
            y = ((ConsumerAdapter)x).r;
        else
            y = x;

        this.id = $.identity(y);
    }

    @Override
    public Term term() {
        return id;
    }
}
