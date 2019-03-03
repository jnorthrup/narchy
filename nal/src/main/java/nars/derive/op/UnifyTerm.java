package nars.derive.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.Param;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.unify.Unification;

import java.util.Map;

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
        public final Taskify taskify;


        public NextUnifyTransform(boolean taskOrBelief, /*@NotNull*/ Term pattern, Taskify taskify) {
            super($.funcFast(UNIFY, label(taskOrBelief), pattern, taskify), pattern);
            this.taskOrBelief = taskOrBelief;
            this.taskify = taskify;
        }

        @Override
        public final boolean test(Derivation d) {

            Map<Term, Term> retransformCopy;
            if (!d.retransform.isEmpty())
                retransformCopy = Map.copyOf(d.retransform);
            else
                retransformCopy = null;

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
                    if (!taskify.test(ii.get(i)::xy, d))
                        return false;
                    else {
                        if (i < fanOut-1) {
                            //HACK
                            if (retransformCopy != null && !d.retransform.equals(retransformCopy)) {
                                d.retransform.clear();
                                d.retransform.putAll(retransformCopy);
                            }
                        }
                    }
                }

            } else if (u instanceof Unification.DeterministicUnification) {
                if (!taskify.test(((Unification.DeterministicUnification) u)::xy, d))
                    return false;
            } else if (u == Unification.Self) {
                throw new UnsupportedOperationException();
//                if (!taskify.test((z)->Null), d)
//                    return false;
            }


            //boolean unified = d.unify(pattern, taskOrBelief ? d.taskTerm : d.beliefTerm, true);

            return true;
        }

    }


    public final static PREDICATE<Derivation> preUnify = new AbstractPred<>($$("preUnify")) {
        @Override
        public boolean test(Derivation d) {
            d.clear();
            d.retransform.clear();
            d.forEachMatch = null;
            return true;
        }
    };
}
