package nars.subterm.util;

import nars.term.Term;
import nars.time.Tense;

import java.util.Set;
import java.util.function.BiPredicate;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;

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
            return container.containsRecursively(x);
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
        if (container.op() != CONJ)
            return false;

        Term xx = x.negIf(neg);

        if (container.impossibleSubTerm(xx))
            return false;

        if (Tense.dtSpecial(container.dt()) && !container.subterms().hasAny(CONJ)) {
            return container.contains(xx);
        } else {

            final long[] last = {-1};
            final long[] found = {-1};

            boolean xNotConj = xx.op() != CONJ;
            boolean decompParallel = xNotConj || xx.dt()!=0;
            boolean decompEternal = xNotConj || xx.dt()!=DTERNAL;

            container.eventsWhile((when, what) -> {

                if (what.equals(xx)) {

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
            }, 0, decompParallel, decompEternal, false, 0);

            return firstOrLast ? found[0] == 0 : found[0] == last[0];
        }
    }

//    private static boolean isEventSequence(Term container, Term subseq, boolean neg, boolean firstOrLast) {
//        if (neg)
//            throw new TODO(); //may not even make sense
//
//        for (Term s : subseq.subterms())
//            if (!container.containsRecursively(s))
//                return false;
//
//        int containerDT = container.dt();
//        if (containerDT ==0 || containerDT ==DTERNAL || containerDT ==XTERNAL)
//            return true; //already met requirements since the container is unordered
//
//
//        //compare the correct order and whether it appears in prefix or suffix as appropriate
////        int range = container.eventRange();
//        long elimStart = Long.MAX_VALUE, elimEnd = Long.MIN_VALUE;
//        FasterList<LongObjectPair<Term>> events = container.eventList();
//        elimNext: for (Term s : subseq.subterms()) {
//            int n = events.size();
//            int start = firstOrLast ? 0 : n-1, inc = firstOrLast ? +1 : -1;
//            int k = start;
//            for (int i = 0; i < n; i++) {
//                LongObjectPair<Term> e = events.get(k);
//                if (e.getTwo().equals(s)) {
//                    long ew = e.getOne();
//                    elimStart = Math.min(elimStart, ew);
//                    elimEnd = Math.max(elimEnd, ew);
//                    events.remove(k);
//                    continue elimNext;
//                }
//                k += inc;
//            }
//            return false; //event not found
//        }
//
//        if (events.isEmpty()) {
//            //fully eliminated
//            return false;
//        }
//
//        for (LongObjectPair<Term> remain : events) {
//            long w = remain.getOne();
//            if (firstOrLast && w < elimStart)
//                return false;//there is a prior event
//            else if (!firstOrLast && w > elimEnd)
//                return false;//there is a later event
//        }
//
//        return true;
//    }
}
