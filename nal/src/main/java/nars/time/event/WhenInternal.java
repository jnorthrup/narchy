package nars.time.event;

import nars.$;
import nars.term.Term;
import nars.term.atom.Atomic;

/** internal, system event */
abstract public class WhenInternal {

    abstract public Term term();

    public Term category() {
        return Atomic.the(getClass().getSimpleName());
    }

    @Override
    public String toString() {
        return $.inh(term(), category()).toString();
    }

}
