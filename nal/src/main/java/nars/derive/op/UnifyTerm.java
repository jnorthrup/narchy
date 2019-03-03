package nars.derive.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.Param;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.util.transform.AbstractTermTransform;
import nars.unify.Unification;

import java.util.function.Function;

import static nars.$.$$;
import static nars.Param.TermutatorFanOut;

/**
 * Created by me on 5/26/16.
 */
abstract public class UnifyTerm extends AbstractPred<Derivation> {


    public final Term pattern;

    UnifyTerm(Term id, Term pattern) {
        super(id);
        this.pattern = pattern;
    }


    private static Atomic label(int subterm) {
        return (subterm == 0 ? Derivation.TaskTerm : Derivation.BeliefTerm);
    }

    public static Atomic label(boolean taskOrBelief) {
        return (taskOrBelief ? Derivation.TaskTerm : Derivation.BeliefTerm);
    }

    protected static final Atomic UNIFY = $.the("unify");

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
        private final boolean taskOrBelief;


        public NextUnify(boolean taskOrBelief, Term pattern) {
            super($.funcFast(UNIFY, label(taskOrBelief), pattern), pattern);
            this.taskOrBelief = taskOrBelief;
        }

        @Override
        public final boolean test(Derivation d) {


            return d.unify(pattern, taskOrBelief ? d.taskTerm : d.beliefTerm, false);
        }

    }

    /**
     * returns false if the deriver is noticed to have depleted TTL,
     * thus interrupting all further work being done by it.
     */
    public static final class NextUnifyTransform extends UnifyTerm {

        /**
         * which premise component to match
         */
        public final boolean taskOrBelief;
        public final Taskify each;


        public NextUnifyTransform(boolean taskOrBelief, /*@NotNull*/ Term pattern, Taskify each) {
            super($.funcFast(UNIFY, label(taskOrBelief), pattern, each), pattern);
            this.taskOrBelief = taskOrBelief;
            this.each = each;
        }

        @Override
        public final boolean test(Derivation d) {

//            d.forEachMatch = each;
            d.forEachMatch = (x) -> true; //HACK

            Unification u = d.unification(pattern, taskOrBelief ? d.taskTerm : d.beliefTerm,
                    TermutatorFanOut,
                    Param.TermutatorSearchTTL);

            if (u instanceof Unification.PermutingUnification) {

                FasterList<Unification.DeterministicUnification> ii =
                        ((Unification.PermutingUnification) u).fork.list.clone();
                ii.shuffleThis(d.random);

                int fanOut = Math.min(ii.size(), TermutatorFanOut);
                for (int i = 0; i < fanOut; i++) {
                    if (!permute(d, ii.get(i)::xy))
                        return false;
                }

            } else if (u instanceof Unification.DeterministicUnification) {
                if (!permute(d, ((Unification.DeterministicUnification) u)::xy))
                    return false;
            } else if (u == Unification.Self) {
                if (!permute(d, (z)->null))
                    return false;
            }


            //boolean unified = d.unify(pattern, taskOrBelief ? d.taskTerm : d.beliefTerm, true);

            return true;
        }

        private boolean permute(Derivation d, Function<Variable,Term> xy) {
            d.transform.xy = xy;
            d.concTerm = null;
            d.concOcc = null;
            d.retransform.clear();

            Term y = AbstractTermTransform.applyBest(each.termify.pattern, d.transform);
            if (y.unneg().op().taskable)
                if (!each.test(y, d))
                    return false;

            return true;
        }
    }


    public final static PREDICATE<Derivation> preUnify = new AbstractPred<>($$("preUnify")) {
        @Override
        public boolean test(Derivation d) {
            d.clear();

            d.forEachMatch = null;
            return true;
        }
    };
}
