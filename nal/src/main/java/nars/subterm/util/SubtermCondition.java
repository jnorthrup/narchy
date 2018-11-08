package nars.subterm.util;

import nars.term.Term;
import nars.time.Tense;

import java.util.Set;
import java.util.function.BiPredicate;

import static nars.Op.CONJ;

/**
 * tests various potential relations between a containing term and a subterm
 */
public enum SubtermCondition implements BiPredicate<Term, Term> {


    Subterm() {
        @Override
        public boolean test(Term container, Term x) {
            return container.contains(x);
        }

        public float cost() {
            return 0.4f;
        }
    },

    Recursive() {
        @Override
        public boolean test(Term container, Term x) {
            return container.containsRecursively(x, false, t -> true);
        }

        public float cost() {
            return 0.8f;
        }
    },

    Event() {
        @Override
        public final boolean test(Term container, Term x) {
            return containsEvent(container, x);
        }

        public float cost() {
            return 1f;
        }
    },
    EventFirst() {
        @Override
        public final boolean test(Term container, Term x) {
            return isEvent(container, x, false, true);
        }

        public float cost() {
            return 1.5f;
        }
    },
    EventLast() {
        @Override
        public final boolean test(Term container, Term x) {
            return isEvent(container, x, false, false);
        }

        public float cost() {
            return 1.75f;
        } //more intensive comparison than first
    },

    /**
     * conj containment of another conj (events) or event
     */
    Events() {
        @Override
        public boolean test(Term container, Term xx) {
            if (container.op() != CONJ || container.volume() <= xx.volume() || !Term.commonStructure(container, xx))
                return false;

            boolean simpleEvent = xx.op() != CONJ;
            if (simpleEvent) {
                if (Tense.dtSpecial(container.dt())) { //simple case
                    return container.contains(xx);
                } else {
                    return !container.eventsWhile((when, what) -> !what.equals(xx),
                            0, true, true, true, 0);
                }
            } else {
                Set<Term> xxe = xx.eventSet();
                container.eventsWhile((when, what) ->
                                !xxe.remove(what) || !xxe.isEmpty(),
                        0, true, true, true, 0);
                return xxe.isEmpty();
            }
        }

        public float cost() {
            return 2f;
        }
    };

    abstract public float cost();

    public final boolean test(Term container, Term contentP, boolean testNegAlso) {
        return test(container, contentP) ||
                (testNegAlso && test(container, contentP.neg()));
    }

    static boolean containsEvent(Term container, Term x) {
        if (container.op() == CONJ && !container.impossibleSubTerm(x)) {
            return !container.eventsWhile((when, what) -> !what.equals(x),
                    0, true, true, true, 0);

        }
        return false;
    }

    static boolean isEvent(Term container, Term x, boolean neg, boolean firstOrLast) {
        if (container.op() == CONJ && !container.impossibleSubTerm(x)) {

            Term xx = x.negIf(neg);

            final int[] count = {0}, found = {-1};

            container.eventsWhile((when, what) -> {

                if (what.equals(xx) || (what.op() == CONJ /* a commutive && that wasnt decomposed */ && what.contains(xx))) {

                    if (!firstOrLast || count[0] == 0) {
                        found[0] = count[0]; //a later event was found

                        if (firstOrLast) {
                            assert( count[0] == 0 );
                            return false; //done
                        }
                    }

                }
                count[0]++;
                return true; //continue looking for last event
            }, 0, false, false, false, 0);

            return firstOrLast ? found[0] == 0 : found[0] == count[0] - 1;
        }
        return false;
    }
}
