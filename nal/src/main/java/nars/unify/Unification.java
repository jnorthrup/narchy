package nars.unify;

import nars.term.Term;

import java.util.List;

/**
 * immutable and memoizable unification result (map of variables to terms) useful for substitution
 */
abstract public class Unification {

    abstract public Iterable<Term> apply(Term x);
    abstract public int forkCount();

    /**
     * indicates unsuccessful unification attempt.
     * TODO distinguish between deterministically impossible and those which stopped before exhausting permutations
     */
    static final Unification Null = new Unification() {
        @Override
        public Iterable<Term> apply(Term x) {
            return List.of();
        }

        @Override
        public int forkCount() {
            return 0;
        }
    };

    /**
     * does this happen in any cases besides .equals, ex: conj seq
     */
    public static final DeterministicUnification Self = new DeterministicUnification() {

        @Override
        protected boolean equals(DeterministicUnification obj) {
            return this == obj;
        }

        @Override
        void apply(Unify y) {

        }

        @Override
        public Term xy(Term x) {
            return x;
        }
    };



}
