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
            return container.containsRoot(x);
        }

        public float cost() {
            return 0.4f;
        }
    },

    Recursive() {
        @Override
        public boolean test(Term container, Term x) {
            return container.containsRecursively(x, true, t -> true);
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

            Term xr = x.root();

            //simple subterm test, which catches compound sub-sequences that the event iteration is too granular for
            if (container.containsRoot(xr))
                return true;

            final boolean[] found = {false};

            container.eventsWhile((when, what) -> {
                if (xr.equals(what.root())) {
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
