package nars.term.util.conj;

import nars.term.Term;
import nars.term.util.TermException;
import nars.term.util.builder.TermConstructor;

import static nars.Op.CONJ;
import static nars.time.Tense.*;

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

    /** TODO TermBuilder */
    public static Term sequence(TermConstructor B, int dt, Term[] u) {
        if (u.length != 2)
            throw new TermException("temporal conjunction with n!=2 subterms", CONJ, dt, u);

        return (dt >= 0) ?
                sequence(u[0], 0, u[1], +dt + u[0].eventRange(), B) :
                sequence(u[1], 0, u[0], -dt + u[1].eventRange(), B);
    }

    /** TODO TermBuilder */
    static public Term sequence(Term a, long aStart, Term b, long bStart, TermConstructor B) {

        if (aStart == ETERNAL) {         assert(bStart == aStart); return CONJ.the(DTERNAL, a, b);
        } else if (aStart == TIMELESS) { assert(bStart == aStart); return CONJ.the(XTERNAL, a, b);
        } else if (aStart == bStart) {                             return CONJ.the(0, a, b);
        } else {
            assert (bStart != ETERNAL && bStart != TIMELESS);

            ConjBuilder c = new Conj();
            if (c.add(aStart, a))
                c.add(bStart, b);
            return c.term();
        }
    }}
