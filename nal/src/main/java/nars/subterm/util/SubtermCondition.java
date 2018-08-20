package nars.subterm.util;

import nars.term.Term;

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
            if (container.op() != CONJ)
                return false;

            if (testNegAlso)
                if (test(container, x.neg()))
                    return true;

            return test(container, x);

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

        @Override
        public final boolean test(Term container, Term x) {

            
            if (container.contains(x))
                return true;


            return container.subterms().hasAny(CONJ) && !container.eventsWhile((when, what) ->
                    !x.equals(what),
        0, true, true, true, 0);

        }

        public float cost() {
            return 2f;
        }
    },

    /** conj containment of another conj (events) or event */
    Events() {

        @Override
        public boolean test(Term container, Term xx) {
            if (container.op() != CONJ || container.volume() <= xx.volume())
                return false;

            
            final boolean[] result = {true};
            xx.eventsWhile((when, x) -> {

                if (container.impossibleSubTerm(x)) {
                    result[0] = false;
                    return false;
                }

                if (container.contains(x))
                    return true; 

                
                if (!(container.subterms().hasAny(CONJ) && !container.eventsWhile((when2, what) ->
                                !x.equals(what),
                        0, true, true, true, 0))) {
                    result[0] = false;
                    return false;
                } else {
                    return true; 
                }

            }, 0, true, true, true, 0);

            return result[0];
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
