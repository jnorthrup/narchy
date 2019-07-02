package nars.derive.condition;

import jcog.data.list.FasterList;
import nars.$;
import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.term.Term;
import nars.term.control.PREDICATE;
import nars.unify.constraint.ConstraintAsPredicate;
import nars.unify.constraint.RelationConstraint;
import nars.unify.constraint.UnaryConstraint;
import nars.unify.constraint.UnifyConstraint;

import java.util.Iterator;
import java.util.function.BiFunction;

abstract public class ConstraintAsPremisePredicate<U extends PreDerivation, C extends UnifyConstraint<U>> extends ConstraintAsPredicate<U,C> {

    private static final BiFunction<Term, Term, Term> TASK = (t, b) -> t;
    private static final BiFunction<Term, Term, Term> BELIEF = (t, b) -> b;

    private ConstraintAsPremisePredicate(Term p, C m, BiFunction<Term, Term, Term> extractX, BiFunction<Term, Term, Term> extractY, float v) {
        super(p, m, extractX, extractY, v);
    }

    /**
     * TODO generify a version of this allowing: U extends Unify
     * @return
     */
    public static ConstraintAsPredicate the(UnifyConstraint m, byte[] xInTask, byte[] xInBelief, byte[] yInTask, byte[] yInBelief) {

        int costPath = 0;

        final BiFunction<Term, Term, Term> extractX;
        Term extractXterm;
        if (xInTask != null) { // && (xInBelief == null || xInTask.length < xInBelief.length)) {
            assert(xInBelief==null);
            extractX = xInTask.length == 0 ? TASK : (t, b) -> t.subPath(xInTask);
            extractXterm = $.p($.p(xInTask), Derivation.Task);
            costPath += xInTask.length;
        } else {
            assert(xInTask==null);
            extractX = xInBelief.length == 0 ? BELIEF : (t, b) -> b.subPath(xInBelief);
            extractXterm = $.p($.p(xInBelief), Derivation.Belief);
            costPath += xInBelief.length;
        }


        float cost = m.cost();
        if (m instanceof RelationConstraint) {
            final BiFunction<Term, Term, Term> extractY;
            Term extractYterm;
            if (yInTask != null && (yInBelief == null || yInTask.length < yInBelief.length)) {
                extractY = yInTask.length == 0 ? TASK : (t, b) -> t.subPath(yInTask);
                extractYterm = $.p($.p(yInTask), Derivation.Task);
                costPath += yInTask.length;
            } else {
                extractY = yInBelief.length == 0 ? BELIEF : (t, b) -> b.subPath(yInBelief);
                extractYterm = $.p($.p(yInBelief), Derivation.Belief);
                costPath += yInBelief.length;
            }
            Term t = $.p(m.ref, $.p(extractXterm, extractYterm));
            return new RelationConstraintAsPremisePredicate(t, (RelationConstraint) m, extractX, extractY, cost + costPath * 0.01f);
        } else {
            Term t = $.p(m.ref, $.p(extractXterm));
            return new UnaryConstraintAsPremisePredicate(t, (UnaryConstraint) m, extractX, cost + costPath * 0.01f);
        }
    }

    public static final class UnaryConstraintAsPremisePredicate extends ConstraintAsPredicate<PreDerivation, UnaryConstraint<PreDerivation>> {

        UnaryConstraintAsPremisePredicate(Term id, UnaryConstraint<PreDerivation> m, BiFunction<Term, Term, Term> extractX, float cost) {
            super(id, m, extractX, null, cost);
        }

        @Override
        public boolean test(PreDerivation p) {
            Term x = extractX.apply(p.taskTerm, p.beliefTerm);
            if (x != null) {
                return constraint.valid(x);
            }
            return true; //<- does this happen?
        }
    }

    public static final class RelationConstraintAsPremisePredicate extends ConstraintAsPredicate<PreDerivation, RelationConstraint<PreDerivation>> {

        RelationConstraintAsPremisePredicate(Term id, RelationConstraint<PreDerivation> m, BiFunction<Term, Term, Term> extractX, BiFunction<Term, Term, Term> extractY, float cost) {
            super(id, m, extractX, extractY, cost);
        }

        @Override
        public boolean reduceIn(FasterList<PREDICATE<PreDerivation>> p) {
            boolean mod = false;

            for (Iterator<PREDICATE<PreDerivation>> iterator = p.iterator(); iterator.hasNext(); ) {
                PREDICATE pp = iterator.next();
                if (pp != this && pp instanceof RelationConstraintAsPremisePredicate) {
                    UnifyConstraint cc = ((RelationConstraintAsPremisePredicate) pp).constraint;
                    if (cc instanceof RelationConstraint) {
                        if (!constraint.remainInAndWith((RelationConstraint) cc)) {
                            iterator.remove();
                            mod = true;
                        }
                    }
                }
            }

            return mod;
        }

        @Override
        public boolean test(PreDerivation preDerivation) {
            Term t = preDerivation.taskTerm, b = preDerivation.beliefTerm;
            Term x = extractX.apply(t, b);
            if (x != null) {
                Term y = extractY.apply(t, b);
                if (y!=null)
                    return !constraint.invalid(x, y, preDerivation);
            }
            return true; //<- does this happen?
        }
    }


}
