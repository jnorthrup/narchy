package nars.time.event;

import nars.$;
import nars.term.Term;
import nars.term.atom.Atomic;

/** internal, system event */
public abstract class WhenInternal {

    public abstract Term term();

    public Term category() {
        return Atomic.the(getClass().getSimpleName());
    }

    @Override
    public String toString() {
        return $.INSTANCE.inh(term(), category()).toString();
    }

}
