package nars.subterm.util;

import nars.term.Term;

import java.util.function.BiPredicate;

import static nars.Op.CONJ;

/**
 * tests various potential relations between a containing term and a subterm
 */
public enum Contains implements BiPredicate<Term, Term> {


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
        public boolean test(Term container, Term x) {
            if (container.op() != CONJ)
                return false;
            //throw new RuntimeException("this possibility should have been filtered");



            //simple subterm test, which catches compound sub-sequences that the event iteration is too granular for
            if (container.contains(x))
                return true;

            final boolean[] found = {false};

            container.eventsWhile((when, what) -> {
                if (x.equals(what.root())) {
                    found[0] = true;
                    return false;
                }
                return true;
            }, 0, true, true, true, 0);

            return found[0];

        }

        public float cost() {
            return 2f;
        }
    };

    abstract public float cost();
}
