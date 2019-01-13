package nars.term.util.conj;

import nars.term.Term;
import org.eclipse.collections.api.iterator.LongIterator;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

public interface ConjBuilder {
    boolean add(long at, Term x);
    boolean remove(long at, Term t);
    boolean removeAll(Term term);

    void negateEvents();

    Term term();

    default boolean addAuto(Term t) {
        return add(t.dt() == DTERNAL ? ETERNAL : 0, t);
    }

    default long shift() {
        long min = Long.MAX_VALUE;
        LongIterator ii = eventOccIterator();
        while (ii.hasNext()) {
            long t = ii.next();
            if (t != ETERNAL) {
                if (t < min)
                    min = t;
            }
        }
        return min == Long.MAX_VALUE ? 0 : min;
    }

    LongIterator eventOccIterator();

}
