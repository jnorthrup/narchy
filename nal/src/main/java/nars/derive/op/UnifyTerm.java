package nars.derive.op;

import nars.$;
import nars.Param;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;

/**
 * Created by me on 5/26/16.
 */
abstract public class UnifyTerm extends AbstractPred<Derivation> {

    public final Term pattern;

    UnifyTerm(Term id, Term pattern) {
        super(id);
        this.pattern = pattern;
    }


    public static Atomic label(int subterm) {
        return (subterm == 0 ? Derivation.Task : Derivation.Belief);
    }

    /**
     * this will be called prior to UnifySubtermThenConclude.
     * so as part of an And condition, it is legitimate for this
     * to return false and interrupt the procedure when unification fails
     * in the first stage.
     */
    public static final class NextUnify extends UnifyTerm {

        /**
         * which premise component, 0 (task) or 1 (belief)
         */
        private final int subterm;

        public NextUnify(int subterm, Term pattern) {
            super($.func("unify", UnifyTerm.label(subterm), pattern), pattern);
            this.subterm = subterm;
        }

        @Override
        public final boolean test(Derivation d) {
            if (d.use(Param.TTL_UNIFY)) {
                return d.unify(pattern, subterm == 0 ? d.taskTerm : d.beliefTerm, false);
            }
            return false;
        }

    }

    /**
     * returns false if the deriver is noticed to have depleted TTL,
     * thus interrupting all further work being done by it.
     */
    public static final class NextUnifyTransform extends UnifyTerm {

        /**
         * which premise component, 0 (task) or 1 (belief)
         */
        public final int subterm;
        public final PREDICATE<Derivation> eachMatch;

        private static final Atomic UNIFY = Atomic.the("unify");

        public NextUnifyTransform(int subterm, /*@NotNull*/ Term pattern, /*@NotNull*/ PREDICATE<Derivation> eachMatch) {
            super($.funcFast(UNIFY, label(subterm), pattern, eachMatch), pattern);
            this.subterm = subterm;
            this.eachMatch = eachMatch;
        }

        @Override
        public final boolean test(Derivation d) {
            d.forEachMatch = eachMatch;

            d.unify(pattern, subterm == 0 ? d.taskTerm : d.beliefTerm, true);

            d.forEachMatch = null;

            return d.use(Param.TTL_UNIFY);
        }
    }
}
