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
        public boolean test(Term container, Term x, boolean testNegAlso) {
            return container.op()==CONJ && super.test(container, x, testNegAlso);

//            if (!testNegAlso) {
//                return test(container, x);
//            } else {
//                Subterms subContainer = container.subterms();
//                if (subContainer.contains(x) || subContainer.containsNeg(x))
//                    return true;
//
//
//                return subContainer.hasAny(CONJ) && !container.eventsWhile((when, what) ->
//                            !(x.equals(what) || x.equalsNeg(what)),
//                    0, true, true, true, 0);
//            }
        }

        private boolean containsEvent(Term container, Term x) {

            if (!container.impossibleSubTerm(x)) {

                for (Term c : container.subterms()) {
                    if (c.equals(x)) return true;
                    if (c.op()==CONJ && containsEvent(c, x))
                        return true;
                }

            }
            return false;
        }

        @Override
        public final boolean test(Term container, Term x) {

            return containsEvent(container, x);

        }

        public float cost() {
            return 1f;
        }
    },

    /** conj containment of another conj (events) or event */
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
    }
    ;

    abstract public float cost();

    public boolean test(Term container, Term contentP, boolean testNegAlso) {
        return test( container, contentP) ||
                (testNegAlso && test( container,  contentP.neg()));
    }

}
