package nars.term.util.conj;

import nars.term.Term;

import static nars.time.Tense.DTERNAL;

/** utilities for working with conjunction sequences (raw sequences, and factored sequences) */
public enum ConjSeq { ;

    public static boolean contains(Term container, Term x, boolean firstOrLast) {
        final long[] last = {-1}, found = {-1};

        int xdt = x.dt();
        container.eventsWhile((when, subEvent) -> {

            if (subEvent == container) return true; //HACK

            if (Conj.containsOrEqualsEvent(subEvent, x)) { //recurse

                if (!firstOrLast || when == 0) {
                    found[0] = when; //a later event was found

                    if (firstOrLast) {
                        assert (when == 0);
                        return false; //done
                    }
                }

            }
            last[0] = when;
            return true; //continue looking for last event
        }, 0, xdt!=0, xdt!=DTERNAL, false);

        return firstOrLast ? found[0] == 0 : found[0] == last[0];

    }
}
