package nars.time.event;

import jcog.event.ConsumerAdapter;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.term.Term;

import java.util.function.Consumer;

class WhenPeriodic extends WhenInternal {

    protected final Consumer<NAL<NAL<NAR>>> run;
    protected final Term id;

    WhenPeriodic(Term id, Consumer<NAL<NAL<NAR>>> x) {
        this.run = x;
        this.id = id;
    }

    WhenPeriodic(Consumer<NAL<NAL<NAR>>> x) {
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
