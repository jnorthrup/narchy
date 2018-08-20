package nars.unify.constraint;

import nars.$;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.Terms;
import nars.term.control.PREDICATE;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;

/** tests a relation between two terms which may be involved (and prevened) from unifying */
abstract public class RelationConstraint extends MatchConstraint {


    protected final Term y, yUnneg;
    protected final boolean yNeg;

    protected RelationConstraint(Term x, Term y, Term id) {
        super(x, id);
        this.y = y;
        this.yUnneg = y.unneg();
        this.yNeg = y.op()==NEG;
    }

    protected RelationConstraint(Term x, Term y, String func, Term... args) {
        this(x, y, $.func(func, x, args.length > 0 ? $.pFast(y, $.pFast(args)) : y));
    }

    public RelationConstraint neg() {
        return new NegRelationConstraint(this);
    }

    @Override
    public RelationConstraint negIf(boolean negate) {
        return negate ? neg() : this;
    }

    @Override
    public @Nullable PREDICATE<Derivation> preFilter(Term taskPattern, Term beliefPattern) {

        //only test one of the directions
        // because the opposite y->x will also be created so we only need one predicate filter for both
        if (x.compareTo(y) < 0)
            return null;

        byte[] xInTask = Terms.constantPath(taskPattern, x);
        byte[] xInBelief = Terms.constantPath(beliefPattern, x);
        if (xInTask!=null || xInBelief!=null) {
            byte[] yInTask = Terms.constantPath(taskPattern, y);
            byte[] yInBelief = Terms.constantPath(beliefPattern, y);
            if ((yInTask != null || yInBelief != null)) {
                return new ConstraintAsPredicate(this, xInTask, xInBelief, yInTask, yInBelief);
            }
        }

        return null;
    }

    @Override
    public final boolean invalid(Term xx, Unify f) {
        Term yy = f.transform(yUnneg);
        return yy != yUnneg
                &&
               invalid(xx, yy.negIf(yNeg));
    }

    abstract public boolean invalid(Term xx, Term yy);

    static final class NegRelationConstraint extends RelationConstraint {

        private final RelationConstraint r;

        public NegRelationConstraint(RelationConstraint r) {
            super(r.x, r.y, r.ref.neg());
            this.r = r;
        }

        @Override
        public RelationConstraint neg() {
            return r;
        }

        @Override
        public boolean invalid(Term xx, Term yy) {
            return !r.invalid(xx, yy);
        }

        @Override
        public float cost() {
            return r.cost();
        }

        @Override
        public @Nullable PREDICATE<Derivation> preFilter(Term taskPattern, Term beliefPattern) {
            PREDICATE<Derivation> p = super.preFilter(taskPattern, beliefPattern);
            return p != null ? p.neg() : null;
        }
    }
}
